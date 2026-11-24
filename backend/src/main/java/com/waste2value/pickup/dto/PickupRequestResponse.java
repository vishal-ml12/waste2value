package com.waste2value.pickup.dto;

import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PickupRequestResponse(
        Long id,
        Long listingId,
        String listingTitle,
        String categoryName,
        BigDecimal listingQuantity,
        String unit,
        BigDecimal expectedPrice,
        Long generatorId,
        String generatorName,
        String generatorEmail,
        String generatorPhone,
        String pickupAddress,
        String pickupCity,
        String pickupState,
        Long collectorId,
        String collectorName,
        String collectorEmail,
        String collectorPhone,
        PickupRequestStatus status,
        BigDecimal proposedPrice,
        String note,
        BigDecimal actualQuantity,
        String completionNote,
        Instant requestedAt,
        Instant respondedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static PickupRequestResponse from(PickupRequest pr) {
        String genName = pr.getListing().getGenerator().getFirstName() + " " + pr.getListing().getGenerator().getLastName();
        String colName = pr.getCollector().getFirstName() + " " + pr.getCollector().getLastName();
        String addressStr = pr.getListing().getAddress().getAddressLine1();
        if (pr.getListing().getAddress().getAddressLine2() != null && !pr.getListing().getAddress().getAddressLine2().isBlank()) {
            addressStr += ", " + pr.getListing().getAddress().getAddressLine2();
        }

        return new PickupRequestResponse(
                pr.getId(),
                pr.getListing().getId(),
                pr.getListing().getTitle(),
                pr.getListing().getCategory().getName(),
                pr.getListing().getQuantity(),
                pr.getListing().getUnit(),
                pr.getListing().getExpectedPrice(),
                pr.getListing().getGenerator().getId(),
                genName,
                pr.getListing().getGenerator().getEmail(),
                pr.getListing().getGenerator().getPhone(),
                addressStr,
                pr.getListing().getAddress().getCity(),
                pr.getListing().getAddress().getState(),
                pr.getCollector().getId(),
                colName,
                pr.getCollector().getEmail(),
                pr.getCollector().getPhone(),
                pr.getStatus(),
                pr.getProposedPrice(),
                pr.getNote(),
                pr.getActualQuantity(),
                pr.getCompletionNote(),
                pr.getRequestedAt(),
                pr.getRespondedAt(),
                pr.getCompletedAt(),
                pr.getCreatedAt(),
                pr.getUpdatedAt()
        );
    }
}
