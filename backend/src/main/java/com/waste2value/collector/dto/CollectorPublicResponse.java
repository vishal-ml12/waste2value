package com.waste2value.collector.dto;

import com.waste2value.collector.CollectorProfile;
import java.math.BigDecimal;

public record CollectorPublicResponse(
        Long id,
        Long userId,
        String displayName,
        String vehicleType,
        String serviceArea,
        BigDecimal maxDistanceKm,
        boolean isVerified,
        BigDecimal avgRating,
        int totalPickups
) {
    public static CollectorPublicResponse from(CollectorProfile p) {
        String displayName = p.getUser().getFirstName() + " " + p.getUser().getLastName().substring(0, 1) + ".";
        return new CollectorPublicResponse(
                p.getId(),
                p.getUser().getId(),
                displayName,
                p.getVehicleType(),
                p.getServiceArea(),
                p.getMaxDistanceKm(),
                p.isVerified(),
                p.getAvgRating(),
                p.getTotalPickups()
        );
    }
}
