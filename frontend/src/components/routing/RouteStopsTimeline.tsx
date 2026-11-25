import React from 'react';
import { Link } from 'react-router-dom';
import { PickupStopResponse } from '../../types';

interface RouteStopsTimelineProps {
  stops: PickupStopResponse[];
  depotName?: string;
  depotCity?: string;
}

export const RouteStopsTimeline: React.FC<RouteStopsTimelineProps> = ({
  stops,
  depotName = 'Collector Operating Depot',
  depotCity = 'Registered Base',
}) => {
  if (!stops || stops.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-slate-500 shadow-sm">
        <div className="w-12 h-12 mx-auto mb-3 text-slate-300">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
        </div>
        <p className="text-sm font-semibold text-slate-700">No Accepted Pickup Stops Found</p>
        <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
          Pickup requests accepted by waste generators will automatically appear in your route schedule here.
        </p>
      </div>
    );
  }

  const unmappableCount = stops.filter((s) => !s.hasValidCoordinates).length;

  return (
    <div className="space-y-4">
      {/* Alert if any stops are missing coordinates */}
      {unmappableCount > 0 && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3 text-xs text-amber-800">
          <span className="text-amber-600 text-base">⚠️</span>
          <div>
            <span className="font-bold">Missing Coordinates Detected ({unmappableCount} stop{unmappableCount > 1 ? 's' : ''}):</span>
            <p className="mt-0.5 text-amber-700">
              The stops marked below lack valid geographic latitude/longitude. They have been preserved at the end of the route sequence for manual navigation.
            </p>
          </div>
        </div>
      )}

      {/* Depot Origin Header Card */}
      <div className="bg-blue-50/70 border border-blue-200 rounded-xl p-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-xs shadow-sm">
            ⚑
          </div>
          <div>
            <div className="text-xs font-bold text-blue-900 uppercase tracking-wider">
              Route Origin (Depot)
            </div>
            <div className="text-sm font-semibold text-slate-800">{depotName}</div>
            <div className="text-xs text-slate-500">{depotCity}</div>
          </div>
        </div>
        <div className="text-right">
          <span className="inline-block px-2.5 py-1 bg-blue-100 text-blue-800 rounded-md text-xs font-semibold">
            Start Location (0.0 km)
          </span>
        </div>
      </div>

      {/* Ordered Stops List */}
      <div className="relative pl-6 space-y-4 before:absolute before:left-3 before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-200">
        {stops.map((stop) => {
          const isMappable = stop.hasValidCoordinates;

          return (
            <div
              key={stop.pickupRequestId}
              className={`relative bg-white rounded-xl border p-5 shadow-sm transition hover:shadow-md ${
                !isMappable
                  ? 'border-amber-300 bg-amber-50/30'
                  : stop.isCoLocated
                  ? 'border-emerald-300 bg-emerald-50/20'
                  : 'border-slate-200'
              }`}
            >
              {/* Node Marker Dot */}
              <div
                className={`absolute -left-[31px] top-6 w-5 h-5 rounded-full border-2 border-white flex items-center justify-center text-[10px] font-bold text-white shadow-sm ${
                  !isMappable
                    ? 'bg-amber-500'
                    : stop.isCoLocated
                    ? 'bg-amber-600'
                    : 'bg-emerald-600'
                }`}
              >
                {stop.stopSequence}
              </div>

              {/* Card Header */}
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                      Stop #{stop.stopSequence}
                    </span>
                    <span className="text-xs font-medium text-slate-500 bg-slate-100 px-2 py-0.5 rounded">
                      PR-{stop.pickupRequestId}
                    </span>
                    {stop.isCoLocated && (
                      <span className="text-xs font-semibold text-amber-800 bg-amber-100 px-2 py-0.5 rounded border border-amber-300 flex items-center gap-1">
                        <span>📍</span>
                        <span>Co-located with Stop #{stop.coLocatedStopSequence}</span>
                      </span>
                    )}
                  </div>

                  <h4 className="text-base font-bold text-slate-800 hover:text-emerald-600 transition">
                    <Link to={`/listings/${stop.listingId}`}>
                      {stop.listingTitle}
                    </Link>
                  </h4>
                  <div className="text-xs text-slate-500">
                    Category: <span className="font-semibold text-slate-700">{stop.categoryName}</span> · Quantity: <span className="font-bold text-slate-800">{stop.quantity} {stop.unit}</span>
                  </div>
                </div>

                {/* Distance Badge */}
                <div className="text-left sm:text-right flex-shrink-0">
                  {isMappable && stop.legDistanceKm !== null ? (
                    <div>
                      <div className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-md inline-block border border-emerald-200">
                        +{stop.legDistanceKm} km straight-line
                      </div>
                      <div className="text-[11px] text-slate-400 mt-1">
                        Cumulative: {stop.cumulativeDistanceKm} km
                      </div>
                    </div>
                  ) : (
                    <span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-amber-100 text-amber-800 border border-amber-300">
                      Distance Unavailable
                    </span>
                  )}
                </div>
              </div>

              {/* Stop Address & Contact Details */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-4 pt-4 border-t border-slate-100 text-xs">
                <div>
                  <span className="text-slate-400 uppercase tracking-wider font-semibold block text-[10px]">
                    Pickup Location:
                  </span>
                  <div className="text-slate-700 font-medium mt-0.5">
                    {stop.addressLine1}
                    {stop.addressLine2 ? `, ${stop.addressLine2}` : ''}
                  </div>
                  <div className="text-slate-500">
                    {stop.city}, {stop.state} - {stop.postalCode}
                  </div>
                  {isMappable && stop.latitude && stop.longitude && (
                    <div className="text-[11px] text-emerald-700 font-mono mt-1">
                      GPS: {Number(stop.latitude).toFixed(4)}, {Number(stop.longitude).toFixed(4)}
                    </div>
                  )}
                </div>

                <div>
                  <span className="text-slate-400 uppercase tracking-wider font-semibold block text-[10px]">
                    Generator Contact:
                  </span>
                  <div className="text-slate-700 font-semibold mt-0.5">
                    {stop.generatorName}
                  </div>
                  {stop.generatorPhone ? (
                    <a
                      href={`tel:${stop.generatorPhone}`}
                      className="text-emerald-600 hover:underline font-medium inline-block mt-0.5"
                    >
                      📞 {stop.generatorPhone}
                    </a>
                  ) : (
                    <span className="text-slate-400 italic">No phone recorded</span>
                  )}
                  <div className="text-[11px] text-slate-500 mt-1">
                    Offered Price: <b className="text-emerald-700">₹{stop.proposedPrice} / {stop.unit}</b>
                  </div>
                </div>
              </div>

              {/* Status Message if Coordinates are missing */}
              {!isMappable && (
                <div className="mt-3 p-2.5 bg-amber-100/60 border border-amber-300 rounded-lg text-xs text-amber-900 flex items-center gap-2">
                  <span>⚠️</span>
                  <span>{stop.coordinateStatusMessage}</span>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
