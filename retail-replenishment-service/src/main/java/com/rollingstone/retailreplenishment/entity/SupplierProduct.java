package com.rollingstone.retailreplenishment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "supplier_product")
public class SupplierProduct extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_product_id")
    private Long supplierProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "supplier_sku", length = 60)
    private String supplierSku;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "min_order_qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal minOrderQty = BigDecimal.ONE;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays;

    @Column(name = "is_preferred", nullable = false)
    private Boolean preferred = false;

    public Long getSupplierProductId() { return supplierProductId; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getSupplierSku() { return supplierSku; }
    public void setSupplierSku(String supplierSku) { this.supplierSku = supplierSku; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getMinOrderQty() { return minOrderQty; }
    public void setMinOrderQty(BigDecimal minOrderQty) { this.minOrderQty = minOrderQty; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public Boolean getPreferred() { return preferred; }
    public void setPreferred(Boolean preferred) { this.preferred = preferred; }
}
