package com.rollingstone.retailreplenishment.mapper;

import com.rollingstone.retailreplenishment.dto.StoreDto;
import com.rollingstone.retailreplenishment.entity.Store;

public final class StoreMapper {

    private StoreMapper() {}

    public static StoreDto toDto(Store s) {
        if (s == null) return null;
        return new StoreDto(
            s.getStoreId(), s.getStoreCode(), s.getStoreName(), s.getRegion(),
            s.getCity(), s.getStateProvince(), s.getActive());
    }
}
