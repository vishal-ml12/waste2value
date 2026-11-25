package com.waste2value.routing;

import com.waste2value.address.Address;
import com.waste2value.category.Category;
import com.waste2value.listing.Listing;
import com.waste2value.matching.DistanceCalculator;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestStatus;
import com.waste2value.routing.dto.LocationDto;
import com.waste2value.routing.dto.PickupStopResponse;
import com.waste2value.routing.dto.RoutePlanResponse;
import com.waste2value.user.Role;
import com.waste2value.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NearestNeighbor2OptRouteOptimizerTest {

    private NearestNeighbor2OptRouteOptimizer optimizer;
    private DistanceCalculator distanceCalculator;
    private StraightLineRoutingProvider routingProvider;

    private User generator;
    private Category category;

    @BeforeEach
    void setUp() {
        distanceCalculator = new DistanceCalculator();
        routingProvider = new StraightLineRoutingProvider(distanceCalculator);
        optimizer = new NearestNeighbor2OptRouteOptimizer(routingProvider);

        generator = new User("Alice", "Generator", "generator@example.com", "hash", "9876543210", Role.GENERATOR);
        category = new Category(null, "Plastics", "plastics", "Recyclable polymers", "kg", true);
    }

    private PickupRequest createMockPickup(Long id, String title, BigDecimal lat, BigDecimal lon, String city) {
        Address addr = new Address(generator, "Street " + id, null, city, "State", "123456", lat, lon, false);
        Listing listing = new Listing(generator, category, addr, title, "Desc", new BigDecimal("100"), "kg", new BigDecimal("50"), null);
        listing.setId(id * 10);

        PickupRequest pr = new PickupRequest();
        pr.setId(id);
        pr.setListing(listing);
        pr.setStatus(PickupRequestStatus.ACCEPTED);
        pr.setProposedPrice(new BigDecimal("500.00"));
        return pr;
    }

    @Test
    @DisplayName("Empty route: returns 0 stops and 0.0 distance")
    void testEmptyRoute() {
        LocationDto depot = new LocationDto("Depot", "Main St", null, "Mumbai", "MH", "400001",
                new BigDecimal("19.0760"), new BigDecimal("72.8777"), true);

        RoutePlanResponse plan = optimizer.optimizeRoute(1L, "Bob", true, depot, List.of(), false);

        assertThat(plan.totalStopsCount()).isEqualTo(0);
        assertThat(plan.mappableStopsCount()).isEqualTo(0);
        assertThat(plan.totalEstimatedDistanceKm()).isEqualTo(0.0);
        assertThat(plan.stops()).isEmpty();
        assertThat(plan.routingMode()).isEqualTo("STRAIGHT_LINE_HAVERSINE");
    }

    @Test
    @DisplayName("Single stop route: calculates leg distance from depot")
    void testSingleStopRoute() {
        // Mumbai CST to Dadar ~ 8.5 km
        LocationDto depot = new LocationDto("Depot", "CST", null, "Mumbai", "MH", "400001",
                new BigDecimal("18.9400"), new BigDecimal("72.8353"), true);

        PickupRequest p1 = createMockPickup(1L, "Dadar Paper", new BigDecimal("19.0178"), new BigDecimal("72.8478"), "Mumbai");

        RoutePlanResponse plan = optimizer.optimizeRoute(1L, "Bob", true, depot, List.of(p1), false);

        assertThat(plan.totalStopsCount()).isEqualTo(1);
        assertThat(plan.mappableStopsCount()).isEqualTo(1);
        assertThat(plan.unmappableStopsCount()).isEqualTo(0);
        assertThat(plan.totalEstimatedDistanceKm()).isGreaterThan(7.0).isLessThan(10.0);

        PickupStopResponse stop1 = plan.stops().get(0);
        assertThat(stop1.stopSequence()).isEqualTo(1);
        assertThat(stop1.hasValidCoordinates()).isTrue();
        assertThat(stop1.legDistanceKm()).isEqualTo(plan.totalEstimatedDistanceKm());
    }

    @Test
    @DisplayName("Multiple stops: orders geographically by nearest-neighbor & 2-opt")
    void testMultipleStopsOrdering() {
        // Depot at South Mumbai (Colaba: 18.9067, 72.8147)
        LocationDto depot = new LocationDto("Depot", "Colaba", null, "Mumbai", "MH", "400005",
                new BigDecimal("18.9067"), new BigDecimal("72.8147"), true);

        // Stops:
        // p1: Thane (North, far: 19.2183, 72.9781)
        // p2: Marine Lines (Very close to Colaba: 18.9438, 72.8232)
        // p3: Dadar (Middle: 19.0178, 72.8478)
        PickupRequest p1 = createMockPickup(1L, "Thane Lot", new BigDecimal("19.2183"), new BigDecimal("72.9781"), "Thane");
        PickupRequest p2 = createMockPickup(2L, "Marine Lines Lot", new BigDecimal("18.9438"), new BigDecimal("72.8232"), "Mumbai");
        PickupRequest p3 = createMockPickup(3L, "Dadar Lot", new BigDecimal("19.0178"), new BigDecimal("72.8478"), "Mumbai");

        // Pass in scrambled order: [Thane, Marine Lines, Dadar]
        RoutePlanResponse plan = optimizer.optimizeRoute(1L, "Bob", true, depot, List.of(p1, p2, p3), false);

        assertThat(plan.totalStopsCount()).isEqualTo(3);
        assertThat(plan.mappableStopsCount()).isEqualTo(3);

        // Sequence should visit Marine Lines first (closest to Colaba), then Dadar, then Thane
        List<PickupStopResponse> stops = plan.stops();
        assertThat(stops.get(0).pickupRequestId()).isEqualTo(2L); // Marine Lines
        assertThat(stops.get(1).pickupRequestId()).isEqualTo(3L); // Dadar
        assertThat(stops.get(2).pickupRequestId()).isEqualTo(1L); // Thane

        assertThat(stops.get(0).stopSequence()).isEqualTo(1);
        assertThat(stops.get(1).stopSequence()).isEqualTo(2);
        assertThat(stops.get(2).stopSequence()).isEqualTo(3);
    }

    @Test
    @DisplayName("Missing and invalid coordinates: isolated and appended to end")
    void testMissingAndInvalidCoordinates() {
        LocationDto depot = new LocationDto("Depot", "Main St", null, "Delhi", "DL", "110001",
                new BigDecimal("28.6139"), new BigDecimal("77.2090"), true);

        PickupRequest valid1 = createMockPickup(1L, "Connaught Place", new BigDecimal("28.6315"), new BigDecimal("77.2167"), "Delhi");
        PickupRequest nullCoords = createMockPickup(2L, "No Lat Long", null, null, "Delhi");
        PickupRequest nullIsland = createMockPickup(3L, "Null Island (0,0)", BigDecimal.ZERO, BigDecimal.ZERO, "Delhi");
        PickupRequest outOfBounds = createMockPickup(4L, "Bad Lat (95.0)", new BigDecimal("95.0000"), new BigDecimal("77.0000"), "Delhi");

        RoutePlanResponse plan = optimizer.optimizeRoute(1L, "Bob", true, depot, List.of(nullCoords, valid1, outOfBounds, nullIsland), false);

        assertThat(plan.totalStopsCount()).isEqualTo(4);
        assertThat(plan.mappableStopsCount()).isEqualTo(1);
        assertThat(plan.unmappableStopsCount()).isEqualTo(3);

        // First stop must be the valid one
        PickupStopResponse stop1 = plan.stops().get(0);
        assertThat(stop1.pickupRequestId()).isEqualTo(1L);
        assertThat(stop1.hasValidCoordinates()).isTrue();
        assertThat(stop1.legDistanceKm()).isNotNull();

        // Remaining stops must be marked invalid
        for (int i = 1; i < 4; i++) {
            PickupStopResponse unmappable = plan.stops().get(i);
            assertThat(unmappable.hasValidCoordinates()).isFalse();
            assertThat(unmappable.legDistanceKm()).isNull();
            assertThat(unmappable.coordinateStatusMessage()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Co-located stops: detects stops within 100 meters")
    void testCoLocatedStops() {
        LocationDto depot = new LocationDto("Depot", "Central Depot", null, "Bengaluru", "KA", "560001",
                new BigDecimal("12.9716"), new BigDecimal("77.5946"), true);

        // Two listings at the exact same factory location
        PickupRequest p1 = createMockPickup(1L, "Factory Plastic Lot A", new BigDecimal("12.9352"), new BigDecimal("77.6245"), "Bengaluru");
        PickupRequest p2 = createMockPickup(2L, "Factory Metal Lot B", new BigDecimal("12.9352"), new BigDecimal("77.6245"), "Bengaluru");

        RoutePlanResponse plan = optimizer.optimizeRoute(1L, "Bob", true, depot, List.of(p1, p2), false);

        assertThat(plan.totalStopsCount()).isEqualTo(2);
        PickupStopResponse stop1 = plan.stops().get(0);
        PickupStopResponse stop2 = plan.stops().get(1);

        assertThat(stop1.isCoLocated()).isFalse();
        assertThat(stop2.isCoLocated()).isTrue();
        assertThat(stop2.coLocatedStopSequence()).isEqualTo(stop1.stopSequence());
        assertThat(stop2.legDistanceKm()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Round trip: calculates return leg to start depot")
    void testRoundTripRoute() {
        LocationDto depot = new LocationDto("Depot", "Depot", null, "Bengaluru", "KA", "560001",
                new BigDecimal("12.9716"), new BigDecimal("77.5946"), true);

        PickupRequest p1 = createMockPickup(1L, "Koramangala", new BigDecimal("12.9352"), new BigDecimal("77.6245"), "Bengaluru");

        RoutePlanResponse oneWay = optimizer.optimizeRoute(1L, "Bob", true, depot, List.of(p1), false);
        RoutePlanResponse roundTrip = optimizer.optimizeRoute(1L, "Bob", true, depot, List.of(p1), true);

        assertThat(oneWay.roundTrip()).isFalse();
        assertThat(oneWay.returnLegDistanceKm()).isNull();

        assertThat(roundTrip.roundTrip()).isTrue();
        assertThat(roundTrip.returnLegDistanceKm()).isNotNull().isGreaterThan(0.0);
        assertThat(roundTrip.totalEstimatedDistanceKm())
                .isEqualTo(BigDecimal.valueOf(oneWay.totalEstimatedDistanceKm() + roundTrip.returnLegDistanceKm())
                        .setScale(1, java.math.RoundingMode.HALF_UP).doubleValue());
    }

    @Test
    @DisplayName("Pickup requests immutability: optimizer does NOT modify statuses or entities")
    void testImmutabilityOfPickupRequests() {
        LocationDto depot = new LocationDto("Depot", "Depot", null, "City", "ST", "12345",
                new BigDecimal("12.0"), new BigDecimal("77.0"), true);

        PickupRequest p1 = createMockPickup(1L, "Lot 1", new BigDecimal("12.1"), new BigDecimal("77.1"), "City");
        PickupRequest p2 = createMockPickup(2L, "Lot 2", new BigDecimal("12.2"), new BigDecimal("77.2"), "City");

        List<PickupRequest> list = new ArrayList<>(List.of(p1, p2));
        optimizer.optimizeRoute(1L, "Bob", true, depot, list, false);

        assertThat(p1.getStatus()).isEqualTo(PickupRequestStatus.ACCEPTED);
        assertThat(p2.getStatus()).isEqualTo(PickupRequestStatus.ACCEPTED);
        assertThat(list).containsExactly(p1, p2); // original collection intact
    }
}
