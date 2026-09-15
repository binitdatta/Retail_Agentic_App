package com.rollingstone.retailreplenishment.mapper;

import com.rollingstone.retailreplenishment.dto.OrderLineDto;
import com.rollingstone.retailreplenishment.dto.ReplenishmentOrderDto;
import com.rollingstone.retailreplenishment.entity.ReplenishmentOrder;
import com.rollingstone.retailreplenishment.entity.ReplenishmentOrderLine;

import java.util.List;

public final class ReplenishmentOrderMapper {

    private ReplenishmentOrderMapper() {}

    public static OrderLineDto toLineDto(ReplenishmentOrderLine l) {
        return new OrderLineDto(
            l.getOrderLineId(), l.getProduct().getProductId(), l.getProduct().getSkuCode(),
            l.getProduct().getProductName(), l.getOrderedQty(), l.getUnitCost(), l.getLineTotal(),
            l.getForecastRun() != null ? l.getForecastRun().getForecastRunId() : null);
    }

    public static ReplenishmentOrderDto toDto(ReplenishmentOrder o) {
        if (o == null) return null;
        List<OrderLineDto> lines = o.getLines().stream()
            .map(ReplenishmentOrderMapper::toLineDto)
            .toList();
        return new ReplenishmentOrderDto(
            o.getReplenishmentOrderId(), o.getOrderNumber(),
            o.getStore().getStoreId(), o.getStore().getStoreCode(),
            o.getSupplier().getSupplierId(), o.getSupplier().getSupplierCode(),
            o.getStatus().getStatusCode(), o.getSourceType(),
            o.getGeneratedByAgentRun() != null ? o.getGeneratedByAgentRun().getAgentRunId() : null,
            o.getTotalCost(), o.getRequestedDeliveryDate(), o.getApprovedBy(),
            o.getApprovedAt(), o.getSentToSupplierAt(), lines);
    }
}
