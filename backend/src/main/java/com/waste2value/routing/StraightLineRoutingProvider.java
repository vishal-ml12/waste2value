package com.waste2value.routing;

import com.waste2value.matching.DistanceCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Default routing provider using great-circle Haversine straight-line distance.
 * Does not simulate road turnings, elevation, traffic, or driving times.
 */
@Component
public class StraightLineRoutingProvider implements RoutingProvider {

    private final DistanceCalculator distanceCalculator;

    public StraightLineRoutingProvider(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public Double calculateDistanceKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        return distanceCalculator.calculateDistanceKm(lat1, lon1, lat2, lon2);
    }

    @Override
    public boolean isValidCoordinate(BigDecimal lat, BigDecimal lon) {
        return distanceCalculator.isValidCoordinate(lat, lon);
    }

    @Override
    public String getProviderName() {
        return "STRAIGHT_LINE_HAVERSINE";
    }

    @Override
    public boolean isRoadNetworkAvailable() {
        return false;
    }
}
