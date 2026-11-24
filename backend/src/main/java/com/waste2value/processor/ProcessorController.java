package com.waste2value.processor;

import com.waste2value.processor.dto.*;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/processors")
@Tag(name = "Processors", description = "Processor facility profile and capability endpoints")
public class ProcessorController {

    private final ProcessorService processorService;

    public ProcessorController(ProcessorService processorService) {
        this.processorService = processorService;
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('PROCESSOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create or update current processor profile")
    public ResponseEntity<ApiResponse<ProcessorProfileResponse>> createOrUpdateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProcessorProfileRequest request
    ) {
        ProcessorProfileResponse response = processorService.createOrUpdateProfile(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response, "Processor profile saved successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROCESSOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get current processor profile")
    public ResponseEntity<ApiResponse<ProcessorProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(processorService.getMyProfile(userDetails.getId())));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('PROCESSOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update current processor profile")
    public ResponseEntity<ApiResponse<ProcessorProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProcessorProfileRequest request
    ) {
        ProcessorProfileResponse response = processorService.createOrUpdateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Processor profile updated successfully"));
    }

    // CAPABILITIES
    @GetMapping("/me/capabilities")
    @PreAuthorize("hasRole('PROCESSOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "List current processor capabilities")
    public ResponseEntity<ApiResponse<List<ProcessorCapabilityResponse>>> getMyCapabilities(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(processorService.getMyCapabilities(userDetails.getId())));
    }

    @PostMapping("/me/capabilities")
    @PreAuthorize("hasRole('PROCESSOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Add a category acceptance capability")
    public ResponseEntity<ApiResponse<ProcessorCapabilityResponse>> addCapability(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProcessorCapabilityRequest request
    ) {
        ProcessorCapabilityResponse response = processorService.addCapability(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Capability added successfully"));
    }

    @PutMapping("/me/capabilities/{id}")
    @PreAuthorize("hasRole('PROCESSOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update capability details")
    public ResponseEntity<ApiResponse<ProcessorCapabilityResponse>> updateCapability(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ProcessorCapabilityRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(processorService.updateCapability(userDetails.getId(), id, request), "Capability updated"));
    }

    @DeleteMapping("/me/capabilities/{id}")
    @PreAuthorize("hasRole('PROCESSOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete capability")
    public ResponseEntity<ApiResponse<Void>> deleteCapability(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        processorService.deleteCapability(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.successMessage("Capability deleted successfully"));
    }

    @PatchMapping("/me/capabilities/{id}/active")
    @PreAuthorize("hasRole('PROCESSOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Toggle capability active state")
    public ResponseEntity<ApiResponse<ProcessorCapabilityResponse>> toggleCapability(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(ApiResponse.success(processorService.toggleCapabilityActive(userDetails.getId(), id, active), "Capability active state updated"));
    }

    // PUBLIC
    @GetMapping
    @Operation(summary = "List verified processors (Public)")
    public ResponseEntity<ApiResponse<PageResponse<ProcessorPublicResponse>>> getPublicProcessors(
            @PageableDefault(size = 20, sort = "avgRating", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(processorService.getPublicProcessors(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public processor detail by ID")
    public ResponseEntity<ApiResponse<ProcessorPublicResponse>> getPublicProcessor(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(processorService.getPublicProcessor(id)));
    }

    @GetMapping("/capabilities/category/{categoryId}")
    @Operation(summary = "Get active verified processor capabilities accepting a specific category")
    public ResponseEntity<ApiResponse<List<ProcessorCapabilityResponse>>> getEligibleCapabilities(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.success(processorService.getEligibleCapabilitiesByCategory(categoryId)));
    }
}
