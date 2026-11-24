import React, { useEffect, useState } from 'react';
import { userApi } from '../../api/client';
import { UserProfile } from '../../types';
import { useAuth } from '../../context/AuthContext';

export const ProfilePage: React.FC = () => {
  const { updateUser } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  // Edit states
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const data = await userApi.getProfile();
      setProfile(data);
      setFirstName(data.firstName);
      setLastName(data.lastName);
      setPhone(data.phone || '');
    } catch (err) {
      console.error('Failed to load profile', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);
    setError(null);
    setSaving(true);

    try {
      const updated = await userApi.updateProfile({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        phone: phone ? phone.trim() : undefined,
      });
      setProfile(updated);
      updateUser(updated);
      setMessage('Profile updated successfully!');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="py-20 text-center text-sm text-gray-500">Loading profile...</div>;
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Account Profile</h1>
        <p className="text-xs text-gray-500 mt-1">Manage your identity and contact information.</p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
        {/* Account Info Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pb-6 border-b border-gray-100">
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="text-[11px] text-gray-400 font-semibold uppercase tracking-wider">Account Role</div>
            <div className="text-sm font-bold text-emerald-800 mt-1">{profile?.role}</div>
          </div>
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="text-[11px] text-gray-400 font-semibold uppercase tracking-wider">Account Status</div>
            <div className="text-sm font-bold text-gray-900 mt-1">{profile?.status}</div>
          </div>
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="text-[11px] text-gray-400 font-semibold uppercase tracking-wider">Member Since</div>
            <div className="text-xs font-semibold text-gray-700 mt-1">
              {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '-'}
            </div>
          </div>
        </div>

        {message && (
          <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-lg text-xs">
            {message}
          </div>
        )}

        {error && (
          <div className="bg-rose-50 border border-rose-200 text-rose-700 px-4 py-3 rounded-lg text-xs">
            {error}
          </div>
        )}

        {/* Update Form */}
        <form onSubmit={handleUpdate} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">First Name</label>
              <input
                type="text"
                required
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Last Name</label>
              <input
                type="text"
                required
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Email Address</label>
            <input
              type="email"
              disabled
              value={profile?.email || ''}
              className="w-full text-xs rounded-md border border-gray-200 bg-gray-50 text-gray-500 px-3 py-2 cursor-not-allowed"
            />
            <span className="text-[10px] text-gray-400">Email cannot be modified directly</span>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Phone Number</label>
            <input
              type="text"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="+91 9876543210"
              className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
            />
          </div>

          <div className="pt-3 flex justify-end">
            <button
              type="submit"
              disabled={saving}
              className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition disabled:opacity-50"
            >
              {saving ? 'Updating...' : 'Save Profile Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
