package com.waste2value.analytics.dto;

import com.waste2value.analytics.EnvironmentalEmissionFactor;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmissionFactorResponse(
        Long id,
        Long categoryId,
        String categoryName,
        BigDecimal co2FactorKgPerUnit,
        BigDecimal landfillDiversionRate,
        String unit,
        String sourceReference,
        String methodologyNotes,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean isActive
) {
    public static EmissionFactorResponse from(EnvironmentalEmissionFactor factor) {
        return new EmissionFactorResponse(
                factor.getId(),
                factor.getCategory().getId(),
                factor.getCategory().getName(),
                factor.getCo2FactorKgPerUnit(),
                factor.getLandfillDiversionRate(),
                factor.getUnit(),
                factor.getSourceReference(),
                factor.getMethodologyNotes(),
                factor.getEffectiveFrom(),
                factor.getEffectiveTo(),
                factor.isActive()
        );
    }
}
