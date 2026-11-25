package com.waste2value.routing.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Detailed stop in a planned pickup route.
 */
public record PickupStopResponse(
        int stopSequence,
        Long pickupRequestId,
        Long listingId,
        String listingTitle,
        String categoryName,
        String categorySlug,
        BigDecimal quantity,
        String unit,
        BigDecimal proposedPrice,
        Long generatorId,
        String generatorName,
        String generatorPhone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean hasValidCoordinates,
        String coordinateStatusMessage,
        Double legDistanceKm,
        Double cumulativeDistanceKm,
        boolean isCoLocated,
        Integer coLocatedStopSequence,
        String status,
        Instant requestedAt
) {}
