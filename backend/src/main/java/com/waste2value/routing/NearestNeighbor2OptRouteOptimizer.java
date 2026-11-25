package com.waste2value.routing;

import com.waste2value.address.Address;
import com.waste2value.listing.Listing;
import com.waste2value.pickup.PickupRequest;
import com.waste2value.routing.dto.LocationDto;
import com.waste2value.routing.dto.PickupStopResponse;
import com.waste2value.routing.dto.RoutePlanResponse;
import com.waste2value.user.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Route optimizer implementation using a Nearest-Neighbor construction heuristic
 * followed by 2-Opt local search improvement for the Traveling Salesperson Problem (TSP).
 * <p>
 * Evaluates straight-line (Haversine) distances between coordinates.
 * Isolates stops with missing or invalid coordinates and places them cleanly at the end.
 * Does NOT mutate any JPA entities or database state.
 */
@Component
public class NearestNeighbor2OptRouteOptimizer implements RouteOptimizer {

    private static final String ROUTING_DISCLAIMER =
            "Distances represent straight-line (Haversine) estimates between geographic coordinates. " +
            "Road network geometry, traffic, elevation, and actual travel times are not simulated.";

    private static final String ALGORITHM_NAME = "NEAREST_NEIGHBOR_2OPT";
    private static final int MAX_2OPT_ITERATIONS = 100;
    private static final double CO_LOCATION_THRESHOLD_KM = 0.1; // 100 meters

    private final RoutingProvider routingProvider;

    public NearestNeighbor2OptRouteOptimizer(RoutingProvider routingProvider) {
        this.routingProvider = routingProvider;
    }

