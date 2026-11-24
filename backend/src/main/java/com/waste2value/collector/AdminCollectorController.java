package com.waste2value.collector;

import com.waste2value.collector.dto.AdminVerifyCollectorRequest;
import com.waste2value.collector.dto.CollectorProfileResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/collectors")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Collectors", description = "Admin collector verification and management")
public class AdminCollectorController {

    private final CollectorProfileService collectorProfileService;

    public AdminCollectorController(CollectorProfileService collectorProfileService) {
        this.collectorProfileService = collectorProfileService;
    }

    @GetMapping("/pending")
    @Operation(summary = "List unverified/pending collector profiles")
    public ResponseEntity<ApiResponse<PageResponse<CollectorProfileResponse>>> getPendingCollectors(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(collectorProfileService.getPendingCollectors(pageable)));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Approve or revoke verification for a collector profile")
    public ResponseEntity<ApiResponse<CollectorProfileResponse>> verifyCollector(
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @PathVariable Long id,
            @Valid @RequestBody AdminVerifyCollectorRequest request
    ) {
        CollectorProfileResponse response = collectorProfileService.verifyCollector(
                adminDetails.getId(),
                id,
                request.verified(),
                request.note()
        );
        String action = Boolean.TRUE.equals(request.verified()) ? "verified" : "unverified";
        return ResponseEntity.ok(ApiResponse.success(response, "Collector profile " + action + " successfully"));
    }
}
