package com.waste2value.processor.dto;

import com.waste2value.processor.ProcessorProfile;
import java.math.BigDecimal;
import java.time.Instant;

public record ProcessorProfileResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String companyName,
        String licenseNumber,
        String description,
        BigDecimal maxCapacityKg,
        BigDecimal currentLoadKg,
        BigDecimal availableCapacityKg,
        boolean isVerified,
        BigDecimal avgRating,
        Instant verifiedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProcessorProfileResponse from(ProcessorProfile p) {
        BigDecimal available = p.getMaxCapacityKg().subtract(p.getCurrentLoadKg());
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            available = BigDecimal.ZERO;
        }

        return new ProcessorProfileResponse(
                p.getId(),
                p.getUser().getId(),
                p.getUser().getFirstName(),
                p.getUser().getLastName(),
                p.getUser().getEmail(),
                p.getUser().getPhone(),
                p.getCompanyName(),
                p.getLicenseNumber(),
                p.getDescription(),
                p.getMaxCapacityKg(),
                p.getCurrentLoadKg(),
                available,
                p.isVerified(),
                p.getAvgRating(),
                p.getVerifiedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
