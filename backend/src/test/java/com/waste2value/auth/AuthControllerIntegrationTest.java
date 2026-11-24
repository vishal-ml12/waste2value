package com.waste2value.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste2value.auth.dto.AuthResponse;
import com.waste2value.auth.dto.LoginRequest;
import com.waste2value.auth.dto.RegisterRequest;
import com.waste2value.auth.dto.UserSummaryResponse;
import com.waste2value.auth.jwt.JwtService;
import com.waste2value.user.AccountStatus;
import com.waste2value.user.CustomUserDetailsService;
import com.waste2value.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void register_ValidRequest_ReturnsCreatedWithEnvelope() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "Smith", "alice@example.com", "Password123!", "1234567890", Role.GENERATOR);
        AuthResponse mockResp = AuthResponse.of("access-token", "refresh-token", 900L,
                new UserSummaryResponse(1L, "Alice", "Smith", "alice@example.com", Role.GENERATOR, AccountStatus.ACTIVE));

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResp);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void register_ValidationFails_ReturnsBadRequestWithValidationErrors() throws Exception {
        RegisterRequest invalidReq = new RegisterRequest("", "", "invalid-email", "short", null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    void unauthenticatedAccessToProtectedResource_ReturnsUnauthorizedEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
