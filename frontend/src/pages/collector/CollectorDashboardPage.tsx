import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../api/client';
import { ApiResponse, CollectorDashboardStats } from '../../types';
import { StatusBadge } from '../../components/common/StatusBadge';
import { RecommendedListingsFeed } from '../../components/matching/RecommendedListingsFeed';

export const CollectorDashboardPage: React.FC = () => {
  const [stats, setStats] = useState<CollectorDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<CollectorDashboardStats>>('/dashboard/collector');
      setStats(res.data.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load collector dashboard');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-12 flex justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Collector Operations Center</h1>
          <p className="text-sm text-gray-500 mt-1">Manage waste collection routes, pickup requests, and deliveries</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            to="/collector/profile"
            className="px-4 py-2 border border-gray-300 text-gray-700 bg-white rounded-lg text-sm font-medium hover:bg-gray-50 shadow-sm"
          >
            Vehicle & Profile Settings
          </Link>
          <Link
            to="/browse"
            className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 shadow-sm transition-colors"
          >
            Browse Waste Listings
          </Link>
        </div>
      </div>

      {/* Verification Notice Banner */}
      {stats && !stats.isVerified && (
        <div className="bg-amber-50 border-l-4 border-amber-500 p-4 rounded-r-lg">
          <div className="flex items-start">
            <div className="flex-shrink-0 text-amber-500 text-lg">⚠️</div>
            <div className="ml-3">
              <h3 className="text-sm font-semibold text-amber-800">Verification Pending</h3>
              <p className="text-xs text-amber-700 mt-1">
                Your collector vehicle profile is currently awaiting platform administrator verification. You can browse listings now, and submit pickup requests once verified.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Metrics Grid */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <span className="text-xs font-medium text-gray-500 uppercase tracking-wider">Available Listings</span>
            <div className="text-2xl font-bold text-gray-900 mt-2">{stats.availableListingsCount}</div>
            <span className="text-[11px] text-emerald-600 mt-1 block">Ready for collection in market</span>
          </div>

          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <span className="text-xs font-medium text-gray-500 uppercase tracking-wider">Accepted Pickups</span>
            <div className="text-2xl font-bold text-emerald-600 mt-2">{stats.acceptedPickupsCount}</div>
            <Link to="/collector/requests" className="text-[11px] text-emerald-700 underline mt-1 block hover:text-emerald-800">
              View active pickups →
            </Link>
          </div>

          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <span className="text-xs font-medium text-gray-500 uppercase tracking-wider">Completed Pickups</span>
            <div className="text-2xl font-bold text-gray-900 mt-2">{stats.completedPickupsCount}</div>
            <span className="text-[11px] text-gray-500 mt-1 block">{stats.totalQuantityCollected} kg collected</span>
          </div>

          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <span className="text-xs font-medium text-gray-500 uppercase tracking-wider">Collector Rating</span>
            <div className="text-2xl font-bold text-amber-500 mt-2 flex items-center">
              ★ {stats.avgRating > 0 ? stats.avgRating.toFixed(2) : '5.00'}
            </div>
            <span className="text-[11px] text-gray-500 mt-1 block">Value: ₹{stats.totalTransactionValue.toLocaleString()}</span>
          </div>
        </div>
      )}

      {/* Quick Navigation Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Link
          to="/collector/requests"
          className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors flex items-center justify-between"
        >
          <div>
            <h4 className="font-semibold text-gray-900 text-sm">My Pickup Requests</h4>
            <p className="text-xs text-gray-500 mt-0.5">Track negotiations, approvals, and status</p>
          </div>
          <span className="text-emerald-600 font-bold text-lg">→</span>
        </Link>

        <Link
          to="/browse"
          className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors flex items-center justify-between"
        >
          <div>
            <h4 className="font-semibold text-gray-900 text-sm">Browse Marketplace</h4>
            <p className="text-xs text-gray-500 mt-0.5">Find recyclable materials ready for pickup</p>
          </div>
          <span className="text-emerald-600 font-bold text-lg">→</span>
        </Link>

        <Link
          to="/collector/deliveries"
          className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors flex items-center justify-between"
        >
          <div>
            <h4 className="font-semibold text-gray-900 text-sm">Processor Deliveries</h4>
            <p className="text-xs text-gray-500 mt-0.5">View intake receipts at processing facilities</p>
          </div>
          <span className="text-emerald-600 font-bold text-lg">→</span>
        </Link>
      </div>

      {/* Recommended Waste Listings (Phase 3A) */}
      <RecommendedListingsFeed userRole="COLLECTOR" />

      {/* Recent Pickup Activity */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h3 className="font-semibold text-gray-900 text-sm">Recent Pickup Requests</h3>
          <Link to="/collector/requests" className="text-xs text-emerald-600 hover:text-emerald-700 font-medium">
            View All Requests →
          </Link>
        </div>

        {stats && stats.recentRequests.length === 0 ? (
          <div className="p-8 text-center text-gray-500 text-xs">
            No pickup requests submitted yet. Browse the marketplace to find available recyclable materials.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-gray-50 text-gray-600 border-b border-gray-100">
                <tr>
                  <th className="px-6 py-3 font-semibold">Listing</th>
                  <th className="px-6 py-3 font-semibold">Category</th>
                  <th className="px-6 py-3 font-semibold">Offered Price</th>
                  <th className="px-6 py-3 font-semibold">Location</th>
                  <th className="px-6 py-3 font-semibold">Status</th>
                  <th className="px-6 py-3 font-semibold">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-gray-700">
                {stats?.recentRequests.map((req) => (
                  <tr key={req.id} className="hover:bg-gray-50">
                    <td className="px-6 py-3 font-medium text-gray-900">{req.listingTitle}</td>
                    <td className="px-6 py-3">{req.categoryName}</td>
                    <td className="px-6 py-3 font-semibold text-emerald-700">₹{req.proposedPrice} / {req.unit}</td>
                    <td className="px-6 py-3">{req.pickupCity}, {req.pickupState}</td>
                    <td className="px-6 py-3">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                        req.status === 'ACCEPTED' ? 'bg-emerald-100 text-emerald-800' :
                        req.status === 'COMPLETED' ? 'bg-blue-100 text-blue-800' :
                        req.status === 'REQUESTED' ? 'bg-amber-100 text-amber-800' :
                        'bg-gray-100 text-gray-800'
                      }`}>
                        {req.status}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-gray-500">{new Date(req.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
