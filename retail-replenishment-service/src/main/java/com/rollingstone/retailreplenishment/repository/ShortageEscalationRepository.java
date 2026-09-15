package com.rollingstone.retailreplenishment.repository;

import com.rollingstone.retailreplenishment.entity.ShortageEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShortageEscalationRepository extends JpaRepository<ShortageEscalation, Long> {

    @Query("""
        SELECT e FROM ShortageEscalation e
        JOIN FETCH e.store
        JOIN FETCH e.product
        JOIN FETCH e.status st
        WHERE (:storeId IS NULL OR e.store.storeId = :storeId)
          AND (:statusCode IS NULL OR st.statusCode = :statusCode)
        ORDER BY e.createdAt DESC
        """)
    List<ShortageEscalation> search(@Param("storeId") Long storeId, @Param("statusCode") String statusCode);
}
