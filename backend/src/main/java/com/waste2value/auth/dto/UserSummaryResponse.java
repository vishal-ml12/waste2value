package com.waste2value.auth.dto;

import com.waste2value.user.AccountStatus;
import com.waste2value.user.Role;
import com.waste2value.user.User;

public record UserSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        AccountStatus status
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }
}
