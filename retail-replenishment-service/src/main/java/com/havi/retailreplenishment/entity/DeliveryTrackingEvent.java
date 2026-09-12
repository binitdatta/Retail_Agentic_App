package com.havi.retailreplenishment.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_tracking_event")
public class DeliveryTrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracking_event_id")
    private Long trackingEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private DeliveryShipment shipment;

    @Column(name = "event_code", nullable = false, length = 40)
    private String eventCode;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    @Column(name = "event_location", length = 150)
    private String eventLocation;

    @Column(name = "notes", length = 500)
    private String notes;

    public Long getTrackingEventId() { return trackingEventId; }
    public DeliveryShipment getShipment() { return shipment; }
    public void setShipment(DeliveryShipment shipment) { this.shipment = shipment; }
    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }
    public LocalDateTime getEventAt() { return eventAt; }
    public void setEventAt(LocalDateTime eventAt) { this.eventAt = eventAt; }
    public String getEventLocation() { return eventLocation; }
    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
