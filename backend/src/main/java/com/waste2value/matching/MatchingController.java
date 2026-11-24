package com.waste2value.matching;

import com.waste2value.matching.dto.CollectorRecommendationResponse;
import com.waste2value.matching.dto.ListingRecommendationResponse;
import com.waste2value.matching.dto.ProcessorRecommendationResponse;
import com.waste2value.shared.ApiResponse;
import com.waste2value.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matching")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Smart Matching & Recommendations", description = "Hybrid rule-based filtering and suitability recommendation engine")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/listings/{listingId}/collectors")
    @PreAuthorize("hasAnyRole('GENERATOR', 'ADMIN')")
    @Operation(summary = "Get recommended certified collectors for a waste listing ranked by suitability score")
    public ResponseEntity<ApiResponse<List<CollectorRecommendationResponse>>> getRecommendedCollectors(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long listingId
    ) {
        List<CollectorRecommendationResponse> response = matchingService.getRecommendedCollectorsForListing(
                userDetails.getId(),
                userDetails.getRole(),
                listingId
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Recommended collectors retrieved successfully"));
    }

    @GetMapping("/listings/{listingId}/processors")
    @PreAuthorize("hasAnyRole('GENERATOR', 'COLLECTOR', 'ADMIN')")
    @Operation(summary = "Get recommended recycling processors for a waste listing ranked by suitability score")
    public ResponseEntity<ApiResponse<List<ProcessorRecommendationResponse>>> getRecommendedProcessors(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long listingId
    ) {
        List<ProcessorRecommendationResponse> response = matchingService.getRecommendedProcessorsForListing(
                userDetails.getId(),
                userDetails.getRole(),
                listingId
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Recommended processors retrieved successfully"));
    }

    @GetMapping("/collectors/me/recommended-listings")
    @PreAuthorize("hasRole('COLLECTOR')")
    @Operation(summary = "Get personalized recommended POSTED waste listings for the current collector")
    public ResponseEntity<ApiResponse<List<ListingRecommendationResponse>>> getCollectorRecommendedListings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ListingRecommendationResponse> response = matchingService.getRecommendedListingsForCollector(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Personalized recommended listings retrieved successfully"));
    }

    @GetMapping("/processors/me/recommended-listings")
    @PreAuthorize("hasRole('PROCESSOR')")
    @Operation(summary = "Get personalized recommended POSTED waste listings for the current processor facility")
    public ResponseEntity<ApiResponse<List<ListingRecommendationResponse>>> getProcessorRecommendedListings(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ListingRecommendationResponse> response = matchingService.getRecommendedListingsForProcessor(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Personalized recommended materials retrieved successfully"));
    }
}
