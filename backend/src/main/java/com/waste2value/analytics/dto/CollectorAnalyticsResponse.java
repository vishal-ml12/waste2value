package com.waste2value.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record CollectorAnalyticsResponse(
        Long collectorId,
        BigDecimal totalCollectedKg,
        long completedPickupsCount,
        BigDecimal totalCollectedValue,
        BigDecimal totalDeliveredToProcessorsKg,
        long processorReceiptsCount,
        List<CategoryAnalyticsPoint> categoryDistribution,
        List<MonthlyTrendPoint> monthlyTrends
) {}
