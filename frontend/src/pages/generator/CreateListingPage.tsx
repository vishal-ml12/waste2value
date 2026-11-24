import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { listingApi, categoryApi, addressApi } from '../../api/client';
import { Category, Address } from '../../types';

export const CreateListingPage: React.FC = () => {
  const navigate = useNavigate();

  const [categories, setCategories] = useState<Category[]>([]);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [loading, setLoading] = useState(true);

  // Form states
  const [categoryId, setCategoryId] = useState('');
  const [addressId, setAddressId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState('kg');
  const [expectedPrice, setExpectedPrice] = useState('0');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const init = async () => {
      try {
        const [cats, addrs] = await Promise.all([
          categoryApi.getFlat(true),
          addressApi.getMyAddresses(),
        ]);
        setCategories(cats);
        setAddresses(addrs);

        if (addrs.length > 0) {
          const defaultAddr = addrs.find((a) => a.isDefault) || addrs[0];
          setAddressId(defaultAddr.id.toString());
        }
      } catch (err) {
        console.error('Failed to initialize listing form', err);
      } finally {
        setLoading(false);
      }
    };
    init();
  }, []);

  const handleCategoryChange = (catIdStr: string) => {
    setCategoryId(catIdStr);
    const selected = categories.find((c) => c.id === Number(catIdStr));
    if (selected) {
      setUnit(selected.unit);
    }
  };

  const handleSave = async (publishImmediately: boolean) => {
    setError(null);
    if (!categoryId) {
      setError('Please select a waste category');
      return;
    }
    if (!addressId) {
      setError('Please select a pickup address');
      return;
    }
    if (!title.trim()) {
      setError('Listing title is required');
      return;
    }
    if (!quantity || Number(quantity) <= 0) {
      setError('Quantity must be greater than zero');
      return;
    }

    setSubmitting(true);
    try {
      const created = await listingApi.create({
        categoryId: Number(categoryId),
        addressId: Number(addressId),
        title: title.trim(),
        description: description.trim(),
        quantity: Number(quantity),
        unit: unit.trim(),
        expectedPrice: expectedPrice ? Number(expectedPrice) : 0,
      });

      if (publishImmediately) {
        await listingApi.publish(created.id);
      }

      navigate(`/listings/${created.id}`);
    } catch (err: any) {
      const resp = err.response?.data;
      if (resp?.validationErrors?.length) {
        setError(resp.validationErrors.map((v: any) => v.message).join(', '));
      } else {
        setError(resp?.message || 'Failed to create listing');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="py-20 text-center text-sm text-gray-500">Loading form resources...</div>;
  }

  if (addresses.length === 0) {
    return (
      <div className="max-w-md mx-auto px-4 py-16 text-center">
        <div className="bg-white rounded-xl border border-gray-200 p-8 shadow-sm">
          <div className="text-3xl mb-3">📍</div>
          <h2 className="text-base font-bold text-gray-900 mb-2">Pickup Address Required</h2>
          <p className="text-xs text-gray-500 mb-6">
            You must register at least one facility or warehouse pickup address with GPS coordinates before creating waste listings.
          </p>
          <Link
            to="/generator/addresses"
            className="inline-flex px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold"
          >
            Add Pickup Address Now
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Create Waste Listing</h1>
          <p className="text-xs text-gray-500 mt-1">
            Specify material specifications and pickup details for collectors and processors.
          </p>
        </div>
        <Link to="/generator/listings" className="text-xs text-gray-500 hover:text-gray-900">
          Cancel
        </Link>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6 sm:p-8 shadow-sm">
        {error && (
          <div className="mb-6 bg-rose-50 border border-rose-200 text-rose-700 px-4 py-3 rounded-lg text-xs">
            {error}
          </div>
        )}

        <form onSubmit={(e) => e.preventDefault()} className="space-y-6">
          {/* Category & Address Row */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">
                Waste Category <span className="text-rose-500">*</span>
              </label>
              <select
                value={categoryId}
                onChange={(e) => handleCategoryChange(e.target.value)}
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 bg-white outline-none focus:ring-1 focus:ring-emerald-500"
              >
                <option value="">Select Category...</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.parentName ? `${c.parentName} → ${c.name}` : c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="block text-xs font-semibold text-gray-700">
                  Pickup Facility / Address <span className="text-rose-500">*</span>
                </label>
                <Link to="/generator/addresses" className="text-[11px] text-emerald-600 hover:underline">
                  + New
                </Link>
              </div>
              <select
                value={addressId}
                onChange={(e) => setAddressId(e.target.value)}
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 bg-white outline-none focus:ring-1 focus:ring-emerald-500"
              >
                {addresses.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.addressLine1}, {a.city} {a.isDefault ? '(Default)' : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Title */}
          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">
              Listing Title <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Baled Post-Industrial PET Bottles"
              className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
            />
          </div>

          {/* Quantity, Unit, Expected Price */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">
                Quantity <span className="text-rose-500">*</span>
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                placeholder="500.00"
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">
                Unit of Measure <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={unit}
                onChange={(e) => setUnit(e.target.value)}
                placeholder="kg, items, ton..."
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">
                Expected Price (₹ / {unit || 'unit'})
              </label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={expectedPrice}
                onChange={(e) => setExpectedPrice(e.target.value)}
                placeholder="0.00"
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
              />
              <span className="text-[10px] text-gray-400">Set 0 for free donation/pickup</span>
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">
              Description & Material Condition <span className="text-rose-500">*</span>
            </label>
            <textarea
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe cleanliness, moisture levels, bundling/baling condition, loading accessibility, etc."
              className="w-full text-xs rounded-md border border-gray-300 p-3 outline-none focus:ring-1 focus:ring-emerald-500"
            />
          </div>

          {/* Submit Actions */}
          <div className="pt-4 border-t border-gray-100 flex flex-col sm:flex-row justify-end gap-3">
            <button
              type="button"
              disabled={submitting}
              onClick={() => handleSave(false)}
              className="px-4 py-2.5 rounded-lg border border-gray-300 text-xs font-semibold text-gray-700 hover:bg-gray-50 transition"
            >
              {submitting ? 'Saving...' : 'Save as DRAFT'}
            </button>
            <button
              type="button"
              disabled={submitting}
              onClick={() => handleSave(true)}
              className="px-5 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold shadow-sm transition"
            >
              {submitting ? 'Publishing...' : 'Save & Publish to Marketplace'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
