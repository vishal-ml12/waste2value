import React, { useState, useEffect } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, CollectorProfile } from '../../types';

export const CollectorProfilePage: React.FC = () => {
  const [profile, setProfile] = useState<CollectorProfile | null>(null);
  const [vehicleType, setVehicleType] = useState('');
  const [serviceArea, setServiceArea] = useState('');
  const [maxDistanceKm, setMaxDistanceKm] = useState<number>(50);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<CollectorProfile>>('/collectors/me');
      const data = res.data.data;
      setProfile(data);
      setVehicleType(data.vehicleType);
      setServiceArea(data.serviceArea);
      setMaxDistanceKm(data.maxDistanceKm);
    } catch {
      // Profile might not exist yet
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
      const res = await apiClient.post<ApiResponse<CollectorProfile>>('/collectors/me', {
        vehicleType,
        serviceArea,
        maxDistanceKm,
      });
      setProfile(res.data.data);
      setMessage({ type: 'success', text: 'Collector vehicle profile saved successfully!' });
    } catch (err: any) {
      setMessage({ type: 'error', text: err.response?.data?.message || 'Failed to save collector profile' });
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
          <h1 className="text-2xl font-bold text-gray-900">Collector Profile & Vehicle Setup</h1>
          <p className="text-sm text-gray-500 mt-1">Configure your logistics capability, vehicle capacity, and service area</p>
        </div>
        {profile && (
          <div className="flex items-center space-x-2">
            <span
              className={`px-3 py-1 rounded-full text-xs font-semibold ${
                profile.isVerified
                  ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                  : 'bg-amber-100 text-amber-800 border border-amber-300'
              }`}
            >
              {profile.isVerified ? '✓ Verified by Admin' : '⏳ Pending Verification'}
            </span>
          </div>
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
            <span className="text-gray-500">Average Rating:</span>
            <span className="font-bold text-amber-600 ml-2">★ {profile.avgRating.toFixed(2)}</span>
          </div>
          <div>
            <span className="text-gray-500">Total Pickups Completed:</span>
            <span className="font-bold text-gray-900 ml-2">{profile.totalPickups}</span>
          </div>
          <div>
            <span className="text-gray-500">Registered On:</span>
            <span className="font-medium text-gray-700 ml-2">{new Date(profile.createdAt).toLocaleDateString()}</span>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm space-y-5">
        <div>
          <label className="block text-xs font-semibold text-gray-700 mb-1">
            Vehicle Specification & Model
          </label>
          <input
            type="text"
            required
            value={vehicleType}
            onChange={(e) => setVehicleType(e.target.value)}
            placeholder="e.g., Tata Ace EV Flatbed (1.5 Ton Capacity), Hydraulic Bale Lift"
            className="w-full text-sm border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
          />
          <span className="text-[11px] text-gray-400 mt-1 block">Specify vehicle class, fuel/EV, and payload capacity.</span>
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-700 mb-1">
            Service Coverage Area / Regions
          </label>
          <input
            type="text"
            required
            value={serviceArea}
            onChange={(e) => setServiceArea(e.target.value)}
            placeholder="e.g., Mumbai MMR, Navi Mumbai, Thane Industrial Corridor"
            className="w-full text-sm border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-gray-700 mb-1">
            Maximum Operating Radius (Kilometers)
          </label>
          <div className="flex items-center space-x-4">
            <input
              type="number"
              min="1"
              max="500"
              required
              value={maxDistanceKm}
              onChange={(e) => setMaxDistanceKm(parseFloat(e.target.value) || 1)}
              className="w-32 text-sm border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
            />
            <span className="text-xs text-gray-500">km from vehicle base depot</span>
          </div>
        </div>

        <div className="pt-4 border-t border-gray-100 flex justify-end">
          <button
            type="submit"
            disabled={saving}
            className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-semibold shadow-sm transition-colors disabled:opacity-50"
          >
            {saving ? 'Saving...' : 'Save Profile'}
          </button>
        </div>
      </form>
    </div>
  );
};
