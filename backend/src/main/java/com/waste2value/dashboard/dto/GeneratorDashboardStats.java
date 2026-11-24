package com.waste2value.dashboard.dto;

import com.waste2value.listing.dto.ListingSummaryResponse;
import java.math.BigDecimal;
import java.util.List;

public record GeneratorDashboardStats(
        long activeListingsCount,
        long draftListingsCount,
        long completedListingsCount,
        long cancelledListingsCount,
        long totalListingsCount,
        BigDecimal totalQuantityListed,
        List<ListingSummaryResponse> recentListings
) {
}
