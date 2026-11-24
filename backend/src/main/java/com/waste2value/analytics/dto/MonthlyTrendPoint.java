package com.waste2value.analytics.dto;

import java.math.BigDecimal;

public record MonthlyTrendPoint(
        String month, // Format: YYYY-MM (e.g. 2026-01)
        BigDecimal collectedKg,
        BigDecimal recycledKg,
        BigDecimal co2AvoidedKg,
        BigDecimal landfillDivertedKg,
        long transactionsCount,
        long receiptsCount
) {}
