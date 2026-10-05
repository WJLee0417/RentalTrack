package com.rentaltrack.damage.domain;

import com.rentaltrack.condition.domain.ConditionRecord;
import com.rentaltrack.global.domain.BaseTimeEntity;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "damage_record")
public class DamageRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condition_record_id", nullable = false)
    private ConditionRecord conditionRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", nullable = false, length = 30)
    private DamageType damageType;

    @Column(name = "damage_location", nullable = false, length = 100)
    private String damageLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private DamageSeverity severity;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "comparison_result", nullable = false, length = 20)
    private DamageComparisonResult comparisonResult;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_before_damage_id", unique = true)
    private DamageRecord matchedBeforeDamage;

    protected DamageRecord() {
    }

    public DamageRecord(
            ConditionRecord conditionRecord,
            DamageType damageType,
            String damageLocation,
            DamageSeverity severity,
            String description,
            DamageComparisonResult comparisonResult
    ) {
        this.conditionRecord = conditionRecord;
        this.damageType = damageType;
        this.damageLocation = damageLocation;
        this.severity = severity;
        this.description = description;
        this.comparisonResult = comparisonResult;
    }

    public void markExisting(DamageRecord beforeDamage) {
        if (beforeDamage == null) {
            throw new IllegalArgumentException("beforeDamage must not be null");
        }
        if (beforeDamage == this || (id != null && id.equals(beforeDamage.id))) {
            throw new IllegalArgumentException("damage record cannot match itself");
        }
        this.comparisonResult = DamageComparisonResult.EXISTING;
        this.matchedBeforeDamage = beforeDamage;
    }

    public void markNew() {
        this.comparisonResult = DamageComparisonResult.NEW;
        this.matchedBeforeDamage = null;
    }

    public Long getId() {
        return id;
    }

    public ConditionRecord getConditionRecord() {
        return conditionRecord;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public String getDamageLocation() {
        return damageLocation;
    }

    public DamageSeverity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public DamageComparisonResult getComparisonResult() {
        return comparisonResult;
    }

    public DamageRecord getMatchedBeforeDamage() {
        return matchedBeforeDamage;
    }
}
