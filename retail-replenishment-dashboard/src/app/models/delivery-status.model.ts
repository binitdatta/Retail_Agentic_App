export interface TrackingEvent {
    eventCode: string;
    eventAt: string;
    eventLocation: string | null;
    notes: string | null;
}

export interface DeliveryStatus {
    replenishmentOrderId: number;
    orderNumber: string;
    shipmentId: number;
    carrierName: string | null;
    trackingNumber: string | null;
    statusCode: string;
    shippedAt: string | null;
    estimatedArrivalAt: string | null;
    actualArrivalAt: string | null;
    events: TrackingEvent[];
}