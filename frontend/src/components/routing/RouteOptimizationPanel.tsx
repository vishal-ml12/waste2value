import React, { useState, useEffect } from 'react';
import { routingApi } from '../../api/client';
import { RoutePlanResponse, OptimizeRouteRequest } from '../../types';
import { RouteMapCanvas } from './RouteMapCanvas';
import { RouteStopsTimeline } from './RouteStopsTimeline';

export const RouteOptimizationPanel: React.FC = () => {
  const [plan, setPlan] = useState<RoutePlanResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [optimizing, setOptimizing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Form controls
  const [roundTrip, setRoundTrip] = useState<boolean>(false);
  const [showCustomOrigin, setShowCustomOrigin] = useState<boolean>(false);
  const [customLat, setCustomLat] = useState<string>('');
  const [customLon, setCustomLon] = useState<string>('');
  const [customAddress, setCustomAddress] = useState<string>('');

  const fetchPlan = async (isOptimization = false) => {
    try {
      if (isOptimization) {
        setOptimizing(true);
      } else {
        setLoading(true);
      }
      setError(null);

      const request: OptimizeRouteRequest = {
        roundTrip,
        startLatitude: customLat ? parseFloat(customLat) : undefined,
        startLongitude: customLon ? parseFloat(customLon) : undefined,
        startAddress: customAddress || undefined,
      };

      const data = isOptimization
        ? await routingApi.optimize(request)
        : await routingApi.getPlan(
            request.startLatitude,
            request.startLongitude,
            request.startAddress,
            request.roundTrip
          );

      setPlan(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load route plan.');
    } finally {
      setLoading(false);
      setOptimizing(false);
    }
  };

  useEffect(() => {
    fetchPlan(false);
  }, [roundTrip]);

  const handleOptimizeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchPlan(true);
  };

  if (loading && !plan) {
    return (
      <div className="py-16 text-center text-slate-400">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600 mx-auto mb-2"></div>
        <span className="text-xs">Computing optimal collection sequence & coordinates...</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Control & Optimization Header */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-bold text-slate-800 flex items-center gap-2">
              <span className="p-1.5 bg-emerald-100 text-emerald-700 rounded-lg">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </span>
              Smart Route Optimization (Nearest-Neighbor + 2-Opt)
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              Sequence eligible accepted pickups to minimize transit distance between collection stops.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer bg-slate-50 px-3 py-2 rounded-lg border border-slate-200 hover:bg-slate-100 transition">
              <input
                type="checkbox"
                checked={roundTrip}
                onChange={(e) => setRoundTrip(e.target.checked)}
                className="w-4 h-4 text-emerald-600 rounded border-slate-300 focus:ring-emerald-500"
              />
              <span>Round-Trip (Return to Depot)</span>
            </label>

            <button
              type="button"
              onClick={() => setShowCustomOrigin(!showCustomOrigin)}
              className="text-xs font-semibold text-slate-600 hover:text-slate-800 underline px-2 py-1"
            >
              {showCustomOrigin ? 'Hide Custom Depot' : 'Custom Depot Location'}
            </button>

            <button
              onClick={handleOptimizeSubmit}
              disabled={optimizing}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition flex items-center gap-2 disabled:opacity-50"
            >
              {optimizing ? (
                <>
                  <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-white"></div>
                  <span>Optimizing...</span>
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  <span>Recompute Route</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Custom Start Location Drawer */}
        {showCustomOrigin && (
          <form onSubmit={handleOptimizeSubmit} className="pt-3 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                Custom Start Latitude
              </label>
              <input
                type="number"
                step="0.000001"
                min="-90"
                max="90"
                value={customLat}
                onChange={(e) => setCustomLat(e.target.value)}
                placeholder="e.g. 19.0760"
                className="w-full px-3 py-1.5 border border-slate-300 rounded-lg text-xs focus:ring-1 focus:ring-emerald-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                Custom Start Longitude
              </label>
              <input
                type="number"
                step="0.000001"
                min="-180"
                max="180"
                value={customLon}
                onChange={(e) => setCustomLon(e.target.value)}
                placeholder="e.g. 72.8777"
                className="w-full px-3 py-1.5 border border-slate-300 rounded-lg text-xs focus:ring-1 focus:ring-emerald-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                Depot Name / Address
              </label>
              <input
                type="text"
                value={customAddress}
                onChange={(e) => setCustomAddress(e.target.value)}
                placeholder="e.g. Central Warehouse, Kurla"
                className="w-full px-3 py-1.5 border border-slate-300 rounded-lg text-xs focus:ring-1 focus:ring-emerald-500 focus:outline-none"
              />
            </div>
          </form>
        )}
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-center justify-between">
          <span>{error}</span>
          <button
            onClick={() => fetchPlan(false)}
            className="text-xs font-semibold underline text-red-800"
          >
            Retry
          </button>
        </div>
      )}

      {plan && (
        <>
          {/* Route Summary KPI Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
              <span className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">
                Total Route Distance
              </span>
              <div className="text-2xl font-bold text-slate-900 mt-2">
                {plan.totalEstimatedDistanceKm.toLocaleString()}{' '}
                <span className="text-sm font-normal text-slate-500">km</span>
              </div>
              <span className="text-[11px] text-slate-400 mt-1 block">
                {plan.roundTrip ? 'Round-trip including return leg' : 'One-way pickup path'}
              </span>
            </div>

            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
              <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
                Pickup Stops
              </span>
              <div className="text-2xl font-bold text-blue-700 mt-2">
                {plan.totalStopsCount}
              </div>
              <span className="text-[11px] text-slate-400 mt-1 block">
                {plan.mappableStopsCount} with GPS · {plan.unmappableStopsCount} missing coords
              </span>
            </div>

            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
              <span className="text-xs font-semibold text-purple-600 uppercase tracking-wider">
                Start Origin (Depot)
              </span>
              <div className="text-sm font-bold text-slate-900 mt-2 truncate">
                {plan.startLocation?.name || 'Registered Depot'}
              </div>
              <span className="text-[11px] text-slate-400 mt-1 block truncate">
                {plan.startLocation?.city || 'Default Base Location'}
              </span>
            </div>

            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
              <span className="text-xs font-semibold text-teal-600 uppercase tracking-wider">
                Routing Mode
              </span>
              <div className="text-sm font-bold text-teal-700 mt-2">
                Straight-Line Haversine
              </div>
              <span className="text-[11px] text-slate-400 mt-1 block">
                Algorithm: {plan.algorithm}
              </span>
            </div>
          </div>

          {/* Map Canvas */}
          <RouteMapCanvas
            startLocation={plan.startLocation}
            stops={plan.stops}
            roundTrip={plan.roundTrip}
          />

          {/* Detailed Stops Timeline */}
          <RouteStopsTimeline
            stops={plan.stops}
            depotName={plan.startLocation?.name}
            depotCity={plan.startLocation?.city ?? undefined}
          />

          {/* Methodology & Routing Limitations Notice */}
          <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl text-slate-600 text-xs leading-relaxed space-y-1.5">
            <span className="font-bold text-slate-700 uppercase tracking-wider block">
              Routing Accuracy & Engineering Disclosure:
            </span>
            <p>
              {plan.disclaimer}
            </p>
            <p className="text-[11px] text-slate-500">
              Visiting sequence is optimized using Nearest-Neighbor tour construction with 2-Opt edge exchange local search heuristics. Road turn-by-turn navigation, traffic delays, and vehicle height/weight restrictions require an accredited road-routing provider and are not fabricated.
            </p>
          </div>
        </>
      )}
    </div>
  );
};
