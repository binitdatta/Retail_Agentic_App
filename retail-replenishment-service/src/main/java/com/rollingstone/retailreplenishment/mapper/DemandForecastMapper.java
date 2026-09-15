package com.rollingstone.retailreplenishment.mapper;

import com.rollingstone.retailreplenishment.dto.DemandForecastDto;
import com.rollingstone.retailreplenishment.entity.DemandForecastRun;

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
