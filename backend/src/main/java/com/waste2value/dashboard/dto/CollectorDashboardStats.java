package com.waste2value.dashboard.dto;

import com.waste2value.pickup.dto.PickupRequestResponse;
import java.math.BigDecimal;
import java.util.List;

public record CollectorDashboardStats(
        boolean isProfileComplete,
        boolean isVerified,
        long availableListingsCount,
        long activeRequestsCount,
        long acceptedPickupsCount,
        long completedPickupsCount,
        BigDecimal totalQuantityCollected,
        BigDecimal totalTransactionValue,
        BigDecimal avgRating,
        List<PickupRequestResponse> recentRequests
) {}
