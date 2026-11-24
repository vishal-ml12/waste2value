package com.waste2value.dashboard.dto;

public record AdminDashboardStats(
        long pendingCollectorsCount,
        long pendingProcessorsCount,
        long totalUsersCount,
        long activeListingsCount,
        long activePickupRequestsCount,
        long totalTransactionsCount,
        long openReportsCount
) {}
