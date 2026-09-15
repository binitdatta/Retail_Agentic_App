package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.AgentDecisionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgentDecisionLogRepository extends JpaRepository<AgentDecisionLog, Long> {

    @Query("""
        SELECT d FROM AgentDecisionLog d
        LEFT JOIN FETCH d.store
        LEFT JOIN FETCH d.product
        WHERE d.agentRun.agentRunId = :agentRunId
        ORDER BY d.executedAt
        """)
    List<AgentDecisionLog> findByAgentRunId(@Param("agentRunId") Long agentRunId);

    // Backs the global audit screen — decisions across every run, newest
    // first, with optional stage/store filters. Both filters are optional
    // ("(:param IS NULL OR ...)" — same pattern as
    // ReplenishmentOrderRepository.search / ShortageEscalationRepository.search).
    // JOIN FETCH d.agentRun here (unlike findByAgentRunId above, which
    // doesn't need it — the caller already knows the run) since this list
    // spans many runs and every row needs to show which one it belongs to.
    @Query("""
        SELECT d FROM AgentDecisionLog d
        JOIN FETCH d.agentRun
        LEFT JOIN FETCH d.store
        LEFT JOIN FETCH d.product
        WHERE (:stageName IS NULL OR d.stageName = :stageName)
          AND (:storeId IS NULL OR d.store.storeId = :storeId)
        ORDER BY d.executedAt DESC
        """)
    List<AgentDecisionLog> searchAcrossRuns(
            @Param("stageName") String stageName,
            @Param("storeId") Long storeId,
            Pageable pageable);
}