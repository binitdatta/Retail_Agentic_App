package com.rollingstone.retailreplenishment.dto;

import java.time.LocalDateTime;

public record DecisionLogDto(
    Long decisionId,
    Long agentRunId,
    String stageName,
    Long storeId,
    String storeCode,
    Long productId,
    String skuCode,
    Object inputSnapshot,
    Object outputDecision,
    String llmModel,
    String llmRationale,
    LocalDateTime executedAt,
    Integer durationMs
) {}
