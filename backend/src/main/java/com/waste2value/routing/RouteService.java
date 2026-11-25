package com.waste2value.routing;

import com.waste2value.address.Address;
import com.waste2value.address.AddressRepository;
import com.waste2value.collector.CollectorProfile;
import com.waste2value.collector.CollectorProfileRepository;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.pickup.PickupRequestRepository;
import com.waste2value.routing.dto.*;
import com.waste2value.shared.exception.ForbiddenException;
import com.waste2value.shared.exception.ResourceNotFoundException;
import com.waste2value.user.User;
import com.waste2value.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class RouteService {

    private final PickupRequestRepository pickupRequestRepository;
    private final CollectorProfileRepository collectorProfileRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final RouteOptimizer routeOptimizer;
    private final RoutingProvider routingProvider;

    public RouteService(
            PickupRequestRepository pickupRequestRepository,
            CollectorProfileRepository collectorProfileRepository,
            AddressRepository addressRepository,
            UserRepository userRepository,
            RouteOptimizer routeOptimizer,
            RoutingProvider routingProvider
    ) {
        this.pickupRequestRepository = pickupRequestRepository;
        this.collectorProfileRepository = collectorProfileRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.routeOptimizer = routeOptimizer;
        this.routingProvider = routingProvider;
    }

    /**
     * Generates or retrieves the optimized route plan for the authenticated collector.
     */
    public RoutePlanResponse getRoutePlan(Long collectorUserId, OptimizeRouteRequest request) {
        CollectorProfile profile = validateAndGetCollectorProfile(collectorUserId);
        User collectorUser = profile.getUser();

        LocationDto startLocation = resolveStartLocation(collectorUser, request);
        boolean roundTrip = (request != null) && request.isRoundTripRequested();

        List<PickupRequest> acceptedPickups = pickupRequestRepository
                .findAcceptedPickupsForCollectorWithDetails(collectorUserId);

        return routeOptimizer.optimizeRoute(
                collectorUserId,
                collectorUser.getFirstName() + " " + collectorUser.getLastName(),
                profile.isVerified(),
                startLocation,
                acceptedPickups,
                roundTrip
        );
    }

    /**
     * Returns the ordered list of stops for the authenticated collector.
     */
    public List<PickupStopResponse> getStops(Long collectorUserId) {
        RoutePlanResponse plan = getRoutePlan(collectorUserId, null);
        return plan.stops();
    }

    /**
     * Explicitly triggers route optimization with optional custom starting coordinates or round-trip settings.
     * Does NOT mutate pickup request entities or database states.
     */
    public RoutePlanResponse optimizeRoute(Long collectorUserId, OptimizeRouteRequest request) {
        return getRoutePlan(collectorUserId, request);
    }

    /**
     * Returns quick summary KPIs for the collector dashboard.
     */
    public RouteSummaryResponse getRouteSummary(Long collectorUserId) {
        RoutePlanResponse plan = getRoutePlan(collectorUserId, null);

        String depotCity = (plan.startLocation() != null) ? plan.startLocation().city() : "Unassigned Depot";

        return new RouteSummaryResponse(
                collectorUserId,
                plan.totalStopsCount(),
                plan.mappableStopsCount(),
                plan.unmappableStopsCount(),
                plan.totalEstimatedDistanceKm(),
                depotCity,
                plan.routingMode(),
                plan.disclaimer()
        );
    }

    /**
     * Verifies that the collector exists and has been approved/verified by an administrator.
     */
    private CollectorProfile validateAndGetCollectorProfile(Long collectorUserId) {
        User user = userRepository.findById(collectorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + collectorUserId));

        CollectorProfile profile = collectorProfileRepository.findByUserId(collectorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Collector profile not found for user: " + collectorUserId));

        if (!profile.isVerified()) {
            throw new ForbiddenException("Collector profile is pending verification. Only verified collectors can access route planning.");
        }

        return profile;
    }

    /**
     * Resolves the start depot location, either from request override or collector's registered addresses.
     */
    private LocationDto resolveStartLocation(User collectorUser, OptimizeRouteRequest request) {
        // 1. Check custom coordinates from request
        if (request != null && request.startLatitude() != null && request.startLongitude() != null) {
            boolean valid = routingProvider.isValidCoordinate(request.startLatitude(), request.startLongitude());
            return new LocationDto(
                    "Custom Start Depot",
                    request.startAddress() != null ? request.startAddress() : "Custom Origin",
                    null,
                    null,
                    null,
                    null,
                    request.startLatitude(),
                    request.startLongitude(),
                    valid
            );
        }

        // 2. Check default registered address
        Optional<Address> defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(collectorUser.getId());
        if (defaultAddress.isPresent()) {
            Address addr = defaultAddress.get();
            boolean valid = routingProvider.isValidCoordinate(addr.getLatitude(), addr.getLongitude());
            return new LocationDto(
                    collectorUser.getFirstName() + "'s Operating Depot",
                    addr.getAddressLine1(),
                    addr.getAddressLine2(),
                    addr.getCity(),
                    addr.getState(),
                    addr.getPostalCode(),
                    addr.getLatitude(),
                    addr.getLongitude(),
                    valid
            );
        }

        // 3. Check any address
        List<Address> addresses = addressRepository.findByUserIdOrderByCreatedAtDesc(collectorUser.getId());
        if (!addresses.isEmpty()) {
            Address addr = addresses.get(0);
            boolean valid = routingProvider.isValidCoordinate(addr.getLatitude(), addr.getLongitude());
            return new LocationDto(
                    collectorUser.getFirstName() + "'s Operating Depot",
                    addr.getAddressLine1(),
                    addr.getAddressLine2(),
                    addr.getCity(),
                    addr.getState(),
                    addr.getPostalCode(),
                    addr.getLatitude(),
                    addr.getLongitude(),
                    valid
            );
        }

        // 4. Fallback if collector has not registered an address yet
        return LocationDto.unconfigured(collectorUser.getFirstName() + "'s Depot (Unconfigured Address)");
    }
}
