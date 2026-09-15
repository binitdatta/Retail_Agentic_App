package com.rollingstone.retailreplenishment.controller;

import com.rollingstone.retailreplenishment.dto.*;
import com.rollingstone.retailreplenishment.dto.*;
import com.rollingstone.retailreplenishment.service.AgentDecisionLogService;
import com.rollingstone.retailreplenishment.service.AgentRunService;
import com.rollingstone.retailreplenishment.service.LlmCallHttpTraceService;
import com.rollingstone.retailreplenishment.service.LlmCallLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent-runs")
public class AgentRunController {

    private final AgentRunService agentRunService;
    private final AgentDecisionLogService decisionLogService;
    private final LlmCallLogService llmCallLogService;
    private final LlmCallHttpTraceService httpTraceService;

    public AgentRunController(
            AgentRunService agentRunService,
            AgentDecisionLogService decisionLogService,
            LlmCallLogService llmCallLogService,
            LlmCallHttpTraceService httpTraceService) {
        this.agentRunService = agentRunService;
        this.decisionLogService = decisionLogService;
        this.llmCallLogService = llmCallLogService;
        this.httpTraceService = httpTraceService;
    }

    // The agent calls this once at the start of a monitored (5-10 min) run.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentRunDto start(@Valid @RequestBody CreateAgentRunRequest request) {
        return agentRunService.start(request);
    }

    @PatchMapping("/{id}")
    public AgentRunDto complete(@PathVariable Long id, @Valid @RequestBody CompleteAgentRunRequest request) {
        return agentRunService.complete(id, request);
    }

    @GetMapping("/{id}")
    public AgentRunDto getById(@PathVariable Long id) {
        return agentRunService.getById(id);
    }

    @GetMapping
    public List<AgentRunDto> findRecent() {
        return agentRunService.findRecent();
    }

    @PostMapping("/{id}/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionLogDto logDecision(@PathVariable Long id, @Valid @RequestBody CreateDecisionLogRequest request) {
        return decisionLogService.create(id, request);
    }

    @GetMapping("/{id}/decisions")
    public List<DecisionLogDto> decisions(@PathVariable Long id) {
        return decisionLogService.findByAgentRunId(id);
    }

    @PostMapping("/{id}/llm-calls")
    @ResponseStatus(HttpStatus.CREATED)
    public LlmCallLogDto logLlmCall(@PathVariable Long id, @Valid @RequestBody CreateLlmCallLogRequest request) {
        return llmCallLogService.create(id, request);
    }

    @GetMapping("/{id}/llm-calls")
    public List<LlmCallLogDto> llmCalls(@PathVariable Long id) {
        return llmCallLogService.findByAgentRunId(id);
    }

    // Same rollup as vw_llm_cost_by_run, scoped to one run — the dashboard's
    // primary read.
    @GetMapping("/{id}/llm-cost")
    public LlmCostRollupDto llmCost(@PathVariable Long id) {
        return llmCallLogService.costRollup(id);
    }

    // Raw wire-level HTTP capture, separate from the logical llm-calls
    // payload above — one trace row per LLM call, created right after that
    // call is logged.
    @PostMapping("/{runId}/llm-calls/{callId}/http-trace")
    @ResponseStatus(HttpStatus.CREATED)
    public LlmCallHttpTraceDto logHttpTrace(
            @PathVariable Long runId, @PathVariable Long callId,
            @Valid @RequestBody CreateLlmCallHttpTraceRequest request) {
        return httpTraceService.create(callId, request);
    }

    // The dedicated "HTTP trace" screen's read — every trace for the run,
    // joined through llm_call_log so the caller only needs the run id.
    @GetMapping("/{id}/http-traces")
    public List<LlmCallHttpTraceDto> httpTraces(@PathVariable Long id) {
        return httpTraceService.findByAgentRunId(id);
    }
}
