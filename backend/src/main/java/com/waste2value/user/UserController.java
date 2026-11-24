package com.waste2value.user;

import com.waste2value.shared.ApiResponse;
import com.waste2value.user.dto.UpdateProfileRequest;
import com.waste2value.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "User Management", description = "Endpoints for managing user profile and accounts")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserProfileResponse profile = userService.getProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/users/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse updated = userService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Profile updated successfully"));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get all users overview")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {
        List<UserProfileResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}