    @Override
    public RoutePlanResponse optimizeRoute(
            Long collectorId,
            String collectorName,
            boolean isVerified,
            LocationDto startLocation,
            List<PickupRequest> pickupRequests,
            boolean roundTrip
    ) {
        Instant now = Instant.now();

        if (pickupRequests == null || pickupRequests.isEmpty()) {
            return new RoutePlanResponse(
                    collectorId,
                    collectorName,
                    isVerified,
                    startLocation,
                    0,
                    0,
                    0,
                    0.0,
                    roundTrip && hasValidCoordinates(startLocation) ? 0.0 : null,
                    roundTrip,
                    routingProvider.getProviderName(),
                    ALGORITHM_NAME,
                    ROUTING_DISCLAIMER,
                    Collections.emptyList(),
                    now
            );
        }

        // 1. Segregate requests into mappable vs unmappable
        List<PickupRequest> mappableRequests = new ArrayList<>();
        List<PickupRequest> unmappableRequests = new ArrayList<>();

        for (PickupRequest pr : pickupRequests) {
            Address addr = (pr.getListing() != null) ? pr.getListing().getAddress() : null;
            if (addr != null && routingProvider.isValidCoordinate(addr.getLatitude(), addr.getLongitude())) {
                mappableRequests.add(pr);
            } else {
                unmappableRequests.add(pr);
            }
        }

        // 2. Order mappable requests using Nearest Neighbor
        List<PickupRequest> orderedMappable = buildNearestNeighborTour(startLocation, mappableRequests);

        // 3. Improve tour using 2-Opt local search
        if (orderedMappable.size() >= 3) {
            orderedMappable = apply2OptOptimization(startLocation, orderedMappable, roundTrip);
        }

        // 4. Construct final stop responses with leg distances and co-location detection
        List<PickupStopResponse> stopResponses = new ArrayList<>();
        double cumulativeDistance = 0.0;
        BigDecimal currentLat = hasValidCoordinates(startLocation) ? startLocation.latitude() : null;
        BigDecimal currentLon = hasValidCoordinates(startLocation) ? startLocation.longitude() : null;

        int stopSequence = 1;

        // Process mappable stops
        for (PickupRequest pr : orderedMappable) {
            Listing listing = pr.getListing();
            Address addr = listing.getAddress();
            User generator = listing.getGenerator();

            Double legDistance = null;
            if (currentLat != null && currentLon != null) {
                legDistance = routingProvider.calculateDistanceKm(currentLat, currentLon, addr.getLatitude(), addr.getLongitude());
                if (legDistance != null) {
                    cumulativeDistance += legDistance;
                }
            } else if (stopSequence == 1) {
                // If depot has no coordinates, first leg is 0.0 km
                legDistance = 0.0;
            }

            // Co-location check: see if matches any earlier stop in stopResponses
            boolean isCoLocated = false;
            Integer coLocatedSeq = null;
            for (PickupStopResponse earlier : stopResponses) {
                if (earlier.hasValidCoordinates()) {
                    Double distBetween = routingProvider.calculateDistanceKm(
                            addr.getLatitude(), addr.getLongitude(),
                            earlier.latitude(), earlier.longitude()
                    );
                    if (distBetween != null && distBetween <= CO_LOCATION_THRESHOLD_KM) {
                        isCoLocated = true;
                        coLocatedSeq = earlier.stopSequence();
                        break;
                    }
                }
            }

            double roundedLeg = legDistance != null ? roundToOneDecimal(legDistance) : 0.0;
            double roundedCum = roundToOneDecimal(cumulativeDistance);

            stopResponses.add(new PickupStopResponse(
                    stopSequence,
                    pr.getId(),
                    listing.getId(),
                    listing.getTitle(),
                    listing.getCategory() != null ? listing.getCategory().getName() : "Unknown",
                    listing.getCategory() != null ? listing.getCategory().getSlug() : "unknown",
                    listing.getQuantity(),
                    listing.getUnit(),
                    pr.getProposedPrice(),
                    generator != null ? generator.getId() : null,
                    generator != null ? generator.getFirstName() + " " + generator.getLastName() : "Unknown",
                    generator != null ? generator.getPhone() : null,
                    addr.getAddressLine1(),
                    addr.getAddressLine2(),
                    addr.getCity(),
                    addr.getState(),
                    addr.getPostalCode(),
                    addr.getLatitude(),
                    addr.getLongitude(),
                    true,
                    "Valid GPS Coordinates",
                    roundedLeg,
                    roundedCum,
                    isCoLocated,
                    coLocatedSeq,
                    pr.getStatus().name(),
                    pr.getRequestedAt()
            ));

            currentLat = addr.getLatitude();
            currentLon = addr.getLongitude();
            stopSequence++;
        }

        // 5. Append unmappable stops (clearly identified with null distance and error message)
        for (PickupRequest pr : unmappableRequests) {
            Listing listing = pr.getListing();
            Address addr = (listing != null) ? listing.getAddress() : null;
            User generator = (listing != null) ? listing.getGenerator() : null;

            String statusMsg = determineInvalidCoordinateReason(addr);

            stopResponses.add(new PickupStopResponse(
                    stopSequence,
                    pr.getId(),
                    listing != null ? listing.getId() : null,
                    listing != null ? listing.getTitle() : "Unknown",
                    listing != null && listing.getCategory() != null ? listing.getCategory().getName() : "Unknown",
                    listing != null && listing.getCategory() != null ? listing.getCategory().getSlug() : "unknown",
                    listing != null ? listing.getQuantity() : BigDecimal.ZERO,
                    listing != null ? listing.getUnit() : "",
                    pr.getProposedPrice(),
                    generator != null ? generator.getId() : null,
                    generator != null ? generator.getFirstName() + " " + generator.getLastName() : "Unknown",
                    generator != null ? generator.getPhone() : null,
                    addr != null ? addr.getAddressLine1() : null,
                    addr != null ? addr.getAddressLine2() : null,
                    addr != null ? addr.getCity() : null,
                    addr != null ? addr.getState() : null,
                    addr != null ? addr.getPostalCode() : null,
                    addr != null ? addr.getLatitude() : null,
                    addr != null ? addr.getLongitude() : null,
                    false,
                    statusMsg,
                    null, // Unknown leg distance
                    roundToOneDecimal(cumulativeDistance),
                    false,
                    null,
                    pr.getStatus().name(),
                    pr.getRequestedAt()
            ));

            stopSequence++;
        }

        // Return leg calculation if requested and start coordinates exist
        Double returnLegDistance = null;
        if (roundTrip && hasValidCoordinates(startLocation) && currentLat != null && currentLon != null) {
            returnLegDistance = routingProvider.calculateDistanceKm(
                    currentLat, currentLon,
                    startLocation.latitude(), startLocation.longitude()
            );
            if (returnLegDistance != null) {
                returnLegDistance = roundToOneDecimal(returnLegDistance);
                cumulativeDistance += returnLegDistance;
            }
        }

        return new RoutePlanResponse(
                collectorId,
                collectorName,
                isVerified,
                startLocation,
                stopResponses.size(),
                mappableRequests.size(),
                unmappableRequests.size(),
                roundToOneDecimal(cumulativeDistance),
                returnLegDistance,
                roundTrip,
                routingProvider.getProviderName(),
                ALGORITHM_NAME,
                ROUTING_DISCLAIMER,
                stopResponses,
                now
        );
    }

