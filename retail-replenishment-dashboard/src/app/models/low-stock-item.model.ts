export interface LowStockItem {
    storeInventoryId: number;
    storeId: number;
    storeCode: string;
    productId: number;
    skuCode: string;
    productName: string;
    critical: boolean;
    onHandQty: number;
    allocatedQty: number;
    availableQty: number;
    reorderPoint: number;
    reorderQty: number;
    safetyStockQty: number;
}