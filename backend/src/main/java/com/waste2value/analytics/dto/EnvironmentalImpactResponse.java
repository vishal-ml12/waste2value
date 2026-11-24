package com.waste2value.analytics.dto;

import com.waste2value.analytics.EnvironmentalImpactCalculator.CategoryImpact;

import java.math.BigDecimal;
import java.util.List;

public record EnvironmentalImpactResponse(
        BigDecimal totalRecycledKg,
        BigDecimal totalCo2AvoidedKg,
        BigDecimal totalCo2AvoidedMt,
        BigDecimal totalLandfillDivertedKg,
        List<CategoryImpact> categoryImpacts,
        List<String> unconfiguredCategories,
        String methodology,
        String disclaimer
) {}
