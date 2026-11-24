package com.waste2value.pickup;

import com.waste2value.pickup.dto.CompletePickupRequest;
import com.waste2value.pickup.dto.CreatePickupRequest;
import com.waste2value.pickup.dto.PickupRequestResponse;
import com.waste2value.pickup.dto.RespondPickupRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Pickup Requests", description = "Collection workflow and pickup negotiation")
public class PickupRequestController {

    private final PickupRequestService pickupRequestService;

    public PickupRequestController(PickupRequestService pickupRequestService) {
        this.pickupRequestService = pickupRequestService;
    }

    @PostMapping("/listings/{listingId}/pickup-requests")
    @PreAuthorize("hasRole('COLLECTOR')")
    @Operation(summary = "Submit a pickup request for a POSTED waste listing")
    public ResponseEntity<ApiResponse<PickupRequestResponse>> createRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long listingId,
            @Valid @RequestBody CreatePickupRequest request
    ) {
        PickupRequestResponse response = pickupRequestService.createPickupRequest(userDetails.getId(), listingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Pickup request submitted successfully"));
    }

    @GetMapping("/listings/{listingId}/pickup-requests")
    @Operation(summary = "List all pickup requests for a specific listing (Listing owner or Admin)")
    public ResponseEntity<ApiResponse<List<PickupRequestResponse>>> getListingRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long listingId
    ) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.getListingRequests(userDetails.getId(), userDetails.getRole(), listingId)));
    }

    @GetMapping("/pickup-requests/my")
    @PreAuthorize("hasRole('COLLECTOR')")
    @Operation(summary = "Get current collector's submitted pickup requests")
    public ResponseEntity<ApiResponse<PageResponse<PickupRequestResponse>>> getMyRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) PickupRequestStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.getMyRequests(userDetails.getId(), status, pageable)));
    }

    @GetMapping("/pickup-requests/received")
    @PreAuthorize("hasRole('GENERATOR')")
    @Operation(summary = "Get all pickup requests received by current generator across all listings")
    public ResponseEntity<ApiResponse<PageResponse<PickupRequestResponse>>> getReceivedRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) PickupRequestStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.getReceivedRequestsForGenerator(userDetails.getId(), status, pageable)));
    }

    @GetMapping("/pickup-requests/{id}")
    @Operation(summary = "Get pickup request detail by ID (Authorized participants or Admin)")
    public ResponseEntity<ApiResponse<PickupRequestResponse>> getRequestById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.getRequestById(userDetails.getId(), userDetails.getRole(), id)));
    }

    @PostMapping("/pickup-requests/{id}/respond")
    @PreAuthorize("hasRole('GENERATOR')")
    @Operation(summary = "Accept or reject a pickup request (Listing owner only)")
    public ResponseEntity<ApiResponse<PickupRequestResponse>> respondToRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody RespondPickupRequest request
    ) {
        PickupRequestResponse response = pickupRequestService.respondToRequest(userDetails.getId(), id, request);
        String action = Boolean.TRUE.equals(request.accept()) ? "accepted and listing allocated" : "declined";
        return ResponseEntity.ok(ApiResponse.success(response, "Pickup request " + action + " successfully"));
    }

    @PostMapping("/pickup-requests/{id}/cancel")
    @PreAuthorize("hasAnyRole('COLLECTOR', 'GENERATOR', 'ADMIN')")
    @Operation(summary = "Cancel a submitted or accepted pickup request (Authorized participant or Admin)")
    public ResponseEntity<ApiResponse<PickupRequestResponse>> cancelRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.cancelRequest(userDetails.getId(), userDetails.getRole(), id), "Pickup request cancelled"));
    }

    @PostMapping("/pickup-requests/{id}/complete")
    @PreAuthorize("hasRole('COLLECTOR')")
    @Operation(summary = "Mark an allocated pickup as completed, recording actual quantity and generating transaction")
    public ResponseEntity<ApiResponse<PickupRequestResponse>> completePickup(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CompletePickupRequest request
    ) {
        PickupRequestResponse response = pickupRequestService.completePickup(userDetails.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Pickup completed and transaction recorded successfully"));
    }

    @GetMapping("/pickup-requests")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all platform pickup requests (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<PickupRequestResponse>>> getAllRequestsForAdmin(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(pickupRequestService.getAllRequestsForAdmin(pageable)));
    }
}
