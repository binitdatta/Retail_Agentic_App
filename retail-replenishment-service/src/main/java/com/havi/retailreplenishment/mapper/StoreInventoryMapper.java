package com.havi.retailreplenishment.mapper;

import com.havi.retailreplenishment.dto.LowStockItemDto;
import com.havi.retailreplenishment.entity.StoreInventory;

public final class StoreInventoryMapper {

    private StoreInventoryMapper() {}

    public static LowStockItemDto toLowStockDto(StoreInventory si) {
        if (si == null) return null;
        return new LowStockItemDto(
            si.getStoreInventoryId(),
            si.getStore().getStoreId(), si.getStore().getStoreCode(),
            si.getProduct().getProductId(), si.getProduct().getSkuCode(), si.getProduct().getProductName(),
            si.getProduct().getCritical(),
            si.getOnHandQty(), si.getAllocatedQty(), si.getAvailableQty(),
            si.getReorderPoint(), si.getReorderQty(), si.getSafetyStockQty());
    }
}
