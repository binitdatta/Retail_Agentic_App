package com.havi.retailreplenishment.mapper;

import com.havi.retailreplenishment.dto.DemandForecastDto;
import com.havi.retailreplenishment.entity.DemandForecastRun;

public final class DemandForecastMapper {

    private DemandForecastMapper() {}

    public static DemandForecastDto toDto(DemandForecastRun f) {
        if (f == null) return null;
        return new DemandForecastDto(
            f.getForecastRunId(), f.getStore().getStoreId(), f.getProduct().getProductId(),
            f.getForecastMethod(), f.getForecastHorizonDays(), f.getForecastedDemandQty(),
            f.getConfidenceScore(), f.getGeneratedAt());
    }
}
