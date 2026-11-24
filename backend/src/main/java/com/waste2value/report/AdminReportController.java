package com.waste2value.report;

import com.waste2value.report.dto.ReportResponse;
import com.waste2value.report.dto.UpdateReportStatusRequest;
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
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Reports & Disputes", description = "Admin resolution of disputes and platform moderation")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @Operation(summary = "List all reports/disputes (Filterable by status)")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getAllReports(status, pageable)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update report status (RESOLVED, DISMISSED, UNDER_REVIEW) with resolution note")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReportStatus(
            @AuthenticationPrincipal CustomUserDetails adminDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportStatusRequest request
    ) {
        ReportResponse response = reportService.updateReportStatus(adminDetails.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Report status updated to " + request.status()));
    }
}
