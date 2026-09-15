package com.rollingstone.retailreplenishment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAgentRunRequest(
    @NotBlank String triggerType,
    String triggerSource,
    Long storeId
) {}
