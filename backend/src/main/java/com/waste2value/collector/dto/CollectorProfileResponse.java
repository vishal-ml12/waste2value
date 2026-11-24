package com.waste2value.collector.dto;

import com.waste2value.collector.CollectorProfile;
import java.math.BigDecimal;
import java.time.Instant;

public record CollectorProfileResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String vehicleType,
        String serviceArea,
        BigDecimal maxDistanceKm,
        boolean isVerified,
        BigDecimal avgRating,
        int totalPickups,
        Instant verifiedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static CollectorProfileResponse from(CollectorProfile p) {
        return new CollectorProfileResponse(
                p.getId(),
                p.getUser().getId(),
                p.getUser().getFirstName(),
                p.getUser().getLastName(),
                p.getUser().getEmail(),
                p.getUser().getPhone(),
                p.getVehicleType(),
                p.getServiceArea(),
                p.getMaxDistanceKm(),
                p.isVerified(),
                p.getAvgRating(),
                p.getTotalPickups(),
                p.getVerifiedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
