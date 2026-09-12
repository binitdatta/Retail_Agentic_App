package com.havi.retailreplenishment.dto;

import java.math.BigDecimal;

public record ProductDto(
    Long productId,
    String skuCode,
    String productName,
    String categoryCode,
    String uomCode,
    BigDecimal unitCost,
    BigDecimal unitPrice,
    Boolean seasonal,
    Boolean critical,
    Integer shelfLifeDays,
    Boolean active
) {}
