package com.rollingstone.retailreplenishment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-keys.low-stock-detected}")
    private String lowStockRoutingKey;

    @Value("${app.rabbitmq.routing-keys.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${app.rabbitmq.routing-keys.delivery-status-updated}")
    private String deliveryStatusRoutingKey;

    @Value("${app.rabbitmq.routing-keys.shortage-escalated}")
    private String shortageEscalatedRoutingKey;

    @Bean
    public TopicExchange replenishmentEventsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    // One durable queue per event type, each bound by its own routing key.
    // A consumer (e.g. a supplier-integration stub, a notification service)
    // binds to whichever queue it cares about.

    @Bean
    public Queue lowStockDetectedQueue() {
        return new Queue(RabbitQueues.LOW_STOCK_DETECTED, true);
    }

    @Bean
    public Binding lowStockDetectedBinding() {
        return BindingBuilder.bind(lowStockDetectedQueue()).to(replenishmentEventsExchange()).with(lowStockRoutingKey);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(RabbitQueues.ORDER_CREATED, true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue()).to(replenishmentEventsExchange()).with(orderCreatedRoutingKey);
    }

    @Bean
    public Queue deliveryStatusUpdatedQueue() {
        return new Queue(RabbitQueues.DELIVERY_STATUS_UPDATED, true);
    }

    @Bean
    public Binding deliveryStatusUpdatedBinding() {
        return BindingBuilder.bind(deliveryStatusUpdatedQueue()).to(replenishmentEventsExchange()).with(deliveryStatusRoutingKey);
    }

    @Bean
    public Queue shortageEscalatedQueue() {
        return new Queue(RabbitQueues.SHORTAGE_ESCALATED, true);
    }

    @Bean
    public Binding shortageEscalatedBinding() {
        return BindingBuilder.bind(shortageEscalatedQueue()).to(replenishmentEventsExchange()).with(shortageEscalatedRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
