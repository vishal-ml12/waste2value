package com.waste2value.analytics.dto;

import java.math.BigDecimal;

public record CategoryAnalyticsPoint(
        Long categoryId,
        String categoryName,
        String categorySlug,
        BigDecimal collectedKg,
        BigDecimal recycledKg,
        BigDecimal co2Factor,
        BigDecimal co2AvoidedKg,
        BigDecimal landfillDivertedKg,
        boolean isFactorConfigured,
        String sourceReference
) {}