    /**
     * Builds an initial tour using the greedy Nearest-Neighbor heuristic.
     */
    private List<PickupRequest> buildNearestNeighborTour(LocationDto startLocation, List<PickupRequest> mappableRequests) {
        if (mappableRequests.size() <= 1) {
            return new ArrayList<>(mappableRequests);
        }

        List<PickupRequest> unvisited = new ArrayList<>(mappableRequests);
        List<PickupRequest> tour = new ArrayList<>(mappableRequests.size());

        BigDecimal currentLat = hasValidCoordinates(startLocation) ? startLocation.latitude() : null;
        BigDecimal currentLon = hasValidCoordinates(startLocation) ? startLocation.longitude() : null;

        // If depot coordinates are missing, start at the first unvisited request
        if (currentLat == null || currentLon == null) {
            PickupRequest first = unvisited.remove(0);
            tour.add(first);
            currentLat = first.getListing().getAddress().getLatitude();
            currentLon = first.getListing().getAddress().getLongitude();
        }

        while (!unvisited.isEmpty()) {
            int nearestIdx = 0;
            double shortestDist = Double.MAX_VALUE;

            for (int i = 0; i < unvisited.size(); i++) {
                PickupRequest candidate = unvisited.get(i);
                Address addr = candidate.getListing().getAddress();
                Double dist = routingProvider.calculateDistanceKm(
                        currentLat, currentLon,
                        addr.getLatitude(), addr.getLongitude()
                );
                if (dist != null && dist < shortestDist) {
                    shortestDist = dist;
                    nearestIdx = i;
                }
            }

            PickupRequest nearest = unvisited.remove(nearestIdx);
            tour.add(nearest);
            currentLat = nearest.getListing().getAddress().getLatitude();
            currentLon = nearest.getListing().getAddress().getLongitude();
        }

        return tour;
    }

