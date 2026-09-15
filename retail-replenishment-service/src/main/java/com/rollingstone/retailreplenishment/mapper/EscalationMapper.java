package com.rollingstone.retailreplenishment.mapper;

import com.rollingstone.retailreplenishment.dto.EscalationDto;
import com.rollingstone.retailreplenishment.entity.ShortageEscalation;

public final class EscalationMapper {

    private EscalationMapper() {}

    public static EscalationDto toDto(ShortageEscalation e) {
        if (e == null) return null;
        return new EscalationDto(
            e.getEscalationId(),
            e.getReplenishmentOrder() != null ? e.getReplenishmentOrder().getReplenishmentOrderId() : null,
            e.getStore().getStoreId(), e.getStore().getStoreCode(),
            e.getProduct().getProductId(), e.getProduct().getSkuCode(),
            e.getEscalationReason(), e.getSeverity(), e.getStatus().getStatusCode(),
            e.getAssignedTo(), e.getResolvedAt(), e.getResolutionNotes(), e.getCreatedAt());
    }
}
