package com.waste2value.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProcessorAnalyticsResponse(
        Long processorProfileId,
        String companyName,
        BigDecimal totalProcessedKg,
        long confirmedReceiptsCount,
        BigDecimal maxCapacityKg,
        BigDecimal currentLoadKg,
        BigDecimal capacityUtilizationPercent,
        BigDecimal estimatedCo2AvoidedKg,
        BigDecimal estimatedCo2AvoidedMt,
        BigDecimal estimatedLandfillDivertedKg,
        List<CategoryAnalyticsPoint> categoryDistribution,
        List<MonthlyTrendPoint> monthlyTrends,
        String disclaimer
) {}