    /**
     * 2-Opt local search improvement.
     * Iteratively swaps sub-tour pairs when reversing a subsegment yields shorter straight-line distance.
     */
    private List<PickupRequest> apply2OptOptimization(
            LocationDto startLocation,
            List<PickupRequest> tour,
            boolean roundTrip
    ) {
        List<PickupRequest> bestTour = new ArrayList<>(tour);
        int n = bestTour.size();
        boolean improved = true;
        int iterations = 0;

        BigDecimal startLat = hasValidCoordinates(startLocation) ? startLocation.latitude() : null;
        BigDecimal startLon = hasValidCoordinates(startLocation) ? startLocation.longitude() : null;

        while (improved && iterations < MAX_2OPT_ITERATIONS) {
            improved = false;
            iterations++;

            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    double delta = compute2OptDelta(startLat, startLon, bestTour, i, j, roundTrip);
                    // If delta is negative, the swap strictly shortens the total path
                    if (delta < -0.001) {
                        reverseSubTour(bestTour, i, j);
                        improved = true;
                        break;
                    }
                }
                if (improved) {
                    break;
                }
            }
        }

        return bestTour;
    }

    /**
     * Computes distance difference (newDistance - oldDistance) for reversing bestTour[i..j].
     */
    private double compute2OptDelta(
            BigDecimal startLat,
            BigDecimal startLon,
            List<PickupRequest> tour,
            int i,
            int j,
            boolean roundTrip
    ) {
        int n = tour.size();
        Address addrI = tour.get(i).getListing().getAddress();
        Address addrJ = tour.get(j).getListing().getAddress();

        double oldEdges = 0.0;
        double newEdges = 0.0;

        // 1. Entering edge
        if (i == 0) {
            if (startLat != null && startLon != null) {
                Double oldStart = routingProvider.calculateDistanceKm(startLat, startLon, addrI.getLatitude(), addrI.getLongitude());
                Double newStart = routingProvider.calculateDistanceKm(startLat, startLon, addrJ.getLatitude(), addrJ.getLongitude());
                if (oldStart != null) oldEdges += oldStart;
                if (newStart != null) newEdges += newStart;
            }
        } else {
            Address prev = tour.get(i - 1).getListing().getAddress();
            Double oldPrev = routingProvider.calculateDistanceKm(prev.getLatitude(), prev.getLongitude(), addrI.getLatitude(), addrI.getLongitude());
            Double newPrev = routingProvider.calculateDistanceKm(prev.getLatitude(), prev.getLongitude(), addrJ.getLatitude(), addrJ.getLongitude());
            if (oldPrev != null) oldEdges += oldPrev;
            if (newPrev != null) newEdges += newPrev;
        }

        // 2. Leaving edge
        if (j == n - 1) {
            if (roundTrip && startLat != null && startLon != null) {
                Double oldEnd = routingProvider.calculateDistanceKm(addrJ.getLatitude(), addrJ.getLongitude(), startLat, startLon);
                Double newEnd = routingProvider.calculateDistanceKm(addrI.getLatitude(), addrI.getLongitude(), startLat, startLon);
                if (oldEnd != null) oldEdges += oldEnd;
                if (newEnd != null) newEdges += newEnd;
            }
        } else {
            Address next = tour.get(j + 1).getListing().getAddress();
            Double oldNext = routingProvider.calculateDistanceKm(addrJ.getLatitude(), addrJ.getLongitude(), next.getLatitude(), next.getLongitude());
            Double newNext = routingProvider.calculateDistanceKm(addrI.getLatitude(), addrI.getLongitude(), next.getLatitude(), next.getLongitude());
            if (oldNext != null) oldEdges += oldNext;
            if (newNext != null) newEdges += newNext;
        }

        return newEdges - oldEdges;
    }

    private void reverseSubTour(List<PickupRequest> tour, int i, int j) {
        while (i < j) {
            PickupRequest temp = tour.get(i);
            tour.set(i, tour.get(j));
            tour.set(j, temp);
            i++;
            j--;
        }
    }

    private boolean hasValidCoordinates(LocationDto loc) {
        return loc != null && loc.isValidCoordinate() && loc.latitude() != null && loc.longitude() != null;
    }

    private String determineInvalidCoordinateReason(Address addr) {
        if (addr == null) {
            return "Missing coordinates: listing has no address assigned";
        }
        if (addr.getLatitude() == null || addr.getLongitude() == null) {
            return "Missing coordinates: address has no latitude/longitude recorded";
        }
        double lat = addr.getLatitude().doubleValue();
        double lon = addr.getLongitude().doubleValue();
        if (Math.abs(lat) < 0.0001 && Math.abs(lon) < 0.0001) {
            return "Invalid coordinates: defaults to Null Island (0.0, 0.0)";
        }
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            return "Invalid coordinates: latitude or longitude out of geographic boundaries";
        }
        return "Unknown coordinate validation failure";
    }

    private double roundToOneDecimal(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
