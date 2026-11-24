package com.waste2value.analytics;

import com.waste2value.analytics.dto.*;
import com.waste2value.shared.ApiResponse;
import com.waste2value.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Environmental Analytics", description = "Endpoints for waste collection, recycling, CO2 emission savings and landfill diversion metrics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // =========================================================================
    // ADMIN ENDPOINTS
    // =========================================================================

    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin: Global platform-wide operational and environmental impact overview")
    public ResponseEntity<ApiResponse<AdminOverviewStats>> getAdminOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        AdminOverviewStats stats = analyticsService.getAdminOverview(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(stats, "Platform overview statistics retrieved successfully"));
    }

    @GetMapping("/admin/waste")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin: Detailed waste collection analytics and category trends")
    public ResponseEntity<ApiResponse<WasteCollectionAnalytics>> getWasteCollectionAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) Long categoryId
    ) {
        WasteCollectionAnalytics analytics = analyticsService.getWasteCollectionAnalytics(startDate, endDate, categoryId);
        return ResponseEntity.ok(ApiResponse.success(analytics, "Waste collection analytics retrieved successfully"));
    }

    @GetMapping("/admin/recycling")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin: Detailed recycling and processor facility intake analytics")
    public ResponseEntity<ApiResponse<RecyclingAnalytics>> getRecyclingAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) Long categoryId
    ) {
        RecyclingAnalytics analytics = analyticsService.getRecyclingAnalytics(startDate, endDate, categoryId);
        return ResponseEntity.ok(ApiResponse.success(analytics, "Recycling analytics retrieved successfully"));
    }

    @GetMapping("/admin/environmental-impact")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin: Calculated avoided CO2 emissions and landfill diversion breakdown")
    public ResponseEntity<ApiResponse<EnvironmentalImpactResponse>> getEnvironmentalImpact(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) Long categoryId
    ) {
        EnvironmentalImpactResponse response = analyticsService.getEnvironmentalImpact(startDate, endDate, categoryId);
        return ResponseEntity.ok(ApiResponse.success(response, "Environmental impact calculation retrieved successfully"));
    }

    @PutMapping("/admin/factors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin: Update emission factor values, landfill diversion rate, or methodology reference")
    public ResponseEntity<ApiResponse<EmissionFactorResponse>> updateEmissionFactor(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmissionFactorRequest request
    ) {
        EmissionFactorResponse response = analyticsService.updateEmissionFactor(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Environmental factor updated successfully"));
    }

    // =========================================================================
    // ROLE-SPECIFIC ENDPOINTS
    // =========================================================================

    @GetMapping("/generator/me")
    @PreAuthorize("hasRole('GENERATOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Generator: Personal waste collection stats and estimated environmental contribution")
    public ResponseEntity<ApiResponse<GeneratorAnalyticsResponse>> getGeneratorAnalytics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        GeneratorAnalyticsResponse response = analyticsService.getGeneratorAnalytics(userDetails.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Generator personal analytics retrieved successfully"));
    }

    @GetMapping("/collector/me")
    @PreAuthorize("hasRole('COLLECTOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Collector: Personal collection performance and processor delivery statistics")
    public ResponseEntity<ApiResponse<CollectorAnalyticsResponse>> getCollectorAnalytics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        CollectorAnalyticsResponse response = analyticsService.getCollectorAnalytics(userDetails.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Collector personal analytics retrieved successfully"));
    }

    @GetMapping("/processor/me")
    @PreAuthorize("hasRole('PROCESSOR')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Processor: Facility processed quantities and environmental contribution")
    public ResponseEntity<ApiResponse<ProcessorAnalyticsResponse>> getProcessorAnalytics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        ProcessorAnalyticsResponse response = analyticsService.getProcessorAnalytics(userDetails.getId(), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response, "Processor facility analytics retrieved successfully"));
    }

    // =========================================================================
    // PUBLIC / SHARED ENDPOINTS
    // =========================================================================

    @GetMapping("/trends")
    @Operation(summary = "Public / Shared: Unified monthly collection, recycling, and carbon avoidance timeline")
    public ResponseEntity<ApiResponse<List<MonthlyTrendPoint>>> getUnifiedMonthlyTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        List<MonthlyTrendPoint> trends = analyticsService.getUnifiedMonthlyTrends(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(trends, "Platform monthly trends retrieved successfully"));
    }

    @GetMapping("/categories")
    @Operation(summary = "Public / Shared: Category-wise waste collection, recycling, and CO2 factors distribution")
    public ResponseEntity<ApiResponse<List<CategoryAnalyticsPoint>>> getUnifiedCategoryAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        List<CategoryAnalyticsPoint> categories = analyticsService.getUnifiedCategoryAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(categories, "Category distribution analytics retrieved successfully"));
    }

    @GetMapping("/factors")
    @Operation(summary = "Public / Shared: Documented greenhouse gas emission factors and calculation methodology")
    public ResponseEntity<ApiResponse<List<EmissionFactorResponse>>> getAllEmissionFactors() {
        List<EmissionFactorResponse> factors = analyticsService.getAllEmissionFactors();
        return ResponseEntity.ok(ApiResponse.success(factors, "Environmental emission factors retrieved successfully"));
    }
}
