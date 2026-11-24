import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, CollectorProfile, PageResponse, ProcessorProfile } from '../../types';

export const AdminVerificationsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'COLLECTORS' | 'PROCESSORS'>('COLLECTORS');
  const [pendingCollectors, setPendingCollectors] = useState<CollectorProfile[]>([]);
  const [pendingProcessors, setPendingProcessors] = useState<ProcessorProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [actioningId, setActioningId] = useState<number | null>(null);

  const fetchPending = async () => {
    setLoading(true);
    try {
      if (activeTab === 'COLLECTORS') {
        const res = await apiClient.get<ApiResponse<PageResponse<CollectorProfile>>>('/admin/collectors/pending');
        setPendingCollectors(res.data.data.content);
      } else {
        const res = await apiClient.get<ApiResponse<PageResponse<ProcessorProfile>>>('/admin/processors/pending');
        setPendingProcessors(res.data.data.content);
      }
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPending();
  }, [activeTab]);

  const handleVerifyCollector = async (profileId: number, verified: boolean) => {
    const note = window.prompt(verified ? 'Verification approval note (optional):' : 'Rejection/suspension reason:') || '';
    setActioningId(profileId);
    try {
      await apiClient.post(`/admin/collectors/${profileId}/verify`, { verified, note });
      fetchPending();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Verification update failed');
    } finally {
      setActioningId(null);
    }
  };

  const handleVerifyProcessor = async (profileId: number, verified: boolean) => {
    const note = window.prompt(verified ? 'Verification approval note (optional):' : 'Rejection/suspension reason:') || '';
    setActioningId(profileId);
    try {
      await apiClient.post(`/admin/processors/${profileId}/verify`, { verified, note });
      fetchPending();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Verification update failed');
    } finally {
      setActioningId(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Participant Verification Queue</h1>
        <p className="text-sm text-gray-500 mt-1">Review regulatory licenses, equipment specs, and authorize platform participants</p>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200 space-x-4 text-xs font-medium">
        <button
          onClick={() => setActiveTab('COLLECTORS')}
          className={`py-2.5 border-b-2 transition-colors ${
            activeTab === 'COLLECTORS'
              ? 'border-emerald-600 text-emerald-700 font-bold'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          Pending Collectors ({pendingCollectors.length})
        </button>
        <button
          onClick={() => setActiveTab('PROCESSORS')}
          className={`py-2.5 border-b-2 transition-colors ${
            activeTab === 'PROCESSORS'
              ? 'border-emerald-600 text-emerald-700 font-bold'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          Pending Processors ({pendingProcessors.length})
        </button>
      </div>

      {/* Content */}
      {loading ? (
        <div className="py-12 flex justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
        </div>
      ) : activeTab === 'COLLECTORS' ? (
        pendingCollectors.length === 0 ? (
          <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
            All collector profiles are verified. No pending reviews in queue.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {pendingCollectors.map((c) => (
              <div key={c.id} className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-4">
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="font-bold text-gray-900 text-base">{c.firstName} {c.lastName}</h3>
                    <p className="text-xs text-gray-500">{c.email} {c.phone ? `· ${c.phone}` : ''}</p>
                  </div>
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800">
                    PENDING REVIEW
                  </span>
                </div>

                <div className="bg-slate-50 p-3 rounded-lg text-xs space-y-1.5 border border-gray-100">
                  <div className="flex justify-between">
                    <span className="text-gray-500">Vehicle Type:</span>
                    <span className="font-semibold text-gray-900">{c.vehicleType}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Service Area:</span>
                    <span className="font-medium text-gray-800">{c.serviceArea}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Operating Radius:</span>
                    <span className="font-medium text-gray-800">{c.maxDistanceKm} km</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Submitted:</span>
                    <span className="text-gray-500">{new Date(c.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>

                <div className="flex justify-end space-x-2 pt-2 border-t border-gray-100">
                  <button
                    disabled={actioningId === c.id}
                    onClick={() => handleVerifyCollector(c.id, false)}
                    className="px-3 py-1.5 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-lg text-xs font-semibold"
                  >
                    Decline
                  </button>
                  <button
                    disabled={actioningId === c.id}
                    onClick={() => handleVerifyCollector(c.id, true)}
                    className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm"
                  >
                    Approve & Verify ✓
                  </button>
                </div>
              </div>
            ))}
          </div>
        )
      ) : pendingProcessors.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
          All processor facility profiles are verified. No pending reviews in queue.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {pendingProcessors.map((p) => (
            <div key={p.id} className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-4">
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="font-bold text-gray-900 text-base">{p.companyName}</h3>
                  <p className="text-xs text-gray-500">Representative: {p.firstName} {p.lastName} ({p.email})</p>
                </div>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800">
                  PENDING LICENSE CHECK
                </span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg text-xs space-y-1.5 border border-gray-100">
                <div className="flex justify-between">
                  <span className="text-gray-500">Trade License:</span>
                  <span className="font-mono font-bold text-gray-900">{p.licenseNumber}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Max Permitted Capacity:</span>
                  <span className="font-semibold text-gray-900">{p.maxCapacityKg.toLocaleString()} kg</span>
                </div>
                {p.description && (
                  <div className="pt-1 text-gray-600">
                    <span className="text-gray-500">Description: </span>
                    {p.description}
                  </div>
                )}
              </div>

              <div className="flex justify-end space-x-2 pt-2 border-t border-gray-100">
                <button
                  disabled={actioningId === p.id}
                  onClick={() => handleVerifyProcessor(p.id, false)}
                  className="px-3 py-1.5 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-lg text-xs font-semibold"
                >
                  Decline
                </button>
                <button
                  disabled={actioningId === p.id}
                  onClick={() => handleVerifyProcessor(p.id, true)}
                  className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm"
                >
                  Approve & Verify ✓
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
