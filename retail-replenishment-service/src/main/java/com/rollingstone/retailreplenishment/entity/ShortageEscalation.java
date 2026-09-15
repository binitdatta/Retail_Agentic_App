package com.rollingstone.retailreplenishment.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shortage_escalation")
public class ShortageEscalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "escalation_id")
    private Long escalationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replenishment_order_id")
    private ReplenishmentOrder replenishmentOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "escalation_reason", nullable = false, length = 60)
    private String escalationReason;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code", nullable = false)
    private RefEscalationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by_agent_run_id")
    private AgentRun raisedByAgentRun;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public Long getEscalationId() { return escalationId; }
    public ReplenishmentOrder getReplenishmentOrder() { return replenishmentOrder; }
    public void setReplenishmentOrder(ReplenishmentOrder replenishmentOrder) { this.replenishmentOrder = replenishmentOrder; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getEscalationReason() { return escalationReason; }
    public void setEscalationReason(String escalationReason) { this.escalationReason = escalationReason; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public RefEscalationStatus getStatus() { return status; }
    public void setStatus(RefEscalationStatus status) { this.status = status; }
    public AgentRun getRaisedByAgentRun() { return raisedByAgentRun; }
    public void setRaisedByAgentRun(AgentRun raisedByAgentRun) { this.raisedByAgentRun = raisedByAgentRun; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
