package com.rollingstone.retailreplenishment.dto;

import java.math.BigDecimal;

public record SupplierDto(
    Long supplierId,
    String supplierCode,
    String legalName,
    Integer avgLeadTimeDays,
    BigDecimal reliabilityScore,
    Boolean active
) {}
