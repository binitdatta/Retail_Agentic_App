package com.rollingstone.retailreplenishment.controller;

import com.rollingstone.retailreplenishment.dto.DecisionLogDto;
import com.rollingstone.retailreplenishment.dto.LlmCallLogDto;
import com.rollingstone.retailreplenishment.dto.LlmCostRollupDto;
import com.rollingstone.retailreplenishment.service.AgentDecisionLogService;
import com.rollingstone.retailreplenishment.service.LlmCallLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AgentDecisionLogService decisionLogService;
    private final LlmCallLogService llmCallLogService;

    public AuditController(AgentDecisionLogService decisionLogService, LlmCallLogService llmCallLogService) {
        this.decisionLogService = decisionLogService;
        this.llmCallLogService = llmCallLogService;
    }

    @GetMapping("/decisions")
    public List<DecisionLogDto> decisions(
            @RequestParam(required = false) String stageName,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "200") int limit) {
        return decisionLogService.searchAcrossRuns(stageName, storeId, limit);
    }

    @GetMapping("/llm-calls")
    public List<LlmCallLogDto> llmCalls(
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) Boolean success,
            @RequestParam(defaultValue = "200") int limit) {
        return llmCallLogService.searchAcrossRuns(providerCode, success, limit);
    }

    @GetMapping("/llm-cost")
    public LlmCostRollupDto globalLlmCost() {
        return llmCallLogService.globalCostRollup();
    }
}