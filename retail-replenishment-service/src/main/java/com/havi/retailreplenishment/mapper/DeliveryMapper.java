package com.havi.retailreplenishment.mapper;

import com.havi.retailreplenishment.dto.DeliveryStatusDto;
import com.havi.retailreplenishment.dto.TrackingEventDto;
import com.havi.retailreplenishment.entity.DeliveryShipment;

import java.util.Comparator;
import java.util.List;

public final class DeliveryMapper {

    private DeliveryMapper() {}

    public static DeliveryStatusDto toDto(DeliveryShipment s) {
        if (s == null) return null;
        List<TrackingEventDto> events = s.getTrackingEvents().stream()
            .sorted(Comparator.comparing(e -> e.getEventAt()))
            .map(e -> new TrackingEventDto(e.getEventCode(), e.getEventAt(), e.getEventLocation(), e.getNotes()))
            .toList();
        return new DeliveryStatusDto(
            s.getReplenishmentOrder().getReplenishmentOrderId(), s.getReplenishmentOrder().getOrderNumber(),
            s.getShipmentId(), s.getCarrierName(), s.getTrackingNumber(), s.getStatus().getStatusCode(),
            s.getShippedAt(), s.getEstimatedArrivalAt(), s.getActualArrivalAt(), events);
    }
}
