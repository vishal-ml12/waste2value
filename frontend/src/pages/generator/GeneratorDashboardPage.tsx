import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi, analyticsApi } from '../../api/client';
import { GeneratorDashboardStats, GeneratorAnalyticsResponse } from '../../types';
import { StatusBadge } from '../../components/common/StatusBadge';
import { useAuth } from '../../context/AuthContext';
import { SmartMatchingRecommendations } from '../../components/matching/SmartMatchingRecommendations';
import { DateRangeFilter } from '../../components/analytics/DateRangeFilter';
import { EnvironmentalImpactSummaryCard } from '../../components/analytics/EnvironmentalImpactSummaryCard';
import { MonthlyTrendChart } from '../../components/analytics/MonthlyTrendChart';
import { CategoryDistributionTable } from '../../components/analytics/CategoryDistributionTable';

export const GeneratorDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'LISTINGS' | 'IMPACT'>('LISTINGS');
  const [stats, setStats] = useState<GeneratorDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedListingId, setSelectedListingId] = useState<number | null>(null);

  // Environmental analytics state
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [impactData, setImpactData] = useState<GeneratorAnalyticsResponse | null>(null);
  const [impactLoading, setImpactLoading] = useState<boolean>(false);
  const [impactError, setImpactError] = useState<string | null>(null);

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

  useEffect(() => {
    if (activeTab === 'IMPACT') {
      setImpactLoading(true);
      setImpactError(null);
      analyticsApi.getGeneratorAnalytics(startDate, endDate)
        .then((res) => setImpactData(res))
        .catch((err) => setImpactError(err?.response?.data?.message || 'Failed to load environmental analytics.'))
        .finally(() => setImpactLoading(false));
    }
  }, [activeTab, startDate, endDate]);

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
            Welcome back, <span className="font-semibold text-gray-900">{user?.firstName}</span>. Track your active lots, collectors, and environmental offsets.
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
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

      {/* Tabs */}
      <div className="flex border-b border-slate-200">
        <button
          onClick={() => setActiveTab('LISTINGS')}
          className={`py-3 px-6 text-sm font-semibold border-b-2 transition ${
            activeTab === 'LISTINGS'
              ? 'border-emerald-600 text-emerald-700'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          }`}
        >
          My Listings & Matching
        </button>
        <button
          onClick={() => setActiveTab('IMPACT')}
          className={`py-3 px-6 text-sm font-semibold border-b-2 transition flex items-center gap-2 ${
            activeTab === 'IMPACT'
              ? 'border-emerald-600 text-emerald-700'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          }`}
        >
          <span>🌱</span>
          <span>My Environmental Impact</span>
        </button>
      </div>

      {activeTab === 'LISTINGS' ? (
        <>
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
        </>
      ) : (
        /* Environmental Impact Tab */
        <div className="space-y-6">
          <DateRangeFilter
            startDate={startDate}
            endDate={endDate}
            onChange={(s, e) => {
              setStartDate(s);
              setEndDate(e);
            }}
          />

          {impactError && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
              {impactError}
            </div>
          )}

          {impactLoading ? (
            <div className="py-16 text-center text-slate-400">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600 mx-auto mb-2"></div>
              <span className="text-xs">Computing personal environmental impact...</span>
            </div>
          ) : impactData ? (
            <>
              {/* Summary Cards */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
                    Transacted Material
                  </span>
                  <div className="text-2xl font-bold text-slate-900 mt-2">
                    {impactData.totalTransactedKg.toLocaleString()} <span className="text-sm font-normal text-slate-500">kg</span>
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {impactData.completedTransactionsCount} completed collections
                  </span>
                </div>

                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">
                    Confirmed Recycled
                  </span>
                  <div className="text-2xl font-bold text-emerald-700 mt-2">
                    {impactData.confirmedRecycledKg.toLocaleString()} <span className="text-sm font-normal text-emerald-500">kg</span>
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Verified through processor intake receipts
                  </span>
                </div>

                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-teal-600 uppercase tracking-wider">
                    Est. CO₂ Avoided
                  </span>
                  <div className="text-2xl font-bold text-teal-700 mt-2">
                    {impactData.estimatedCo2AvoidedMt !== null && impactData.estimatedCo2AvoidedMt !== undefined
                      ? `${impactData.estimatedCo2AvoidedMt.toFixed(2)} MT`
                      : 'N/A'}
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {impactData.estimatedCo2AvoidedKg?.toLocaleString() || 0} kg CO₂e offset
                  </span>
                </div>

                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-purple-600 uppercase tracking-wider">
                    Total Earnings
                  </span>
                  <div className="text-2xl font-bold text-purple-700 mt-2">
                    ₹{impactData.totalEarnings.toLocaleString()}
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Economic value generated from waste
                  </span>
                </div>
              </div>

              {/* Environmental Summary Card */}
              <EnvironmentalImpactSummaryCard
                totalRecycledKg={impactData.confirmedRecycledKg}
                totalCollectedKg={impactData.totalTransactedKg}
                totalLandfillDivertedKg={impactData.estimatedLandfillDivertedKg}
                totalCo2AvoidedKg={impactData.estimatedCo2AvoidedKg}
                disclaimer={impactData.disclaimer}
              />

              {/* Monthly Trend Chart */}
              <MonthlyTrendChart
                data={impactData.monthlyTrends}
                title="Your Monthly Waste Diverted vs. Recycled"
                description="Comparing your lots collected against downstream verified processor receipts."
              />

              {/* Category Breakdown */}
              <CategoryDistributionTable
                data={impactData.categoryDistribution}
                title="Your Waste Material Distribution"
                description="Breakdown of your generated waste lots by category and verified carbon offsets."
                showCollected={true}
                showRecycled={true}
              />
            </>
          ) : null}
        </div>
      )}
    </div>
  );
};
