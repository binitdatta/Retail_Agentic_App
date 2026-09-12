package com.havi.retailreplenishment.mapper;

import com.havi.retailreplenishment.dto.ProductDto;
import com.havi.retailreplenishment.entity.Product;

public final class ProductMapper {

    private ProductMapper() {}

    public static ProductDto toDto(Product p) {
        if (p == null) return null;
        return new ProductDto(
            p.getProductId(), p.getSkuCode(), p.getProductName(),
            p.getCategory() != null ? p.getCategory().getCategoryCode() : null,
            p.getUom() != null ? p.getUom().getUomCode() : null,
            p.getUnitCost(), p.getUnitPrice(), p.getSeasonal(), p.getCritical(),
            p.getShelfLifeDays(), p.getActive());
    }
}
