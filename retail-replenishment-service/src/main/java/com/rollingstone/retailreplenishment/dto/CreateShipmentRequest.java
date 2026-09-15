package com.rollingstone.retailreplenishment.dto;

import java.time.LocalDateTime;

public record CreateShipmentRequest(
    String carrierName,
    String trackingNumber,
    LocalDateTime estimatedArrivalAt
) {}
