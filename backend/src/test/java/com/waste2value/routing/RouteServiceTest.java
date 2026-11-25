package com.waste2value.routing;

import com.waste2value.address.Address;
import com.waste2value.address.AddressRepository;
import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.matching.DistanceCalculator;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.routing.dto.OptimizeRouteRequest;
import com.waste2value.routing.dto.PickupStopResponse;
import com.waste2value.routing.dto.RoutePlanResponse;
import com.waste2value.routing.dto.RouteSummaryResponse;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private PickupRequestRepository pickupRequestRepository;

    @Mock
    private CollectorProfileRepository collectorProfileRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RouteOptimizer routeOptimizer;

    private RoutingProvider routingProvider;
    private RouteService routeService;

    private User collectorUser;
    private CollectorProfile verifiedProfile;
    private CollectorProfile unverifiedProfile;

    @BeforeEach
    void setUp() {
        routingProvider = new StraightLineRoutingProvider(new DistanceCalculator());
        routeService = new RouteService(
                pickupRequestRepository,
                collectorProfileRepository,
                addressRepository,
                userRepository,
                routeOptimizer,
                routingProvider
        );

        collectorUser = new User("Bob", "Collector", "bob@collector.com", "hash", "1234567890", Role.COLLECTOR);
        collectorUser.setId(10L);

        verifiedProfile = new CollectorProfile();
        verifiedProfile.setId(100L);
        verifiedProfile.setUser(collectorUser);
        verifiedProfile.setVerified(true);

        unverifiedProfile = new CollectorProfile();
        unverifiedProfile.setId(101L);
        unverifiedProfile.setUser(collectorUser);
        unverifiedProfile.setVerified(false);
    }

    @Test
    @DisplayName("Unverified collector is rejected with ForbiddenException")
    void testUnverifiedCollectorRejected() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(collectorUser));
        when(collectorProfileRepository.findByUserId(10L)).thenReturn(Optional.of(unverifiedProfile));

        assertThatThrownBy(() -> routeService.getRoutePlan(10L, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Collector profile is pending verification");
    }

    @Test
    @DisplayName("Non-existent user is rejected with ResourceNotFoundException")
    void testNonExistentUserRejected() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.getRoutePlan(999L, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Verified collector gets route plan successfully")
    void testVerifiedCollectorGetsPlan() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(collectorUser));
        when(collectorProfileRepository.findByUserId(10L)).thenReturn(Optional.of(verifiedProfile));

        Address defaultAddr = new Address(collectorUser, "Main Depot", null, "Mumbai", "MH", "400001",
                new BigDecimal("19.0760"), new BigDecimal("72.8777"), true);
        when(addressRepository.findByUserIdAndIsDefaultTrue(10L)).thenReturn(Optional.of(defaultAddr));

        List<PickupRequest> emptyPickups = Collections.emptyList();
        when(pickupRequestRepository.findAcceptedPickupsForCollectorWithDetails(10L)).thenReturn(emptyPickups);

        RoutePlanResponse mockPlan = new RoutePlanResponse(
                10L, "Bob Collector", true, null, 0, 0, 0, 0.0, null, false,
                "STRAIGHT_LINE_HAVERSINE", "NEAREST_NEIGHBOR_2OPT", "Disclaimer", Collections.emptyList(), java.time.Instant.now()
        );
        when(routeOptimizer.optimizeRoute(eq(10L), eq("Bob Collector"), eq(true), any(), eq(emptyPickups), eq(false)))
                .thenReturn(mockPlan);

        RoutePlanResponse result = routeService.getRoutePlan(10L, null);

        assertThat(result).isNotNull();
        assertThat(result.collectorId()).isEqualTo(10L);
        assertThat(result.isCollectorVerified()).isTrue();
    }

    @Test
    @DisplayName("Custom start coordinates in request override default address")
    void testCustomStartCoordinatesOverrideDefaultAddress() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(collectorUser));
        when(collectorProfileRepository.findByUserId(10L)).thenReturn(Optional.of(verifiedProfile));
        when(pickupRequestRepository.findAcceptedPickupsForCollectorWithDetails(10L)).thenReturn(Collections.emptyList());

        OptimizeRouteRequest req = new OptimizeRouteRequest(
                new BigDecimal("28.6139"), new BigDecimal("77.2090"), "New Delhi Depot", true
        );

        RoutePlanResponse mockPlan = new RoutePlanResponse(
                10L, "Bob Collector", true, null, 0, 0, 0, 0.0, 0.0, true,
                "STRAIGHT_LINE_HAVERSINE", "NEAREST_NEIGHBOR_2OPT", "Disclaimer", Collections.emptyList(), java.time.Instant.now()
        );
        when(routeOptimizer.optimizeRoute(eq(10L), eq("Bob Collector"), eq(true), any(), any(), eq(true)))
                .thenReturn(mockPlan);

        RoutePlanResponse result = routeService.optimizeRoute(10L, req);

        assertThat(result).isNotNull();
        assertThat(result.roundTrip()).isTrue();
    }

    @Test
    @DisplayName("getRouteSummary returns quick KPI overview")
    void testGetRouteSummary() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(collectorUser));
        when(collectorProfileRepository.findByUserId(10L)).thenReturn(Optional.of(verifiedProfile));
        when(addressRepository.findByUserIdAndIsDefaultTrue(10L)).thenReturn(Optional.empty());
        when(addressRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(Collections.emptyList());
        when(pickupRequestRepository.findAcceptedPickupsForCollectorWithDetails(10L)).thenReturn(Collections.emptyList());

        RoutePlanResponse mockPlan = new RoutePlanResponse(
                10L, "Bob Collector", true, null, 5, 4, 1, 34.5, null, false,
                "STRAIGHT_LINE_HAVERSINE", "NEAREST_NEIGHBOR_2OPT", "Disclaimer", Collections.emptyList(), java.time.Instant.now()
        );
        when(routeOptimizer.optimizeRoute(eq(10L), eq("Bob Collector"), eq(true), any(), any(), eq(false)))
                .thenReturn(mockPlan);

        RouteSummaryResponse summary = routeService.getRouteSummary(10L);

        assertThat(summary).isNotNull();
        assertThat(summary.collectorId()).isEqualTo(10L);
        assertThat(summary.acceptedPickupsCount()).isEqualTo(5);
        assertThat(summary.stopsWithCoordinatesCount()).isEqualTo(4);
        assertThat(summary.stopsMissingCoordinatesCount()).isEqualTo(1);
        assertThat(summary.estimatedTotalDistanceKm()).isEqualTo(34.5);
    }
}
