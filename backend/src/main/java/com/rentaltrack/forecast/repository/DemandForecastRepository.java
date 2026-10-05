package com.rentaltrack.forecast.repository;

import com.rentaltrack.forecast.domain.DemandForecast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {

    @EntityGraph(attributePaths = {"modelRun", "rentalLocation", "productCategory"})
    @Query("""
            select df
            from DemandForecast df
            where (:modelRunId is null or df.modelRun.id = :modelRunId)
              and (:locationId is null or df.rentalLocation.id = :locationId)
              and (:categoryId is null or df.productCategory.id = :categoryId)
            """)
    Page<DemandForecast> findAllFiltered(
            @Param("modelRunId") Long modelRunId,
            @Param("locationId") Long locationId,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"modelRun", "rentalLocation", "productCategory"})
    @Query("select df from DemandForecast df where df.id = :id")
    Optional<DemandForecast> findDetailById(@Param("id") Long id);
}
