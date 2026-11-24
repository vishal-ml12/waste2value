package com.waste2value.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waste2value.analytics.dto.*;
import com.waste2value.user.AccountStatus;
import com.waste2value.user.CustomUserDetails;
import com.waste2value.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("Public analytics endpoints (trends, categories, factors) are accessible without authentication")
    void publicAnalyticsAccessibleWithoutAuth() throws Exception {
        when(analyticsService.getUnifiedMonthlyTrends(any(), any()))
                .thenReturn(List.of(new MonthlyTrendPoint("2026-05", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, 1, 1)));

        mockMvc.perform(get("/api/v1/analytics/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        when(analyticsService.getUnifiedCategoryAnalytics(any(), any()))
                .thenReturn(List.of(new CategoryAnalyticsPoint(1L, "Plastic", "plastic", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, true, "EPA")));

        mockMvc.perform(get("/api/v1/analytics/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        when(analyticsService.getAllEmissionFactors())
                .thenReturn(List.of(new EmissionFactorResponse(1L, 1L, "Plastic", BigDecimal.ONE, BigDecimal.ONE, "kg", "EPA", "Notes", LocalDate.now(), null, true)));

        mockMvc.perform(get("/api/v1/analytics/factors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Admin overview unauthenticated request is rejected with 401")
    void unauthenticatedAdminOverviewRejected() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/admin/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin overview accessed by Generator or Collector is forbidden with 403")
    void nonAdminForbiddenFromAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/admin/overview")
                        .with(user(new CustomUserDetails(2L, "gen@test.com", "pass", Role.GENERATOR, AccountStatus.ACTIVE))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/analytics/admin/overview")
                        .with(user(new CustomUserDetails(3L, "col@test.com", "pass", Role.COLLECTOR, AccountStatus.ACTIVE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin successfully queries admin overview and waste analytics")
    void adminCanAccessOverviewAndWaste() throws Exception {
        AdminOverviewStats stats = new AdminOverviewStats(
                new BigDecimal("500.00"), new BigDecimal("350.00"), new BigDecimal("450.00"),
                new BigDecimal("0.450"), new BigDecimal("350.00"), 5, 3, 10, 4, 2, "Disclaimer"
        );
        when(analyticsService.getAdminOverview(any(), any())).thenReturn(stats);

        mockMvc.perform(get("/api/v1/analytics/admin/overview")
                        .with(user(new CustomUserDetails(1L, "admin@test.com", "pass", Role.ADMIN, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalWasteCollectedKg").value(500.00))
                .andExpect(jsonPath("$.data.totalWasteRecycledKg").value(350.00));
    }

    @Test
    @DisplayName("Generator successfully queries own analytics and is forbidden from collector analytics")
    void generatorAnalyticsAccessControl() throws Exception {
        GeneratorAnalyticsResponse res = new GeneratorAnalyticsResponse(
                2L, new BigDecimal("450.00"), new BigDecimal("200.00"), 1,
                new BigDecimal("3000.00"), List.of(), new BigDecimal("200.00"),
                new BigDecimal("306.00"), new BigDecimal("0.306"), new BigDecimal("200.00"),
                List.of(), "Disclaimer"
        );
        when(analyticsService.getGeneratorAnalytics(eq(2L), any(), any())).thenReturn(res);

        mockMvc.perform(get("/api/v1/analytics/generator/me")
                        .with(user(new CustomUserDetails(2L, "gen@test.com", "pass", Role.GENERATOR, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.generatorId").value(2L))
                .andExpect(jsonPath("$.data.totalTransactedKg").value(200.00));

        // Generator cannot access collector me
        mockMvc.perform(get("/api/v1/analytics/collector/me")
                        .with(user(new CustomUserDetails(2L, "gen@test.com", "pass", Role.GENERATOR, AccountStatus.ACTIVE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Collector successfully queries own analytics and is forbidden from processor analytics")
    void collectorAnalyticsAccessControl() throws Exception {
        CollectorAnalyticsResponse res = new CollectorAnalyticsResponse(
                3L, new BigDecimal("200.00"), 1, new BigDecimal("3000.00"),
                new BigDecimal("200.00"), 1, List.of(), List.of()
        );
        when(analyticsService.getCollectorAnalytics(eq(3L), any(), any())).thenReturn(res);

        mockMvc.perform(get("/api/v1/analytics/collector/me")
                        .with(user(new CustomUserDetails(3L, "col@test.com", "pass", Role.COLLECTOR, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.collectorId").value(3L));

        // Collector cannot access processor me
        mockMvc.perform(get("/api/v1/analytics/processor/me")
                        .with(user(new CustomUserDetails(3L, "col@test.com", "pass", Role.COLLECTOR, AccountStatus.ACTIVE))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Processor successfully queries own facility analytics")
    void processorAnalyticsAccessControl() throws Exception {
        ProcessorAnalyticsResponse res = new ProcessorAnalyticsResponse(
                10L, "Circular Refining", new BigDecimal("200.00"), 1,
                new BigDecimal("10000.00"), new BigDecimal("2500.00"), new BigDecimal("25.0"),
                new BigDecimal("306.00"), new BigDecimal("0.306"), new BigDecimal("200.00"),
                List.of(), List.of(), "Disclaimer"
        );
        when(analyticsService.getProcessorAnalytics(eq(4L), any(), any())).thenReturn(res);

        mockMvc.perform(get("/api/v1/analytics/processor/me")
                        .with(user(new CustomUserDetails(4L, "proc@test.com", "pass", Role.PROCESSOR, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyName").value("Circular Refining"))
                .andExpect(jsonPath("$.data.capacityUtilizationPercent").value(25.0));
    }

    @Test
    @DisplayName("Admin can update emission factors via PUT endpoint")
    void adminCanUpdateEmissionFactors() throws Exception {
        EmissionFactorResponse resp = new EmissionFactorResponse(
                1L, 8L, "PET Bottles", new BigDecimal("1.6500"), new BigDecimal("1.0000"),
                "kg", "Updated EPA WARM", "Updated methodology", LocalDate.now(), null, true
        );
        when(analyticsService.updateEmissionFactor(eq(1L), any())).thenReturn(resp);

        UpdateEmissionFactorRequest req = new UpdateEmissionFactorRequest(
                new BigDecimal("1.6500"), new BigDecimal("1.0000"),
                "Updated EPA WARM", "Updated methodology", true
        );

        mockMvc.perform(put("/api/v1/analytics/admin/factors/1")
                        .with(user(new CustomUserDetails(1L, "admin@test.com", "pass", Role.ADMIN, AccountStatus.ACTIVE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.co2FactorKgPerUnit").value(1.6500));
    }
}
