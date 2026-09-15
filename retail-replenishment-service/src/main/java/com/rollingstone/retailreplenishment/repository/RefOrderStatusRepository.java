package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.RefOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefOrderStatusRepository extends JpaRepository<RefOrderStatus, String> {
}
