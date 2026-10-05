package com.rentaltrack.rental.domain;

import com.rentaltrack.global.domain.BaseTimeEntity;
import com.rentaltrack.location.domain.RentalLocation;
import com.rentaltrack.member.domain.Member;
import com.rentaltrack.product.domain.RentalProduct;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "rental_contract")
public class RentalContract extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_code", nullable = false, unique = true, length = 50)
    private String contractCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_product_id", nullable = false)
    private RentalProduct rentalProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_location_id", nullable = false)
    private RentalLocation pickupLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_location_id")
    private RentalLocation returnLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RentalContractStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "rented_at")
    private LocalDateTime rentedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RentalContract() {
    }

    public RentalContract(
            String contractCode,
            Member member,
            RentalProduct rentalProduct,
            RentalLocation pickupLocation,
            LocalDateTime requestedAt
    ) {
        this.contractCode = contractCode;
        this.member = member;
        this.rentalProduct = rentalProduct;
        this.pickupLocation = pickupLocation;
        this.requestedAt = requestedAt;
        this.status = RentalContractStatus.REQUESTED;
    }

    public void approve(LocalDateTime approvedAt) {
        requireStatus(RentalContractStatus.REQUESTED, "Only REQUESTED rentals can be approved");
        this.status = RentalContractStatus.APPROVED;
        this.approvedAt = approvedAt;
    }

    public void markPickedUp(LocalDateTime pickedUpAt) {
        requireStatus(RentalContractStatus.APPROVED, "Only APPROVED rentals can be picked up");
        this.status = RentalContractStatus.PICKED_UP;
        this.pickedUpAt = pickedUpAt;
    }

    public void startRental(LocalDateTime rentedAt, LocalDateTime dueAt) {
        requireStatus(RentalContractStatus.PICKED_UP, "Only PICKED_UP rentals can start actual use");
        if (dueAt == null || !dueAt.isAfter(rentedAt)) {
            throw new IllegalArgumentException("dueAt must be after rentedAt");
        }
        this.status = RentalContractStatus.IN_USE;
        this.rentedAt = rentedAt;
        this.dueAt = dueAt;
    }

    public void markReturned(RentalLocation returnLocation, LocalDateTime returnedAt) {
        requireStatus(RentalContractStatus.IN_USE, "Only IN_USE rentals can be returned");
        if (returnLocation == null || !returnLocation.isActive()) {
            throw new IllegalArgumentException("Return location must be active");
        }
        if (returnedAt == null || returnedAt.isBefore(rentedAt)) {
            throw new IllegalArgumentException("returnedAt must not be before rentedAt");
        }
        this.returnLocation = returnLocation;
        this.returnedAt = returnedAt;
        this.status = RentalContractStatus.RETURNED;
    }

    public void beginInspection() {
        requireStatus(RentalContractStatus.RETURNED, "Only RETURNED rentals can enter inspection");
        this.status = RentalContractStatus.INSPECTING;
    }

    public void completeInspection(LocalDateTime completedAt) {
        requireStatus(RentalContractStatus.INSPECTING, "Only INSPECTING rentals can be completed");
        this.status = RentalContractStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    private void requireStatus(RentalContractStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message);
        }
    }

    public Long getId() {
        return id;
    }

    public String getContractCode() {
        return contractCode;
    }

    public Member getMember() {
        return member;
    }

    public RentalProduct getRentalProduct() {
        return rentalProduct;
    }

    public RentalLocation getPickupLocation() {
        return pickupLocation;
    }

    public RentalLocation getReturnLocation() {
        return returnLocation;
    }

    public RentalContractStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public LocalDateTime getRentedAt() {
        return rentedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public long getVersion() {
        return version;
    }
}
