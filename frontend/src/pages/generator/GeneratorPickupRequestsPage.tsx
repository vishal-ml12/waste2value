import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, PageResponse, PickupRequest } from '../../types';
import { ReviewModal } from '../../components/common/ReviewModal';
import { ReportDisputeModal } from '../../components/common/ReportDisputeModal';

export const GeneratorPickupRequestsPage: React.FC = () => {
  const [requests, setRequests] = useState<PickupRequest[]>([]);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [loading, setLoading] = useState(true);
  const [respondingReq, setRespondingReq] = useState<PickupRequest | null>(null);
  const [acceptDecision, setAcceptDecision] = useState<boolean>(true);
  const [responseNote, setResponseNote] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modals
  const [reviewTarget, setReviewTarget] = useState<{ reqId: number; targetId: number; targetName: string } | null>(null);
  const [reportTarget, setReportTarget] = useState<{ id: number; title: string } | null>(null);

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const url = selectedStatus === 'ALL'
        ? '/pickup-requests/received'
        : `/pickup-requests/received?status=${selectedStatus}`;
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

  const handleRespondSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!respondingReq) return;
    setSubmitting(true);
    setError(null);

    try {
      await apiClient.post(`/pickup-requests/${respondingReq.id}/respond`, {
        accept: acceptDecision,
        note: responseNote.trim(),
      });
      setRespondingReq(null);
      setResponseNote('');
      fetchRequests();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to respond to request');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Pickup Requests Received</h1>
          <p className="text-sm text-gray-500 mt-1">Review collector bids on your waste listings, negotiate rates, and allocate pickups</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200 overflow-x-auto space-x-2 text-xs font-medium">
        {['ALL', 'REQUESTED', 'ACCEPTED', 'COMPLETED', 'REJECTED', 'CANCELLED'].map((st) => (
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

      {/* List */}
      {loading ? (
        <div className="py-12 flex justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
        </div>
      ) : requests.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
          No received pickup requests under status "{selectedStatus}".
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {requests.map((req) => (
            <div key={req.id} className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm space-y-4">
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-xs text-gray-500 font-medium">Request #{req.id} for Listing #{req.listingId}</span>
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
                      : 'bg-gray-100 text-gray-600'
                  }`}
                >
                  {req.status}
                </span>
              </div>

              <div className="bg-slate-50 p-3 rounded-lg text-xs space-y-1.5 border border-gray-100">
                <div className="flex justify-between">
                  <span className="text-gray-500">Delivering Collector:</span>
                  <span className="font-bold text-gray-900">{req.collectorName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Offered Rate:</span>
                  <span className="font-bold text-emerald-700">₹{req.proposedPrice} / {req.unit}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500">Total Offered Value:</span>
                  <span className="font-bold text-gray-900">₹{(req.proposedPrice * req.listingQuantity).toFixed(2)}</span>
                </div>
                {req.actualQuantity && (
                  <div className="flex justify-between">
                    <span className="text-gray-500">Actual Weighed:</span>
                    <span className="font-bold text-blue-700">{req.actualQuantity} {req.unit} (Final: ₹{(req.actualQuantity * req.proposedPrice).toFixed(2)})</span>
                  </div>
                )}
                {req.status === 'ACCEPTED' && (
                  <div className="pt-2 border-t border-gray-200 text-xs">
                    <div className="text-emerald-800 font-semibold mb-1">Collector Contact:</div>
                    <div className="text-gray-700">Phone: {req.collectorPhone || 'N/A'}</div>
                    <div className="text-gray-700">Email: {req.collectorEmail}</div>
                  </div>
                )}
              </div>

              {req.note && (
                <div className="text-xs text-gray-600 bg-gray-50 p-2.5 rounded border border-gray-100">
                  <span className="font-semibold text-gray-700">Collector's proposal note: </span>
                  {req.note}
                </div>
              )}

              {req.completionNote && (
                <div className="text-xs text-gray-600 bg-blue-50/50 p-2.5 rounded border border-blue-100">
                  <span className="font-semibold text-blue-800">Resolution note: </span>
                  {req.completionNote}
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex items-center justify-between pt-2 border-t border-gray-100 text-xs">
                <button
                  onClick={() => setReportTarget({ id: req.id, title: req.listingTitle })}
                  className="text-gray-500 hover:text-rose-600 font-medium"
                >
                  Report Issue
                </button>

                <div className="flex gap-2">
                  {req.status === 'REQUESTED' && (
                    <>
                      <button
                        onClick={() => {
                          setRespondingReq(req);
                          setAcceptDecision(false);
                        }}
                        className="px-3 py-1.5 border border-rose-300 text-rose-700 hover:bg-rose-50 rounded-lg font-medium"
                      >
                        Decline
                      </button>
                      <button
                        onClick={() => {
                          setRespondingReq(req);
                          setAcceptDecision(true);
                        }}
                        className="px-4 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-semibold shadow-sm"
                      >
                        Accept & Allocate ✓
                      </button>
                    </>
                  )}

                  {req.status === 'COMPLETED' && (
                    <button
                      onClick={() => setReviewTarget({
                        reqId: req.id,
                        targetId: req.collectorId,
                        targetName: req.collectorName,
                      })}
                      className="px-3 py-1.5 bg-amber-500 hover:bg-amber-600 text-white rounded-lg font-semibold shadow-sm"
                    >
                      ★ Rate Collector
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Respond Modal */}
      {respondingReq && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl relative">
            <h3 className="text-lg font-bold text-gray-900 mb-1">
              {acceptDecision ? 'Accept Pickup Request' : 'Decline Pickup Request'}
            </h3>
            <p className="text-xs text-gray-500 mb-4">
              {acceptDecision
                ? `Accepting will allocate listing "${respondingReq.listingTitle}" exclusively to ${respondingReq.collectorName} and reject other competing bids.`
                : `Declining collector's request on "${respondingReq.listingTitle}".`}
            </p>

            {error && (
              <div className="mb-4 p-3 bg-rose-50 text-rose-700 text-xs rounded-lg border border-rose-200">
                {error}
              </div>
            )}

            <form onSubmit={handleRespondSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Optional Message to Collector
                </label>
                <textarea
                  rows={3}
                  value={responseNote}
                  onChange={(e) => setResponseNote(e.target.value)}
                  placeholder={acceptDecision ? 'e.g., Gate 2 entry, please call warehouse manager on arrival.' : 'e.g., Rate below our reserve.'}
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setRespondingReq(null)}
                  disabled={submitting}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className={`px-4 py-2 text-white rounded-lg text-xs font-semibold shadow-sm ${
                    acceptDecision ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'
                  }`}
                >
                  {submitting ? 'Updating...' : acceptDecision ? 'Confirm & Allocate' : 'Confirm Decline'}
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
