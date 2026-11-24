package com.waste2value.matching;

import com.waste2value.matching.dto.CollectorRecommendationResponse;
import com.waste2value.matching.dto.ListingRecommendationResponse;
import com.waste2value.matching.dto.ProcessorRecommendationResponse;
import com.waste2value.user.AccountStatus;
import com.waste2value.user.CustomUserDetails;
import com.waste2value.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchingService matchingService;

    @Test
    @DisplayName("Unauthenticated request to matching endpoints returns 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/matching/listings/1/collectors"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/matching/collectors/me/recommended-listings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "collector@test.com", roles = "COLLECTOR")
    @DisplayName("Collector attempting to query listing collectors returns 403")
    void collectorForbiddenFromListingCollectors() throws Exception {
        mockMvc.perform(get("/api/v1/matching/listings/1/collectors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Generator successfully queries recommended collectors for their listing")
    void generatorCanQueryRecommendedCollectors() throws Exception {
        CollectorRecommendationResponse rec = new CollectorRecommendationResponse(
                1L, 3L, "City Collector", "Truck", "Mumbai",
                new BigDecimal("50.00"), true, new BigDecimal("4.80"), 12,
                4.2, true, 88.5, List.of("Verified Collector \u2705", "Close proximity (4.2 km away)")
        );

        when(matchingService.getRecommendedCollectorsForListing(eq(2L), eq(Role.GENERATOR), eq(100L)))
                .thenReturn(List.of(rec));

        mockMvc.perform(get("/api/v1/matching/listings/100/collectors")
                        .with(user(new CustomUserDetails(2L, "generator@test.com", "pass", Role.GENERATOR, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].collectorName").value("City Collector"))
                .andExpect(jsonPath("$.data[0].suitabilityScore").value(88.5))
                .andExpect(jsonPath("$.data[0].isDistanceAvailable").value(true));
    }

    @Test
    @DisplayName("Generator successfully queries recommended processors for their listing")
    void generatorCanQueryRecommendedProcessors() throws Exception {
        ProcessorRecommendationResponse rec = new ProcessorRecommendationResponse(
                10L, 4L, "Eco Refining Ltd", "LIC-11", true, new BigDecimal("4.90"),
                new BigDecimal("5000.00"), new BigDecimal("1000.00"), new BigDecimal("4000.00"),
                "PET Bottles", new BigDecimal("50.00"), new BigDecimal("1000.00"),
                new BigDecimal("22.00"), 8.5, true, 92.0,
                List.of("Verified Facility \uD83C\uDFED", "Specialized intake")
        );

        when(matchingService.getRecommendedProcessorsForListing(eq(2L), eq(Role.GENERATOR), eq(100L)))
                .thenReturn(List.of(rec));

        mockMvc.perform(get("/api/v1/matching/listings/100/processors")
                        .with(user(new CustomUserDetails(2L, "generator@test.com", "pass", Role.GENERATOR, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].companyName").value("Eco Refining Ltd"))
                .andExpect(jsonPath("$.data[0].suitabilityScore").value(92.0));
    }

    @Test
    @DisplayName("Collector successfully retrieves personalized recommended listings")
    void collectorCanQueryRecommendedListings() throws Exception {
        ListingRecommendationResponse rec = new ListingRecommendationResponse(
                55L, "Clean PET Bottles", 8L, "PET Bottles", "pet-bottles",
                new BigDecimal("150.00"), "kg", new BigDecimal("20.00"),
                2L, "Eco Generator", "Mumbai", "Maharashtra", 3.2, true,
                89.0, List.of("Close proximity (3.2 km)"), Instant.now()
        );

        when(matchingService.getRecommendedListingsForCollector(eq(3L)))
                .thenReturn(List.of(rec));

        mockMvc.perform(get("/api/v1/matching/collectors/me/recommended-listings")
                        .with(user(new CustomUserDetails(3L, "collector@test.com", "pass", Role.COLLECTOR, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Clean PET Bottles"))
                .andExpect(jsonPath("$.data[0].suitabilityScore").value(89.0));
    }

    @Test
    @DisplayName("Processor successfully retrieves personalized recommended listings")
    void processorCanQueryRecommendedListings() throws Exception {
        ListingRecommendationResponse rec = new ListingRecommendationResponse(
                55L, "Clean PET Bottles", 8L, "PET Bottles", "pet-bottles",
                new BigDecimal("150.00"), "kg", new BigDecimal("20.00"),
                2L, "Eco Generator", "Mumbai", "Maharashtra", 12.0, true,
                91.5, List.of("Volume compatible"), Instant.now()
        );

        when(matchingService.getRecommendedListingsForProcessor(eq(4L)))
                .thenReturn(List.of(rec));

        mockMvc.perform(get("/api/v1/matching/processors/me/recommended-listings")
                        .with(user(new CustomUserDetails(4L, "processor@test.com", "pass", Role.PROCESSOR, AccountStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Clean PET Bottles"))
                .andExpect(jsonPath("$.data[0].suitabilityScore").value(91.5));
    }
}
