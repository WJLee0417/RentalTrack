package com.rentaltrack.forecast.repository;

import com.rentaltrack.forecast.domain.InventoryRecommendation;
import com.rentaltrack.forecast.domain.RecommendationStatus;
import com.rentaltrack.forecast.domain.RiskLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRecommendationRepository extends JpaRepository<InventoryRecommendation, Long> {

    @EntityGraph(attributePaths = {
            "demandForecast",
            "demandForecast.modelRun",
            "demandForecast.rentalLocation",
            "demandForecast.productCategory"
    })
    @Query("""
            select ir
            from InventoryRecommendation ir
            where (:status is null or ir.status = :status)
              and (:riskLevel is null or ir.riskLevel = :riskLevel)
              and (:locationId is null or ir.demandForecast.rentalLocation.id = :locationId)
              and (:categoryId is null or ir.demandForecast.productCategory.id = :categoryId)
            """)
    Page<InventoryRecommendation> findAllFiltered(
            @Param("status") RecommendationStatus status,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("locationId") Long locationId,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "demandForecast",
            "demandForecast.modelRun",
            "demandForecast.rentalLocation",
            "demandForecast.productCategory"
    })
    @Query("select ir from InventoryRecommendation ir where ir.id = :id")
    Optional<InventoryRecommendation> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "demandForecast",
            "demandForecast.modelRun",
            "demandForecast.rentalLocation",
            "demandForecast.productCategory"
    })
    @Query("select ir from InventoryRecommendation ir where ir.id = :id")
    Optional<InventoryRecommendation> findDetailByIdForUpdate(@Param("id") Long id);
}
