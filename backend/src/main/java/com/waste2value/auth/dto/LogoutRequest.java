package com.waste2value.auth.dto;

public record LogoutRequest(
        String refreshToken
) {
}
