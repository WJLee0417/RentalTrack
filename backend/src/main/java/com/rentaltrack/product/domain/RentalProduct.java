package com.rentaltrack.product.domain;

import com.rentaltrack.global.domain.BaseTimeEntity;
import com.rentaltrack.location.domain.RentalLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "rental_product")
public class RentalProduct extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_code", nullable = false, unique = true, length = 50)
    private String assetCode;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_location_id", nullable = false)
    private RentalLocation currentLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RentalProductStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RentalProduct() {
    }

    public RentalProduct(
            String assetCode,
            String displayName,
            ProductCategory category,
            RentalLocation currentLocation
    ) {
        this.assetCode = assetCode;
        this.displayName = displayName;
        this.category = category;
        this.currentLocation = currentLocation;
        this.status = RentalProductStatus.AVAILABLE;
    }

    public void reserve() {
        requireStatus(RentalProductStatus.AVAILABLE, "Only AVAILABLE products can be reserved");
        this.status = RentalProductStatus.RESERVED;
    }

    public void startRental() {
        requireStatus(RentalProductStatus.RESERVED, "Only RESERVED products can start rental");
        this.status = RentalProductStatus.RENTED;
    }

    private void requireStatus(RentalProductStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message);
        }
    }

    public Long getId() {
        return id;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public RentalLocation getCurrentLocation() {
        return currentLocation;
    }

    public RentalProductStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}
