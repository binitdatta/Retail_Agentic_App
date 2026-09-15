package com.rollingstone.retailreplenishment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ForecastRequest(
    @NotNull Long storeId,
    @NotNull Long productId,
    @Positive Integer lookbackDays,
    @Positive Integer horizonDays,
    Long generatedByAgentRunId
) {}
