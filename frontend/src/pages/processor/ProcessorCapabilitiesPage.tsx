import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, Category, ProcessorCapability } from '../../types';

export const ProcessorCapabilitiesPage: React.FC = () => {
  const [capabilities, setCapabilities] = useState<ProcessorCapability[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddModal, setShowAddModal] = useState(false);

  // Form state
  const [categoryId, setCategoryId] = useState<number | ''>('');
  const [minQuantity, setMinQuantity] = useState<number>(100);
  const [maxQuantity, setMaxQuantity] = useState<number>(5000);
  const [pricePerUnit, setPricePerUnit] = useState<number>(20);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchCapabilities = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<ProcessorCapability[]>>('/processors/me/capabilities');
      setCapabilities(res.data.data);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  const fetchCategories = async () => {
    try {
      const res = await apiClient.get<ApiResponse<Category[]>>('/categories');
      setCategories(res.data.data);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    fetchCapabilities();
    fetchCategories();
  }, []);

  const handleToggle = async (id: number, currentActive: boolean) => {
    try {
      await apiClient.patch(`/processors/me/capabilities/${id}/active`, { active: !currentActive });
      setCapabilities((prev) =>
        prev.map((c) => (c.id === id ? { ...c, isActive: !currentActive } : c))
      );
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to toggle capability');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to remove this capability?')) return;
    try {
      await apiClient.delete(`/processors/me/capabilities/${id}`);
      setCapabilities((prev) => prev.filter((c) => c.id !== id));
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete capability');
    }
  };

  const handleAddSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!categoryId) return;
    setSaving(true);
    setError(null);

    try {
      await apiClient.post('/processors/me/capabilities', {
        categoryId,
        minQuantity,
        maxQuantity,
        pricePerUnit,
        isActive: true,
      });
      setShowAddModal(false);
      setCategoryId('');
      fetchCapabilities();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to add capability');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Material Acceptance Capabilities</h1>
          <p className="text-sm text-gray-500 mt-1">
            Specify which waste categories your plant accepts, along with minimum batches and offered purchase rates
          </p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition-colors"
        >
          + Add Material Capability
        </button>
      </div>

      {loading ? (
        <div className="py-12 flex justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
        </div>
      ) : capabilities.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center text-gray-500 text-sm">
          No capabilities declared yet. Click "+ Add Material Capability" to specify the waste streams your facility purchases.
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-gray-50 text-gray-600 border-b border-gray-100">
                <tr>
                  <th className="px-6 py-3 font-semibold">Accepted Category</th>
                  <th className="px-6 py-3 font-semibold">Min Batch</th>
                  <th className="px-6 py-3 font-semibold">Max Batch</th>
                  <th className="px-6 py-3 font-semibold">Purchase Price</th>
                  <th className="px-6 py-3 font-semibold">Active Status</th>
                  <th className="px-6 py-3 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-gray-700">
                {capabilities.map((cap) => (
                  <tr key={cap.id} className="hover:bg-gray-50">
                    <td className="px-6 py-3 font-semibold text-gray-900">{cap.categoryName}</td>
                    <td className="px-6 py-3">{cap.minQuantity} {cap.unit}</td>
                    <td className="px-6 py-3">{cap.maxQuantity} {cap.unit}</td>
                    <td className="px-6 py-3 font-bold text-emerald-700">₹{cap.pricePerUnit} / {cap.unit}</td>
                    <td className="px-6 py-3">
                      <button
                        onClick={() => handleToggle(cap.id, cap.isActive)}
                        className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                          cap.isActive ? 'bg-emerald-100 text-emerald-800' : 'bg-gray-100 text-gray-500'
                        }`}
                      >
                        {cap.isActive ? 'ACTIVE' : 'PAUSED'}
                      </button>
                    </td>
                    <td className="px-6 py-3 text-right">
                      <button
                        onClick={() => handleDelete(cap.id)}
                        className="text-rose-600 hover:text-rose-700 font-medium text-xs"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl relative">
            <h3 className="text-lg font-bold text-gray-900 mb-1">Add Waste Stream Capability</h3>
            <p className="text-xs text-gray-500 mb-4">Declare purchase rates and minimum acceptance thresholds</p>

            {error && (
              <div className="mb-4 p-3 bg-rose-50 text-rose-700 text-xs rounded-lg border border-rose-200">
                {error}
              </div>
            )}

            <form onSubmit={handleAddSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Waste Category</label>
                <select
                  required
                  value={categoryId}
                  onChange={(e) => setCategoryId(parseInt(e.target.value) || '')}
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none bg-white"
                >
                  <option value="">Select a category...</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} ({c.unit})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Min Batch Quantity</label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={minQuantity}
                    onChange={(e) => setMinQuantity(parseFloat(e.target.value) || 0)}
                    className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Max Batch Quantity</label>
                  <input
                    type="number"
                    min="0"
                    required
                    value={maxQuantity}
                    onChange={(e) => setMaxQuantity(parseFloat(e.target.value) || 0)}
                    className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Offered Price Per Unit (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  required
                  value={pricePerUnit}
                  onChange={(e) => setPricePerUnit(parseFloat(e.target.value) || 0)}
                  className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="flex justify-end space-x-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  disabled={saving}
                  className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm"
                >
                  {saving ? 'Adding...' : 'Add Capability'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
