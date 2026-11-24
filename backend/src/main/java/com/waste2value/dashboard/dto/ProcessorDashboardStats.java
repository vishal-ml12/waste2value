package com.waste2value.dashboard.dto;

import com.waste2value.receipt.dto.ProcessorReceiptResponse;
import java.math.BigDecimal;
import java.util.List;

public record ProcessorDashboardStats(
        boolean isProfileComplete,
        boolean isVerified,
        BigDecimal maxCapacityKg,
        BigDecimal currentLoadKg,
        BigDecimal availableCapacityKg,
        int activeCapabilitiesCount,
        long totalReceiptsCount,
        BigDecimal avgRating,
        List<ProcessorReceiptResponse> recentReceipts
) {}
