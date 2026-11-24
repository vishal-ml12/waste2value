package com.waste2value.review;

import com.waste2value.review.dto.CreateReviewRequest;
import com.waste2value.review.dto.ReviewResponse;
import com.waste2value.review.dto.ReviewSummaryResponse;
import com.waste2value.shared.ApiResponse;
import com.waste2value.shared.PageResponse;
import com.waste2value.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Reviews", description = "Ratings, reviews, and trust verification")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Submit a rating & review for a completed waste pickup")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewResponse response = reviewService.submitReview(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Review submitted successfully"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reviews and rating summary for a user profile (Public)")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getUserReviews(
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getUserReviews(userId, pageable)));
    }

    @GetMapping("/my")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get reviews submitted by current user")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMySubmittedReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getMySubmittedReviews(userDetails.getId(), pageable)));
    }
}
