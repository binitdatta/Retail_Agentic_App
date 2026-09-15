package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.ReplenishmentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReplenishmentOrderRepository extends JpaRepository<ReplenishmentOrder, Long> {

    @Query("""
        SELECT DISTINCT o FROM ReplenishmentOrder o
        JOIN FETCH o.store
        JOIN FETCH o.supplier
        JOIN FETCH o.status
        LEFT JOIN FETCH o.lines l
        LEFT JOIN FETCH l.product
        WHERE o.replenishmentOrderId = :id
        """)
    Optional<ReplenishmentOrder> findByIdWithLines(@Param("id") Long id);

    @Query("""
        SELECT o FROM ReplenishmentOrder o
        JOIN FETCH o.store
        JOIN FETCH o.supplier
        JOIN FETCH o.status st
        WHERE (:storeId IS NULL OR o.store.storeId = :storeId)
          AND (:statusCode IS NULL OR st.statusCode = :statusCode)
        ORDER BY o.createdAt DESC
        """)
    List<ReplenishmentOrder> search(@Param("storeId") Long storeId, @Param("statusCode") String statusCode);
}
