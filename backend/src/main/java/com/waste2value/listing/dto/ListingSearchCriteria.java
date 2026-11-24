package com.waste2value.listing.dto;

import com.waste2value.listing.ListingStatus;
import java.math.BigDecimal;

public record ListingSearchCriteria(
        String keyword,
        Long categoryId,
        String city,
        String state,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ListingStatus status,
        Double latitude,
        Double longitude,
        Double maxDistanceKm,
        String sortBy
) {
}
