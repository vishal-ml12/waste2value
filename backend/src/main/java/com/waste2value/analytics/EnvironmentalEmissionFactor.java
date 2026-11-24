package com.waste2value.analytics;

import com.waste2value.category.Category;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "environmental_emission_factors", uniqueConstraints = {
        @UniqueConstraint(name = "uk_eef_category_effective", columnNames = {"category_id", "effective_from"})
})
public class EnvironmentalEmissionFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "co2_factor_kg_per_unit", precision = 10, scale = 4)
    private BigDecimal co2FactorKgPerUnit;

    @Column(name = "landfill_diversion_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal landfillDiversionRate = BigDecimal.ONE;

    @Column(nullable = false, length = 20)
    private String unit = "kg";

    @Column(name = "source_reference", nullable = false, length = 255)
    private String sourceReference;

    @Column(name = "methodology_notes", columnDefinition = "TEXT")
    private String methodologyNotes;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public EnvironmentalEmissionFactor() {}

    public EnvironmentalEmissionFactor(
            Category category,
            BigDecimal co2FactorKgPerUnit,
            BigDecimal landfillDiversionRate,
            String unit,
            String sourceReference,
            String methodologyNotes,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            boolean isActive
    ) {
        this.category = category;
        this.co2FactorKgPerUnit = co2FactorKgPerUnit;
        this.landfillDiversionRate = landfillDiversionRate != null ? landfillDiversionRate : BigDecimal.ONE;
        this.unit = unit != null ? unit : "kg";
        this.sourceReference = sourceReference;
        this.methodologyNotes = methodologyNotes;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getCo2FactorKgPerUnit() {
        return co2FactorKgPerUnit;
    }

    public void setCo2FactorKgPerUnit(BigDecimal co2FactorKgPerUnit) {
        this.co2FactorKgPerUnit = co2FactorKgPerUnit;
    }

    public BigDecimal getLandfillDiversionRate() {
        return landfillDiversionRate;
    }

    public void setLandfillDiversionRate(BigDecimal landfillDiversionRate) {
        this.landfillDiversionRate = landfillDiversionRate;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public String getMethodologyNotes() {
        return methodologyNotes;
    }

    public void setMethodologyNotes(String methodologyNotes) {
        this.methodologyNotes = methodologyNotes;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
