package com.waste2value.matching.dto;

import java.math.BigDecimal;
import java.util.List;

public record CollectorRecommendationResponse(
        Long collectorId,
        Long userId,
        String collectorName,
        String vehicleType,
        String serviceArea,
        BigDecimal maxDistanceKm,
        boolean isVerified,
        BigDecimal avgRating,
        int totalPickups,
        Double distanceKm,
        boolean isDistanceAvailable,
        double suitabilityScore,
        List<String> matchReasons
) {}
