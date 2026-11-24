import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { matchingApi } from '../../api/client';
import { ListingRecommendation } from '../../types';

interface RecommendedListingsFeedProps {
  userRole: 'COLLECTOR' | 'PROCESSOR';
}

export const RecommendedListingsFeed: React.FC<RecommendedListingsFeedProps> = ({ userRole }) => {
  const [recommendations, setRecommendations] = useState<ListingRecommendation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchRecommendations = async () => {
    setLoading(true);
    setError(null);
    try {
      if (userRole === 'COLLECTOR') {
        const data = await matchingApi.getCollectorRecommendedListings();
        setRecommendations(data);
      } else {
        const data = await matchingApi.getProcessorRecommendedListings();
        setRecommendations(data);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load smart recommendations');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecommendations();
  }, [userRole]);

  const getScoreColor = (score: number) => {
    if (score >= 80) return 'bg-emerald-50 text-emerald-800 border-emerald-200';
    if (score >= 60) return 'bg-amber-50 text-amber-800 border-amber-200';
    return 'bg-blue-50 text-blue-800 border-blue-200';
  };

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-gray-100 pb-3">
        <div>
          <div className="flex items-center space-x-2">
            <span className="text-xl">🎯</span>
            <h3 className="font-bold text-gray-900 text-sm">
              {userRole === 'COLLECTOR' ? 'Recommended Waste Listings' : 'Recommended Materials'}
            </h3>
          </div>
          <p className="text-xs text-gray-500 mt-0.5">
            {userRole === 'COLLECTOR'
              ? 'Tailored collection opportunities based on your operating territory and vehicle radius'
              : 'Suitable waste lots matching your declared processing capabilities and available plant headroom'}
          </p>
        </div>
        <button
          onClick={fetchRecommendations}
          className="text-xs font-semibold text-emerald-600 hover:text-emerald-700 self-start sm:self-auto"
        >
          Refresh Matches ↻
        </button>
      </div>

      <div className="bg-slate-50 border border-slate-200 rounded-lg p-2.5 text-[11px] text-slate-600 flex items-start space-x-2">
        <span>ℹ️</span>
        <span>
          Suitability estimates (0–100) are generated from distance, capacity, category, and volume criteria. Distances indicate approximate straight-line radius. Recommendations do not automatically commit or allocate lots.
        </span>
      </div>

      {loading ? (
        <div className="py-8 text-center text-xs text-gray-500">
          Scanning marketplace listings through recommendation filters...
        </div>
      ) : error ? (
        <div className="p-3 bg-rose-50 text-rose-700 text-xs rounded-lg border border-rose-200 flex justify-between items-center">
          <span>{error}</span>
          <button onClick={fetchRecommendations} className="font-semibold underline ml-2">
            Retry
          </button>
        </div>
      ) : recommendations.length === 0 ? (
        <div className="py-8 text-center text-xs text-gray-400 bg-gray-50 rounded-lg border border-dashed border-gray-200">
          {userRole === 'COLLECTOR'
            ? 'No POSTED waste listings currently match your verified vehicle service area and distance parameters.'
            : 'No POSTED waste lots currently match your declared processing capabilities and capacity headroom.'}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {recommendations.map((rec) => (
            <div
              key={rec.listingId}
              className="border border-gray-200 rounded-xl p-4 bg-white hover:border-emerald-400 hover:shadow-sm transition-all flex flex-col justify-between space-y-3"
            >
              <div className="space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <h4 className="font-bold text-gray-900 text-sm line-clamp-1">{rec.title}</h4>
                  <span className={`px-2 py-0.5 rounded-full text-[11px] font-bold border flex-shrink-0 ${getScoreColor(rec.suitabilityScore)}`}>
                    {rec.suitabilityScore}% Match
                  </span>
                </div>

                <div className="flex items-center space-x-2 text-xs text-gray-600">
                  <span className="bg-gray-100 text-gray-700 px-2 py-0.5 rounded text-[10px] font-medium">
                    {rec.categoryName}
                  </span>
                  <span>•</span>
                  <span className="font-semibold text-gray-900">
                    {rec.quantity} {rec.unit}
                  </span>
                  <span>•</span>
                  <span className="font-semibold text-emerald-700">₹{rec.expectedPrice}</span>
                </div>

                <div className="text-xs text-gray-500 flex items-center space-x-1">
                  <span>📍 {rec.city ? `${rec.city}, ${rec.state}` : 'Location on file'}</span>
                  {rec.isDistanceAvailable && rec.distanceKm != null && (
                    <span className="text-[11px] font-medium text-emerald-700">
                      (~{rec.distanceKm} km straight-line)
                    </span>
                  )}
                </div>

                {rec.matchReasons.length > 0 && (
                  <div className="flex flex-wrap gap-1 pt-2 border-t border-gray-100">
                    {rec.matchReasons.slice(0, 3).map((reason, idx) => (
                      <span
                        key={idx}
                        className="bg-gray-100 text-gray-700 text-[10px] px-1.5 py-0.5 rounded font-medium"
                      >
                        {reason}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              <div className="pt-2 border-t border-gray-50 flex items-center justify-between">
                <span className="text-[10px] text-gray-400">By {rec.generatorName}</span>
                <Link
                  to={`/listings/${rec.listingId}`}
                  className="px-3 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded text-xs font-semibold shadow-sm transition"
                >
                  View Details →
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
