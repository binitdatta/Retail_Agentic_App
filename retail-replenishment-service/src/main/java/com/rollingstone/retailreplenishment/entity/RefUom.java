package com.rollingstone.retailreplenishment.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ref_uom")
public class RefUom {

    @Id
    @Column(name = "uom_code", length = 10)
    private String uomCode;

    @Column(name = "description", nullable = false, length = 80)
    private String description;

    public String getUomCode() { return uomCode; }
    public void setUomCode(String uomCode) { this.uomCode = uomCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
