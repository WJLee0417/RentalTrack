package com.rentaltrack.forecast.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_recommendation")
public class InventoryRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demand_forecast_id", nullable = false, unique = true)
    private DemandForecast demandForecast;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "safety_stock", nullable = false)
    private int safetyStock;

    @Column(name = "recommended_additional_quantity", nullable = false)
    private int recommendedAdditionalQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecommendationStatus status;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected InventoryRecommendation() {
    }

    public void decide(
            RecommendationStatus decision,
            LocalDateTime decidedAt
    ) {
        if (this.status != RecommendationStatus.OPEN) {
            throw new IllegalStateException("Recommendation decision is already finalized");
        }

        if (decision == null || decision == RecommendationStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Recommendation decision must be ACCEPTED or DISMISSED"
            );
        }

        this.status = decision;
        this.decidedAt = decidedAt;
    }

    public Long getId() {
        return id;
    }

    public DemandForecast getDemandForecast() {
        return demandForecast;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getSafetyStock() {
        return safetyStock;
    }

    public int getRecommendedAdditionalQuantity() {
        return recommendedAdditionalQuantity;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
