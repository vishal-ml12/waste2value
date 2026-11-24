import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient, { analyticsApi } from '../../api/client';
import { ApiResponse, ProcessorDashboardStats, ProcessorAnalyticsResponse } from '../../types';
import { RecommendedListingsFeed } from '../../components/matching/RecommendedListingsFeed';
import { DateRangeFilter } from '../../components/analytics/DateRangeFilter';
import { EnvironmentalImpactSummaryCard } from '../../components/analytics/EnvironmentalImpactSummaryCard';
import { MonthlyTrendChart } from '../../components/analytics/MonthlyTrendChart';
import { CategoryDistributionTable } from '../../components/analytics/CategoryDistributionTable';

export const ProcessorDashboardPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'OPERATIONS' | 'ANALYTICS'>('OPERATIONS');
  const [stats, setStats] = useState<ProcessorDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  // Environmental analytics state
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [analyticsData, setAnalyticsData] = useState<ProcessorAnalyticsResponse | null>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState<boolean>(false);
  const [analyticsError, setAnalyticsError] = useState<string | null>(null);

  const fetchStats = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<ProcessorDashboardStats>>('/dashboard/processor');
      setStats(res.data.data);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  const fetchAnalytics = async () => {
    setAnalyticsLoading(true);
    setAnalyticsError(null);
    try {
      const res = await analyticsApi.getProcessorAnalytics(startDate, endDate);
      setAnalyticsData(res);
    } catch (err: any) {
      setAnalyticsError(err?.response?.data?.message || 'Failed to load processor analytics.');
    } finally {
      setAnalyticsLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
  }, []);

  useEffect(() => {
    if (activeTab === 'ANALYTICS') {
      fetchAnalytics();
    }
  }, [activeTab, startDate, endDate]);

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-12 flex justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  const capacityPct = stats && stats.maxCapacityKg > 0
    ? Math.min(100, Math.round((stats.currentLoadKg / stats.maxCapacityKg) * 100))
    : 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Processing Plant Operations</h1>
          <p className="text-sm text-gray-500 mt-1">Manage processing capabilities, plant load, and verified intake receipts</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            to="/processor/capabilities"
            className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 shadow-sm transition"
          >
            Manage Capabilities
          </Link>
          <Link
            to="/processor/receipts"
            className="px-4 py-2 border border-gray-300 text-gray-700 bg-white rounded-lg text-sm font-medium hover:bg-gray-50 shadow-sm transition"
          >
            Record Material Intake
          </Link>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200">
        <button
          onClick={() => setActiveTab('OPERATIONS')}
          className={`py-3 px-6 text-sm font-semibold border-b-2 transition ${
            activeTab === 'OPERATIONS'
              ? 'border-emerald-600 text-emerald-700'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          }`}
        >
          Operations & Intake
        </button>
        <button
          onClick={() => setActiveTab('ANALYTICS')}
          className={`py-3 px-6 text-sm font-semibold border-b-2 transition flex items-center gap-2 ${
            activeTab === 'ANALYTICS'
              ? 'border-emerald-600 text-emerald-700'
              : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
          }`}
        >
          <span>🌱</span>
          <span>Recycling Throughput & Emission Offsets</span>
        </button>
      </div>

      {activeTab === 'OPERATIONS' ? (
        <>
          {/* Verification Notice Banner */}
          {stats && !stats.isVerified && (
            <div className="bg-amber-50 border-l-4 border-amber-500 p-4 rounded-r-lg">
              <div className="flex items-start">
                <div className="flex-shrink-0 text-amber-500 text-lg">⚠️</div>
                <div className="ml-3">
                  <h3 className="text-sm font-semibold text-amber-800">Facility Verification Pending</h3>
                  <p className="text-xs text-amber-700 mt-1">
                    Your recycling plant profile is awaiting platform administrator license verification. Once verified, you can record material receipts and advertise active pricing capabilities.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* Capacity & Metrics Card */}
          {stats && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm col-span-1 md:col-span-2 space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-xs font-medium text-gray-500 uppercase tracking-wider">Facility Capacity Status</span>
                  <span className="text-xs font-bold text-gray-700">{capacityPct}% utilized</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                  <div
                    className={`h-3 rounded-full transition-all duration-500 ${
                      capacityPct > 90 ? 'bg-rose-500' : capacityPct > 75 ? 'bg-amber-500' : 'bg-emerald-600'
                    }`}
                    style={{ width: `${capacityPct}%` }}
                  ></div>
                </div>
                <div className="flex justify-between text-xs text-gray-600 pt-1">
                  <span>Current Load: <b className="text-gray-900">{stats.currentLoadKg.toLocaleString()} kg</b></span>
                  <span>Available: <b className="text-emerald-700">{stats.availableCapacityKg.toLocaleString()} kg</b></span>
                  <span>Max: <b className="text-gray-900">{stats.maxCapacityKg.toLocaleString()} kg</b></span>
                </div>
              </div>

              <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
                <span className="text-xs font-medium text-gray-500 uppercase tracking-wider">Active Capabilities</span>
                <div className="text-2xl font-bold text-emerald-600 mt-2">{stats.activeCapabilitiesCount} Categories</div>
                <Link to="/processor/capabilities" className="text-[11px] text-emerald-700 underline mt-1 block">
                  Configure rates & limits →
                </Link>
              </div>

              <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm">
                <span className="text-xs font-medium text-gray-500 uppercase tracking-wider">Facility Rating</span>
                <div className="text-2xl font-bold text-amber-500 mt-2 flex items-center">
                  ★ {stats.avgRating > 0 ? stats.avgRating.toFixed(2) : '5.00'}
                </div>
                <span className="text-[11px] text-gray-500 mt-1 block">{stats.totalReceiptsCount} deliveries received</span>
              </div>
            </div>
          )}

          {/* Quick Navigation Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Link
              to="/processor/capabilities"
              className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors flex items-center justify-between"
            >
              <div>
                <h4 className="font-semibold text-gray-900 text-sm">Material Acceptance Capabilities</h4>
                <p className="text-xs text-gray-500 mt-0.5">Declare categories, minimums, and purchase rates</p>
              </div>
              <span className="text-emerald-600 font-bold text-lg">→</span>
            </Link>

            <Link
              to="/processor/receipts"
              className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors flex items-center justify-between"
            >
              <div>
                <h4 className="font-semibold text-gray-900 text-sm">Intake Material Receipts</h4>
                <p className="text-xs text-gray-500 mt-0.5">Record incoming deliveries and update facility load</p>
              </div>
              <span className="text-emerald-600 font-bold text-lg">→</span>
            </Link>

            <Link
              to="/processor/profile"
              className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors flex items-center justify-between"
            >
              <div>
                <h4 className="font-semibold text-gray-900 text-sm">Facility & License Details</h4>
                <p className="text-xs text-gray-500 mt-0.5">Manage company license, description, and capacity</p>
              </div>
              <span className="text-emerald-600 font-bold text-lg">→</span>
            </Link>
          </div>

          {/* Recommended Materials (Phase 3A) */}
          <RecommendedListingsFeed userRole="PROCESSOR" />

          {/* Recent Receipts Table */}
          <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
              <h3 className="font-semibold text-gray-900 text-sm">Recent Material Deliveries Received</h3>
              <Link to="/processor/receipts" className="text-xs text-emerald-600 hover:text-emerald-700 font-medium">
                View All Receipts →
              </Link>
            </div>

            {stats && stats.recentReceipts.length === 0 ? (
              <div className="p-8 text-center text-gray-500 text-xs">
                No material receipts recorded yet. Click "Record Material Intake" when collectors deliver waste.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-gray-50 text-gray-600 border-b border-gray-100">
                    <tr>
                      <th className="px-6 py-3 font-semibold">Receipt #</th>
                      <th className="px-6 py-3 font-semibold">Collector</th>
                      <th className="px-6 py-3 font-semibold">Category</th>
                      <th className="px-6 py-3 font-semibold">Quantity</th>
                      <th className="px-6 py-3 font-semibold">Price</th>
                      <th className="px-6 py-3 font-semibold">Total</th>
                      <th className="px-6 py-3 font-semibold">Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 text-gray-700">
                    {stats?.recentReceipts.map((rcpt) => (
                      <tr key={rcpt.id} className="hover:bg-gray-50">
                        <td className="px-6 py-3 font-mono font-bold text-gray-900">PR-{rcpt.id}</td>
                        <td className="px-6 py-3 font-medium text-gray-900">{rcpt.collectorName}</td>
                        <td className="px-6 py-3">{rcpt.categoryName}</td>
                        <td className="px-6 py-3 font-bold text-gray-900">{rcpt.quantity} {rcpt.unit}</td>
                        <td className="px-6 py-3 text-emerald-700 font-semibold">₹{rcpt.agreedPrice} / {rcpt.unit}</td>
                        <td className="px-6 py-3 font-bold text-emerald-800">₹{rcpt.totalAmount.toLocaleString()}</td>
                        <td className="px-6 py-3 text-gray-500">{new Date(rcpt.receivedDate).toLocaleDateString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      ) : (
        /* Recycling Throughput & Emission Offsets Tab */
        <div className="space-y-6">
          <DateRangeFilter
            startDate={startDate}
            endDate={endDate}
            onChange={(s, e) => {
              setStartDate(s);
              setEndDate(e);
            }}
          />

          {analyticsError && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
              {analyticsError}
            </div>
          )}

          {analyticsLoading ? (
            <div className="py-16 text-center text-slate-400">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600 mx-auto mb-2"></div>
              <span className="text-xs">Aggregating processing throughput & carbon accounting...</span>
            </div>
          ) : analyticsData ? (
            <>
              {/* Summary KPIs */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">
                    Total Processed
                  </span>
                  <div className="text-2xl font-bold text-emerald-700 mt-2">
                    {analyticsData.totalProcessedKg.toLocaleString()} <span className="text-sm font-normal text-emerald-500">kg</span>
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {analyticsData.confirmedReceiptsCount} verified intake receipts
                  </span>
                </div>

                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
                    Plant Capacity
                  </span>
                  <div className="text-2xl font-bold text-blue-700 mt-2">
                    {analyticsData.capacityUtilizationPercent.toFixed(1)}%
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {analyticsData.currentLoadKg.toLocaleString()} / {analyticsData.maxCapacityKg.toLocaleString()} kg
                  </span>
                </div>

                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-teal-600 uppercase tracking-wider">
                    Est. CO₂ Avoided
                  </span>
                  <div className="text-2xl font-bold text-teal-700 mt-2">
                    {analyticsData.estimatedCo2AvoidedMt !== null && analyticsData.estimatedCo2AvoidedMt !== undefined
                      ? `${analyticsData.estimatedCo2AvoidedMt.toFixed(2)} MT`
                      : 'N/A'}
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {analyticsData.estimatedCo2AvoidedKg?.toLocaleString() || 0} kg CO₂e offset
                  </span>
                </div>

                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-purple-600 uppercase tracking-wider">
                    Landfill Diverted
                  </span>
                  <div className="text-2xl font-bold text-purple-700 mt-2">
                    {analyticsData.estimatedLandfillDivertedKg.toLocaleString()} <span className="text-sm font-normal text-purple-400">kg</span>
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Prevented from municipal dumping
                  </span>
                </div>
              </div>

              {/* Environmental Summary Card */}
              <EnvironmentalImpactSummaryCard
                totalRecycledKg={analyticsData.totalProcessedKg}
                totalCollectedKg={analyticsData.totalProcessedKg}
                totalLandfillDivertedKg={analyticsData.estimatedLandfillDivertedKg}
                totalCo2AvoidedKg={analyticsData.estimatedCo2AvoidedKg}
                disclaimer={analyticsData.disclaimer}
              />

              {/* Monthly Trend Chart */}
              <MonthlyTrendChart
                data={analyticsData.monthlyTrends}
                title="Your Monthly Facility Intake & Recycling Volume"
                description="Tracking physical material intakes recorded and verified at your plant."
              />

              {/* Category Breakdown */}
              <CategoryDistributionTable
                data={analyticsData.categoryDistribution}
                title="Materials Processed by Category"
                description="Breakdown of recycled throughput by category and verified regulatory carbon factors."
                showCollected={false}
                showRecycled={true}
              />
            </>
          ) : null}
        </div>
      )}
    </div>
  );
};
