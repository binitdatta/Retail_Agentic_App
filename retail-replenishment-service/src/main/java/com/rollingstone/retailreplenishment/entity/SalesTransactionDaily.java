package com.rollingstone.retailreplenishment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_transaction_daily")
public class SalesTransactionDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_sales_id")
    private Long dailySalesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sales_date", nullable = false)
    private LocalDate salesDate;

    @Column(name = "units_sold", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitsSold = BigDecimal.ZERO;

    @Column(name = "gross_revenue", nullable = false, precision = 14, scale = 4)
    private BigDecimal grossRevenue = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getDailySalesId() { return dailySalesId; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public LocalDate getSalesDate() { return salesDate; }
    public void setSalesDate(LocalDate salesDate) { this.salesDate = salesDate; }
    public BigDecimal getUnitsSold() { return unitsSold; }
    public void setUnitsSold(BigDecimal unitsSold) { this.unitsSold = unitsSold; }
    public BigDecimal getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(BigDecimal grossRevenue) { this.grossRevenue = grossRevenue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
