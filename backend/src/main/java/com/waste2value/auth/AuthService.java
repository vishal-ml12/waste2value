package com.waste2value.auth;

import com.waste2value.auth.dto.*;
import com.waste2value.auth.jwt.JwtService;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.shared.exception.UnauthorizedException;
import com.waste2value.user.AccountStatus;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("Public registration as ADMIN is not permitted");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phone() != null ? request.phone().trim() : null,
                request.role()
        );
        user.setStatus(AccountStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        return createAuthSession(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new ForbiddenException("Account has been suspended");
        }

        return createAuthSession(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawToken = request.refreshToken();
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String tokenHash = hashToken(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Reuse Detection: If already revoked, token theft or reuse occurred!
        if (token.isRevoked()) {
            log.warn("Refresh token reuse detected for family {}! Revoking entire token family.", token.getTokenFamily());
            refreshTokenRepository.revokeFamily(token.getTokenFamily(), Instant.now());
            throw new UnauthorizedException("Token reuse detected. All sessions in this chain have been revoked.");
        }

        // Expiry check
        if (token.isExpired()) {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
            throw new UnauthorizedException("Refresh token expired. Please log in again.");
        }

        User user = token.getUser();
        if (user.getStatus() == AccountStatus.SUSPENDED) {
            throw new ForbiddenException("Account has been suspended");
        }

        // Secure Rotation: Revoke current token and record replacement
        String newRawRefreshToken = generateRandomToken();
        String newTokenHash = hashToken(newRawRefreshToken);

        token.setRevokedAt(Instant.now());
        token.setReplacedByTokenHash(newTokenHash);
        refreshTokenRepository.save(token);

        // Issue new token in the same token family
        RefreshToken newRefreshToken = new RefreshToken(
                newTokenHash,
                user,
                Instant.now().plusMillis(refreshTokenExpirationMs),
                token.getTokenFamily()
        );
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.of(
                newAccessToken,
                newRawRefreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                UserSummaryResponse.from(user)
        );
    }

    @Transactional
    public void logout(LogoutRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String tokenHash = hashToken(request.refreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                refreshTokenRepository.revokeFamily(token.getTokenFamily(), Instant.now());
            });
        }
        SecurityContextHolder.clearContext();
    }

    private AuthResponse createAuthSession(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = generateRandomToken();
        String tokenHash = hashToken(rawRefreshToken);
        String tokenFamily = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken(
                tokenHash,
                user,
                Instant.now().plusMillis(refreshTokenExpirationMs),
                tokenFamily
        );
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.of(
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                UserSummaryResponse.from(user)
        );
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String generateRandomToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
