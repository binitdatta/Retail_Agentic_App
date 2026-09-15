package com.rollingstone.retailreplenishment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LlmCallLogDto(
    Long llmCallId,
    Long agentRunId,
    Long decisionId,
    String providerCode,
    String modelName,
    Integer promptTokens,
    Integer completionTokens,
    Integer totalTokens,
    BigDecimal estimatedCostUsd,
    Integer latencyMs,
    Boolean success,
    LocalDateTime requestedAt
) {}
