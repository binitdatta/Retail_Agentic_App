package com.havi.retailreplenishment.dto;

import java.time.LocalDateTime;

public record CreateShipmentRequest(
    String carrierName,
    String trackingNumber,
    LocalDateTime estimatedArrivalAt
) {}
