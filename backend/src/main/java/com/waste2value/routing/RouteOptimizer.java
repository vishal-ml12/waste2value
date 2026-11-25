package com.waste2value.routing;

import com.waste2value.pickup.PickupRequest;
import com.waste2value.routing.dto.LocationDto;
import com.waste2value.routing.dto.RoutePlanResponse;

import java.util.List;

/**
 * Strategy interface for optimizing the visiting sequence of pickup stops.
 */
public interface RouteOptimizer {

    /**
     * Orders the provided pickup requests into an efficient visiting sequence.
     * Does NOT mutate pickup request entities or database records.
     *
     * @param collectorId        ID of the collector
     * @param collectorName      Name of the collector
     * @param isVerified         Whether collector profile is verified
     * @param startLocation      Depot / starting coordinates
     * @param pickupRequests     List of accepted pickup requests
     * @param roundTrip          Whether the route returns to start location
     * @return Fully populated RoutePlanResponse
     */
    RoutePlanResponse optimizeRoute(
            Long collectorId,
            String collectorName,
            boolean isVerified,
            LocationDto startLocation,
            List<PickupRequest> pickupRequests,
            boolean roundTrip
    );
}
