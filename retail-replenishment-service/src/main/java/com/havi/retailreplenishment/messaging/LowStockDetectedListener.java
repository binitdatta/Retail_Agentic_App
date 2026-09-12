package com.havi.retailreplenishment.messaging;

import com.havi.retailreplenishment.config.RabbitQueues;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Currently just an audit log line. This queue is the hook point for a
 * future event-driven agent trigger — instead of the agent polling
 * GET /api/inventory/low-stock on a fixed interval, Spring Boot could
 * publish this event the moment a sale or count drops a SKU below its
 * reorder point, and an agent worker subscribed here would wake on demand.
 * Nothing currently publishes to this routing key from application code
 * (detection is agent-initiated via the polling GET today) — it's wired up
 * so that wiring is a one-line addition, not a new queue/exchange design.
 */
@Component
public class LowStockDetectedListener {

    @RabbitListener(queues = RabbitQueues.LOW_STOCK_DETECTED)
    public void onLowStockDetected(Map<String, Object> event) {
        System.out.println("LowStockDetected: store=" + event.get("storeId")
            + " sku=" + event.get("skuCode") + " productId=" + event.get("productId"));
    }
}
