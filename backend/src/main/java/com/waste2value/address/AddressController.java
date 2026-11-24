package com.waste2value.address;

import com.waste2value.address.dto.AddressRequest;
import com.waste2value.address.dto.AddressResponse;
import com.waste2value.shared.ApiResponse;
import com.waste2value.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@Tag(name = "Address Management", description = "Endpoints for managing user multiple pickup/delivery addresses")
@SecurityRequirement(name = "BearerAuth")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @Operation(summary = "Create a new address for current user")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse created = addressService.createAddress(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created, "Address created successfully"));
    }

    @GetMapping
    @Operation(summary = "List all addresses for current user")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AddressResponse> addresses = addressService.getMyAddresses(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific address by ID (ownership checked)")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        AddressResponse address = addressService.getAddressById(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address (ownership checked)")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse updated = addressService.updateAddress(id, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Address updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address (ownership checked)")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        addressService.deleteAddress(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.successMessage("Address deleted successfully"));
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "Mark address as default for user")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        AddressResponse updated = addressService.setDefaultAddress(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(updated, "Default address set successfully"));
    }
}
