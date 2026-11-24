import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../api/client';
import { AdminDashboardStats, ApiResponse } from '../../types';

export const AdminDashboardPage: React.FC = () => {
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Platform Administration & Oversight</h1>
          <p className="text-sm text-gray-500 mt-1">
            Monitor circular supply chain integrity, verify participants, and moderate platform activity
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            to="/admin/verifications"
            className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg text-sm font-semibold shadow-sm"
          >
            Review Pending Verifications ({((stats?.pendingCollectorsCount || 0) + (stats?.pendingProcessorsCount || 0))})
          </Link>
        </div>
      </div>

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
          <p className="text-xs text-gray-500 mt-1">Resolve flagged listings, weigh discrepancies, and misconduct</p>
        </Link>
      </div>
    </div>
  );
};
