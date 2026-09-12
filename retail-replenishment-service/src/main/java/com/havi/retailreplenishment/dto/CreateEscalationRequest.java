package com.havi.retailreplenishment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEscalationRequest(
    @NotNull Long storeId,
    @NotNull Long productId,
    Long replenishmentOrderId,
    @NotBlank String escalationReason,
    @NotBlank String severity,
    Long raisedByAgentRunId
) {}
