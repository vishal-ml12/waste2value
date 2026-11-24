package com.waste2value.matching.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProcessorRecommendationResponse(
        Long processorId,
        Long userId,
        String companyName,
        String licenseNumber,
        boolean isVerified,
        BigDecimal avgRating,
        BigDecimal maxCapacityKg,
        BigDecimal currentLoadKg,
        BigDecimal availableCapacityKg,
        String categoryName,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        BigDecimal pricePerUnit,
        Double distanceKm,
        boolean isDistanceAvailable,
        double suitabilityScore,
        List<String> matchReasons
) {}
