import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, Category, PageResponse, ProcessorProfile, ProcessorReceipt } from '../../types';

export const ProcessorReceiptsPage: React.FC = () => {
  const [receipts, setReceipts] = useState<ProcessorReceipt[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [profile, setProfile] = useState<ProcessorProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);

  // Form
  const [collectorId, setCollectorId] = useState<number | ''>(3); // pre-fill with demo collector ID
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [quantity, setQuantity] = useState<number>(200);
  const [unit, setUnit] = useState<string>('kg');
  const [agreedPrice, setAgreedPrice] = useState<number>(22.5);
  const [notes, setNotes] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchReceipts = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<PageResponse<ProcessorReceipt>>>('/processor-receipts/my');
      setReceipts(res.data.data.content);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  const fetchProfileAndCategories = async () => {
    try {
      const [profRes, catRes] = await Promise.all([
        apiClient.get<ApiResponse<ProcessorProfile>>('/processors/me'),
        apiClient.get<ApiResponse<Category[]>>('/categories'),
      ]);
      setProfile(profRes.data.data);
      setCategories(catRes.data.data);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    fetchReceipts();
    fetchProfileAndCategories();
  }, []);

  const handleRecordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!collectorId || !categoryId) return;
    setSubmitting(true);
    setError(null);

    try {
      await apiClient.post('/processor-receipts', {
        collectorId,
        categoryId,
        quantity,
        unit,
        agreedPrice,
        notes: notes.trim(),
      });
      setShowAddModal(false);
      fetchReceipts();
      fetchProfileAndCategories(); // update available capacity
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to record material intake');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Material Intake & Processing Receipts</h1>
          <p className="text-sm text-gray-500 mt-1">
            Log incoming waste deliveries from certified collectors, verify net weight, and issue formal receipts
          </p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition-colors"
        >
          + Record Delivery Intake
        </button>
      </div>

      {profile && (
        <div className="bg-slate-50 border border-gray-200 p-4 rounded-xl flex flex-wrap justify-between items-center text-xs text-gray-600 gap-4">
          <div>
            <span>Plant Load: </span>
            <span className="font-bold text-gray-900">{profile.currentLoadKg.toLocaleString()} kg</span>
          </div>
          <div>
            <span>Available Storage Capacity: </span>
            <span className="font-bold text-emerald-700">{profile.availableCapacityKg.toLocaleString()} kg</span>
          </div>
          <div>
            <span>Max Permitted: </span>
            <span className="font-bold text-gray-900">{profile.maxCapacityKg.toLocaleString()} kg</span>
          </div>
        </div>
      )}

      {loading ? (
        <div className="py-12 flex justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
        </div>
      ) : receipts.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
          No material deliveries recorded yet. When a collector arrives with material, click "+ Record Delivery Intake".
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-gray-50 text-gray-600 border-b border-gray-100">
                <tr>
                  <th className="px-6 py-3 font-semibold">Receipt #</th>
                  <th className="px-6 py-3 font-semibold">Collector</th>
                  <th className="px-6 py-3 font-semibold">Material</th>
                  <th className="px-6 py-3 font-semibold">Net Weight</th>
                  <th className="px-6 py-3 font-semibold">Rate</th>
                  <th className="px-6 py-3 font-semibold">Total Credited</th>
                  <th className="px-6 py-3 font-semibold">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-gray-700">
                {receipts.map((rcpt) => (
                  <tr key={rcpt.id} className="hover:bg-gray-50">
                    <td className="px-6 py-3 font-mono font-bold text-gray-900">PR-{rcpt.id}</td>
                    <td className="px-6 py-3 font-medium text-gray-900">
                      {rcpt.collectorName}
                      {rcpt.generatorName && (
                        <span className="block text-[11px] text-gray-500 font-normal">Origin: {rcpt.generatorName}</span>
                      )}
                      {rcpt.transactionId && (
                        <span className="block text-[10px] text-emerald-600 font-mono">TX-{rcpt.transactionId}</span>
                      )}
                    </td>
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
        </div>
      )}

      {/* Record Intake Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl relative">
            <h3 className="text-lg font-bold text-gray-900 mb-1">Record Waste Drop-off Intake</h3>
            <p className="text-xs text-gray-500 mb-4">Log verified truck weight and credit collector</p>

            {error && (
              <div className="mb-4 p-3 bg-rose-50 text-rose-700 text-xs rounded-lg border border-rose-200">
                {error}
              </div>
            )}

            <form onSubmit={handleRecordSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Delivering Collector User ID</label>
                <input
                  type="number"
                  required
                  value={collectorId}
                  onChange={(e) => setCollectorId(parseInt(e.target.value) || '')}
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
                <span className="text-[10px] text-gray-400 mt-0.5 block">Pre-filled with City Collector (ID: 3)</span>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Material Category</label>
                <select
                  required
                  value={categoryId}
                  onChange={(e) => {
                    const id = parseInt(e.target.value) || '';
                    setCategoryId(id);
                    const cat = categories.find((c) => c.id === id);
                    if (cat) setUnit(cat.unit);
                  }}
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none bg-white"
                >
                  <option value="">Select category...</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} ({c.unit})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Quantity ({unit})</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={quantity}
                    onChange={(e) => setQuantity(parseFloat(e.target.value) || 0)}
                    className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Agreed Rate (₹/{unit})</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={agreedPrice}
                    onChange={(e) => setAgreedPrice(parseFloat(e.target.value) || 0)}
                    className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="text-xs text-gray-600 bg-emerald-50 p-2.5 rounded border border-emerald-200 flex justify-between font-medium">
                <span>Total Payout Value:</span>
                <span className="font-bold text-emerald-800">₹{(quantity * agreedPrice).toFixed(2)}</span>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Quality Inspection Notes</label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="e.g., Clean bales, moisture under 3%, density approved."
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
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
                  {submitting ? 'Recording...' : 'Record Receipt'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
