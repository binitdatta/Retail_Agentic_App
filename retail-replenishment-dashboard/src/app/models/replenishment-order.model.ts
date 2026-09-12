export interface OrderLine {
    orderLineId: number;
    productId: number;
    skuCode: string;
    productName: string;
    orderedQty: number;
    unitCost: number;
    lineTotal: number | null;
    forecastRunId: number | null;
}

export interface ReplenishmentOrder {
    replenishmentOrderId: number;
    orderNumber: string;
    storeId: number;
    storeCode: string;
    supplierId: number;
    supplierCode: string;
    statusCode: string;
    sourceType: string;
    generatedByAgentRunId: number | null;
    totalCost: number;
    requestedDeliveryDate: string | null;
    approvedBy: string | null;
    approvedAt: string | null;
    sentToSupplierAt: string | null;
    lines: OrderLine[];
}