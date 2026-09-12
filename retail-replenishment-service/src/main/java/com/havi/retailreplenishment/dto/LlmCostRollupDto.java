package com.havi.retailreplenishment.dto;

import java.math.BigDecimal;

public record LlmCostRollupDto(
    Long agentRunId,
    Long callCount,
    Long totalPromptTokens,
    Long totalCompletionTokens,
    BigDecimal totalEstimatedCostUsd,
    Double avgLatencyMs,
    Long errorCount
) {}
