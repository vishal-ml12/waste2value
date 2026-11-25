package com.waste2value.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record GeneratorAnalyticsResponse(
        Long generatorId,
        BigDecimal totalListedKg,
        BigDecimal totalTransactedKg,
        long completedTransactionsCount,
        BigDecimal totalEarnings,
        List<CategoryAnalyticsPoint> categoryDistribution,
        BigDecimal confirmedRecycledKg,
        BigDecimal estimatedCo2AvoidedKg,
        BigDecimal estimatedCo2AvoidedMt,
        BigDecimal estimatedLandfillDivertedKg,
        List<MonthlyTrendPoint> monthlyTrends,
        String disclaimer
) {}
