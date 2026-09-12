package com.havi.retailreplenishment.repository;

import com.havi.retailreplenishment.entity.ReplenishmentOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplenishmentOrderLineRepository extends JpaRepository<ReplenishmentOrderLine, Long> {
}
