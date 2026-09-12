package com.havi.retailreplenishment.messaging;

import com.havi.retailreplenishment.config.RabbitQueues;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stub for the "escalate shortage" stage's notification fan-out. Formats a
 * message the way a real Slack/email integration would receive it; a
 * production variant swaps the System.out.println for an actual webhook
 * call and keeps everything else — the event contract doesn't change.
 */
@Component
public class ShortageEscalationNotificationListener {

    @RabbitListener(queues = RabbitQueues.SHORTAGE_ESCALATED)
    public void onShortageEscalated(Map<String, Object> event) {
        String severity = String.valueOf(event.get("severity"));
        String urgencyPrefix = switch (severity) {
            case "CRITICAL" -> "\uD83D\uDD34 CRITICAL";
            case "HIGH" -> "\uD83D\uDFE0 HIGH";
            case "MEDIUM" -> "\uD83D\uDFE1 MEDIUM";
            default -> "\u26AA LOW";
        };
        System.out.println("[supply-chain-alerts] " + urgencyPrefix + " shortage escalation #" + event.get("escalationId")
            + " — store=" + event.get("storeId") + " product=" + event.get("productId")
            + ". Review at /api/escalations/" + event.get("escalationId"));
    }
}
