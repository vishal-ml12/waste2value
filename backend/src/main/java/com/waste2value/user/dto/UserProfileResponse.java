package com.waste2value.user.dto;

import com.waste2value.user.AccountStatus;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        Role role,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
