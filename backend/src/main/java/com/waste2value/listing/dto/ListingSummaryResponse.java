package com.waste2value.listing.dto;

import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ListingSummaryResponse(
        Long id,
        Long generatorId,
        String generatorName,
        Long categoryId,
        String categoryName,
        String title,
        BigDecimal quantity,
        String unit,
        BigDecimal expectedPrice,
        String city,
        String state,
        ListingStatus status,
        String primaryImageUrl,
        Double distanceKm,
        Instant createdAt
) {
    public static ListingSummaryResponse from(Listing listing) {
        return from(listing, null);
    }

    public static ListingSummaryResponse from(Listing listing, Double distanceKm) {
        String primaryImage = listing.getImages().isEmpty()
                ? null
                : "/api/v1/files/" + listing.getImages().get(0).getFileName();

        return new ListingSummaryResponse(
                listing.getId(),
                listing.getGenerator().getId(),
                listing.getGenerator().getFirstName() + " " + listing.getGenerator().getLastName(),
                listing.getCategory().getId(),
                listing.getCategory().getName(),
                listing.getTitle(),
                listing.getQuantity(),
                listing.getUnit(),
                listing.getExpectedPrice(),
                listing.getAddress().getCity(),
                listing.getAddress().getState(),
                listing.getStatus(),
                primaryImage,
                distanceKm,
                listing.getCreatedAt()
        );
    }
}
