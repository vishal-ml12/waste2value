package com.waste2value.review.dto;

import com.waste2value.shared.PageResponse;
import java.math.BigDecimal;

public record ReviewSummaryResponse(
        Long targetUserId,
        BigDecimal avgRating,
        long totalReviews,
        PageResponse<ReviewResponse> reviews
) {}
