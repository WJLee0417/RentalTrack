package com.rentaltrack.forecast.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_run")
public class ModelRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type", nullable = false, length = 30)
    private ModelRunType runType;

    @Column(name = "dataset_version", nullable = false, length = 100)
    private String datasetVersion;

    @Column(name = "train_from")
    private LocalDateTime trainFrom;

    @Column(name = "train_to")
    private LocalDateTime trainTo;

    @Column(name = "evaluation_from")
    private LocalDateTime evaluationFrom;

    @Column(name = "evaluation_to")
    private LocalDateTime evaluationTo;

    @Column(name = "mae", precision = 18, scale = 6)
    private BigDecimal mae;

    @Column(name = "rmse", precision = 18, scale = 6)
    private BigDecimal rmse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ModelRunStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ModelRun() {
    }

    public Long getId() {
        return id;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public ModelRunType getRunType() {
        return runType;
    }

    public String getDatasetVersion() {
        return datasetVersion;
    }

    public LocalDateTime getTrainFrom() {
        return trainFrom;
    }

    public LocalDateTime getTrainTo() {
        return trainTo;
    }

    public LocalDateTime getEvaluationFrom() {
        return evaluationFrom;
    }

    public LocalDateTime getEvaluationTo() {
        return evaluationTo;
    }

    public BigDecimal getMae() {
        return mae;
    }

    public BigDecimal getRmse() {
        return rmse;
    }

    public ModelRunStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
