package com.havi.retailreplenishment.config;

/** Queue name constants shared between RabbitMqConfig (declares them) and the listeners (consume them). */
public final class RabbitQueues {

    private RabbitQueues() {}

    public static final String LOW_STOCK_DETECTED = "inventory.low-stock.detected.q";
    public static final String ORDER_CREATED = "replenishment.order.created.q";
    public static final String DELIVERY_STATUS_UPDATED = "delivery.status.updated.q";
    public static final String SHORTAGE_ESCALATED = "shortage.escalated.q";
}
