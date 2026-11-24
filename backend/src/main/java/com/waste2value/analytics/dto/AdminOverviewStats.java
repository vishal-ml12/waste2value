package com.waste2value.analytics.dto;

import java.math.BigDecimal;

public record AdminOverviewStats(
        BigDecimal totalWasteCollectedKg,
        BigDecimal totalWasteRecycledKg,
        BigDecimal totalCo2AvoidedKg,
        BigDecimal totalCo2AvoidedMt,
        BigDecimal totalLandfillDivertedKg,
        long completedTransactionsCount,
        long confirmedReceiptsCount,
        long activeGeneratorsCount,
        long activeCollectorsCount,
        long activeProcessorsCount,
        String disclaimer
) {}
