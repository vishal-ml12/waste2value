package com.waste2value.collector;

import com.waste2value.collector.dto.CollectorProfileRequest;
import com.waste2value.collector.dto.CollectorProfileResponse;
import com.waste2value.collector.dto.CollectorPublicResponse;
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

@RestController
@RequestMapping("/api/v1/collectors")
@Tag(name = "Collectors", description = "Collector profile endpoints")
public class CollectorController {

    private final CollectorProfileService collectorProfileService;

    public CollectorController(CollectorProfileService collectorProfileService) {
        this.collectorProfileService = collectorProfileService;
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('COLLECTOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create or update current collector profile")
    public ResponseEntity<ApiResponse<CollectorProfileResponse>> createOrUpdateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CollectorProfileRequest request
    ) {
        CollectorProfileResponse response = collectorProfileService.createOrUpdateProfile(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response, "Collector profile saved successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('COLLECTOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get current collector profile")
    public ResponseEntity<ApiResponse<CollectorProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(collectorProfileService.getMyProfile(userDetails.getId())));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('COLLECTOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update current collector profile")
    public ResponseEntity<ApiResponse<CollectorProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CollectorProfileRequest request
    ) {
        CollectorProfileResponse response = collectorProfileService.createOrUpdateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Collector profile updated successfully"));
    }

    @GetMapping
    @Operation(summary = "List verified collectors (Public)")
    public ResponseEntity<ApiResponse<PageResponse<CollectorPublicResponse>>> getPublicCollectors(
            @PageableDefault(size = 20, sort = "avgRating", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(collectorProfileService.getPublicCollectors(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public collector details by profile ID")
    public ResponseEntity<ApiResponse<CollectorPublicResponse>> getPublicCollector(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(collectorProfileService.getPublicCollector(id)));
    }
}
