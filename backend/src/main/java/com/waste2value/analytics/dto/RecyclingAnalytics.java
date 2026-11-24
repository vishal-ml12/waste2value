package com.waste2value.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecyclingAnalytics(
        BigDecimal totalRecycledKg,
        long confirmedReceiptsCount,
        long activeFacilitiesCount,
        List<CategoryAnalyticsPoint> categoryBreakdown,
        List<ProcessorAnalyticsPoint> processorBreakdown,
        List<MonthlyTrendPoint> monthlyTrends,
        Instant startDate,
        Instant endDate
) {}
