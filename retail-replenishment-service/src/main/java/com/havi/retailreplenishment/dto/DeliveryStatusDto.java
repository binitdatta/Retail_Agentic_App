package com.havi.retailreplenishment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeliveryStatusDto(
    Long replenishmentOrderId,
    String orderNumber,
    Long shipmentId,
    String carrierName,
    String trackingNumber,
    String statusCode,
    LocalDateTime shippedAt,
    LocalDateTime estimatedArrivalAt,
    LocalDateTime actualArrivalAt,
    List<TrackingEventDto> events
) {}
