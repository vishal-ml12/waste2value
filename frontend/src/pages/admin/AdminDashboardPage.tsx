import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient, { analyticsApi } from '../../api/client';
import {
  AdminDashboardStats,
  AdminOverviewStats,
  WasteCollectionAnalytics,
  RecyclingAnalytics,
  ApiResponse,
} from '../../types';
import { DateRangeFilter } from '../../components/analytics/DateRangeFilter';
import { EnvironmentalImpactSummaryCard } from '../../components/analytics/EnvironmentalImpactSummaryCard';
import { MonthlyTrendChart } from '../../components/analytics/MonthlyTrendChart';
import { CategoryDistributionTable } from '../../components/analytics/CategoryDistributionTable';
import { EmissionFactorsModal } from '../../components/analytics/EmissionFactorsModal';

export const AdminDashboardPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'OPERATIONS' | 'ANALYTICS'>('OPERATIONS');
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  // Analytics states
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [overview, setOverview] = useState<AdminOverviewStats | null>(null);
  const [wasteData, setWasteData] = useState<WasteCollectionAnalytics | null>(null);
  const [recyclingData, setRecyclingData] = useState<RecyclingAnalytics | null>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState<boolean>(false);
  const [analyticsError, setAnalyticsError] = useState<string | null>(null);
  const [isFactorsModalOpen, setIsFactorsModalOpen] = useState<boolean>(false);

  const fetchStats = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<AdminDashboardStats>>('/dashboard/admin');
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
      const [overviewRes, wasteRes, recyclingRes] = await Promise.all([
        analyticsApi.getAdminOverview(startDate, endDate),
        analyticsApi.getWasteCollectionAnalytics(startDate, endDate),
        analyticsApi.getRecyclingAnalytics(startDate, endDate),
      ]);
      setOverview(overviewRes);
      setWasteData(wasteRes);
      setRecyclingData(recyclingRes);
    } catch (err: any) {
      setAnalyticsError(err?.response?.data?.message || 'Failed to load platform analytics.');
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

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Platform Administration & Oversight</h1>
          <p className="text-sm text-gray-500 mt-1">
            Monitor circular supply chain integrity, verify participants, and audit environmental impact
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/admin/verifications"
            className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg text-sm font-semibold shadow-sm transition"
          >
            Review Pending Verifications ({((stats?.pendingCollectorsCount || 0) + (stats?.pendingProcessorsCount || 0))})
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
          Operations & Moderation
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
          <span>Environmental Impact & Analytics</span>
        </button>
      </div>

      {activeTab === 'OPERATIONS' ? (
        <>
          {/* Operations View */}
          {stats && (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <Link
                to="/admin/verifications"
                className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-amber-500 transition-colors"
              >
                <span className="text-xs font-semibold text-amber-600 uppercase tracking-wider">Pending Verifications</span>
                <div className="text-2xl font-bold text-gray-900 mt-2">
                  {stats.pendingCollectorsCount + stats.pendingProcessorsCount}
                </div>
                <span className="text-[11px] text-gray-500 mt-1 block">
                  {stats.pendingCollectorsCount} Collectors · {stats.pendingProcessorsCount} Processors
                </span>
              </Link>

              <Link
                to="/admin/reports"
                className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-rose-500 transition-colors"
              >
                <span className="text-xs font-semibold text-rose-600 uppercase tracking-wider">Open Disputes</span>
                <div className="text-2xl font-bold text-rose-700 mt-2">{stats.openReportsCount}</div>
                <span className="text-[11px] text-gray-500 mt-1 block">Requires admin mediation</span>
              </Link>

              <Link
                to="/admin/pickups"
                className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors"
              >
                <span className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">Active Allocations</span>
                <div className="text-2xl font-bold text-gray-900 mt-2">{stats.activePickupRequestsCount}</div>
                <span className="text-[11px] text-gray-500 mt-1 block">In-progress pickups</span>
              </Link>

              <Link
                to="/admin/transactions"
                className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:border-blue-500 transition-colors"
              >
                <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">Total Transactions</span>
                <div className="text-2xl font-bold text-gray-900 mt-2">{stats.totalTransactionsCount}</div>
                <span className="text-[11px] text-gray-500 mt-1 block">Financial records logged</span>
              </Link>
            </div>
          )}

          {/* Admin Quick Links */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <Link
              to="/admin/verifications"
              className="p-5 bg-white rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors"
            >
              <h3 className="font-bold text-gray-900 text-sm">Participant Verifications</h3>
              <p className="text-xs text-gray-500 mt-1">Audit vehicle payload specs and processor environmental licenses</p>
            </Link>

            <Link
              to="/admin/categories"
              className="p-5 bg-white rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors"
            >
              <h3 className="font-bold text-gray-900 text-sm">Category Hierarchy</h3>
              <p className="text-xs text-gray-500 mt-1">Manage root and sub-category taxonomy, metrics, and units</p>
            </Link>

            <Link
              to="/admin/users"
              className="p-5 bg-white rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors"
            >
              <h3 className="font-bold text-gray-900 text-sm">User Directory & Status</h3>
              <p className="text-xs text-gray-500 mt-1">View registered platform users, roles, and suspend/activate accounts</p>
            </Link>

            <Link
              to="/admin/pickups"
              className="p-5 bg-white rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors"
            >
              <h3 className="font-bold text-gray-900 text-sm">Pickup Operations Ledger</h3>
              <p className="text-xs text-gray-500 mt-1">Full cross-platform audit of all pickup requests and states</p>
            </Link>

            <Link
              to="/admin/transactions"
              className="p-5 bg-white rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors"
            >
              <h3 className="font-bold text-gray-900 text-sm">Financial Transaction History</h3>
              <p className="text-xs text-gray-500 mt-1">Formal supply chain settlement records across all parties</p>
            </Link>

            <Link
              to="/admin/reports"
              className="p-5 bg-white rounded-xl border border-gray-200 shadow-sm hover:border-emerald-500 transition-colors"
            >
              <h3 className="font-bold text-gray-900 text-sm">Dispute & Report Center</h3>
              <p className="text-xs text-gray-500 mt-1">Resolve flagged listings, weight discrepancies, and misconduct</p>
            </Link>
          </div>
        </>
      ) : (
        /* Analytics View */
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <DateRangeFilter
              startDate={startDate}
              endDate={endDate}
              onChange={(s, e) => {
                setStartDate(s);
                setEndDate(e);
              }}
            />

            <button
              onClick={() => setIsFactorsModalOpen(true)}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition flex items-center gap-2 self-start sm:self-auto"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
              </svg>
              Manage Regulatory Emission Factors
            </button>
          </div>

          {analyticsError && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
              {analyticsError}
            </div>
          )}

          {analyticsLoading ? (
            <div className="py-16 text-center text-slate-400">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600 mx-auto mb-2"></div>
              <span className="text-xs">Aggregating cross-platform analytics...</span>
            </div>
          ) : overview ? (
            <>
              {/* Top Overview Cards */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-blue-600 uppercase tracking-wider">
                    Collected Waste
                  </span>
                  <div className="text-2xl font-bold text-slate-900 mt-2">
                    {overview.totalWasteCollectedKg.toLocaleString()} <span className="text-sm font-normal text-slate-500">kg</span>
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {overview.completedTransactionsCount} completed transactions
                  </span>
                </div>

                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">
                    Recycled / Diverted
                  </span>
                  <div className="text-2xl font-bold text-emerald-700 mt-2">
                    {overview.totalWasteRecycledKg.toLocaleString()} <span className="text-sm font-normal text-emerald-500">kg</span>
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {overview.confirmedReceiptsCount} verified processor receipts
                  </span>
                </div>

                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-teal-600 uppercase tracking-wider">
                    Est. CO₂ Avoided
                  </span>
                  <div className="text-2xl font-bold text-teal-700 mt-2">
                    {overview.totalCo2AvoidedMt !== null && overview.totalCo2AvoidedMt !== undefined
                      ? `${overview.totalCo2AvoidedMt.toFixed(2)} MT`
                      : 'N/A'}
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {overview.totalCo2AvoidedKg?.toLocaleString() || 0} kg CO₂e offset
                  </span>
                </div>

                <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                  <span className="text-xs font-semibold text-purple-600 uppercase tracking-wider">
                    Active Participants
                  </span>
                  <div className="text-2xl font-bold text-purple-700 mt-2">
                    {overview.activeGeneratorsCount + overview.activeCollectorsCount + overview.activeProcessorsCount}
                  </div>
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    {overview.activeGeneratorsCount} Gen · {overview.activeCollectorsCount} Col · {overview.activeProcessorsCount} Proc
                  </span>
                </div>
              </div>

              {/* Environmental Summary Card */}
              <EnvironmentalImpactSummaryCard
                totalRecycledKg={overview.totalWasteRecycledKg}
                totalCollectedKg={overview.totalWasteCollectedKg}
                totalLandfillDivertedKg={overview.totalLandfillDivertedKg}
                totalCo2AvoidedKg={overview.totalCo2AvoidedKg}
                disclaimer={overview.disclaimer}
              />

              {/* Monthly Trend Chart */}
              {wasteData?.monthlyTrends && (
                <MonthlyTrendChart
                  data={wasteData.monthlyTrends}
                  title="Cross-Platform Monthly Recovery Trends"
                  description="Tracking monthly completed collections (transactions) against physical recycling intakes (processor receipts)."
                />
              )}

              {/* Category Breakdown */}
              {recyclingData?.categoryBreakdown && (
                <CategoryDistributionTable
                  data={recyclingData.categoryBreakdown}
                  title="Material Recycling Distribution & Carbon Accounting"
                  description="Breakdown of processed commodities, individual landfill diversion rates, and verified regulatory factors."
                  showCollected={true}
                  showRecycled={true}
                />
              )}
            </>
          ) : null}
        </div>
      )}

      {/* Emission Factors Modal for Admin (with edit capabilities) */}
      <EmissionFactorsModal
        isOpen={isFactorsModalOpen}
        onClose={() => setIsFactorsModalOpen(false)}
        isAdmin={true}
      />
    </div>
  );
};
