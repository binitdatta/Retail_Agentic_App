package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.RefShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefShipmentStatusRepository extends JpaRepository<RefShipmentStatus, String> {
}
