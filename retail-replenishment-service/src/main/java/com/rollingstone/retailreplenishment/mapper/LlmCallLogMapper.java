package com.rollingstone.retailreplenishment.mapper;

import com.rollingstone.retailreplenishment.dto.LlmCallLogDto;
import com.rollingstone.retailreplenishment.entity.LlmCallLog;
import org.springframework.stereotype.Component;

@Component
public class LlmCallLogMapper {

    public LlmCallLogDto toDto(LlmCallLog c) {
        if (c == null) return null;
        return new LlmCallLogDto(
            c.getLlmCallId(), c.getAgentRun().getAgentRunId(),
            c.getDecision() != null ? c.getDecision().getDecisionId() : null,
            c.getProvider().getProviderCode(), c.getModelName(),
            c.getPromptTokens(), c.getCompletionTokens(), c.getTotalTokens(),
            c.getEstimatedCostUsd(), c.getLatencyMs(), c.getSuccess(), c.getRequestedAt());
    }
}
