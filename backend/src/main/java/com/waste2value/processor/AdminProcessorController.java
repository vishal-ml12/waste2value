package com.waste2value.processor;

import com.waste2value.processor.dto.AdminVerifyProcessorRequest;
import com.waste2value.processor.dto.ProcessorProfileResponse;
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
@RequestMapping("/api/v1/admin/processors")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Processors", description = "Admin processor verification and monitoring")
public class AdminProcessorController {

    private final ProcessorService processorService;

    public AdminProcessorController(ProcessorService processorService) {
        this.processorService = processorService;
    }

    @GetMapping("/pending")
    @Operation(summary = "List unverified/pending processor profiles")
    public ResponseEntity<ApiResponse<PageResponse<ProcessorProfileResponse>>> getPendingProcessors(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(processorService.getPendingProcessors(pageable)));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Approve or revoke verification for a processor profile")
    public ResponseEntity<ApiResponse<ProcessorProfileResponse>> verifyProcessor(
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @PathVariable Long id,
            @Valid @RequestBody AdminVerifyProcessorRequest request
    ) {
        ProcessorProfileResponse response = processorService.verifyProcessor(
                adminDetails.getId(),
                id,
                request.verified(),
                request.note()
        );
        String action = Boolean.TRUE.equals(request.verified()) ? "verified" : "unverified";
        return ResponseEntity.ok(ApiResponse.success(response, "Processor profile " + action + " successfully"));
    }
}
