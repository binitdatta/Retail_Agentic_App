package com.havi.retailreplenishment.mapper;

import com.havi.retailreplenishment.dto.SupplierDto;
import com.havi.retailreplenishment.entity.Supplier;

public final class SupplierMapper {

    private SupplierMapper() {}

    public static SupplierDto toDto(Supplier s) {
        if (s == null) return null;
        return new SupplierDto(
            s.getSupplierId(), s.getSupplierCode(), s.getLegalName(),
            s.getAvgLeadTimeDays(), s.getReliabilityScore(), s.getActive());
    }
}
