import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, PageResponse, PickupRequest, PickupRequestStatus } from '../../types';
import { ReviewModal } from '../../components/common/ReviewModal';
import { ReportDisputeModal } from '../../components/common/ReportDisputeModal';

export const CollectorPickupRequestsPage: React.FC = () => {
  const [requests, setRequests] = useState<PickupRequest[]>([]);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [loading, setLoading] = useState(true);
  const [completingRequest, setCompletingRequest] = useState<PickupRequest | null>(null);
  const [actualQuantity, setActualQuantity] = useState<number>(0);
  const [completionNote, setCompletionNote] = useState<string>('');
  const [submittingCompletion, setSubmittingCompletion] = useState(false);

  // Modals
  const [reviewTarget, setReviewTarget] = useState<{ reqId: number; targetId: number; targetName: string } | null>(null);
  const [reportTarget, setReportTarget] = useState<{ id: number; title: string } | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const url = selectedStatus === 'ALL'
        ? '/pickup-requests/my'
        : `/pickup-requests/my?status=${selectedStatus}`;
      const res = await apiClient.get<ApiResponse<PageResponse<PickupRequest>>>(url);
      setRequests(res.data.data.content);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, [selectedStatus]);

  const handleCancel = async (id: number) => {
    if (!window.confirm('Are you sure you want to cancel this pickup request?')) return;
    try {
      await apiClient.post(`/pickup-requests/${id}/cancel`);
      fetchRequests();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel request');
    }
  };

  const handleCompleteSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!completingRequest) return;
    setSubmittingCompletion(true);
    setActionError(null);

    try {
      await apiClient.post(`/pickup-requests/${completingRequest.id}/complete`, {
        actualQuantity,
        completionNote: completionNote.trim(),
      });
      setCompletingRequest(null);
      fetchRequests();
    } catch (err: any) {
      setActionError(err.response?.data?.message || 'Failed to complete pickup');
    } finally {
      setSubmittingCompletion(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">My Pickup Requests & Deliveries</h1>
          <p className="text-sm text-gray-500 mt-1">Track requested waste pickups, schedule collections, and log completion</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200 overflow-x-auto space-x-2 text-xs font-medium">
        {['ALL', 'REQUESTED', 'ACCEPTED', 'COMPLETED', 'CANCELLED', 'REJECTED'].map((st) => (
          <button
            key={st}
            onClick={() => setSelectedStatus(st)}
            className={`px-4 py-2.5 border-b-2 whitespace-nowrap transition-colors ${
              selectedStatus === st
                ? 'border-emerald-600 text-emerald-700 font-semibold'
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
      ) : requests.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
          No pickup requests found under status "{selectedStatus}".
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {requests.map((req) => (
            <div key={req.id} className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm space-y-4">
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-xs text-gray-500 font-medium">Pickup Request #{req.id}</span>
                  <h3 className="font-bold text-gray-900 text-base mt-0.5">{req.listingTitle}</h3>
                  <span className="text-xs text-emerald-700 font-medium">{req.categoryName}</span>
                </div>
                <span
                  className={`px-2.5 py-1 rounded-full text-[10px] font-bold ${
                    req.status === 'ACCEPTED'
                      ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                      : req.status === 'COMPLETED'
                      ? 'bg-blue-100 text-blue-800'
                      : req.status === 'REQUESTED'
                      ? 'bg-amber-100 text-amber-800'
                      : req.status === 'CANCELLED'
                      ? 'bg-gray-100 text-gray-600'
                      : 'bg-rose-100 text-rose-800'
                  }`}
                >
                  {req.status}
                </span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg text-xs space-y-1.5 border border-gray-100">
                <div className="flex justify-between">
                  <span className="text-gray-500">Material Quantity:</span>
                  <span className="font-semibold text-gray-900">{req.listingQuantity} {req.unit}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Your Offered Price:</span>
                  <span className="font-bold text-emerald-700">₹{req.proposedPrice} / {req.unit}</span>
                </div>
                {req.actualQuantity && (
                  <div className="flex justify-between">
                    <span className="text-gray-500">Actual Collected:</span>
                    <span className="font-bold text-blue-700">{req.actualQuantity} {req.unit} (₹{(req.actualQuantity * req.proposedPrice).toFixed(2)})</span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-gray-500">Pickup Address:</span>
                  <span className="font-medium text-gray-800">{req.pickupAddress}, {req.pickupCity}</span>
                </div>
                {req.status === 'ACCEPTED' && (
                  <div className="pt-2 border-t border-gray-200 text-xs">
                    <div className="text-emerald-800 font-semibold mb-1">Generator Contact Information:</div>
                    <div className="text-gray-700">Name: {req.generatorName}</div>
                    {req.generatorPhone && <div className="text-gray-700">Phone: {req.generatorPhone}</div>}
                    <div className="text-gray-700">Email: {req.generatorEmail}</div>
                  </div>
                )}
              </div>

              {req.note && (
                <div className="text-xs text-gray-600 bg-gray-50 p-2.5 rounded border border-gray-100">
                  <span className="font-semibold text-gray-700">Your note: </span>
                  {req.note}
                </div>
              )}

              {req.completionNote && (
                <div className="text-xs text-gray-600 bg-blue-50/50 p-2.5 rounded border border-blue-100">
                  <span className="font-semibold text-blue-800">Status note: </span>
                  {req.completionNote}
                </div>
              )}

              {/* Actions */}
              <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-gray-100 text-xs">
                <div className="flex gap-2">
                  <button
                    onClick={() => setReportTarget({ id: req.id, title: req.listingTitle })}
                    className="text-gray-500 hover:text-rose-600 font-medium"
                  >
                    Report Issue
                  </button>
                </div>

                <div className="flex gap-2">
                  {(req.status === 'REQUESTED' || req.status === 'ACCEPTED') && (
                    <button
                      onClick={() => handleCancel(req.id)}
                      className="px-3 py-1.5 border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50 font-medium"
                    >
                      Cancel Request
                    </button>
                  )}

                  {req.status === 'ACCEPTED' && (
                    <button
                      onClick={() => {
                        setCompletingRequest(req);
                        setActualQuantity(req.listingQuantity);
                      }}
                      className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-semibold shadow-sm"
                    >
                      Complete Pickup ✓
                    </button>
                  )}

                  {req.status === 'COMPLETED' && (
                    <button
                      onClick={() => setReviewTarget({
                        reqId: req.id,
                        targetId: req.generatorId,
                        targetName: req.generatorName,
                      })}
                      className="px-3 py-1.5 bg-amber-500 hover:bg-amber-600 text-white rounded-lg font-semibold shadow-sm"
                    >
                      ★ Rate Generator
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Complete Pickup Modal */}
      {completingRequest && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl relative">
            <h3 className="text-lg font-bold text-gray-900 mb-1">Confirm Waste Pickup Completion</h3>
            <p className="text-xs text-gray-500 mb-4">
              Recording verified actual weight and generating final transaction for "{completingRequest.listingTitle}".
            </p>

            {actionError && (
              <div className="mb-4 p-3 bg-rose-50 text-rose-700 text-xs rounded-lg border border-rose-200">
                {actionError}
              </div>
            )}

            <form onSubmit={handleCompleteSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Actual Collected Quantity ({completingRequest.unit})
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  value={actualQuantity}
                  onChange={(e) => setActualQuantity(parseFloat(e.target.value) || 0)}
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
                <div className="flex justify-between text-[11px] text-gray-500 mt-1">
                  <span>Agreed rate: ₹{completingRequest.proposedPrice}/{completingRequest.unit}</span>
                  <span className="font-bold text-gray-900">Total: ₹{(actualQuantity * completingRequest.proposedPrice).toFixed(2)}</span>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Completion / Scale Notes</label>
                <textarea
                  rows={3}
                  value={completionNote}
                  onChange={(e) => setCompletionNote(e.target.value)}
                  placeholder="e.g., Weighed on certified digital scale, packed in 4 bundles."
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setCompletingRequest(null)}
                  disabled={submittingCompletion}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingCompletion}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm"
                >
                  {submittingCompletion ? 'Recording...' : 'Confirm Completion'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Review Modal */}
      {reviewTarget && (
        <ReviewModal
          isOpen={true}
          onClose={() => setReviewTarget(null)}
          pickupRequestId={reviewTarget.reqId}
          targetUserId={reviewTarget.targetId}
          targetName={reviewTarget.targetName}
          onSuccess={fetchRequests}
        />
      )}

      {/* Report Modal */}
      {reportTarget && (
        <ReportDisputeModal
          isOpen={true}
          onClose={() => setReportTarget(null)}
          targetType="PICKUP"
          targetId={reportTarget.id}
          targetTitle={reportTarget.title}
          onSuccess={() => alert('Report submitted successfully to platform administrators')}
        />
      )}
    </div>
  );
};
