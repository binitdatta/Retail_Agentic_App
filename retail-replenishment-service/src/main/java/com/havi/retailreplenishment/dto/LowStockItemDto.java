package com.havi.retailreplenishment.dto;

import java.math.BigDecimal;

public record LowStockItemDto(
    Long storeInventoryId,
    Long storeId,
    String storeCode,
    Long productId,
    String skuCode,
    String productName,
    Boolean critical,
    BigDecimal onHandQty,
    BigDecimal allocatedQty,
    BigDecimal availableQty,
    BigDecimal reorderPoint,
    BigDecimal reorderQty,
    BigDecimal safetyStockQty
) {}
