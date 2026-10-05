package com.rentaltrack.condition.domain;

import com.rentaltrack.global.domain.BaseTimeEntity;
import com.rentaltrack.member.domain.Member;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "condition_record")
public class ConditionRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_contract_id", nullable = false)
    private RentalContract rentalContract;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 20)
    private ConditionRecordType recordType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_member_id")
    private Member recordedByMember;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    protected ConditionRecord() {
    }

    public ConditionRecord(
            RentalContract rentalContract,
            ConditionRecordType recordType,
            Member recordedByMember,
            String summary,
            LocalDateTime recordedAt
    ) {
        this.rentalContract = rentalContract;
        this.recordType = recordType;
        this.recordedByMember = recordedByMember;
        this.summary = summary;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public RentalContract getRentalContract() {
        return rentalContract;
    }

    public ConditionRecordType getRecordType() {
        return recordType;
    }

    public Member getRecordedByMember() {
        return recordedByMember;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
