package com.havi.retailreplenishment.repository;

import com.havi.retailreplenishment.entity.AgentDecisionLog;
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
}
