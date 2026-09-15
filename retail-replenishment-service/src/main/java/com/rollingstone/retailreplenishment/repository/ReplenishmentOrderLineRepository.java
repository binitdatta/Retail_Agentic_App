package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.ReplenishmentOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplenishmentOrderLineRepository extends JpaRepository<ReplenishmentOrderLine, Long> {
}
