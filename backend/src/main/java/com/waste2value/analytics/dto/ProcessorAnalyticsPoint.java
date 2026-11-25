package com.waste2value.analytics.dto;

import java.math.BigDecimal;

public record ProcessorAnalyticsPoint(
        Long processorId,
        String companyName,
        String licenseNumber,
        BigDecimal recycledKg,
        long receiptsCount,
        BigDecimal co2AvoidedKg,
        BigDecimal landfillDivertedKg
) {}
