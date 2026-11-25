package com.waste2value.routing;

import com.waste2value.routing.dto.OptimizeRouteRequest;
import com.waste2value.routing.dto.PickupStopResponse;
import com.waste2value.routing.dto.RoutePlanResponse;
import com.waste2value.routing.dto.RouteSummaryResponse;
import com.waste2value.shared.ApiResponse;
import com.waste2value.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes/collectors/me")
@Tag(name = "Route Planning & Optimization", description = "Smart pickup route planning and traveling sequence optimization for verified collectors")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('COLLECTOR')")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/plan")
    @Operation(summary = "Get current optimized pickup route plan for authenticated collector")
    public ResponseEntity<ApiResponse<RoutePlanResponse>> getRoutePlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Double startLat,
            @RequestParam(required = false) Double startLon,
            @RequestParam(required = false) String startAddress,
            @RequestParam(required = false, defaultValue = "false") Boolean roundTrip
    ) {
        OptimizeRouteRequest request = null;
        if (startLat != null || startLon != null || startAddress != null || (roundTrip != null && roundTrip)) {
            request = new OptimizeRouteRequest(
                    startLat != null ? java.math.BigDecimal.valueOf(startLat) : null,
                    startLon != null ? java.math.BigDecimal.valueOf(startLon) : null,
                    startAddress,
                    roundTrip
            );
        }
        RoutePlanResponse plan = routeService.getRoutePlan(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(plan, "Route plan retrieved successfully"));
    }

    @GetMapping("/stops")
    @Operation(summary = "Get ordered pickup stops in the collector's current route sequence")
    public ResponseEntity<ApiResponse<List<PickupStopResponse>>> getStops(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<PickupStopResponse> stops = routeService.getStops(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(stops, "Ordered pickup stops retrieved successfully"));
    }

    @PostMapping("/optimize")
    @Operation(summary = "Recompute and optimize visiting sequence using Nearest-Neighbor + 2-Opt")
    public ResponseEntity<ApiResponse<RoutePlanResponse>> optimizeRoute(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) OptimizeRouteRequest request
    ) {
        RoutePlanResponse plan = routeService.optimizeRoute(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(plan, "Route optimized successfully"));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get quick route summary KPIs for collector dashboard")
    public ResponseEntity<ApiResponse<RouteSummaryResponse>> getRouteSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        RouteSummaryResponse summary = routeService.getRouteSummary(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(summary, "Route summary retrieved successfully"));
    }
}
