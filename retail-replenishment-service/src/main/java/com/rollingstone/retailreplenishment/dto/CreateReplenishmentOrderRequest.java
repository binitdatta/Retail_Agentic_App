package com.rollingstone.retailreplenishment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateReplenishmentOrderRequest(
    @NotNull Long storeId,
    @NotNull Long supplierId,
    @NotNull String sourceType,
    Long generatedByAgentRunId,
    LocalDate requestedDeliveryDate,
    String approvedBy,
    @NotEmpty @Valid List<OrderLineRequest> lines
) {}
