package com.rollingstone.retailreplenishment.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * Thin wrapper around RabbitTemplate so services publish by intent
 * (orderCreated, shortageEscalated, ...) instead of knowing exchange/routing
 * key details.
 *
 * Every publish call here happens from inside an @Transactional service
 * method. If sent immediately, the message can reach a consumer before the
 * surrounding database transaction commits — the consumer then reads
 * stale/pre-commit state (observed: an order.created re-publish on
 * approval was picked up by SupplierIntegrationStubListener before the
 * APPROVED status had actually committed, so it read RECOMMENDED and
 * declined to transmit). To fix this at the root rather than per call
 * site, every publish is deferred to fire only after the current
 * transaction commits, via TransactionSynchronizationManager. If there is
 * no active transaction (e.g. called from a non-transactional context or a
 * test), it sends immediately.
 *
 * Failures here are logged, not thrown — a messaging outage should not
 * roll back a REST transaction that already persisted state.
 */
@Component
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String lowStockRoutingKey;
    private final String orderCreatedRoutingKey;
    private final String deliveryStatusRoutingKey;
    private final String shortageEscalatedRoutingKey;

    public EventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchangeName,
            @Value("${app.rabbitmq.routing-keys.low-stock-detected}") String lowStockRoutingKey,
            @Value("${app.rabbitmq.routing-keys.order-created}") String orderCreatedRoutingKey,
            @Value("${app.rabbitmq.routing-keys.delivery-status-updated}") String deliveryStatusRoutingKey,
            @Value("${app.rabbitmq.routing-keys.shortage-escalated}") String shortageEscalatedRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.lowStockRoutingKey = lowStockRoutingKey;
        this.orderCreatedRoutingKey = orderCreatedRoutingKey;
        this.deliveryStatusRoutingKey = deliveryStatusRoutingKey;
        this.shortageEscalatedRoutingKey = shortageEscalatedRoutingKey;
    }

    public void publishLowStockDetected(Long storeId, Long productId, String skuCode) {
        publish(lowStockRoutingKey, Map.of("storeId", storeId, "productId", productId, "skuCode", skuCode));
    }

    public void publishOrderCreated(Long orderId, String orderNumber, Long storeId, Long supplierId) {
        publish(orderCreatedRoutingKey, Map.of(
                "replenishmentOrderId", orderId, "orderNumber", orderNumber,
                "storeId", storeId, "supplierId", supplierId));
    }

    public void publishDeliveryStatusUpdated(Long shipmentId, Long orderId, String statusCode) {
        publish(deliveryStatusRoutingKey, Map.of(
                "shipmentId", shipmentId, "replenishmentOrderId", orderId, "statusCode", statusCode));
    }

    public void publishShortageEscalated(Long escalationId, Long storeId, Long productId, String severity) {
        publish(shortageEscalatedRoutingKey, Map.of(
                "escalationId", escalationId, "storeId", storeId, "productId", productId, "severity", severity));
    }

    private void publish(String routingKey, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(routingKey, payload);
                }
            });
        } else {
            doPublish(routingKey, payload);
        }
    }

    private void doPublish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
        } catch (Exception e) {
            // Intentionally swallowed: see class javadoc. A real deployment
            // would emit a metric/log alert here rather than a bare no-op.
            System.err.println("Failed to publish event to " + routingKey + ": " + e.getMessage());
        }
    }
}