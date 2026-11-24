package com.waste2value.report;

import com.waste2value.report.dto.CreateReportRequest;
import com.waste2value.report.dto.ReportResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Reports & Disputes", description = "User reports, issue flagging, and dispute submissions")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @Operation(summary = "Submit a report or dispute regarding a listing, pickup, transaction, or user")
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateReportRequest request
    ) {
        ReportResponse response = reportService.createReport(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Report submitted successfully for admin review"));
    }

    @GetMapping("/my")
    @Operation(summary = "List reports submitted by current user")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getMyReports(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getMyReports(userDetails.getId(), pageable)));
    }
}
