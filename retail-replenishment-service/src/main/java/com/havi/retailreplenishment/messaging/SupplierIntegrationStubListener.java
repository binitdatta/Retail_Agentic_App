package com.havi.retailreplenishment.messaging;

import com.havi.retailreplenishment.config.RabbitQueues;
import com.havi.retailreplenishment.dto.CreateShipmentRequest;
import com.havi.retailreplenishment.dto.DeliveryStatusDto;
import com.havi.retailreplenishment.dto.UpdateOrderStatusRequest;
import com.havi.retailreplenishment.entity.DeliveryShipment;
import com.havi.retailreplenishment.entity.ReplenishmentOrder;
import com.havi.retailreplenishment.repository.DeliveryShipmentRepository;
import com.havi.retailreplenishment.repository.ReplenishmentOrderRepository;
import com.havi.retailreplenishment.service.DeliveryService;
import com.havi.retailreplenishment.service.ReplenishmentOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stands in for a real supplier EDI/API integration. Per the architecture:
 * "replenishment.order.created — published when agent creates an order,
 * consumed by a stub supplier-integration listener."
 *
 * Deliberately does NOT ship on every message: a RECOMMENDED order is still
 * awaiting SUPPLY_CHAIN_MANAGER approval, so no supplier has actually been
 * contacted yet. Only APPROVED / SENT_TO_SUPPLIER orders get transmitted
 * and shipped. When an order is later approved via
 * PATCH /api/replenishment-orders/{id}/status, ReplenishmentOrderService
 * re-publishes to this same queue so the stub gets a second chance to act.
 */
@Component
public class SupplierIntegrationStubListener {

    private final ReplenishmentOrderRepository orderRepository;
    private final ReplenishmentOrderService orderService;
    private final DeliveryService deliveryService;
    private final DeliveryShipmentRepository shipmentRepository;
    private final DeliverySimulatorService simulatorService;

    public SupplierIntegrationStubListener(
            ReplenishmentOrderRepository orderRepository,
            ReplenishmentOrderService orderService,
            DeliveryService deliveryService,
            DeliveryShipmentRepository shipmentRepository,
            DeliverySimulatorService simulatorService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.deliveryService = deliveryService;
        this.shipmentRepository = shipmentRepository;
        this.simulatorService = simulatorService;
    }

    @RabbitListener(queues = RabbitQueues.ORDER_CREATED)
    public void onOrderCreated(Map<String, Object> event) {
        Long orderId = asLong(event.get("replenishmentOrderId"));
        if (orderId == null) {
            System.err.println("SupplierIntegrationStub: order event missing replenishmentOrderId, ignoring: " + event);
            return;
        }

        ReplenishmentOrder order = orderRepository.findByIdWithLines(orderId).orElse(null);
        if (order == null) {
            System.err.println("SupplierIntegrationStub: order " + orderId + " no longer exists, ignoring");
            return;
        }

        String statusCode = order.getStatus().getStatusCode();
        if ("RECOMMENDED".equals(statusCode)) {
            System.out.println("SupplierIntegrationStub: order " + order.getOrderNumber()
                + " is awaiting human approval — not transmitting to " + order.getSupplier().getLegalName() + " yet");
            return;
        }

        List<DeliveryShipment> existing = shipmentRepository.findByOrderId(orderId);
        if (!existing.isEmpty()) {
            // Redelivery of the same event (e.g. after a broker reconnect) — a
            // shipment already exists, nothing further to do.
            return;
        }

        if ("APPROVED".equals(statusCode)) {
            orderService.updateStatus(orderId, new UpdateOrderStatusRequest("SENT_TO_SUPPLIER", "supplier-integration-stub"));
        }

        String origin = order.getSupplier().getLegalName() + " Distribution Center";
        String destination = order.getStore().getStoreName() + " (" + order.getStore().getStoreCode() + ")";
        String carrier = order.getSupplier().getLegalName() + " Logistics";
        String trackingNumber = "TRK-" + order.getOrderNumber().replace("RO-", "") + "-"
            + ThreadLocalRandom.current().nextInt(1000, 9999);

        DeliveryStatusDto shipment = deliveryService.createShipment(
            orderId, new CreateShipmentRequest(carrier, trackingNumber, LocalDateTime.now().plusMinutes(6)));

        System.out.println("SupplierIntegrationStub: order " + order.getOrderNumber() + " transmitted to "
            + order.getSupplier().getLegalName() + " — shipment " + shipment.shipmentId()
            + " (" + carrier + ", tracking " + trackingNumber + ") created and handed off to the delivery simulator");

        simulatorService.scheduleProgression(shipment.shipmentId(), origin, destination);
    }

    private Long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
}
