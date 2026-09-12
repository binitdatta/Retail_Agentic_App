package com.havi.retailreplenishment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "replenishment_order")
public class ReplenishmentOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "replenishment_order_id")
    private Long replenishmentOrderId;

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code", nullable = false)
    private RefOrderStatus status;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_agent_run_id")
    private AgentRun generatedByAgentRun;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "sent_to_supplier_at")
    private LocalDateTime sentToSupplierAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "replenishmentOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReplenishmentOrderLine> lines = new ArrayList<>();

    public Long getReplenishmentOrderId() { return replenishmentOrderId; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public RefOrderStatus getStatus() { return status; }
    public void setStatus(RefOrderStatus status) { this.status = status; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public AgentRun getGeneratedByAgentRun() { return generatedByAgentRun; }
    public void setGeneratedByAgentRun(AgentRun generatedByAgentRun) { this.generatedByAgentRun = generatedByAgentRun; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public LocalDate getRequestedDeliveryDate() { return requestedDeliveryDate; }
    public void setRequestedDeliveryDate(LocalDate requestedDeliveryDate) { this.requestedDeliveryDate = requestedDeliveryDate; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getSentToSupplierAt() { return sentToSupplierAt; }
    public void setSentToSupplierAt(LocalDateTime sentToSupplierAt) { this.sentToSupplierAt = sentToSupplierAt; }
    public Long getVersion() { return version; }
    public List<ReplenishmentOrderLine> getLines() { return lines; }

    public void addLine(ReplenishmentOrderLine line) {
        line.setReplenishmentOrder(this);
        this.lines.add(line);
    }
}
