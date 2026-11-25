package com.waste2value.routing;

import com.waste2value.routing.dto.LocationDto;
import com.waste2value.routing.dto.PickupStopResponse;
import com.waste2value.routing.dto.RoutePlanResponse;
import com.waste2value.routing.dto.RouteSummaryResponse;
import com.waste2value.shared.exception.ForbiddenException;
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RouteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RouteService routeService;

    private CustomUserDetails createCollectorUser(Long id) {
        return new CustomUserDetails(id, "collector" + id + "@test.com", "pass", Role.COLLECTOR, AccountStatus.ACTIVE);
    }

    private CustomUserDetails createGeneratorUser(Long id) {
        return new CustomUserDetails(id, "generator" + id + "@test.com", "pass", Role.GENERATOR, AccountStatus.ACTIVE);
    }

    private CustomUserDetails createProcessorUser(Long id) {
        return new CustomUserDetails(id, "processor" + id + "@test.com", "pass", Role.PROCESSOR, AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Unauthenticated request to route endpoints returns 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/routes/collectors/me/plan"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/routes/collectors/me/stops"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/routes/collectors/me/optimize"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/routes/collectors/me/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Generator role attempting to access route planner returns 403 Forbidden")
    void generatorForbiddenFromRouting() throws Exception {
        mockMvc.perform(get("/api/v1/routes/collectors/me/plan")
                        .with(user(createGeneratorUser(2L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Processor role attempting to access route planner returns 403 Forbidden")
    void processorForbiddenFromRouting() throws Exception {
        mockMvc.perform(get("/api/v1/routes/collectors/me/plan")
                        .with(user(createProcessorUser(3L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verified collector successfully retrieves route plan")
    void verifiedCollectorGetsRoutePlan() throws Exception {
        LocationDto depot = new LocationDto("Mumbai Hub", "Port Road", null, "Mumbai", "MH", "400001",
                new BigDecimal("19.0760"), new BigDecimal("72.8777"), true);

        PickupStopResponse stop1 = new PickupStopResponse(
                1, 101L, 501L, "PET Bottles Lot", "Plastics", "plastics",
                new BigDecimal("250.00"), "kg", new BigDecimal("1500.00"),
                20L, "John Factory", "9876543210",
                "Industrial Area", null, "Mumbai", "MH", "400010",
                new BigDecimal("19.0800"), new BigDecimal("72.8800"),
                true, "Valid GPS Coordinates", 0.6, 0.6, false, null, "ACCEPTED", Instant.now()
        );

        RoutePlanResponse mockPlan = new RoutePlanResponse(
                1L, "Eco Hauler", true, depot, 1, 1, 0, 0.6, null, false,
                "STRAIGHT_LINE_HAVERSINE", "NEAREST_NEIGHBOR_2OPT",
                "Straight-line disclaimer", List.of(stop1), Instant.now()
        );

        when(routeService.getRoutePlan(eq(1L), any())).thenReturn(mockPlan);

        mockMvc.perform(get("/api/v1/routes/collectors/me/plan")
                        .with(user(createCollectorUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.collectorId").value(1))
                .andExpect(jsonPath("$.data.collectorName").value("Eco Hauler"))
                .andExpect(jsonPath("$.data.isCollectorVerified").value(true))
                .andExpect(jsonPath("$.data.totalStopsCount").value(1))
                .andExpect(jsonPath("$.data.totalEstimatedDistanceKm").value(0.6))
                .andExpect(jsonPath("$.data.stops[0].listingTitle").value("PET Bottles Lot"))
                .andExpect(jsonPath("$.data.routingMode").value("STRAIGHT_LINE_HAVERSINE"));
    }

    @Test
    @DisplayName("Verified collector successfully optimizes route via POST /optimize")
    void verifiedCollectorOptimizesRoute() throws Exception {
        RoutePlanResponse mockPlan = new RoutePlanResponse(
                1L, "Eco Hauler", true, null, 0, 0, 0, 0.0, null, false,
                "STRAIGHT_LINE_HAVERSINE", "NEAREST_NEIGHBOR_2OPT",
                "Straight-line disclaimer", Collections.emptyList(), Instant.now()
        );

        when(routeService.optimizeRoute(eq(1L), any())).thenReturn(mockPlan);

        mockMvc.perform(post("/api/v1/routes/collectors/me/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundTrip\": true}")
                        .with(user(createCollectorUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.collectorId").value(1));
    }

    @Test
    @DisplayName("Collector successfully retrieves route summary KPIs")
    void collectorGetsRouteSummary() throws Exception {
        RouteSummaryResponse summary = new RouteSummaryResponse(
                1L, 4, 3, 1, 18.2, "Mumbai", "STRAIGHT_LINE_HAVERSINE", "Disclaimer"
        );

        when(routeService.getRouteSummary(eq(1L))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/routes/collectors/me/summary")
                        .with(user(createCollectorUser(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.acceptedPickupsCount").value(4))
                .andExpect(jsonPath("$.data.stopsWithCoordinatesCount").value(3))
                .andExpect(jsonPath("$.data.stopsMissingCoordinatesCount").value(1))
                .andExpect(jsonPath("$.data.estimatedTotalDistanceKm").value(18.2));
    }

    @Test
    @DisplayName("Unverified collector is rejected with 403 Forbidden")
    void unverifiedCollectorRejected() throws Exception {
        when(routeService.getRoutePlan(eq(1L), any()))
                .thenThrow(new ForbiddenException("Collector profile is pending verification. Only verified collectors can access route planning."));

        mockMvc.perform(get("/api/v1/routes/collectors/me/plan")
                        .with(user(createCollectorUser(1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));
    }
}
