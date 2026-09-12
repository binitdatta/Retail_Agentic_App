package com.havi.retailreplenishment.dto;

import java.time.LocalDateTime;

public record EscalationDto(
    Long escalationId,
    Long replenishmentOrderId,
    Long storeId,
    String storeCode,
    Long productId,
    String skuCode,
    String escalationReason,
    String severity,
    String statusCode,
    String assignedTo,
    LocalDateTime resolvedAt,
    String resolutionNotes,
    LocalDateTime createdAt
) {}
