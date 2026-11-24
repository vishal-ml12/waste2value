import React, { useEffect, useState } from 'react';
import { addressApi } from '../../api/client';
import { Address, AddressRequest } from '../../types';

export const AddressesPage: React.FC = () => {
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);

  // Form modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [isDefault, setIsDefault] = useState(false);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAddresses = async () => {
    setLoading(true);
    try {
      const data = await addressApi.getMyAddresses();
      setAddresses(data);
    } catch (err) {
      console.error('Failed to load addresses', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAddresses();
  }, []);

  const handleSetDefault = async (id: number) => {
    try {
      await addressApi.setDefault(id);
      fetchAddresses();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to set default address');
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this address?')) return;
    try {
      await addressApi.deleteAddress(id);
      fetchAddresses();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete address');
    }
  };

  const handleUseGPS = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLatitude(pos.coords.latitude.toFixed(6));
          setLongitude(pos.coords.longitude.toFixed(6));
        },
        (err) => {
          alert('GPS Error: ' + err.message);
        }
      );
    } else {
      alert('Geolocation is not supported by your browser');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const latNum = Number(latitude);
    const lonNum = Number(longitude);

    if (isNaN(latNum) || latNum < -90 || latNum > 90) {
      setError('Latitude must be between -90 and 90');
      return;
    }
    if (isNaN(lonNum) || lonNum < -180 || lonNum > 180) {
      setError('Longitude must be between -180 and 180');
      return;
    }

    setSaving(true);
    try {
      const req: AddressRequest = {
        addressLine1: addressLine1.trim(),
        addressLine2: addressLine2 ? addressLine2.trim() : undefined,
        city: city.trim(),
        state: state.trim(),
        postalCode: postalCode.trim(),
        latitude: latNum,
        longitude: lonNum,
        isDefault,
      };

      await addressApi.createAddress(req);
      setModalOpen(false);
      // Reset form
      setAddressLine1('');
      setAddressLine2('');
      setCity('');
      setState('');
      setPostalCode('');
      setLatitude('');
      setLongitude('');
      setIsDefault(false);
      fetchAddresses();
    } catch (err: any) {
      const resp = err.response?.data;
      if (resp?.validationErrors?.length) {
        setError(resp.validationErrors.map((v: any) => v.message).join(', '));
      } else {
        setError(resp?.message || 'Failed to save address');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Facility & Warehouse Addresses
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Maintain multiple pickup locations with accurate GPS coordinates for collector routing.
          </p>
        </div>

        <button
          onClick={() => {
            setError(null);
            setModalOpen(true);
          }}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
        >
          + Add New Address
        </button>
      </div>

      {/* Address Cards Grid */}
      {loading ? (
        <div className="py-20 text-center text-sm text-gray-500">Loading addresses...</div>
      ) : addresses.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center shadow-sm">
          <div className="text-3xl mb-2">📍</div>
          <h3 className="font-bold text-gray-900 mb-1">No Addresses Registered</h3>
          <p className="text-xs text-gray-500 mb-4 max-w-sm mx-auto">
            You must add an address before creating waste listings so collectors know where lots are situated.
          </p>
          <button
            onClick={() => setModalOpen(true)}
            className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-xs font-semibold hover:bg-emerald-700"
          >
            Add First Address
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {addresses.map((addr) => (
            <div
              key={addr.id}
              className={`bg-white rounded-xl border p-5 shadow-sm space-y-3 relative ${
                addr.isDefault ? 'border-emerald-500 ring-1 ring-emerald-500/20' : 'border-gray-200'
              }`}
            >
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="font-bold text-gray-900 text-sm">{addr.addressLine1}</h3>
                  {addr.addressLine2 && (
                    <div className="text-xs text-gray-600">{addr.addressLine2}</div>
                  )}
                  <div className="text-xs text-gray-600">
                    {addr.city}, {addr.state} — {addr.postalCode}
                  </div>
                </div>

                {addr.isDefault && (
                  <span className="text-[10px] font-bold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                    Default Address
                  </span>
                )}
              </div>

              <div className="bg-gray-50 p-2.5 rounded-lg text-[11px] text-gray-600 flex justify-between items-center font-mono">
                <span>GPS: {addr.latitude.toFixed(5)}, {addr.longitude.toFixed(5)}</span>
                <a
                  href={`https://www.google.com/maps?q=${addr.latitude},${addr.longitude}`}
                  target="_blank"
                  rel="noreferrer"
                  className="text-emerald-600 font-sans font-semibold hover:underline"
                >
                  Map ↗
                </a>
              </div>

              <div className="pt-2 border-t border-gray-100 flex justify-between items-center text-xs">
                {!addr.isDefault ? (
                  <button
                    onClick={() => handleSetDefault(addr.id)}
                    className="text-emerald-600 hover:text-emerald-800 font-semibold"
                  >
                    Set as Default
                  </button>
                ) : (
                  <span className="text-gray-400 text-[11px]">Primary pickup point</span>
                )}

                <button
                  onClick={() => handleDelete(addr.id)}
                  className="text-rose-600 hover:text-rose-800 font-medium"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal: Add Address */}
      {modalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 sm:p-8 shadow-xl space-y-4 my-8">
            <div className="flex justify-between items-center border-b border-gray-100 pb-3">
              <h3 className="font-bold text-base text-gray-900">Add Pickup Address</h3>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="text-gray-400 hover:text-gray-600 text-lg"
              >
                ✕
              </button>
            </div>

            {error && (
              <div className="bg-rose-50 border border-rose-200 text-rose-700 px-3 py-2 rounded text-xs">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Address Line 1 <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={addressLine1}
                  onChange={(e) => setAddressLine1(e.target.value)}
                  placeholder="Plot 42, MIDC Industrial Area"
                  className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  Address Line 2 (Optional)
                </label>
                <input
                  type="text"
                  value={addressLine2}
                  onChange={(e) => setAddressLine2(e.target.value)}
                  placeholder="Near Gate 3 / Warehouse B"
                  className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
                />
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">
                    City <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Mumbai"
                    className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">
                    State <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={state}
                    onChange={(e) => setState(e.target.value)}
                    placeholder="Maharashtra"
                    className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">
                    Postal Code <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={postalCode}
                    onChange={(e) => setPostalCode(e.target.value)}
                    placeholder="400093"
                    className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>
              </div>

              {/* Coordinates */}
              <div className="bg-gray-50 p-4 rounded-xl border border-gray-200 space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-xs font-bold text-gray-800">Geolocation Coordinates</span>
                  <button
                    type="button"
                    onClick={handleUseGPS}
                    className="text-[11px] text-emerald-600 hover:text-emerald-700 font-semibold"
                  >
                    📍 Use Current Device GPS
                  </button>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[11px] text-gray-500 mb-1">
                      Latitude (-90 to 90) <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="number"
                      step="any"
                      required
                      value={latitude}
                      onChange={(e) => setLatitude(e.target.value)}
                      placeholder="19.11360"
                      className="w-full text-xs rounded border border-gray-300 px-2.5 py-1.5 bg-white outline-none focus:ring-1 focus:ring-emerald-500"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] text-gray-500 mb-1">
                      Longitude (-180 to 180) <span className="text-rose-500">*</span>
                    </label>
                    <input
                      type="number"
                      step="any"
                      required
                      value={longitude}
                      onChange={(e) => setLongitude(e.target.value)}
                      placeholder="72.86970"
                      className="w-full text-xs rounded border border-gray-300 px-2.5 py-1.5 bg-white outline-none focus:ring-1 focus:ring-emerald-500"
                    />
                  </div>
                </div>
              </div>

              {/* Default checkbox */}
              <div className="flex items-center space-x-2 pt-1">
                <input
                  type="checkbox"
                  id="defaultAddress"
                  checked={isDefault}
                  onChange={(e) => setIsDefault(e.target.checked)}
                  className="rounded border-gray-300 text-emerald-600 focus:ring-emerald-500"
                />
                <label htmlFor="defaultAddress" className="text-xs text-gray-700">
                  Mark this address as my default pickup location
                </label>
              </div>

              <div className="flex justify-end space-x-3 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-xs font-semibold text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm disabled:opacity-50"
                >
                  {saving ? 'Saving...' : 'Save Address'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
