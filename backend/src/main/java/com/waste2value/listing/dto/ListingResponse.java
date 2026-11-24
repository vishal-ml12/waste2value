package com.waste2value.listing.dto;

import com.waste2value.listing.Listing;
import com.waste2value.listing.ListingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ListingResponse(
        Long id,
        Long generatorId,
        String generatorName,
        String generatorEmail,
        String generatorPhone,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String parentCategoryName,
        Long addressId,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        String title,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal expectedPrice,
        ListingStatus status,
        Long version,
        List<ListingImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {
    public static ListingResponse from(Listing listing) {
        String parentName = listing.getCategory().getParentCategory() != null
                ? listing.getCategory().getParentCategory().getName()
                : null;

        List<ListingImageResponse> imageList = listing.getImages().stream()
                .map(ListingImageResponse::from)
                .toList();

        return new ListingResponse(
                listing.getId(),
                listing.getGenerator().getId(),
                listing.getGenerator().getFirstName() + " " + listing.getGenerator().getLastName(),
                listing.getGenerator().getEmail(),
                listing.getGenerator().getPhone(),
                listing.getCategory().getId(),
                listing.getCategory().getName(),
                listing.getCategory().getSlug(),
                parentName,
                listing.getAddress().getId(),
                listing.getAddress().getAddressLine1(),
                listing.getAddress().getAddressLine2(),
                listing.getAddress().getCity(),
                listing.getAddress().getState(),
                listing.getAddress().getPostalCode(),
                listing.getAddress().getLatitude(),
                listing.getAddress().getLongitude(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getQuantity(),
                listing.getUnit(),
                listing.getExpectedPrice(),
                listing.getStatus(),
                listing.getVersion(),
                imageList,
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }
}
