import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, ProcessorProfile } from '../../types';

export const ProcessorProfilePage: React.FC = () => {
  const [profile, setProfile] = useState<ProcessorProfile | null>(null);
  const [companyName, setCompanyName] = useState('');
  const [licenseNumber, setLicenseNumber] = useState('');
  const [description, setDescription] = useState('');
  const [maxCapacityKg, setMaxCapacityKg] = useState<number>(10000);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<ProcessorProfile>>('/processors/me');
      const data = res.data.data;
      setProfile(data);
      setCompanyName(data.companyName);
      setLicenseNumber(data.licenseNumber);
      setDescription(data.description || '');
      setMaxCapacityKg(data.maxCapacityKg);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setMessage(null);

    try {
      const res = await apiClient.post<ApiResponse<ProcessorProfile>>('/processors/me', {
        companyName,
        licenseNumber,
        description,
        maxCapacityKg,
      });
      setProfile(res.data.data);
      setMessage({ type: 'success', text: 'Processor facility profile saved successfully!' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.response?.data?.message || 'Failed to save processor profile' });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12 flex justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Processor Facility Profile</h1>
          <p className="text-sm text-gray-500 mt-1">Configure regulatory license, facility description, and plant intake capacity</p>
        </div>
        {profile && (
          <span
            className={`px-3 py-1 rounded-full text-xs font-semibold ${
              profile.isVerified
                ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                : 'bg-amber-100 text-amber-800 border border-amber-300'
            }`}
          >
            {profile.isVerified ? '✓ Verified Facility' : '⏳ Pending Verification'}
          </span>
        )}
      </div>

      {message && (
        <div
          className={`p-4 rounded-lg text-sm border ${
            message.type === 'success'
              ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
              : 'bg-rose-50 text-rose-800 border-rose-200'
          }`}
        >
          {message.text}
        </div>
      )}

      {profile && (
        <div className="grid grid-cols-3 gap-4 bg-slate-50 p-4 rounded-xl border border-gray-200 text-xs">
          <div>
            <span className="text-gray-500">Current Load:</span>
            <span className="font-bold text-gray-900 ml-2">{profile.currentLoadKg.toLocaleString()} kg</span>
          </div>
          <div>
            <span className="text-gray-500">Available Space:</span>
            <span className="font-bold text-emerald-700 ml-2">{profile.availableCapacityKg.toLocaleString()} kg</span>
          </div>
          <div>
            <span className="text-gray-500">Facility Rating:</span>
            <span className="font-bold text-amber-600 ml-2">★ {profile.avgRating.toFixed(2)}</span>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm space-y-5">
        <div>
          <label className="block text-xs font-semibold text-gray-700 mb-1">Company / Facility Name</label>
          <input
            type="text"
            required
            value={companyName}
            onChange={(e) => setCompanyName(e.target.value)}
            placeholder="e.g., CleanPolymer Recovery & Refining Ltd."
            className="w-full text-sm border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-700 mb-1">Environmental / Trade License Number</label>
          <input
            type="text"
            required
            value={licenseNumber}
            onChange={(e) => setLicenseNumber(e.target.value)}
            placeholder="e.g., SPCB-RECYCLE-2024-MH-9921"
            className="w-full text-sm border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-700 mb-1">Maximum Storage & Processing Capacity (kg)</label>
          <div className="flex items-center space-x-3">
            <input
              type="number"
              min="1"
              required
              value={maxCapacityKg}
              onChange={(e) => setMaxCapacityKg(parseFloat(e.target.value) || 0)}
              className="w-48 text-sm border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
            />
            <span className="text-xs text-gray-500">kg total facility capacity</span>
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-700 mb-1">Plant Description & Equipment Overview</label>
          <textarea
            rows={4}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe shredding, washing, optical sorting, extrusion, or smelting machinery and accepted grades..."
            className="w-full text-sm border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
          />
        </div>

        <div className="pt-4 border-t border-gray-100 flex justify-end">
          <button
            type="submit"
            disabled={saving}
            className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition-colors disabled:opacity-50"
          >
            {saving ? 'Saving...' : 'Save Facility Profile'}
          </button>
        </div>
      </form>
    </div>
  );
};
