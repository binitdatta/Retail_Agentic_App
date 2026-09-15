package com.rollingstone.retailreplenishment.service;

import com.rollingstone.retailreplenishment.dto.*;
import com.rollingstone.retailreplenishment.entity.*;
import com.rollingstone.retailreplenishment.dto.AgentRunDto;
import com.rollingstone.retailreplenishment.dto.CompleteAgentRunRequest;
import com.rollingstone.retailreplenishment.dto.CreateAgentRunRequest;
import com.rollingstone.retailreplenishment.entity.AgentRun;
import com.rollingstone.retailreplenishment.entity.Store;
import com.rollingstone.retailreplenishment.exception.ResourceNotFoundException;
import com.rollingstone.retailreplenishment.mapper.AgentRunMapper;
import com.rollingstone.retailreplenishment.repository.AgentRunRepository;
import com.rollingstone.retailreplenishment.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The agent calls POST at the start of a monitored run and PATCH when it
 * finishes (or times out); every decision and LLM call in between is filed
 * against the returned agentRunId.
 */
@Service
@Transactional
public class AgentRunService {

    private final AgentRunRepository agentRunRepository;
    private final StoreRepository storeRepository;

    public AgentRunService(AgentRunRepository agentRunRepository, StoreRepository storeRepository) {
        this.agentRunRepository = agentRunRepository;
        this.storeRepository = storeRepository;
    }

    public AgentRunDto start(CreateAgentRunRequest request) {
        AgentRun run = new AgentRun();
        run.setRunUuid(UUID.randomUUID().toString());
        run.setTriggerType(request.triggerType());
        run.setTriggerSource(request.triggerSource());
        run.setStartedAt(LocalDateTime.now());
        run.setStatus("RUNNING");
        if (request.storeId() != null) {
            Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + request.storeId()));
            run.setStore(store);
        }
        return AgentRunMapper.toDto(agentRunRepository.save(run));
    }

    public AgentRunDto complete(Long id, CompleteAgentRunRequest request) {
        AgentRun run = agentRunRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agent run not found: " + id));
        run.setStatus(request.status());
        run.setSummary(request.summary());
        run.setCompletedAt(LocalDateTime.now());
        return AgentRunMapper.toDto(agentRunRepository.save(run));
    }

    @Transactional(readOnly = true)
    public AgentRunDto getById(Long id) {
        return agentRunRepository.findById(id)
            .map(AgentRunMapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Agent run not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AgentRunDto> findRecent() {
        return agentRunRepository.findRecent().stream().map(AgentRunMapper::toDto).toList();
    }
}
