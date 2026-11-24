package com.waste2value.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste2value.auth.dto.RegisterRequest;
import com.waste2value.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Audit 4 Authorization: Unauthenticated request to protected endpoints is rejected with 401")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/admin"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/pickup-requests/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Audit 4 Authorization: Public self-registration as ADMIN is forbidden")
    void cannotRegisterAsAdmin() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "Fake", "Admin", "fraudulent@admin.com", "Password123!", "9876543210", Role.ADMIN
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Public registration as ADMIN is not permitted"));
    }

    @Test
    @WithMockUser(username = "gen@test.com", roles = "GENERATOR")
    @DisplayName("Audit 4 Authorization: Generator is forbidden from accessing Admin, Collector, or Processor endpoints")
    void generatorForbiddenFromOtherRoles() throws Exception {
        // Generator cannot access Admin dashboard
        mockMvc.perform(get("/api/v1/dashboard/admin"))
                .andExpect(status().isForbidden());

        // Generator cannot access Collector dashboard
        mockMvc.perform(get("/api/v1/dashboard/collector"))
                .andExpect(status().isForbidden());

        // Generator cannot access Processor dashboard
        mockMvc.perform(get("/api/v1/dashboard/processor"))
                .andExpect(status().isForbidden());

        // Generator cannot access Admin collector verification list
        mockMvc.perform(get("/api/v1/admin/collectors/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "col@test.com", roles = "COLLECTOR")
    @DisplayName("Audit 4 Authorization: Collector is forbidden from accessing Admin, Generator, or Processor endpoints")
    void collectorForbiddenFromOtherRoles() throws Exception {
        // Collector cannot access Admin dashboard
        mockMvc.perform(get("/api/v1/dashboard/admin"))
                .andExpect(status().isForbidden());

        // Collector cannot access Generator dashboard
        mockMvc.perform(get("/api/v1/dashboard/generator"))
                .andExpect(status().isForbidden());

        // Collector cannot access Processor dashboard
        mockMvc.perform(get("/api/v1/dashboard/processor"))
                .andExpect(status().isForbidden());

        // Collector cannot access Admin processor verification list
        mockMvc.perform(get("/api/v1/admin/processors/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "proc@test.com", roles = "PROCESSOR")
    @DisplayName("Audit 4 Authorization: Processor is forbidden from accessing Admin, Generator, or Collector endpoints")
    void processorForbiddenFromOtherRoles() throws Exception {
        // Processor cannot access Admin dashboard
        mockMvc.perform(get("/api/v1/dashboard/admin"))
                .andExpect(status().isForbidden());

        // Processor cannot access Generator dashboard
        mockMvc.perform(get("/api/v1/dashboard/generator"))
                .andExpect(status().isForbidden());

        // Processor cannot access Collector dashboard
        mockMvc.perform(get("/api/v1/dashboard/collector"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("Audit 4 Authorization: Admin can access platform admin endpoints")
    void adminCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/admin"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/collectors/pending"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/processors/pending"))
                .andExpect(status().isOk());
    }
}
