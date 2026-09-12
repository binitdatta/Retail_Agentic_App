package com.havi.retailreplenishment.messaging;

import com.havi.retailreplenishment.config.RabbitQueues;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Audit/log sink for every delivery status transition (including the ones
 * DeliverySimulatorService generates). Real deliverable: this is the
 * natural attachment point for a live dashboard feed (e.g. push over
 * WebSocket/SSE) without touching DeliveryService itself.
 */
@Component
public class DeliveryStatusAuditListener {

    @RabbitListener(queues = RabbitQueues.DELIVERY_STATUS_UPDATED)
    public void onDeliveryStatusUpdated(Map<String, Object> event) {
        System.out.println("DeliveryStatusUpdated: shipment=" + event.get("shipmentId")
            + " order=" + event.get("replenishmentOrderId") + " status=" + event.get("statusCode"));
    }
}
