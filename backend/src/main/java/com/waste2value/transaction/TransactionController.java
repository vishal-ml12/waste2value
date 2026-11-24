package com.waste2value.transaction;

import com.waste2value.shared.ApiResponse;
import com.waste2value.shared.PageResponse;
import com.waste2value.transaction.dto.TransactionResponse;
import com.waste2value.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Transactions", description = "Waste transfer and financial record management")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/my")
    @Operation(summary = "Get user transactions (Generator, Collector, or Processor)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getMyTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getMyTransactions(userDetails.getId(), userDetails.getRole(), pageable)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getTransactionById(userDetails.getId(), userDetails.getRole(), id)
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all platform transactions (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAllTransactions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getAllTransactions(pageable)));
    }
}
