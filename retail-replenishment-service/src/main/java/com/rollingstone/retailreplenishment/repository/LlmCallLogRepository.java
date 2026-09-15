package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.LlmCallLog;
import org.springframework.data.domain.Pageable;
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

    // Backs the global audit screen — LLM calls across every run, newest
    // first, with optional provider/success filters. JOIN FETCH c.agentRun
    // (not needed in findByAgentRunId above, since that caller already
    // knows the run) because every row here needs to show which run it
    // belongs to.
    @Query("""
        SELECT c FROM LlmCallLog c
        JOIN FETCH c.agentRun
        JOIN FETCH c.provider
        WHERE (:providerCode IS NULL OR c.provider.providerCode = :providerCode)
          AND (:success IS NULL OR c.success = :success)
        ORDER BY c.requestedAt DESC
        """)
    List<LlmCallLog> searchAcrossRuns(
            @Param("providerCode") String providerCode,
            @Param("success") Boolean success,
            Pageable pageable);

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

    // Same shape as costRollupForRun, but across every run ever recorded —
    // the all-time total the global audit screen's stat cards show.
    @Query("""
        SELECT COUNT(c), COALESCE(SUM(c.promptTokens), 0), COALESCE(SUM(c.completionTokens), 0),
               COALESCE(SUM(c.estimatedCostUsd), 0), COALESCE(AVG(c.latencyMs), 0),
               COALESCE(SUM(CASE WHEN c.success = false THEN 1 ELSE 0 END), 0)
        FROM LlmCallLog c
        """)
    List<Object[]> globalCostRollup();
}