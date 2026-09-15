package com.rollingstone.retailreplenishment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_inventory")
public class StoreInventory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_inventory_id")
    private Long storeInventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "on_hand_qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal onHandQty = BigDecimal.ZERO;

    @Column(name = "allocated_qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal allocatedQty = BigDecimal.ZERO;

    @Column(name = "reorder_point", nullable = false, precision = 14, scale = 4)
    private BigDecimal reorderPoint;

    @Column(name = "reorder_qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal reorderQty;

    @Column(name = "safety_stock_qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal safetyStockQty = BigDecimal.ZERO;

    @Column(name = "max_stock_qty", precision = 14, scale = 4)
    private BigDecimal maxStockQty;

    @Column(name = "last_counted_at")
    private LocalDateTime lastCountedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Long getStoreInventoryId() { return storeInventoryId; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public BigDecimal getOnHandQty() { return onHandQty; }
    public void setOnHandQty(BigDecimal onHandQty) { this.onHandQty = onHandQty; }
    public BigDecimal getAllocatedQty() { return allocatedQty; }
    public void setAllocatedQty(BigDecimal allocatedQty) { this.allocatedQty = allocatedQty; }
    public BigDecimal getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(BigDecimal reorderPoint) { this.reorderPoint = reorderPoint; }
    public BigDecimal getReorderQty() { return reorderQty; }
    public void setReorderQty(BigDecimal reorderQty) { this.reorderQty = reorderQty; }
    public BigDecimal getSafetyStockQty() { return safetyStockQty; }
    public void setSafetyStockQty(BigDecimal safetyStockQty) { this.safetyStockQty = safetyStockQty; }
    public BigDecimal getMaxStockQty() { return maxStockQty; }
    public void setMaxStockQty(BigDecimal maxStockQty) { this.maxStockQty = maxStockQty; }
    public LocalDateTime getLastCountedAt() { return lastCountedAt; }
    public void setLastCountedAt(LocalDateTime lastCountedAt) { this.lastCountedAt = lastCountedAt; }
    public Long getVersion() { return version; }

    public BigDecimal getAvailableQty() {
        return onHandQty.subtract(allocatedQty);
    }
}
