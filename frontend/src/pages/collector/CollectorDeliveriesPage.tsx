import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, PageResponse, ProcessorReceipt } from '../../types';

export const CollectorDeliveriesPage: React.FC = () => {
  const [deliveries, setDeliveries] = useState<ProcessorReceipt[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchDeliveries = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<PageResponse<ProcessorReceipt>>>('/processor-receipts/deliveries');
      setDeliveries(res.data.data.content);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDeliveries();
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Processor Deliveries & Receipts</h1>
        <p className="text-sm text-gray-500 mt-1">Official verified receipts recorded by recycling processors for your waste drops</p>
      </div>

      {loading ? (
        <div className="py-12 flex justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
        </div>
      ) : deliveries.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
          No delivery receipts recorded yet by processors.
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-gray-50 text-gray-600 border-b border-gray-100">
                <tr>
                  <th className="px-6 py-3 font-semibold">Receipt #</th>
                  <th className="px-6 py-3 font-semibold">Processor Facility</th>
                  <th className="px-6 py-3 font-semibold">Material Category</th>
                  <th className="px-6 py-3 font-semibold">Quantity</th>
                  <th className="px-6 py-3 font-semibold">Agreed Rate</th>
                  <th className="px-6 py-3 font-semibold">Total Amount</th>
                  <th className="px-6 py-3 font-semibold">Status</th>
                  <th className="px-6 py-3 font-semibold">Drop Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-gray-700">
                {deliveries.map((d) => (
                  <tr key={d.id} className="hover:bg-gray-50">
                    <td className="px-6 py-3 font-mono font-bold text-gray-900">PR-{d.id}</td>
                    <td className="px-6 py-3 font-semibold text-gray-900">
                      {d.companyName}
                      {d.generatorName && (
                        <span className="block text-[10px] text-gray-500 font-normal">Origin: {d.generatorName}</span>
                      )}
                      {d.transactionId && (
                        <span className="block text-[10px] text-emerald-600 font-mono">TX-{d.transactionId}</span>
                      )}
                    </td>
                    <td className="px-6 py-3">{d.categoryName}</td>
                    <td className="px-6 py-3 font-bold text-gray-900">{d.quantity} {d.unit}</td>
                    <td className="px-6 py-3 text-emerald-700">₹{d.agreedPrice} / {d.unit}</td>
                    <td className="px-6 py-3 font-bold text-emerald-800">₹{d.totalAmount.toLocaleString()}</td>
                    <td className="px-6 py-3">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-100 text-emerald-800">
                        {d.status}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-gray-500">{new Date(d.receivedDate).toLocaleDateString()}</td>
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
