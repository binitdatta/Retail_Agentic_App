package com.havi.retailreplenishment.mapper;

import com.havi.retailreplenishment.dto.AgentRunDto;
import com.havi.retailreplenishment.entity.AgentRun;

public final class AgentRunMapper {

    private AgentRunMapper() {}

    public static AgentRunDto toDto(AgentRun a) {
        if (a == null) return null;
        return new AgentRunDto(
            a.getAgentRunId(), a.getRunUuid(), a.getTriggerType(), a.getTriggerSource(),
            a.getStore() != null ? a.getStore().getStoreId() : null,
            a.getStore() != null ? a.getStore().getStoreCode() : null,
            a.getStartedAt(), a.getCompletedAt(), a.getStatus(), a.getSummary());
    }
}
