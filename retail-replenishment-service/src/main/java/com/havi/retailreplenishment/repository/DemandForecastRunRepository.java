package com.havi.retailreplenishment.repository;

import com.havi.retailreplenishment.entity.DemandForecastRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DemandForecastRunRepository extends JpaRepository<DemandForecastRun, Long> {

    @Query("""
        SELECT f FROM DemandForecastRun f
        WHERE f.store.storeId = :storeId AND f.product.productId = :productId
        ORDER BY f.generatedAt DESC
        LIMIT 1
        """)
    Optional<DemandForecastRun> findLatest(@Param("storeId") Long storeId, @Param("productId") Long productId);
}
