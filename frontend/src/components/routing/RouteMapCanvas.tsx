import React, { useState } from 'react';
import { LocationDto, PickupStopResponse } from '../../types';

interface RouteMapCanvasProps {
  startLocation?: LocationDto | null;
  stops: PickupStopResponse[];
  roundTrip: boolean;
}

export const RouteMapCanvas: React.FC<RouteMapCanvasProps> = ({
  startLocation,
  stops,
  roundTrip,
}) => {
  const [hoveredStop, setHoveredStop] = useState<PickupStopResponse | null>(null);

  const mappableStops = stops.filter(
    (s) => s.hasValidCoordinates && s.latitude != null && s.longitude != null
  );

  const hasDepot =
    startLocation?.isValidCoordinate &&
    startLocation.latitude != null &&
    startLocation.longitude != null;

  if (mappableStops.length === 0 && !hasDepot) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-slate-400 shadow-sm">
        <div className="w-12 h-12 mx-auto mb-3 text-slate-300">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
          </svg>
        </div>
        <p className="text-sm font-semibold text-slate-700">Map Visualization Unavailable</p>
        <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
          No valid geographic coordinates were found for the accepted pickup stops or start depot.
        </p>
      </div>
    );
  }

  // Collect all valid points to calculate bounding box
  const allPoints: Array<{ lat: number; lon: number; label: string; isDepot?: boolean }> = [];
  if (hasDepot && startLocation?.latitude && startLocation?.longitude) {
    allPoints.push({
      lat: Number(startLocation.latitude),
      lon: Number(startLocation.longitude),
      label: startLocation.name || 'Start Depot',
      isDepot: true,
    });
  }

  mappableStops.forEach((s) => {
    allPoints.push({
      lat: Number(s.latitude!),
      lon: Number(s.longitude!),
      label: `#${s.stopSequence} ${s.listingTitle}`,
    });
  });

  const lats = allPoints.map((p) => p.lat);
  const lons = allPoints.map((p) => p.lon);

  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLon = Math.min(...lons);
  const maxLon = Math.max(...lons);

  // SVG viewBox parameters with padding
  const svgWidth = 600;
  const svgHeight = 360;
  const padding = 50;

  const latRange = maxLat - minLat || 0.01;
  const lonRange = maxLon - minLon || 0.01;

  // Project lat/lon to SVG canvas coordinate space
  // Longitude maps to X (left to right)
  // Latitude maps to Y (top to bottom; higher lat is north/up, so invert)
  const projectX = (lon: number) => {
    return padding + ((lon - minLon) / lonRange) * (svgWidth - 2 * padding);
  };

  const projectY = (lat: number) => {
    return svgHeight - padding - ((lat - minLat) / latRange) * (svgHeight - 2 * padding);
  };

  // Build the route polyline points
  const routePoints: Array<{ x: number; y: number }> = [];
  if (hasDepot && startLocation?.latitude && startLocation?.longitude) {
    routePoints.push({
      x: projectX(Number(startLocation.latitude)),
      y: projectY(Number(startLocation.latitude)),
    });
  }

  mappableStops.forEach((s) => {
    routePoints.push({
      x: projectX(Number(s.longitude!)),
      y: projectY(Number(s.latitude!)),
    });
  });

  if (roundTrip && hasDepot && routePoints.length > 1) {
    routePoints.push(routePoints[0]); // Return to start depot
  }

  const polylinePath = routePoints.map((p) => `${p.x},${p.y}`).join(' ');

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm overflow-hidden space-y-4">
      {/* Header & Legend */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100">
        <div>
          <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2">
            <span className="p-1 bg-emerald-100 text-emerald-700 rounded-md">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
              </svg>
            </span>
            Geographic Route Topology & Stop Sequence
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Straight-line vector visualization of the optimized visiting order.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3 text-xs">
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-blue-600 inline-block"></span>
            <span className="text-slate-600 font-medium">Depot</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-emerald-600 inline-block"></span>
            <span className="text-slate-600 font-medium">Pickup Stop</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-4 h-0.5 bg-emerald-500 inline-block"></span>
            <span className="text-slate-600 font-medium">Path</span>
          </div>
          {roundTrip && (
            <div className="flex items-center gap-1.5">
              <span className="w-4 h-0.5 border-t border-dashed border-blue-400 inline-block"></span>
              <span className="text-slate-600 font-medium">Return Leg</span>
            </div>
          )}
        </div>
      </div>

      {/* SVG Canvas Map */}
      <div className="relative bg-slate-50 rounded-xl border border-slate-200 overflow-hidden">
        <svg
          viewBox={`0 0 ${svgWidth} ${svgHeight}`}
          className="w-full h-72 sm:h-80 select-none"
        >
          {/* Subtle Grid Background */}
          <defs>
            <pattern id="route-grid" width="30" height="30" patternUnits="userSpaceOnUse">
              <path d="M 30 0 L 0 0 0 30" fill="none" stroke="#e2e8f0" strokeWidth="0.5" />
            </pattern>
          </defs>
          <rect width={svgWidth} height={svgHeight} fill="url(#route-grid)" />

          {/* Polyline Route Vector */}
          {routePoints.length > 1 && (
            <polyline
              points={polylinePath}
              fill="none"
              stroke="#059669"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeDasharray={roundTrip ? 'none' : 'none'}
            />
          )}

          {/* Return Leg dashed if round trip */}
          {roundTrip && routePoints.length > 2 && (
            <line
              x1={routePoints[routePoints.length - 2].x}
              y1={routePoints[routePoints.length - 2].y}
              x2={routePoints[routePoints.length - 1].x}
              y2={routePoints[routePoints.length - 1].y}
              stroke="#3b82f6"
              strokeWidth="2"
              strokeDasharray="5,5"
            />
          )}

          {/* Start Depot Marker */}
          {hasDepot && startLocation?.latitude && startLocation?.longitude && (
            <g
              transform={`translate(${projectX(Number(startLocation.longitude))}, ${projectY(
                Number(startLocation.latitude)
              )})`}
              className="cursor-pointer"
            >
              <circle r="12" fill="#1e40af" opacity="0.2" />
              <circle r="7" fill="#1d4ed8" stroke="#ffffff" strokeWidth="2" />
              <text
                y="-11"
                textAnchor="middle"
                fontSize="10"
                fontWeight="bold"
                fill="#1e3a8a"
              >
                Depot
              </text>
            </g>
          )}

          {/* Pickup Stops Markers */}
          {mappableStops.map((stop) => {
            const x = projectX(Number(stop.longitude!));
            const y = projectY(Number(stop.latitude!));
            const isHovered = hoveredStop?.pickupRequestId === stop.pickupRequestId;

            return (
              <g
                key={stop.pickupRequestId}
                transform={`translate(${x}, ${y})`}
                className="cursor-pointer transition-transform duration-200"
                onMouseEnter={() => setHoveredStop(stop)}
                onMouseLeave={() => setHoveredStop(null)}
              >
                {/* Outer Pulse ring on hover */}
                {isHovered && <circle r="14" fill="#10b981" opacity="0.3" />}

                {/* Pin Circle */}
                <circle
                  r={isHovered ? '9' : '8'}
                  fill={stop.isCoLocated ? '#d97706' : '#059669'}
                  stroke="#ffffff"
                  strokeWidth="2"
                />

                {/* Sequence Number Label */}
                <text
                  textAnchor="middle"
                  dy="3.5"
                  fontSize="9"
                  fontWeight="bold"
                  fill="#ffffff"
                >
                  {stop.stopSequence}
                </text>

                {/* Stop Title Tag */}
                <text
                  y="18"
                  textAnchor="middle"
                  fontSize="9"
                  fontWeight="600"
                  fill="#334155"
                  className="bg-white"
                >
                  #{stop.stopSequence}
                </text>
              </g>
            );
          })}
        </svg>

        {/* Hover Tooltip Overlay */}
        {hoveredStop && (
          <div className="absolute bottom-3 left-3 z-10 bg-slate-900/90 text-white rounded-lg p-3 text-xs shadow-xl backdrop-blur-sm pointer-events-none max-w-xs">
            <div className="font-bold text-emerald-400">
              Stop #{hoveredStop.stopSequence}: {hoveredStop.listingTitle}
            </div>
            <div className="text-slate-300 mt-1">
              {hoveredStop.categoryName} · {hoveredStop.quantity} {hoveredStop.unit}
            </div>
            <div className="text-slate-400 text-[11px] mt-0.5">
              {hoveredStop.city}, {hoveredStop.state}
            </div>
            <div className="text-teal-300 font-semibold mt-1">
              Leg: +{hoveredStop.legDistanceKm ?? 0} km (Cum: {hoveredStop.cumulativeDistanceKm} km)
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
