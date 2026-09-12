package com.havi.retailreplenishment.dto;

import java.math.BigDecimal;

public record OrderLineDto(
    Long orderLineId,
    Long productId,
    String skuCode,
    String productName,
    BigDecimal orderedQty,
    BigDecimal unitCost,
    BigDecimal lineTotal,
    Long forecastRunId
) {}
