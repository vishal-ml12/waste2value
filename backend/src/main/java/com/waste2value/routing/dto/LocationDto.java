package com.waste2value.routing.dto;

import java.math.BigDecimal;

/**
 * Representation of a geographic coordinate location (e.g. Collector Depot or Pickup address).
 */
public record LocationDto(
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean isValidCoordinate
) {
    public static LocationDto unconfigured(String name) {
        return new LocationDto(name, null, null, null, null, null, null, null, false);
    }
}
