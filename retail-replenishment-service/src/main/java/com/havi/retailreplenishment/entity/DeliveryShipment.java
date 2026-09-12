package com.havi.retailreplenishment.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_shipment")
public class DeliveryShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Long shipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replenishment_order_id", nullable = false)
    private ReplenishmentOrder replenishmentOrder;

    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Column(name = "tracking_number", length = 80)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code", nullable = false)
    private RefShipmentStatus status;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "estimated_arrival_at")
    private LocalDateTime estimatedArrivalAt;

    @Column(name = "actual_arrival_at")
    private LocalDateTime actualArrivalAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryTrackingEvent> trackingEvents = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getShipmentId() { return shipmentId; }
    public ReplenishmentOrder getReplenishmentOrder() { return replenishmentOrder; }
    public void setReplenishmentOrder(ReplenishmentOrder replenishmentOrder) { this.replenishmentOrder = replenishmentOrder; }
    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public RefShipmentStatus getStatus() { return status; }
    public void setStatus(RefShipmentStatus status) { this.status = status; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getEstimatedArrivalAt() { return estimatedArrivalAt; }
    public void setEstimatedArrivalAt(LocalDateTime estimatedArrivalAt) { this.estimatedArrivalAt = estimatedArrivalAt; }
    public LocalDateTime getActualArrivalAt() { return actualArrivalAt; }
    public void setActualArrivalAt(LocalDateTime actualArrivalAt) { this.actualArrivalAt = actualArrivalAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<DeliveryTrackingEvent> getTrackingEvents() { return trackingEvents; }

    public void addTrackingEvent(DeliveryTrackingEvent event) {
        event.setShipment(this);
        this.trackingEvents.add(event);
    }
}
