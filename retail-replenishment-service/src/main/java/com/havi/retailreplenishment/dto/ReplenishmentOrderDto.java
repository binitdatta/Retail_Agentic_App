package com.havi.retailreplenishment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReplenishmentOrderDto(
    Long replenishmentOrderId,
    String orderNumber,
    Long storeId,
    String storeCode,
    Long supplierId,
    String supplierCode,
    String statusCode,
    String sourceType,
    Long generatedByAgentRunId,
    BigDecimal totalCost,
    LocalDate requestedDeliveryDate,
    String approvedBy,
    LocalDateTime approvedAt,
    LocalDateTime sentToSupplierAt,
    List<OrderLineDto> lines
) {}
