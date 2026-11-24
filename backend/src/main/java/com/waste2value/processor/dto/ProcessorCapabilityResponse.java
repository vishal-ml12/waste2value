package com.waste2value.processor.dto;

import com.waste2value.processor.ProcessorCapability;
import java.math.BigDecimal;
import java.time.Instant;

public record ProcessorCapabilityResponse(
        Long id,
        Long processorId,
        String companyName,
        Long categoryId,
        String categoryName,
        String unit,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        BigDecimal pricePerUnit,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProcessorCapabilityResponse from(ProcessorCapability c) {
        return new ProcessorCapabilityResponse(
                c.getId(),
                c.getProcessor().getId(),
                c.getProcessor().getCompanyName(),
                c.getCategory().getId(),
                c.getCategory().getName(),
                c.getCategory().getUnit(),
                c.getMinQuantity(),
                c.getMaxQuantity(),
                c.getPricePerUnit(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
