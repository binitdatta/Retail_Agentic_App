package com.havi.retailreplenishment.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ref_escalation_status")
public class RefEscalationStatus {

    @Id
    @Column(name = "status_code", length = 30)
    private String statusCode;

    @Column(name = "description", nullable = false, length = 150)
    private String description;

    @Column(name = "is_terminal", nullable = false)
    private Boolean terminal = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getTerminal() { return terminal; }
    public void setTerminal(Boolean terminal) { this.terminal = terminal; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
