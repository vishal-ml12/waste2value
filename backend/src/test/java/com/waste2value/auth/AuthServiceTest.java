package com.waste2value.auth;

import com.waste2value.auth.dto.*;
import com.waste2value.auth.jwt.JwtService;
import com.waste2value.shared.exception.BadRequestException;
import com.waste2value.shared.exception.ConflictException;
import com.waste2value.shared.exception.UnauthorizedException;
import com.waste2value.user.AccountStatus;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService, 604800000L);
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "Password123!", "1234567890", Role.GENERATOR);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_pass");

        User savedUser = new User("John", "Doe", "john@example.com", "encoded_pass", "1234567890", Role.GENERATOR);
        savedUser.setId(10L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access_token_123");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token_123", response.accessToken());
        assertEquals("john@example.com", response.user().email());
        assertEquals(Role.GENERATOR, response.user().role());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void register_AdminRole_ThrowsBadRequest() {
        RegisterRequest request = new RegisterRequest("Admin", "User", "admin@example.com", "Password123!", "1234567890", Role.ADMIN);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("ADMIN is not permitted"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_DuplicateEmail_ThrowsConflict() {
        RegisterRequest request = new RegisterRequest("Jane", "Doe", "jane@example.com", "Password123!", "1234567890", Role.COLLECTOR);
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("already registered"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");
        User user = new User("User", "One", "user@example.com", "hashed_pass", "1234567890", Role.COLLECTOR);
        user.setId(20L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed_pass")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("valid_jwt");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("valid_jwt", response.accessToken());
        assertEquals("user@example.com", response.user().email());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_InvalidPassword_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword");
        User user = new User("User", "One", "user@example.com", "hashed_pass", "1234567890", Role.COLLECTOR);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed_pass")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void login_UserNotFound_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("missing@example.com", "Password123!");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void refreshToken_Success_RotatesTokenInSameFamily() {
        String rawToken = "my_valid_refresh_token_string";
        String tokenHash = AuthService.hashToken(rawToken);

        User user = new User("User", "One", "user@example.com", "hashed_pass", null, Role.PROCESSOR);
        user.setId(30L);
        RefreshToken token = new RefreshToken(tokenHash, user, Instant.now().plusSeconds(3600), "family-uuid-123");

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        when(jwtService.generateAccessToken(user)).thenReturn("new_access_token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest(rawToken));

        assertNotNull(response);
        assertEquals("new_access_token", response.accessToken());
        assertTrue(token.isRevoked(), "Old token should be revoked");
        assertNotNull(token.getReplacedByTokenHash(), "Old token should record replacement");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        RefreshToken newToken = captor.getAllValues().get(1);
        assertEquals("family-uuid-123", newToken.getTokenFamily(), "New token must stay in the same family");
    }

    @Test
    void refreshToken_ReuseDetected_RevokesEntireFamily() {
        String rawToken = "compromised_token";
        String tokenHash = AuthService.hashToken(rawToken);

        User user = new User("User", "One", "user@example.com", "hashed_pass", null, Role.GENERATOR);
        RefreshToken revokedToken = new RefreshToken(tokenHash, user, Instant.now().plusSeconds(3600), "family-xyz");
        revokedToken.setRevokedAt(Instant.now().minusSeconds(10)); // Already revoked!

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refreshToken(new RefreshTokenRequest(rawToken)));

        assertTrue(ex.getMessage().contains("Token reuse detected"));
        verify(refreshTokenRepository).revokeFamily(eq("family-xyz"), any(Instant.class));
    }

    @Test
    void logout_RevokesFamily() {
        String rawToken = "token_to_logout";
        String tokenHash = AuthService.hashToken(rawToken);

        User user = new User("User", "One", "user@example.com", "hashed_pass", null, Role.GENERATOR);
        RefreshToken token = new RefreshToken(tokenHash, user, Instant.now().plusSeconds(3600), "family-logout");

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        authService.logout(new LogoutRequest(rawToken));

        verify(refreshTokenRepository).revokeFamily(eq("family-logout"), any(Instant.class));
    }
}
