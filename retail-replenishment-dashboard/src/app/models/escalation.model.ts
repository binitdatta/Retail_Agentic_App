export interface Escalation {
    escalationId: number;
    replenishmentOrderId: number | null;
    storeId: number;
    storeCode: string;
    productId: number;
    skuCode: string;
    escalationReason: string;
    severity: string;
    statusCode: string;
    assignedTo: string | null;
    resolvedAt: string | null;
    resolutionNotes: string | null;
    createdAt: string;
}