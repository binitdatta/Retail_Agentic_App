package com.havi.retailreplenishment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderLineRequest(
    @NotNull Long productId,
    @NotNull @Positive BigDecimal orderedQty,
    @NotNull BigDecimal unitCost,
    Long forecastRunId
) {}
