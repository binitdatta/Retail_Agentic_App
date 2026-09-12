package com.havi.retailreplenishment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "sku_code", nullable = false, unique = true, length = 40)
    private String skuCode;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code")
    private RefSkuCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_code", nullable = false)
    private RefUom uom;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "is_seasonal", nullable = false)
    private Boolean seasonal = false;

    @Column(name = "is_critical", nullable = false)
    private Boolean critical = false;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Long getProductId() { return productId; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public RefSkuCategory getCategory() { return category; }
    public void setCategory(RefSkuCategory category) { this.category = category; }
    public RefUom getUom() { return uom; }
    public void setUom(RefUom uom) { this.uom = uom; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Boolean getSeasonal() { return seasonal; }
    public void setSeasonal(Boolean seasonal) { this.seasonal = seasonal; }
    public Boolean getCritical() { return critical; }
    public void setCritical(Boolean critical) { this.critical = critical; }
    public Integer getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(Integer shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Long getVersion() { return version; }
}
