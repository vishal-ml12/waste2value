import React, { useState, useEffect } from 'react';
import { EmissionFactor, UpdateEmissionFactorRequest } from '../../types';
import { analyticsApi } from '../../api/client';

interface EmissionFactorsModalProps {
  isOpen: boolean;
  onClose: () => void;
  isAdmin?: boolean;
}

export const EmissionFactorsModal: React.FC<EmissionFactorsModalProps> = ({
  isOpen,
  onClose,
  isAdmin = false,
}) => {
  const [factors, setFactors] = useState<EmissionFactor[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [editingFactor, setEditingFactor] = useState<EmissionFactor | null>(null);
  const [formData, setFormData] = useState<UpdateEmissionFactorRequest>({
    co2FactorKgPerUnit: 0,
    landfillDiversionRate: 1.0,
    sourceReference: '',
    methodologyNotes: '',
    isActive: true,
  });
  const [saving, setSaving] = useState<boolean>(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const fetchFactors = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await analyticsApi.getEmissionFactors();
      setFactors(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load emission factors');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchFactors();
      setEditingFactor(null);
      setSuccessMsg(null);
    }
  }, [isOpen]);

  const handleEditClick = (f: EmissionFactor) => {
    setEditingFactor(f);
    setFormData({
      co2FactorKgPerUnit: f.co2FactorKgPerUnit ?? 0,
      landfillDiversionRate: f.landfillDiversionRate,
      sourceReference: f.sourceReference || '',
      methodologyNotes: f.methodologyNotes || '',
      isActive: f.isActive,
    });
    setSuccessMsg(null);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingFactor) return;

    try {
      setSaving(true);
      setError(null);
      await analyticsApi.updateEmissionFactor(editingFactor.id, formData);
      setSuccessMsg(`Successfully updated factor for ${editingFactor.categoryName}`);
      setEditingFactor(null);
      fetchFactors();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to update emission factor');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden animate-fadeIn">
        {/* Header */}
        <div className="p-6 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div>
            <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
              <span className="p-1.5 bg-emerald-100 text-emerald-700 rounded-lg">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064" />
                </svg>
              </span>
              Environmental Emission Factors & Methodology
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              Authoritative factors based on US EPA WARM (Waste Reduction Model) v15 & UK DEFRA / WRAP.
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200 transition"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto flex-1">
          {error && (
            <div className="mb-4 p-3.5 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-center gap-2">
              <svg className="w-5 h-5 text-red-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>{error}</span>
            </div>
          )}

          {successMsg && (
            <div className="mb-4 p-3.5 bg-emerald-50 border border-emerald-200 rounded-lg text-sm text-emerald-800 flex items-center gap-2">
              <svg className="w-5 h-5 text-emerald-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              <span>{successMsg}</span>
            </div>
          )}

          {/* Edit Form if editing */}
          {editingFactor && (
            <div className="mb-6 p-5 bg-emerald-50/50 border border-emerald-200 rounded-xl">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-bold text-slate-800">
                  Update Emission Factor: <span className="text-emerald-700">{editingFactor.categoryName}</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setEditingFactor(null)}
                  className="text-xs text-slate-500 hover:text-slate-700 underline"
                >
                  Cancel
                </button>
              </div>

              <form onSubmit={handleSave} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    CO₂ Factor (kg CO₂e per kg)
                  </label>
                  <input
                    type="number"
                    step="0.0001"
                    min="0"
                    value={formData.co2FactorKgPerUnit ?? ''}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        co2FactorKgPerUnit: e.target.value === '' ? undefined : parseFloat(e.target.value),
                      })
                    }
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                    placeholder="e.g. 1.4300"
                  />
                  <span className="text-[11px] text-slate-500">Leave blank for unconfigured/pending.</span>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Landfill Diversion Rate (0.0 to 1.0)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    max="1.0"
                    required
                    value={formData.landfillDiversionRate}
                    onChange={(e) =>
                      setFormData({ ...formData, landfillDiversionRate: parseFloat(e.target.value) })
                    }
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                  <span className="text-[11px] text-slate-500">1.0 = 100% diversion of verified recycled weight.</span>
                </div>

                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Source Reference
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.sourceReference}
                    onChange={(e) => setFormData({ ...formData, sourceReference: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                    placeholder="e.g. US EPA WARM v15 (2020)"
                  />
                </div>

                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    Methodology Notes
                  </label>
                  <textarea
                    rows={2}
                    value={formData.methodologyNotes || ''}
                    onChange={(e) => setFormData({ ...formData, methodologyNotes: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                    placeholder="Documentation of lifecycle baseline and recycling offsets..."
                  />
                </div>

                <div className="md:col-span-2 flex items-center justify-between pt-2">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={formData.isActive}
                      onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                      className="w-4 h-4 text-emerald-600 rounded border-slate-300 focus:ring-emerald-500"
                    />
                    <span className="text-xs font-medium text-slate-700">Factor Active</span>
                  </label>
                  <button
                    type="submit"
                    disabled={saving}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow transition disabled:opacity-50"
                  >
                    {saving ? 'Saving...' : 'Save Factor Changes'}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Factors Table */}
          {loading ? (
            <div className="py-12 flex justify-center items-center text-slate-400">
              <svg className="animate-spin h-6 w-6 text-emerald-600 mr-2" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
              </svg>
              <span>Loading emission factors...</span>
            </div>
          ) : (
            <div className="border border-slate-200 rounded-xl overflow-hidden shadow-sm">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-slate-600 text-xs uppercase font-semibold border-b border-slate-200">
                  <tr>
                    <th className="px-4 py-3">Category</th>
                    <th className="px-4 py-3 text-right">CO₂ Factor (kg/kg)</th>
                    <th className="px-4 py-3 text-center">Diversion</th>
                    <th className="px-4 py-3">Source & Methodology</th>
                    <th className="px-4 py-3 text-center">Status</th>
                    {isAdmin && <th className="px-4 py-3 text-right">Action</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {factors.map((f) => (
                    <tr key={f.id} className="hover:bg-slate-50 transition">
                      <td className="px-4 py-3 font-semibold text-slate-800">
                        {f.categoryName}
                      </td>
                      <td className="px-4 py-3 text-right">
                        {f.co2FactorKgPerUnit !== null && f.co2FactorKgPerUnit !== undefined ? (
                          <span className="font-bold text-teal-700">
                            {f.co2FactorKgPerUnit.toFixed(4)}
                          </span>
                        ) : (
                          <span className="text-amber-600 font-medium text-xs bg-amber-50 px-2 py-0.5 rounded border border-amber-200">
                            Unconfigured
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-center text-slate-600 font-medium">
                        {(f.landfillDiversionRate * 100).toFixed(0)}%
                      </td>
                      <td className="px-4 py-3">
                        <div className="font-medium text-slate-800 text-xs">{f.sourceReference}</div>
                        {f.methodologyNotes && (
                          <div className="text-[11px] text-slate-500 truncate max-w-xs">
                            {f.methodologyNotes}
                          </div>
                        )}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span
                          className={`inline-block px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                            f.isActive
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-slate-100 text-slate-600'
                          }`}
                        >
                          {f.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      {isAdmin && (
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => handleEditClick(f)}
                            className="px-2.5 py-1 text-xs font-semibold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 rounded-md transition"
                          >
                            Edit
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Academic / Regulatory Disclaimer */}
          <div className="mt-6 p-4 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-500 leading-relaxed">
            <span className="font-bold text-slate-700 uppercase tracking-wider block mb-1">
              Methodology & Transparency Notice:
            </span>
            Calculations represent estimates based on lifecycle emission factors from the US EPA Waste Reduction Model (WARM v15) and UK DEFRA/WRAP carbon accounting standards. Net emissions savings reflect avoided virgin material extraction, manufacturing energy, and landfill methane prevention when waste is verifiably recycled. Categories without accredited regulatory factors (such as complex mixed e-waste) are marked unconfigured and excluded from carbon calculations.
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-200 flex justify-end bg-slate-50">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 text-xs font-semibold rounded-lg transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
