package com.waste2value.routing.dto;

/**
 * Summary metrics of a collector's route status.
 */
public record RouteSummaryResponse(
        Long collectorId,
        int acceptedPickupsCount,
        int stopsWithCoordinatesCount,
        int stopsMissingCoordinatesCount,
        Double estimatedTotalDistanceKm,
        String depotCity,
        String routingMode,
        String disclaimer
) {}
