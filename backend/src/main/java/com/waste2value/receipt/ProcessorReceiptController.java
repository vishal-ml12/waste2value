package com.waste2value.receipt;

import com.waste2value.receipt.dto.CreateProcessorReceiptRequest;
import com.waste2value.receipt.dto.ProcessorReceiptResponse;
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
@RequestMapping("/api/v1/processor-receipts")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Processor Receipts", description = "Material delivery and processing intake management")
public class ProcessorReceiptController {

    private final ProcessorReceiptService processorReceiptService;

    public ProcessorReceiptController(ProcessorReceiptService processorReceiptService) {
        this.processorReceiptService = processorReceiptService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PROCESSOR')")
    @Operation(summary = "Record incoming material receipt from a collector")
    public ResponseEntity<ApiResponse<ProcessorReceiptResponse>> recordReceipt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateProcessorReceiptRequest request
    ) {
        ProcessorReceiptResponse response = processorReceiptService.recordReceipt(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Material receipt recorded successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get material receipt details by ID (Authorized parties or Admin)")
    public ResponseEntity<ApiResponse<ProcessorReceiptResponse>> getReceiptById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(processorReceiptService.getReceiptById(userDetails.getId(), userDetails.getRole(), id)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PROCESSOR')")
    @Operation(summary = "List current processor's material receipts")
    public ResponseEntity<ApiResponse<PageResponse<ProcessorReceiptResponse>>> getMyReceipts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "receivedDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(processorReceiptService.getMyReceipts(userDetails.getId(), pageable)));
    }

    @GetMapping("/deliveries")
    @PreAuthorize("hasRole('COLLECTOR')")
    @Operation(summary = "List current collector's deliveries to processors")
    public ResponseEntity<ApiResponse<PageResponse<ProcessorReceiptResponse>>> getCollectorDeliveries(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "receivedDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(processorReceiptService.getCollectorDeliveries(userDetails.getId(), pageable)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all material receipts (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<ProcessorReceiptResponse>>> getAllReceipts(
            @PageableDefault(size = 20, sort = "receivedDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(processorReceiptService.getAllReceipts(pageable)));
    }
}
