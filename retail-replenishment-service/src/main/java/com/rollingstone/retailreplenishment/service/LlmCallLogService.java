package com.rollingstone.retailreplenishment.service;

import com.rollingstone.retailreplenishment.dto.CreateLlmCallLogRequest;
import com.rollingstone.retailreplenishment.dto.LlmCallLogDto;
import com.rollingstone.retailreplenishment.dto.LlmCostRollupDto;
import com.rollingstone.retailreplenishment.entity.*;
import com.rollingstone.retailreplenishment.entity.AgentDecisionLog;
import com.rollingstone.retailreplenishment.entity.AgentRun;
import com.rollingstone.retailreplenishment.entity.LlmCallLog;
import com.rollingstone.retailreplenishment.entity.RefLlmProvider;
import com.rollingstone.retailreplenishment.exception.ResourceNotFoundException;
import com.rollingstone.retailreplenishment.mapper.JsonUtil;
import com.rollingstone.retailreplenishment.mapper.LlmCallLogMapper;
import com.rollingstone.retailreplenishment.repository.*;
import com.rollingstone.retailreplenishment.repository.AgentDecisionLogRepository;
import com.rollingstone.retailreplenishment.repository.AgentRunRepository;
import com.rollingstone.retailreplenishment.repository.LlmCallLogRepository;
import com.rollingstone.retailreplenishment.repository.RefLlmProviderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    public List<LlmCallLogDto> searchAcrossRuns(String providerCode, Boolean success, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return llmCallLogRepository.searchAcrossRuns(providerCode, success, pageable).stream()
                .map(llmCallLogMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LlmCostRollupDto costRollup(Long agentRunId) {
        List<Object[]> rows = llmCallLogRepository.costRollupForRun(agentRunId);
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

    // Same as costRollup, but agentRunId is null — this is the all-time
    // total across every run, not scoped to one.
    @Transactional(readOnly = true)
    public LlmCostRollupDto globalCostRollup() {
        List<Object[]> rows = llmCallLogRepository.globalCostRollup();
        Object[] row = rows.get(0);
        return new LlmCostRollupDto(
                null,
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                (BigDecimal) row[3],
                ((Number) row[4]).doubleValue(),
                ((Number) row[5]).longValue());
    }
}