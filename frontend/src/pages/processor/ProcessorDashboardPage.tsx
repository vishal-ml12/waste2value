import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../api/client';
import { ApiResponse, ProcessorDashboardStats } from '../../types';
import { RecommendedListingsFeed } from '../../components/matching/RecommendedListingsFeed';

export const ProcessorDashboardPage: React.FC = () => {
  const [stats, setStats] = useState<ProcessorDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

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

  const capacityPct = stats && stats.maxCapacityKg > 0
    ? Math.min(100, Math.round((stats.currentLoadKg / stats.maxCapacityKg) * 100))
    : 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Processing Plant Operations</h1>
          <p className="text-sm text-gray-500 mt-1">Manage processing capabilities, plant load, and intake receipts</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            to="/processor/capabilities"
            className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 shadow-sm"
          >
            Manage Capabilities
          </Link>
          <Link
            to="/processor/receipts"
            className="px-4 py-2 border border-gray-300 text-gray-700 bg-white rounded-lg text-sm font-medium hover:bg-gray-50 shadow-sm"
          >
            Record Material Intake
          </Link>
        </div>
      </div>

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
    </div>
  );
};
