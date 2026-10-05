package com.rentaltrack.forecast.repository;

import com.rentaltrack.forecast.domain.ModelRun;
import com.rentaltrack.forecast.domain.ModelRunStatus;
import com.rentaltrack.forecast.domain.ModelRunType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ModelRunRepository extends JpaRepository<ModelRun, Long> {

    @Query("""
            select mr
            from ModelRun mr
            where (:runType is null or mr.runType = :runType)
              and (:status is null or mr.status = :status)
              and (:datasetVersion is null or mr.datasetVersion = :datasetVersion)
            """)
    Page<ModelRun> findAllFiltered(
            @Param("runType") ModelRunType runType,
            @Param("status") ModelRunStatus status,
            @Param("datasetVersion") String datasetVersion,
            Pageable pageable
    );

    Optional<ModelRun> findFirstByStatusOrderByCompletedAtDescIdDesc(
            ModelRunStatus status
    );

    List<ModelRun> findAllByDatasetVersionAndStatusOrderByStartedAtAscIdAsc(
            String datasetVersion,
            ModelRunStatus status
    );
}
