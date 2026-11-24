package com.waste2value.processor.dto;

import com.waste2value.processor.ProcessorProfile;
import java.math.BigDecimal;

public record ProcessorPublicResponse(
        Long id,
        Long userId,
        String companyName,
        String description,
        BigDecimal maxCapacityKg,
        BigDecimal currentLoadKg,
        BigDecimal availableCapacityKg,
        boolean isVerified,
        BigDecimal avgRating
) {
    public static ProcessorPublicResponse from(ProcessorProfile p) {
        BigDecimal available = p.getMaxCapacityKg().subtract(p.getCurrentLoadKg());
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            available = BigDecimal.ZERO;
        }

        return new ProcessorPublicResponse(
                p.getId(),
                p.getUser().getId(),
                p.getCompanyName(),
                p.getDescription(),
                p.getMaxCapacityKg(),
                p.getCurrentLoadKg(),
                available,
                p.isVerified(),
                p.getAvgRating()
        );
    }
}
