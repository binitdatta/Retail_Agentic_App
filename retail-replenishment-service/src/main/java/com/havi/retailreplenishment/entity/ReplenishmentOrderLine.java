package com.havi.retailreplenishment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "replenishment_order_line")
public class ReplenishmentOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_line_id")
    private Long orderLineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replenishment_order_id", nullable = false)
    private ReplenishmentOrder replenishmentOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "ordered_qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal orderedQty;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "line_total", insertable = false, updatable = false, precision = 18, scale = 4)
    private BigDecimal lineTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forecast_run_id")
    private DemandForecastRun forecastRun;

    public Long getOrderLineId() { return orderLineId; }
    public ReplenishmentOrder getReplenishmentOrder() { return replenishmentOrder; }
    public void setReplenishmentOrder(ReplenishmentOrder replenishmentOrder) { this.replenishmentOrder = replenishmentOrder; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public BigDecimal getOrderedQty() { return orderedQty; }
    public void setOrderedQty(BigDecimal orderedQty) { this.orderedQty = orderedQty; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public DemandForecastRun getForecastRun() { return forecastRun; }
    public void setForecastRun(DemandForecastRun forecastRun) { this.forecastRun = forecastRun; }
}
