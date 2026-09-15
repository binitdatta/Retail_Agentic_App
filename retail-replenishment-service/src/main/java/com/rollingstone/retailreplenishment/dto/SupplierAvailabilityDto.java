package com.rollingstone.retailreplenishment.dto;

import java.math.BigDecimal;

public record SupplierAvailabilityDto(
    Long supplierId,
    String supplierCode,
    String legalName,
    Integer leadTimeDays,
    BigDecimal reliabilityScore,
    BigDecimal unitCost,
    BigDecimal minOrderQty,
    Boolean preferred
) {}
