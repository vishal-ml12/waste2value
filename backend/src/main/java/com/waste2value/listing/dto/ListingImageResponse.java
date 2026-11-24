package com.waste2value.listing.dto;

import com.waste2value.listing.ListingImage;
import java.time.Instant;

public record ListingImageResponse(
        Long id,
        String fileName,
        String fileUrl,
        Long fileSize,
        String contentType,
        Instant createdAt
) {
    public static ListingImageResponse from(ListingImage image) {
        String url = "/api/v1/files/" + image.getFileName();
        return new ListingImageResponse(
                image.getId(),
                image.getFileName(),
                url,
                image.getFileSize(),
                image.getContentType(),
                image.getCreatedAt()
        );
    }
}
