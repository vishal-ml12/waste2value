import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, PageResponse, PickupRequest } from '../../types';

export const AdminPickupRequestsPage: React.FC = () => {
  const [requests, setRequests] = useState<PickupRequest[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<PageResponse<PickupRequest>>>('/pickup-requests');
      setRequests(res.data.data.content);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Platform Pickup Operations Audit</h1>
        <p className="text-sm text-gray-500 mt-1">Cross-system audit of all pickup requests, allocations, and transitions</p>
      </div>

      {loading ? (
        <div className="py-12 flex justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
        </div>
      ) : requests.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
          No pickup requests recorded in platform database yet.
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-gray-50 text-gray-600 border-b border-gray-100">
                <tr>
                  <th className="px-6 py-3 font-semibold">Request ID</th>
                  <th className="px-6 py-3 font-semibold">Listing Title</th>
                  <th className="px-6 py-3 font-semibold">Generator</th>
                  <th className="px-6 py-3 font-semibold">Collector</th>
                  <th className="px-6 py-3 font-semibold">Proposed Rate</th>
                  <th className="px-6 py-3 font-semibold">Quantity</th>
                  <th className="px-6 py-3 font-semibold">Status</th>
                  <th className="px-6 py-3 font-semibold">Requested At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-gray-700">
                {requests.map((r) => (
                  <tr key={r.id} className="hover:bg-gray-50">
                    <td className="px-6 py-3 font-mono font-bold text-gray-900">PR-{r.id}</td>
                    <td className="px-6 py-3 font-semibold text-gray-900">{r.listingTitle}</td>
                    <td className="px-6 py-3">{r.generatorName}</td>
                    <td className="px-6 py-3 font-medium text-emerald-800">{r.collectorName}</td>
                    <td className="px-6 py-3 text-emerald-700 font-semibold">₹{r.proposedPrice} / {r.unit}</td>
                    <td className="px-6 py-3 font-bold text-gray-900">
                      {r.actualQuantity ? `${r.actualQuantity} ${r.unit}` : `${r.listingQuantity} ${r.unit}`}
                    </td>
                    <td className="px-6 py-3">
                      <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                        r.status === 'ACCEPTED' ? 'bg-emerald-100 text-emerald-800' :
                        r.status === 'COMPLETED' ? 'bg-blue-100 text-blue-800' :
                        r.status === 'REQUESTED' ? 'bg-amber-100 text-amber-800' :
                        'bg-gray-100 text-gray-800'
                      }`}>
                        {r.status}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-gray-500">{new Date(r.requestedAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
