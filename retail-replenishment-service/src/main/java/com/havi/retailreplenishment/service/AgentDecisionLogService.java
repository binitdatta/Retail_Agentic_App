package com.havi.retailreplenishment.service;

import com.havi.retailreplenishment.dto.CreateDecisionLogRequest;
import com.havi.retailreplenishment.dto.DecisionLogDto;
import com.havi.retailreplenishment.entity.*;
import com.havi.retailreplenishment.exception.ResourceNotFoundException;
import com.havi.retailreplenishment.mapper.DecisionLogMapper;
import com.havi.retailreplenishment.mapper.JsonUtil;
import com.havi.retailreplenishment.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AgentDecisionLogService {

    private static final Set<String> VALID_STAGES = Set.of(
        "DETECT_LOW_INVENTORY", "FORECAST_DEMAND", "CHECK_SUPPLIER_AVAILABILITY",
        "RECOMMEND_REPLENISHMENT", "MONITOR_DELIVERY", "ESCALATE_SHORTAGE");

    private final AgentDecisionLogRepository decisionLogRepository;
    private final AgentRunRepository agentRunRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final DecisionLogMapper decisionLogMapper;
    private final JsonUtil jsonUtil;

    public AgentDecisionLogService(
            AgentDecisionLogRepository decisionLogRepository,
            AgentRunRepository agentRunRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            DecisionLogMapper decisionLogMapper,
            JsonUtil jsonUtil) {
        this.decisionLogRepository = decisionLogRepository;
        this.agentRunRepository = agentRunRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.decisionLogMapper = decisionLogMapper;
        this.jsonUtil = jsonUtil;
    }

    public DecisionLogDto create(Long agentRunId, CreateDecisionLogRequest request) {
        if (!VALID_STAGES.contains(request.stageName())) {
            throw new IllegalArgumentException("Unknown stage name: " + request.stageName());
        }
        AgentRun agentRun = agentRunRepository.findById(agentRunId)
            .orElseThrow(() -> new ResourceNotFoundException("Agent run not found: " + agentRunId));

        AgentDecisionLog log = new AgentDecisionLog();
        log.setAgentRun(agentRun);
        log.setStageName(request.stageName());
        log.setInputSnapshot(jsonUtil.toJson(request.inputSnapshot()));
        log.setOutputDecision(jsonUtil.toJson(request.outputDecision()));
        log.setLlmModel(request.llmModel());
        log.setLlmRationale(request.llmRationale());
        log.setDurationMs(request.durationMs());
        log.setExecutedAt(LocalDateTime.now());

        if (request.storeId() != null) {
            log.setStore(storeRepository.findById(request.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + request.storeId())));
        }
        if (request.productId() != null) {
            log.setProduct(productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId())));
        }

        return decisionLogMapper.toDto(decisionLogRepository.save(log));
    }

    @Transactional(readOnly = true)
    public List<DecisionLogDto> findByAgentRunId(Long agentRunId) {
        return decisionLogRepository.findByAgentRunId(agentRunId).stream()
            .map(decisionLogMapper::toDto)
            .toList();
    }
}
