package com.waste2value.address.dto;

import com.waste2value.address.Address;
import java.math.BigDecimal;
import java.time.Instant;

public record AddressResponse(
        Long id,
        Long userId,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getUser().getId(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getLatitude(),
                address.getLongitude(),
                address.isDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
