package com.havi.retailreplenishment.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ref_sku_category")
public class RefSkuCategory {

    @Id
    @Column(name = "category_code", length = 40)
    private String categoryCode;

    @Column(name = "description", nullable = false, length = 150)
    private String description;

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
