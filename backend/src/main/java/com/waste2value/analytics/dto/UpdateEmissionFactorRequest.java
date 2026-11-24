package com.waste2value.analytics.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateEmissionFactorRequest(
        @DecimalMin(value = "0.0000", message = "CO2 factor must be non-negative if specified")
        BigDecimal co2FactorKgPerUnit,

        @NotNull(message = "Landfill diversion rate is required")
        @DecimalMin(value = "0.0000", message = "Landfill diversion rate must be between 0.0000 and 1.0000")
        BigDecimal landfillDiversionRate,

        @NotBlank(message = "Source reference is required")
        String sourceReference,

        String methodologyNotes,

        Boolean isActive
) {}
