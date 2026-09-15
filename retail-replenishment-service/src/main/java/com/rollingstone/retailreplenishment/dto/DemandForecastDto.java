package com.rollingstone.retailreplenishment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DemandForecastDto(
    Long forecastRunId,
    Long storeId,
    Long productId,
    String forecastMethod,
    Integer forecastHorizonDays,
    BigDecimal forecastedDemandQty,
    BigDecimal confidenceScore,
    LocalDateTime generatedAt
) {}
