package com.havi.retailreplenishment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateLlmCallLogRequest(
    Long decisionId,
    @NotBlank String providerCode,
    @NotBlank String modelName,
    @NotNull Object requestPayload,
    Object responsePayload,
    Integer promptTokens,
    Integer completionTokens,
    BigDecimal estimatedCostUsd,
    Integer latencyMs,
    Integer httpStatusCode,
    Boolean success,
    String errorMessage
) {}
