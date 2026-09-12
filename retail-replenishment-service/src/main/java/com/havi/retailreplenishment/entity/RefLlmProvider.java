package com.havi.retailreplenishment.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ref_llm_provider")
public class RefLlmProvider {

    @Id
    @Column(name = "provider_code", length = 30)
    private String providerCode;

    @Column(name = "description", nullable = false, length = 150)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
