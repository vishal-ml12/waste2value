import React, { useEffect, useState } from 'react';
import { matchingApi } from '../../api/client';
import { CollectorRecommendation, ProcessorRecommendation } from '../../types';

interface SmartMatchingRecommendationsProps {
  listingId: number;
  listingStatus: string;
  isOwner: boolean;
}

export const SmartMatchingRecommendations: React.FC<SmartMatchingRecommendationsProps> = ({
  listingId,
  listingStatus,
  isOwner,
}) => {
  const [activeTab, setActiveTab] = useState<'collectors' | 'processors'>('collectors');
  const [collectors, setCollectors] = useState<CollectorRecommendation[]>([]);
  const [processors, setProcessors] = useState<ProcessorRecommendation[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchRecommendations = async () => {
    if (listingStatus !== 'POSTED' && listingStatus !== 'ALLOCATED') return;
    setLoading(true);
    setError(null);
    try {
      if (activeTab === 'collectors') {
        if (listingStatus === 'POSTED') {
          const data = await matchingApi.getRecommendedCollectors(listingId);
          setCollectors(data);
        }
      } else {
        const data = await matchingApi.getRecommendedProcessors(listingId);
        setProcessors(data);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to retrieve smart recommendations');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecommendations();
  }, [listingId, activeTab]);

  if (listingStatus !== 'POSTED' && listingStatus !== 'ALLOCATED') {
    return null;
  }

  const getScoreColor = (score: number) => {
    if (score >= 80) return 'bg-emerald-50 text-emerald-700 border-emerald-200';
    if (score >= 60) return 'bg-amber-50 text-amber-700 border-amber-200';
    return 'bg-blue-50 text-blue-700 border-blue-200';
  };

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-gray-100 pb-4">
        <div>
          <div className="flex items-center space-x-2">
            <span className="text-xl">⚡</span>
            <h3 className="text-base font-bold text-gray-900">Smart Matching Recommendations</h3>
          </div>
          <p className="text-xs text-gray-500 mt-0.5">
            Rule-filtered and ranked by normalized suitability score (0–100)
          </p>
        </div>

        <div className="flex bg-gray-100 p-1 rounded-lg text-xs font-semibold">
          {listingStatus === 'POSTED' && (
            <button
              onClick={() => setActiveTab('collectors')}
              className={`px-3 py-1.5 rounded-md transition-colors ${
                activeTab === 'collectors'
                  ? 'bg-white text-emerald-800 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Eligible Collectors
            </button>
          )}
          <button
            onClick={() => setActiveTab('processors')}
            className={`px-3 py-1.5 rounded-md transition-colors ${
              activeTab === 'processors'
                ? 'bg-white text-emerald-800 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Eligible Recycling Processors
          </button>
        </div>
      </div>

      {/* Accuracy & Transparency Notice */}
      <div className="bg-slate-50 border border-slate-200 rounded-lg p-3 text-[11px] text-slate-600 flex items-start space-x-2">
        <span className="text-sm">ℹ️</span>
        <div>
          <span className="font-semibold text-slate-800">Transparency Note: </span>
          Suitability scores are algorithmic estimates based on verified credentials, service territory, capacity, and historical rating. Distances indicate approximate straight-line radius, not road travel distance. Recommendations do not automatically allocate or commit listings.
        </div>
      </div>

      {loading ? (
        <div className="py-8 text-center text-xs text-gray-500">
          Evaluating candidates through hybrid eligibility filters and scoring algorithm...
        </div>
      ) : error ? (
        <div className="p-4 bg-rose-50 text-rose-700 text-xs rounded-lg border border-rose-200 flex justify-between items-center">
          <span>{error}</span>
          <button
            onClick={fetchRecommendations}
            className="text-xs font-semibold underline hover:text-rose-900"
          >
            Retry
          </button>
        </div>
      ) : activeTab === 'collectors' ? (
        collectors.length === 0 ? (
          <div className="py-8 text-center text-xs text-gray-400 bg-gray-50 rounded-lg border border-dashed border-gray-200">
            No collectors currently meet all eligibility filters (verified status, operating radius, and active standing) for this pickup location.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {collectors.map((c) => (
              <div
                key={c.collectorId}
                className="border border-gray-200 rounded-xl p-4 bg-white hover:border-emerald-300 transition-all flex flex-col justify-between space-y-3"
              >
                <div className="space-y-2">
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="font-bold text-gray-900 text-sm">{c.collectorName}</h4>
                      <div className="text-xs text-gray-500 flex items-center space-x-2 mt-0.5">
                        <span>🚛 {c.vehicleType}</span>
                        <span>•</span>
                        <span>📍 {c.serviceArea}</span>
                      </div>
                    </div>
                    <div className={`px-2.5 py-1 rounded-full text-xs font-bold border ${getScoreColor(c.suitabilityScore)}`}>
                      {c.suitabilityScore}% Match
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 text-xs text-gray-600 pt-1">
                    {c.avgRating > 0 ? (
                      <span className="font-semibold text-amber-700">★ {c.avgRating.toFixed(1)} / 5.0</span>
                    ) : (
                      <span className="text-gray-400">New verified collector</span>
                    )}
                    <span>•</span>
                    <span>{c.totalPickups} completed pickups</span>
                    <span>•</span>
                    <span>
                      {c.isDistanceAvailable && c.distanceKm != null
                        ? `~${c.distanceKm} km straight-line`
                        : 'Distance unavailable'}
                    </span>
                  </div>

                  {c.matchReasons.length > 0 && (
                    <div className="flex flex-wrap gap-1 pt-2 border-t border-gray-100">
                      {c.matchReasons.map((reason, idx) => (
                        <span
                          key={idx}
                          className="bg-gray-100 text-gray-700 text-[10px] px-2 py-0.5 rounded-md font-medium"
                        >
                          {reason}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                <div className="text-[11px] text-gray-400 pt-2 border-t border-gray-50">
                  Max Radius: up to {c.maxDistanceKm} km
                </div>
              </div>
            ))}
          </div>
        )
      ) : processors.length === 0 ? (
        <div className="py-8 text-center text-xs text-gray-400 bg-gray-50 rounded-lg border border-dashed border-gray-200">
          No verified recycling processors currently have declared intake capabilities or capacity headroom for this material category.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {processors.map((p) => (
            <div
              key={p.processorId}
              className="border border-gray-200 rounded-xl p-4 bg-white hover:border-emerald-300 transition-all flex flex-col justify-between space-y-3"
            >
              <div className="space-y-2">
                <div className="flex items-start justify-between">
                  <div>
                    <h4 className="font-bold text-gray-900 text-sm">{p.companyName}</h4>
                    <div className="text-xs text-emerald-700 font-medium mt-0.5">
                      Intake: {p.categoryName}
                    </div>
                  </div>
                  <div className={`px-2.5 py-1 rounded-full text-xs font-bold border ${getScoreColor(p.suitabilityScore)}`}>
                    {p.suitabilityScore}% Match
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2 text-xs text-gray-600 pt-1">
                  {p.avgRating > 0 ? (
                    <span className="font-semibold text-amber-700">★ {p.avgRating.toFixed(1)} / 5.0</span>
                  ) : (
                    <span className="text-gray-400">Verified facility</span>
                  )}
                  <span>•</span>
                  <span className="font-medium text-emerald-700">
                    {p.availableCapacityKg.toLocaleString()} kg available
                  </span>
                  <span>•</span>
                  <span>
                    {p.isDistanceAvailable && p.distanceKm != null
                      ? `~${p.distanceKm} km straight-line`
                      : 'Distance unavailable'}
                  </span>
                </div>

                {p.pricePerUnit > 0 && (
                  <div className="text-xs text-gray-700">
                    Indicative Price: <span className="font-semibold text-emerald-800">₹{p.pricePerUnit} / unit</span>
                  </div>
                )}

                {p.matchReasons.length > 0 && (
                  <div className="flex flex-wrap gap-1 pt-2 border-t border-gray-100">
                    {p.matchReasons.map((reason, idx) => (
                      <span
                        key={idx}
                        className="bg-gray-100 text-gray-700 text-[10px] px-2 py-0.5 rounded-md font-medium"
                      >
                        {reason}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              <div className="text-[11px] text-gray-400 pt-2 border-t border-gray-50 flex justify-between">
                <span>License: {p.licenseNumber}</span>
                <span>Max: {p.maxCapacityKg.toLocaleString()} kg</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
