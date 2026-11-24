import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../../api/client';
import { GeneratorDashboardStats } from '../../types';
import { StatusBadge } from '../../components/common/StatusBadge';
import { useAuth } from '../../context/AuthContext';
import { SmartMatchingRecommendations } from '../../components/matching/SmartMatchingRecommendations';

export const GeneratorDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState<GeneratorDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedListingId, setSelectedListingId] = useState<number | null>(null);

  useEffect(() => {
    dashboardApi.getGeneratorStats()
      .then((data) => {
        setStats(data);
        const posted = data.recentListings?.filter((l) => l.status === 'POSTED') || [];
        if (posted.length > 0) {
          setSelectedListingId(posted[0].id);
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const postedListings = stats?.recentListings?.filter((l) => l.status === 'POSTED') || [];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Generator Dashboard
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Welcome back, <span className="font-semibold text-gray-900">{user?.firstName}</span>. Track your active lots and recovery activity.
          </p>
        </div>

        <div className="flex space-x-3">
          <Link
            to="/generator/addresses"
            className="px-4 py-2 border border-gray-300 rounded-lg text-xs font-semibold text-gray-700 bg-white hover:bg-gray-50 transition"
          >
            Manage Addresses
          </Link>
          <Link
            to="/generator/requests"
            className="px-4 py-2 border border-emerald-300 rounded-lg text-xs font-semibold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 transition"
          >
            Pickup Requests
          </Link>
          <Link
            to="/generator/listings/new"
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
          >
            + Create Listing
          </Link>
        </div>
      </div>

      {/* Metrics Cards Grid */}
      {loading ? (
        <div className="py-12 text-center text-sm text-gray-500">Loading accurate dashboard metrics...</div>
      ) : stats ? (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <div className="text-xs font-medium text-gray-500">Active (Posted)</div>
            <div className="text-2xl font-bold text-emerald-600 mt-2">{stats.activeListingsCount}</div>
            <div className="text-[11px] text-gray-400 mt-1">Ready for pickup</div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <div className="text-xs font-medium text-gray-500">Draft Lots</div>
            <div className="text-2xl font-bold text-amber-600 mt-2">{stats.draftListingsCount}</div>
            <div className="text-[11px] text-gray-400 mt-1">Pending publication</div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <div className="text-xs font-medium text-gray-500">Completed</div>
            <div className="text-2xl font-bold text-purple-600 mt-2">{stats.completedListingsCount}</div>
            <div className="text-[11px] text-gray-400 mt-1">Successfully collected</div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
            <div className="text-xs font-medium text-gray-500">Cancelled</div>
            <div className="text-2xl font-bold text-gray-500 mt-2">{stats.cancelledListingsCount}</div>
            <div className="text-[11px] text-gray-400 mt-1">Withdrawn lots</div>
          </div>

          <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm col-span-2 md:col-span-1">
            <div className="text-xs font-medium text-gray-500">Total Material Listed</div>
            <div className="text-2xl font-bold text-gray-900 mt-2">{stats.totalQuantityListed.toLocaleString()}</div>
            <div className="text-[11px] text-emerald-600 font-medium mt-1">Aggregated units</div>
          </div>
        </div>
      ) : null}

      {/* Recommended Collectors & Processors (Phase 3A) */}
      {postedListings.length > 0 && (
        <div className="space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <h2 className="text-base font-bold text-gray-900">Recommended Collectors & Processors</h2>
            {postedListings.length > 1 && (
              <div className="flex items-center space-x-2 text-xs">
                <span className="text-gray-500">For Active Lot:</span>
                <select
                  value={selectedListingId || postedListings[0].id}
                  onChange={(e) => setSelectedListingId(Number(e.target.value))}
                  className="border border-gray-300 rounded px-2 py-1 text-xs font-medium focus:ring-1 focus:ring-emerald-500 bg-white"
                >
                  {postedListings.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.title} ({l.quantity} {l.unit})
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          <SmartMatchingRecommendations
            listingId={selectedListingId || postedListings[0].id}
            listingStatus="POSTED"
            isOwner={true}
          />
        </div>
      )}

      {/* Recent Listings Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center">
          <h2 className="font-bold text-sm text-gray-900">Recent Listings Activity</h2>
          <Link to="/generator/listings" className="text-xs font-semibold text-emerald-600 hover:text-emerald-700">
            View All My Listings ({stats?.totalListingsCount || 0}) →
          </Link>
        </div>

        {stats?.recentListings && stats.recentListings.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-left text-xs">
              <thead className="bg-gray-50 text-gray-500 font-medium">
                <tr>
                  <th className="px-6 py-3">Listing Title</th>
                  <th className="px-6 py-3">Category</th>
                  <th className="px-6 py-3">Quantity</th>
                  <th className="px-6 py-3">Price</th>
                  <th className="px-6 py-3">Location</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {stats.recentListings.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50 transition">
                    <td className="px-6 py-4 font-semibold text-gray-900">
                      <Link to={`/listings/${item.id}`} className="hover:text-emerald-600">
                        {item.title}
                      </Link>
                    </td>
                    <td className="px-6 py-4 text-gray-600">{item.categoryName}</td>
                    <td className="px-6 py-4 font-medium text-gray-900">{item.quantity} {item.unit}</td>
                    <td className="px-6 py-4 text-emerald-700 font-semibold">₹{item.expectedPrice}</td>
                    <td className="px-6 py-4 text-gray-500">{item.city}, {item.state}</td>
                    <td className="px-6 py-4">
                      <StatusBadge status={item.status} size="sm" />
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <Link
                        to={`/listings/${item.id}`}
                        className="text-emerald-600 hover:text-emerald-800 font-semibold"
                      >
                        Details
                      </Link>
                      {item.status === 'DRAFT' && (
                        <Link
                          to={`/generator/listings/${item.id}/edit`}
                          className="text-gray-600 hover:text-gray-900"
                        >
                          Edit
                        </Link>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-8 text-center text-xs text-gray-500">
            No listings created yet. Click "+ Create Listing" above to create your first waste lot.
          </div>
        )}
      </div>
    </div>
  );
};
