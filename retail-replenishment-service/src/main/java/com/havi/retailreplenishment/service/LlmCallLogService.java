package com.havi.retailreplenishment.service;

import com.havi.retailreplenishment.dto.CreateLlmCallLogRequest;
import com.havi.retailreplenishment.dto.LlmCallLogDto;
import com.havi.retailreplenishment.dto.LlmCostRollupDto;
import com.havi.retailreplenishment.entity.*;
import com.havi.retailreplenishment.exception.ResourceNotFoundException;
import com.havi.retailreplenishment.mapper.JsonUtil;
import com.havi.retailreplenishment.mapper.LlmCallLogMapper;
import com.havi.retailreplenishment.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Same pattern as the SpecRight SDM chatbot's cost/audit dashboard table:
 * one row per LLM invocation with full request/response payloads, feeding
 * the same kind of cost/token rollup vw_llm_cost_by_run provides in SQL.
 */
@Service
@Transactional
public class LlmCallLogService {

    private final LlmCallLogRepository llmCallLogRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentDecisionLogRepository decisionLogRepository;
    private final RefLlmProviderRepository providerRepository;
    private final LlmCallLogMapper llmCallLogMapper;
    private final JsonUtil jsonUtil;

    public LlmCallLogService(
            LlmCallLogRepository llmCallLogRepository,
            AgentRunRepository agentRunRepository,
            AgentDecisionLogRepository decisionLogRepository,
            RefLlmProviderRepository providerRepository,
            LlmCallLogMapper llmCallLogMapper,
            JsonUtil jsonUtil) {
        this.llmCallLogRepository = llmCallLogRepository;
        this.agentRunRepository = agentRunRepository;
        this.decisionLogRepository = decisionLogRepository;
        this.providerRepository = providerRepository;
        this.llmCallLogMapper = llmCallLogMapper;
        this.jsonUtil = jsonUtil;
    }

    public LlmCallLogDto create(Long agentRunId, CreateLlmCallLogRequest request) {
        AgentRun agentRun = agentRunRepository.findById(agentRunId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent run not found: " + agentRunId));
        RefLlmProvider provider = providerRepository.findById(request.providerCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown LLM provider: " + request.providerCode()));

        LlmCallLog log = new LlmCallLog();
        log.setAgentRun(agentRun);
        log.setProvider(provider);
        log.setModelName(request.modelName());
        log.setRequestPayload(jsonUtil.toJson(request.requestPayload()));
        log.setResponsePayload(jsonUtil.toJson(request.responsePayload()));
        log.setPromptTokens(request.promptTokens());
        log.setCompletionTokens(request.completionTokens());
        log.setEstimatedCostUsd(request.estimatedCostUsd());
        log.setLatencyMs(request.latencyMs());
        log.setHttpStatusCode(request.httpStatusCode());
        log.setSuccess(request.success() != null ? request.success() : true);
        log.setErrorMessage(request.errorMessage());

        LocalDateTime requestedAt = LocalDateTime.now();
        log.setRequestedAt(requestedAt);
        if (request.latencyMs() != null) {
            log.setCompletedAt(requestedAt.plusNanos(request.latencyMs() * 1_000_000L));
        }

        if (request.decisionId() != null) {
            AgentDecisionLog decision = decisionLogRepository.findById(request.decisionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Decision log not found: " + request.decisionId()));
            log.setDecision(decision);
        }

        return llmCallLogMapper.toDto(llmCallLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<LlmCallLogDto> findByAgentRunId(Long agentRunId) {
        return llmCallLogRepository.findByAgentRunId(agentRunId).stream()
                .map(llmCallLogMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LlmCostRollupDto costRollup(Long agentRunId) {
        List<Object[]> rows = llmCallLogRepository.costRollupForRun(agentRunId);
        // Always exactly one row: an aggregate query with no GROUP BY
        // returns one row even when zero source rows matched.
        Object[] row = rows.get(0);
        return new LlmCostRollupDto(
                agentRunId,
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                (BigDecimal) row[3],
                ((Number) row[4]).doubleValue(),
                ((Number) row[5]).longValue());
    }
}