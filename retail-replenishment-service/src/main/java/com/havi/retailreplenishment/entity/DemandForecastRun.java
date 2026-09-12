package com.havi.retailreplenishment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demand_forecast_run")
public class DemandForecastRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forecast_run_id")
    private Long forecastRunId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "forecast_method", nullable = false, length = 40)
    private String forecastMethod;

    @Column(name = "forecast_horizon_days", nullable = false)
    private Integer forecastHorizonDays;

    @Column(name = "forecasted_demand_qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal forecastedDemandQty;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_agent_run_id")
    private AgentRun generatedByAgentRun;

    public Long getForecastRunId() { return forecastRunId; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getForecastMethod() { return forecastMethod; }
    public void setForecastMethod(String forecastMethod) { this.forecastMethod = forecastMethod; }
    public Integer getForecastHorizonDays() { return forecastHorizonDays; }
    public void setForecastHorizonDays(Integer forecastHorizonDays) { this.forecastHorizonDays = forecastHorizonDays; }
    public BigDecimal getForecastedDemandQty() { return forecastedDemandQty; }
    public void setForecastedDemandQty(BigDecimal forecastedDemandQty) { this.forecastedDemandQty = forecastedDemandQty; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public AgentRun getGeneratedByAgentRun() { return generatedByAgentRun; }
    public void setGeneratedByAgentRun(AgentRun generatedByAgentRun) { this.generatedByAgentRun = generatedByAgentRun; }
}
