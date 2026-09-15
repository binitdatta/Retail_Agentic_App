package com.rollingstone.retailreplenishment.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record UpdateShipmentStatusRequest(
    @NotBlank String statusCode,
    String eventLocation,
    String notes,
    LocalDateTime actualArrivalAt
) {}
