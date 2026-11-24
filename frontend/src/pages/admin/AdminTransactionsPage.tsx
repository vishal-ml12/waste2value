import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, PageResponse, Transaction } from '../../types';

export const AdminTransactionsPage: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<PageResponse<Transaction>>>('/transactions');
      setTransactions(res.data.data.content);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Platform Financial & Transfer Transactions</h1>
        <p className="text-sm text-gray-500 mt-1">Audit trail of completed waste transactions, settlement rates, and totals</p>
      </div>

      {loading ? (
        <div className="py-12 flex justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
        </div>
      ) : transactions.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
          No completed transactions logged yet. Completed pickups will record transactions here.
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-gray-50 text-gray-600 border-b border-gray-100">
                <tr>
                  <th className="px-6 py-3 font-semibold">Tx ID</th>
                  <th className="px-6 py-3 font-semibold">Listing</th>
                  <th className="px-6 py-3 font-semibold">Generator</th>
                  <th className="px-6 py-3 font-semibold">Collector</th>
                  <th className="px-6 py-3 font-semibold">Volume</th>
                  <th className="px-6 py-3 font-semibold">Agreed Rate</th>
                  <th className="px-6 py-3 font-semibold">Total Amount</th>
                  <th className="px-6 py-3 font-semibold">Status</th>
                  <th className="px-6 py-3 font-semibold">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-gray-700">
                {transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50">
                    <td className="px-6 py-3 font-mono font-bold text-gray-900">TX-{tx.id}</td>
                    <td className="px-6 py-3 font-semibold text-gray-900">{tx.listingTitle}</td>
                    <td className="px-6 py-3">{tx.generatorName}</td>
                    <td className="px-6 py-3 font-medium text-emerald-800">{tx.collectorName}</td>
                    <td className="px-6 py-3 font-bold text-gray-900">{tx.quantity} {tx.unit}</td>
                    <td className="px-6 py-3 text-emerald-700 font-semibold">₹{tx.agreedPrice} / {tx.unit}</td>
                    <td className="px-6 py-3 font-bold text-emerald-800">₹{tx.totalAmount.toLocaleString()}</td>
                    <td className="px-6 py-3">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-100 text-blue-800">
                        {tx.status}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-gray-500">{new Date(tx.transactionDate).toLocaleDateString()}</td>
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
