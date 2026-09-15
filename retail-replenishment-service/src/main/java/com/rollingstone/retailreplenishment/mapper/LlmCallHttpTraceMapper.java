package com.rollingstone.retailreplenishment.mapper;

import com.rollingstone.retailreplenishment.dto.LlmCallHttpTraceDto;
import com.rollingstone.retailreplenishment.entity.LlmCallHttpTrace;
import org.springframework.stereotype.Component;

@Component
public class LlmCallHttpTraceMapper {

    private final JsonUtil jsonUtil;

    public LlmCallHttpTraceMapper(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
    }

    public LlmCallHttpTraceDto toDto(LlmCallHttpTrace t) {
        if (t == null) return null;
        return new LlmCallHttpTraceDto(
            t.getHttpTraceId(),
            t.getLlmCall().getLlmCallId(),
            t.getLlmCall().getProvider().getProviderCode(),
            t.getLlmCall().getModelName(),
            t.getHttpMethod(),
            t.getRequestUrl(),
            jsonUtil.fromJson(t.getRequestHeaders()),
            jsonUtil.fromJson(t.getRequestParams()),
            jsonUtil.fromJson(t.getRequestBody()),
            t.getResponseStatusCode(),
            jsonUtil.fromJson(t.getResponseHeaders()),
            jsonUtil.fromJson(t.getResponseBody()),
            t.getCreatedAt());
    }
}
