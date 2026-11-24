package com.waste2value.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummaryResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserSummaryResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
