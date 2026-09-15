package com.rollingstone.retailreplenishment.messaging;

import com.rollingstone.retailreplenishment.dto.UpdateShipmentStatusRequest;
import com.rollingstone.retailreplenishment.service.DeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a carrier's status feed for a shipment the supplier-integration
 * stub just created, so a demo run has real delivery events to show without
 * anyone manually PATCH-ing shipment status. Timing is a fixed wall-clock
 * cadence (see application.yml: app.delivery-simulator.*), not derived from
 * the supplier's real lead_time_days — a production variant would replace
 * this whole class with an actual carrier webhook/polling integration and
 * keep DeliveryService untouched, since all of this only calls the same
 * public service methods a real integration would call.
 */
@Component
public class DeliverySimulatorService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final DeliveryService deliveryService;

    private final boolean enabled;
    private final int departedDelaySeconds;
    private final int inTransitDelaySeconds;
    private final int outForDeliveryDelaySeconds;
    private final int deliveredDelaySeconds;
    private final double delayInjectionProbability;

    public DeliverySimulatorService(
            ThreadPoolTaskScheduler deliverySimulatorTaskScheduler,
            DeliveryService deliveryService,
            @Value("${app.delivery-simulator.enabled}") boolean enabled,
            @Value("${app.delivery-simulator.departed-delay-seconds}") int departedDelaySeconds,
            @Value("${app.delivery-simulator.in-transit-delay-seconds}") int inTransitDelaySeconds,
            @Value("${app.delivery-simulator.out-for-delivery-delay-seconds}") int outForDeliveryDelaySeconds,
            @Value("${app.delivery-simulator.delivered-delay-seconds}") int deliveredDelaySeconds,
            @Value("${app.delivery-simulator.delay-injection-probability}") double delayInjectionProbability) {
        this.taskScheduler = deliverySimulatorTaskScheduler;
        this.deliveryService = deliveryService;
        this.enabled = enabled;
        this.departedDelaySeconds = departedDelaySeconds;
        this.inTransitDelaySeconds = inTransitDelaySeconds;
        this.outForDeliveryDelaySeconds = outForDeliveryDelaySeconds;
        this.deliveredDelaySeconds = deliveredDelaySeconds;
        this.delayInjectionProbability = delayInjectionProbability;
    }

    public void scheduleProgression(Long shipmentId, String originLocation, String destinationLocation) {
        if (!enabled) {
            return;
        }

        Instant now = Instant.now();
        boolean injectDelay = ThreadLocalRandom.current().nextDouble() < delayInjectionProbability;

        schedule(shipmentId, "DEPARTED", originLocation,
            "Picked up and scanned at origin facility",
            now.plusSeconds(departedDelaySeconds), null);

        schedule(shipmentId, "IN_TRANSIT", "In transit",
            "En route to destination",
            now.plusSeconds(inTransitDelaySeconds), null);

        if (injectDelay) {
            long midpoint = (inTransitDelaySeconds + outForDeliveryDelaySeconds) / 2L;
            schedule(shipmentId, "DELAYED", "In transit",
                "Carrier reported a delay; revised ETA pending",
                now.plusSeconds(midpoint), null);
        }

        schedule(shipmentId, "OUT_FOR_DELIVERY", destinationLocation,
            "On vehicle for final delivery",
            now.plusSeconds(outForDeliveryDelaySeconds), null);

        schedule(shipmentId, "DELIVERED", destinationLocation,
            "Delivered and signed for",
            now.plusSeconds(deliveredDelaySeconds), now.plusSeconds(deliveredDelaySeconds));
    }

    private void schedule(Long shipmentId, String statusCode, String location, String notes, Instant when, Instant actualArrivalAt) {
        taskScheduler.schedule(() -> applyStatus(shipmentId, statusCode, location, notes, actualArrivalAt), when);
    }

    private void applyStatus(Long shipmentId, String statusCode, String location, String notes, Instant actualArrivalAt) {
        try {
            UpdateShipmentStatusRequest request = new UpdateShipmentStatusRequest(
                statusCode, location, notes,
                actualArrivalAt != null ? java.time.LocalDateTime.ofInstant(actualArrivalAt, java.time.ZoneId.systemDefault()) : null);
            deliveryService.updateShipmentStatus(shipmentId, request);
        } catch (Exception e) {
            // A shipment could be legitimately gone or already terminal by the
            // time a delayed task fires (e.g. status corrected by a human in
            // between) — log and move on rather than crash a background thread.
            System.err.println("Delivery simulator: failed to apply " + statusCode
                + " to shipment " + shipmentId + ": " + e.getMessage());
        }
    }
}
