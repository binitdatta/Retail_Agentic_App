package com.havi.retailreplenishment.repository;

import com.havi.retailreplenishment.entity.LlmCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LlmCallLogRepository extends JpaRepository<LlmCallLog, Long> {

    @Query("""
        SELECT c FROM LlmCallLog c
        JOIN FETCH c.provider
        WHERE c.agentRun.agentRunId = :agentRunId
        ORDER BY c.requestedAt
        """)
    List<LlmCallLog> findByAgentRunId(@Param("agentRunId") Long agentRunId);

    // Backs the same rollup as vw_llm_cost_by_run for a single run.
    // Two things had to be fixed here:
    //   1. Every aggregate is wrapped in COALESCE — a run with zero
    //      llm_call_log rows yet (viewed before its first LLM call lands)
    //      must return zeros, not NULLs.
    //   2. Declared as List<Object[]>, not Object[] — Spring Data JPA
    //      executes a multi-column, no-GROUP-BY aggregate as a one-element
    //      List internally regardless of the declared return type. If the
    //      method is declared to return a bare Object[], Spring converts
    //      that List to an array via toArray(), which wraps the real
    //      6-element tuple inside an outer single-element array — so
    //      row[0] ends up being the whole tuple, not the count, and
    //      casting it to Number throws a ClassCastException. Declaring
    //      List<Object[]> and unwrapping the one element in the service
    //      avoids the implicit conversion entirely.
    @Query("""
        SELECT COUNT(c), COALESCE(SUM(c.promptTokens), 0), COALESCE(SUM(c.completionTokens), 0),
               COALESCE(SUM(c.estimatedCostUsd), 0), COALESCE(AVG(c.latencyMs), 0),
               COALESCE(SUM(CASE WHEN c.success = false THEN 1 ELSE 0 END), 0)
        FROM LlmCallLog c
        WHERE c.agentRun.agentRunId = :agentRunId
        """)
    List<Object[]> costRollupForRun(@Param("agentRunId") Long agentRunId);
}