package com.havi.retailreplenishment.mapper;

import com.havi.retailreplenishment.dto.SupplierAvailabilityDto;
import com.havi.retailreplenishment.entity.SupplierProduct;

public final class SupplierProductMapper {

    private SupplierProductMapper() {}

    public static SupplierAvailabilityDto toDto(SupplierProduct sp) {
        if (sp == null) return null;
        return new SupplierAvailabilityDto(
            sp.getSupplier().getSupplierId(), sp.getSupplier().getSupplierCode(), sp.getSupplier().getLegalName(),
            sp.getLeadTimeDays(), sp.getSupplier().getReliabilityScore(),
            sp.getUnitCost(), sp.getMinOrderQty(), sp.getPreferred());
    }
}
