import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, PageResponse, ReportItem, ReportStatus } from '../../types';

export const AdminReportsPage: React.FC = () => {
  const [reports, setReports] = useState<ReportItem[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [loading, setLoading] = useState(true);
  const [resolvingReport, setResolvingReport] = useState<ReportItem | null>(null);
  const [newStatus, setNewStatus] = useState<ReportStatus>('RESOLVED');
  const [adminNote, setAdminNote] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);

  const fetchReports = async () => {
    setLoading(true);
    try {
      const url = statusFilter === 'ALL'
        ? '/admin/reports'
        : `/admin/reports?status=${statusFilter}`;
      const res = await apiClient.get<ApiResponse<PageResponse<ReportItem>>>(url);
      setReports(res.data.data.content);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReports();
  }, [statusFilter]);

  const handleUpdateStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resolvingReport) return;
    setSubmitting(true);

    try {
      await apiClient.patch(`/admin/reports/${resolvingReport.id}`, {
        status: newStatus,
        adminNote: adminNote.trim(),
      });
      setResolvingReport(null);
      setAdminNote('');
      fetchReports();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update report status');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Disputes & Safety Incident Reports</h1>
        <p className="text-sm text-gray-500 mt-1">Review flagged platform activity, mediate participant disputes, and issue resolutions</p>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200 space-x-2 text-xs font-medium">
        {['ALL', 'OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED'].map((st) => (
          <button
            key={st}
            onClick={() => setStatusFilter(st)}
            className={`px-4 py-2.5 border-b-2 transition-colors ${
              statusFilter === st
                ? 'border-emerald-600 text-emerald-700 font-bold'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {st}
          </button>
        ))}
      </div>

      {/* Content */}
      {loading ? (
        <div className="py-12 flex justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
        </div>
      ) : reports.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
          No reports found under status "{statusFilter}".
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {reports.map((r) => (
            <div key={r.id} className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-4">
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-xs text-gray-400 font-mono">Report #{r.id}</span>
                  <h3 className="font-bold text-gray-900 text-base mt-0.5">{r.reason}</h3>
                  <span className="text-xs text-emerald-700 font-medium">Target: {r.targetType} #{r.targetId}</span>
                </div>
                <span
                  className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                    r.status === 'OPEN'
                      ? 'bg-rose-100 text-rose-800'
                      : r.status === 'UNDER_REVIEW'
                      ? 'bg-amber-100 text-amber-800'
                      : r.status === 'RESOLVED'
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-gray-100 text-gray-600'
                  }`}
                >
                  {r.status}
                </span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg text-xs space-y-2 border border-gray-100">
                <div className="text-gray-600">
                  <span className="font-semibold text-gray-700">Reporter: </span>
                  {r.reporterName} (ID: {r.reporterId})
                </div>
                <div className="text-gray-700 leading-relaxed">
                  <span className="font-semibold text-gray-900">Description: </span>
                  {r.description}
                </div>
                {r.adminNote && (
                  <div className="pt-2 border-t border-gray-200 text-xs text-blue-900 bg-blue-50/50 p-2 rounded">
                    <span className="font-bold">Admin Resolution: </span>
                    {r.adminNote}
                    {r.resolvedByUserName && (
                      <span className="block text-[10px] text-gray-500 mt-1">
                        Resolved by {r.resolvedByUserName} on {r.resolvedAt ? new Date(r.resolvedAt).toLocaleDateString() : ''}
                      </span>
                    )}
                  </div>
                )}
              </div>

              <div className="flex justify-between items-center pt-2 border-t border-gray-100 text-xs">
                <span className="text-gray-400">Filed: {new Date(r.createdAt).toLocaleDateString()}</span>
                {r.status !== 'RESOLVED' && r.status !== 'DISMISSED' && (
                  <button
                    onClick={() => {
                      setResolvingReport(r);
                      setNewStatus('RESOLVED');
                      setAdminNote('');
                    }}
                    className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-semibold shadow-sm"
                  >
                    Resolve Dispute →
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Resolution Modal */}
      {resolvingReport && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl relative">
            <h3 className="text-lg font-bold text-gray-900 mb-1">Resolve Incident #{resolvingReport.id}</h3>
            <p className="text-xs text-gray-500 mb-4">
              Reason: "{resolvingReport.reason}" on {resolvingReport.targetType} #{resolvingReport.targetId}
            </p>

            <form onSubmit={handleUpdateStatus} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Resolution Decision</label>
                <select
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value as ReportStatus)}
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none bg-white"
                >
                  <option value="RESOLVED">RESOLVED (Action taken, parties notified)</option>
                  <option value="DISMISSED">DISMISSED (No violation or invalid claim)</option>
                  <option value="UNDER_REVIEW">UNDER_REVIEW (Investigation in progress)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Administrative Resolution Note</label>
                <textarea
                  rows={3}
                  required
                  value={adminNote}
                  onChange={(e) => setAdminNote(e.target.value)}
                  placeholder="Explain findings, adjustment made, or outcome..."
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setResolvingReport(null)}
                  disabled={submitting}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm"
                >
                  {submitting ? 'Saving...' : 'Apply Resolution'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
