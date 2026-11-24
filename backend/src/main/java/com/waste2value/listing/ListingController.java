package com.waste2value.listing;

import com.waste2value.listing.dto.*;
import com.waste2value.shared.ApiResponse;
import com.waste2value.shared.PageResponse;
import com.waste2value.user.CustomUserDetails;
import com.waste2value.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/listings")
@Tag(name = "Waste Listings", description = "Marketplace waste listing operations and search")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('GENERATOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Generator: Create a new waste listing in DRAFT status")
    public ResponseEntity<ApiResponse<ListingResponse>> createListing(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateListingRequest request) {
        ListingResponse created = listingService.createListing(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Listing created as DRAFT"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get listing details by ID (Public if POSTED, or Owner/Admin)")
    public ResponseEntity<ApiResponse<ListingResponse>> getListing(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        boolean isAdmin = userDetails != null && userDetails.getRole() == Role.ADMIN;
        ListingResponse response = listingService.getListing(id, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GENERATOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Generator: Update DRAFT listing")
    public ResponseEntity<ApiResponse<ListingResponse>> updateListing(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateListingRequest request) {
        ListingResponse updated = listingService.updateListing(id, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Listing updated successfully"));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('GENERATOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Generator: Publish DRAFT listing to marketplace (POSTED)")
    public ResponseEntity<ApiResponse<ListingResponse>> publishListing(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ListingResponse published = listingService.publishListing(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(published, "Listing published successfully"));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('GENERATOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Generator: Cancel a listing (DRAFT or POSTED)")
    public ResponseEntity<ApiResponse<ListingResponse>> cancelListing(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ListingResponse cancelled = listingService.cancelListing(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(cancelled, "Listing cancelled"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('GENERATOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Generator: List my listings with pagination and status filter")
    public ResponseEntity<ApiResponse<PageResponse<ListingSummaryResponse>>> getMyListings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ListingStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ListingSummaryResponse> page = listingService.getMyListings(userDetails.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping
    @Operation(summary = "Public: Browse and search active marketplace waste listings")
    public ResponseEntity<ApiResponse<PageResponse<ListingSummaryResponse>>> searchListings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) BigDecimal minQuantity,
            @RequestParam(required = false) BigDecimal maxQuantity,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(required = false) String sortBy,
            @PageableDefault(size = 12) Pageable pageable) {

        ListingSearchCriteria criteria = new ListingSearchCriteria(
                keyword, categoryId, city, state, minQuantity, maxQuantity,
                minPrice, maxPrice, ListingStatus.POSTED, latitude, longitude, maxDistanceKm, sortBy
        );

        PageResponse<ListingSummaryResponse> response = listingService.searchListings(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('GENERATOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Generator: Upload image for listing (max 5)")
    public ResponseEntity<ApiResponse<ListingImageResponse>> uploadImage(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        ListingImageResponse response = listingService.addImageToListing(id, userDetails.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Image uploaded successfully"));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasRole('GENERATOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Generator: Delete image from DRAFT listing")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        listingService.removeImageFromListing(id, imageId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.successMessage("Image deleted successfully"));
    }
}
