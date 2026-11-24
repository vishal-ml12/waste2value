package com.waste2value.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record WasteCollectionAnalytics(
        BigDecimal totalCollectedKg,
        long completedTransactionsCount,
        BigDecimal totalTransactionValue,
        List<CategoryAnalyticsPoint> categoryBreakdown,
        List<MonthlyTrendPoint> monthlyTrends,
        Instant startDate,
        Instant endDate
) {}
