package com.waste2value.matching;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reusable Haversine distance calculator.
 * Computes approximate straight-line great-circle distance between two geographic coordinates on Earth.
 * NOTE: Straight-line distance is an estimate and does not represent actual road or driving distance.
 */
@Component
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates the approximate straight-line distance between two coordinates in kilometers.
     * Returns null if any coordinate is missing or invalid.
     */
    public Double calculateDistanceKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (!isValidCoordinate(lat1, lon1) || !isValidCoordinate(lat2, lon2)) {
            return null;
        }

        double phi1 = Math.toRadians(lat1.doubleValue());
        double phi2 = Math.toRadians(lat2.doubleValue());
        double deltaPhi = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLambda = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);

        // Guard against floating-point rounding exceeding 1.0
        double c = 2.0 * Math.atan2(Math.sqrt(Math.min(1.0, a)), Math.sqrt(Math.max(0.0, 1.0 - a)));

        double distance = EARTH_RADIUS_KM * c;
        // Round to 1 decimal place (e.g. 12.4 km)
        return BigDecimal.valueOf(distance).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Checks if coordinates are valid and non-null within geographic boundaries (-90..90 for lat, -180..180 for lon).
     * Excludes default (0, 0) coordinates which denote unset or missing values.
     */
    public boolean isValidCoordinate(BigDecimal lat, BigDecimal lon) {
        if (lat == null || lon == null) {
            return false;
        }
        double latitude = lat.doubleValue();
        double longitude = lon.doubleValue();

        // 0.0, 0.0 is Null Island - treated as unspecified coordinates
        if (Math.abs(latitude) < 0.0001 && Math.abs(longitude) < 0.0001) {
            return false;
        }

        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
    }

    /**
     * Determines whether distance between two locations is within the specified maximum distance.
     * If coordinates are missing, returns true (non-blocking for distance, but distance is marked unavailable).
     */
    public boolean isWithinMaxDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2, BigDecimal maxDistanceKm) {
        Double distance = calculateDistanceKm(lat1, lon1, lat2, lon2);
        if (distance == null) {
            return true; // Coordinates unavailable, cannot reject purely on distance
        }
        if (maxDistanceKm == null || maxDistanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return distance <= maxDistanceKm.doubleValue();
    }
}
