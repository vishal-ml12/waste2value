package com.waste2value.dashboard;

import com.waste2value.dashboard.dto.AdminDashboardStats;
import com.waste2value.dashboard.dto.CollectorDashboardStats;
import com.waste2value.dashboard.dto.GeneratorDashboardStats;
import com.waste2value.dashboard.dto.ProcessorDashboardStats;
import com.waste2value.shared.ApiResponse;
import com.waste2value.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for user and role-specific dashboard metrics")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/generator")
    @PreAuthorize("hasRole('GENERATOR')")
    @Operation(summary = "Generator: Get real dashboard metrics and recent listings")
    public ResponseEntity<ApiResponse<GeneratorDashboardStats>> getGeneratorDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        GeneratorDashboardStats stats = dashboardService.getGeneratorStats(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/collector")
    @PreAuthorize("hasRole('COLLECTOR')")
    @Operation(summary = "Collector: Get real dashboard metrics and recent pickup requests")
    public ResponseEntity<ApiResponse<CollectorDashboardStats>> getCollectorDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CollectorDashboardStats stats = dashboardService.getCollectorStats(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/processor")
    @PreAuthorize("hasRole('PROCESSOR')")
    @Operation(summary = "Processor: Get real dashboard metrics and capacity status")
    public ResponseEntity<ApiResponse<ProcessorDashboardStats>> getProcessorDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProcessorDashboardStats stats = dashboardService.getProcessorStats(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get platform-wide operational overview statistics")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getAdminDashboard() {
        AdminDashboardStats stats = dashboardService.getAdminStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
