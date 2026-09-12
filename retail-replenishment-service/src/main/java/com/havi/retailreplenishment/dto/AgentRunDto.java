package com.havi.retailreplenishment.dto;

import java.time.LocalDateTime;

public record AgentRunDto(
    Long agentRunId,
    String runUuid,
    String triggerType,
    String triggerSource,
    Long storeId,
    String storeCode,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    String status,
    String summary
) {}
