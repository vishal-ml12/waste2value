package com.waste2value.review.dto;

import com.waste2value.review.Review;
import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long reviewerId,
        String reviewerName,
        Long targetUserId,
        String targetUserName,
        Long pickupRequestId,
        int rating,
        String comment,
        Instant createdAt
) {
    public static ReviewResponse from(Review r) {
        String revName = r.getReviewer().getFirstName() + " " + r.getReviewer().getLastName().substring(0, 1) + ".";
        String tgtName = r.getTargetUser().getFirstName() + " " + r.getTargetUser().getLastName();

        return new ReviewResponse(
                r.getId(),
                r.getReviewer().getId(),
                revName,
                r.getTargetUser().getId(),
                tgtName,
                r.getPickupRequest().getId(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
