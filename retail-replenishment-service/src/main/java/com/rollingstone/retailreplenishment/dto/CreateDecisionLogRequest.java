package com.rollingstone.retailreplenishment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDecisionLogRequest(
    @NotBlank String stageName,
    Long storeId,
    Long productId,
    @NotNull Object inputSnapshot,
    @NotNull Object outputDecision,
    String llmModel,
    String llmRationale,
    Integer durationMs
) {}
