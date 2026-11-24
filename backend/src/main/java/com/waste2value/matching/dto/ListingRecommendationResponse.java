package com.waste2value.matching.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ListingRecommendationResponse(
        Long listingId,
        String title,
        Long categoryId,
        String categoryName,
        String categorySlug,
        BigDecimal quantity,
        String unit,
        BigDecimal expectedPrice,
        Long generatorId,
        String generatorName,
        String city,
        String state,
        Double distanceKm,
        boolean isDistanceAvailable,
        double suitabilityScore,
        List<String> matchReasons,
        Instant createdAt
) {}
