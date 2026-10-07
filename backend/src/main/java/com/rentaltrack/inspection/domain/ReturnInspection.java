package com.rentaltrack.inspection.domain;

import com.rentaltrack.global.domain.BaseTimeEntity;
import com.rentaltrack.rental.domain.RentalContract;
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
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "return_inspection")
public class ReturnInspection extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_contract_id", nullable = false, unique = true)
    private RentalContract rentalContract;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 30)
    private ReturnInspectionResult result;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "inspected_at", nullable = false)
    private LocalDateTime inspectedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ReturnInspection() {
    }

    public ReturnInspection(
            RentalContract rentalContract,
            ReturnInspectionResult result,
            String note,
            LocalDateTime inspectedAt
    ) {
        this.rentalContract = rentalContract;
        this.result = result;
        this.note = note;
        this.inspectedAt = inspectedAt;
    }

    public void revise(
            ReturnInspectionResult result,
            String note,
            LocalDateTime inspectedAt
    ) {
        this.result = result;
        this.note = note;
        this.inspectedAt = inspectedAt;
    }

    public Long getId() {
        return id;
    }

    public RentalContract getRentalContract() {
        return rentalContract;
    }

    public ReturnInspectionResult getResult() {
        return result;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getInspectedAt() {
        return inspectedAt;
    }

    public long getVersion() {
        return version;
    }
}
