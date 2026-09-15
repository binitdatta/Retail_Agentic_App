package com.rollingstone.retailreplenishment.mapper;

import com.rollingstone.retailreplenishment.dto.SupplierDto;
import com.rollingstone.retailreplenishment.entity.Supplier;

public final class SupplierMapper {

    private SupplierMapper() {}

    public static SupplierDto toDto(Supplier s) {
        if (s == null) return null;
        return new SupplierDto(
            s.getSupplierId(), s.getSupplierCode(), s.getLegalName(),
            s.getAvgLeadTimeDays(), s.getReliabilityScore(), s.getActive());
    }
}
