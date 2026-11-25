package com.waste2value.routing.dto;

import java.math.BigDecimal;

/**
 * Request parameters for running route optimization.
 */
public record OptimizeRouteRequest(
        BigDecimal startLatitude,
        BigDecimal startLongitude,
        String startAddress,
        Boolean roundTrip
) {
    public boolean isRoundTripRequested() {
        return roundTrip != null && roundTrip;
    }
}
