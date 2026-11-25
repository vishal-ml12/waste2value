package com.waste2value.routing.dto;

import java.time.Instant;
import java.util.List;

/**
 * Complete response representing an optimized pickup route plan for a collector.
 */
public record RoutePlanResponse(
        Long collectorId,
        String collectorName,
        boolean isCollectorVerified,
        LocationDto startLocation,
        int totalStopsCount,
        int mappableStopsCount,
        int unmappableStopsCount,
        Double totalEstimatedDistanceKm,
        Double returnLegDistanceKm,
        boolean roundTrip,
        String routingMode,
        String algorithm,
        String disclaimer,
        List<PickupStopResponse> stops,
        Instant generatedAt
) {}
