package com.rentaltrack.forecast.domain;

import com.rentaltrack.location.domain.RentalLocation;
import com.rentaltrack.product.domain.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demand_forecast")
public class DemandForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_run_id", nullable = false)
    private ModelRun modelRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_location_id", nullable = false)
    private RentalLocation rentalLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_category_id", nullable = false)
    private ProductCategory productCategory;

    @Column(name = "forecast_start_at", nullable = false)
    private LocalDateTime forecastStartAt;

    @Column(name = "forecast_end_at", nullable = false)
    private LocalDateTime forecastEndAt;

    @Column(name = "predicted_demand", nullable = false, precision = 18, scale = 6)
    private BigDecimal predictedDemand;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DemandForecast() {
    }

    public Long getId() {
        return id;
    }

    public ModelRun getModelRun() {
        return modelRun;
    }

    public RentalLocation getRentalLocation() {
        return rentalLocation;
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public LocalDateTime getForecastStartAt() {
        return forecastStartAt;
    }

    public LocalDateTime getForecastEndAt() {
        return forecastEndAt;
    }

    public BigDecimal getPredictedDemand() {
        return predictedDemand;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
