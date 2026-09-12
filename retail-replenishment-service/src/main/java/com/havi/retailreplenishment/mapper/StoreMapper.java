package com.havi.retailreplenishment.mapper;

import com.havi.retailreplenishment.dto.StoreDto;
import com.havi.retailreplenishment.entity.Store;

public final class StoreMapper {

    private StoreMapper() {}

    public static StoreDto toDto(Store s) {
        if (s == null) return null;
        return new StoreDto(
            s.getStoreId(), s.getStoreCode(), s.getStoreName(), s.getRegion(),
            s.getCity(), s.getStateProvince(), s.getActive());
    }
}
