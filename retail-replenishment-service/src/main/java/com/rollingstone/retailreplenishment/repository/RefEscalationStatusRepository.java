package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.RefEscalationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefEscalationStatusRepository extends JpaRepository<RefEscalationStatus, String> {
}
