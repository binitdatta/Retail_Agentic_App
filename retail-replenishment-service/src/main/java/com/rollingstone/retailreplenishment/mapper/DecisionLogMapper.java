package com.rollingstone.retailreplenishment.mapper;

import com.rollingstone.retailreplenishment.dto.DecisionLogDto;
import com.rollingstone.retailreplenishment.entity.AgentDecisionLog;
import org.springframework.stereotype.Component;

@Component
public class DecisionLogMapper {

    private final JsonUtil jsonUtil;

    public DecisionLogMapper(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
    }

    public DecisionLogDto toDto(AgentDecisionLog d) {
        if (d == null) return null;
        return new DecisionLogDto(
            d.getDecisionId(), d.getAgentRun().getAgentRunId(), d.getStageName(),
            d.getStore() != null ? d.getStore().getStoreId() : null,
            d.getStore() != null ? d.getStore().getStoreCode() : null,
            d.getProduct() != null ? d.getProduct().getProductId() : null,
            d.getProduct() != null ? d.getProduct().getSkuCode() : null,
            jsonUtil.fromJson(d.getInputSnapshot()),
            jsonUtil.fromJson(d.getOutputDecision()),
            d.getLlmModel(), d.getLlmRationale(), d.getExecutedAt(), d.getDurationMs());
    }
}
