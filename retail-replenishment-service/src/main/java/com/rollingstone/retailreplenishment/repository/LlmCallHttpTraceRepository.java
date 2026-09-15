package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.LlmCallHttpTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LlmCallHttpTraceRepository extends JpaRepository<LlmCallHttpTrace, Long> {

    Optional<LlmCallHttpTrace> findByLlmCall_LlmCallId(Long llmCallId);

    // Joins through llm_call_log so one call gets every trace for an
    // agent_run without the caller needing to know individual call IDs
    // first — this is what backs the run-level "HTTP trace" screen.
    @Query("""
        SELECT t FROM LlmCallHttpTrace t
        JOIN FETCH t.llmCall c
        JOIN FETCH c.provider
        WHERE c.agentRun.agentRunId = :agentRunId
        ORDER BY t.createdAt
        """)
    List<LlmCallHttpTrace> findByAgentRunId(@Param("agentRunId") Long agentRunId);
}
