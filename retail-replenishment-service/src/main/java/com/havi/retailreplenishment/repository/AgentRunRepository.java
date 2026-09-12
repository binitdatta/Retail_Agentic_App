package com.havi.retailreplenishment.repository;

import com.havi.retailreplenishment.entity.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {

    Optional<AgentRun> findByRunUuid(String runUuid);

    @Query("SELECT ar FROM AgentRun ar ORDER BY ar.startedAt DESC")
    List<AgentRun> findRecent();
}
