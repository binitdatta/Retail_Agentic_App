package com.havi.retailreplenishment.service;

import com.havi.retailreplenishment.dto.CreateLlmCallHttpTraceRequest;
import com.havi.retailreplenishment.dto.LlmCallHttpTraceDto;
import com.havi.retailreplenishment.entity.LlmCallHttpTrace;
import com.havi.retailreplenishment.entity.LlmCallLog;
import com.havi.retailreplenishment.exception.ResourceNotFoundException;
import com.havi.retailreplenishment.mapper.JsonUtil;
import com.havi.retailreplenishment.mapper.LlmCallHttpTraceMapper;
import com.havi.retailreplenishment.repository.LlmCallHttpTraceRepository;
import com.havi.retailreplenishment.repository.LlmCallLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Raw wire-level HTTP capture for LLM calls — separate from LlmCallLogService,
 * which handles the logical request/response payload used by the cost
 * dashboard. This is one row per llm_call_log row (1:1), holding the actual
 * URL, method, headers, and raw body as sent/received. The caller (the
 * Python agent) is responsible for redacting any real API key out of
 * requestHeaders before this endpoint is ever called — see llm_clients.py.
 */
@Service
@Transactional
public class LlmCallHttpTraceService {

    private final LlmCallHttpTraceRepository traceRepository;
    private final LlmCallLogRepository llmCallLogRepository;
    private final LlmCallHttpTraceMapper mapper;
    private final JsonUtil jsonUtil;

    public LlmCallHttpTraceService(
            LlmCallHttpTraceRepository traceRepository,
            LlmCallLogRepository llmCallLogRepository,
            LlmCallHttpTraceMapper mapper,
            JsonUtil jsonUtil) {
        this.traceRepository = traceRepository;
        this.llmCallLogRepository = llmCallLogRepository;
        this.mapper = mapper;
        this.jsonUtil = jsonUtil;
    }

    public LlmCallHttpTraceDto create(Long llmCallId, CreateLlmCallHttpTraceRequest request) {
        LlmCallLog llmCall = llmCallLogRepository.findById(llmCallId)
            .orElseThrow(() -> new ResourceNotFoundException("LLM call log not found: " + llmCallId));

        LlmCallHttpTrace trace = new LlmCallHttpTrace();
        trace.setLlmCall(llmCall);
        trace.setHttpMethod(request.httpMethod());
        trace.setRequestUrl(request.requestUrl());
        trace.setRequestHeaders(jsonUtil.toJson(request.requestHeaders()));
        trace.setRequestParams(jsonUtil.toJson(request.requestParams()));
        trace.setRequestBody(jsonUtil.toJson(request.requestBody()));
        trace.setResponseStatusCode(request.responseStatusCode());
        trace.setResponseHeaders(jsonUtil.toJson(request.responseHeaders()));
        trace.setResponseBody(jsonUtil.toJson(request.responseBody()));

        return mapper.toDto(traceRepository.save(trace));
    }

    @Transactional(readOnly = true)
    public List<LlmCallHttpTraceDto> findByAgentRunId(Long agentRunId) {
        return traceRepository.findByAgentRunId(agentRunId).stream()
            .map(mapper::toDto)
            .toList();
    }
}
