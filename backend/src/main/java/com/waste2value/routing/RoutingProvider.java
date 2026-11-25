package com.waste2value.routing;

import java.math.BigDecimal;

/**
 * Pluggable abstraction for calculating distances between geographical coordinates.
 * Isolates route optimization from specific distance providers (Straight-line vs Road Network).
 */
public interface RoutingProvider {

    /**
     * Calculates the estimated distance between two coordinates in kilometers.
     * Returns null if coordinates are missing or invalid.
     */
    Double calculateDistanceKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2);

    /**
     * Returns true if coordinates are non-null and within valid Earth bounds (-90..90, -180..180, not (0,0)).
     */
    boolean isValidCoordinate(BigDecimal lat, BigDecimal lon);

    /**
     * Name and mode of the routing provider.
     */
    String getProviderName();

    /**
     * True if the provider models real road network topology and turns; false for straight-line Haversine.
     */
    boolean isRoadNetworkAvailable();
}
