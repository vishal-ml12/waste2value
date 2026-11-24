import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { listingApi, categoryApi, addressApi } from '../../api/client';
import { Category, Address, ListingDetail } from '../../types';

export const EditListingPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [categories, setCategories] = useState<Category[]>([]);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [listing, setListing] = useState<ListingDetail | null>(null);
  const [loading, setLoading] = useState(true);

  // Form states
  const [categoryId, setCategoryId] = useState('');
  const [addressId, setAddressId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState('kg');
  const [expectedPrice, setExpectedPrice] = useState('0');

  const [uploadingImage, setUploadingImage] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [cats, addrs, listData] = await Promise.all([
        categoryApi.getFlat(true),
        addressApi.getMyAddresses(),
        listingApi.getById(Number(id)),
      ]);

      if (listData.status !== 'DRAFT') {
        alert('Only DRAFT listings can be edited. This listing is ' + listData.status);
        navigate(`/listings/${listData.id}`);
        return;
      }

      setCategories(cats);
      setAddresses(addrs);
      setListing(listData);

      setCategoryId(listData.categoryId.toString());
      setAddressId(listData.addressId.toString());
      setTitle(listData.title);
      setDescription(listData.description);
      setQuantity(listData.quantity.toString());
      setUnit(listData.unit);
      setExpectedPrice(listData.expectedPrice.toString());
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load listing for editing');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [id]);

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !listing) return;
    setError(null);
    setSubmitting(true);

    try {
      await listingApi.update(listing.id, {
        categoryId: Number(categoryId),
        addressId: Number(addressId),
        title: title.trim(),
        description: description.trim(),
        quantity: Number(quantity),
        unit: unit.trim(),
        expectedPrice: expectedPrice ? Number(expectedPrice) : 0,
      });

      alert('Listing updated successfully!');
      navigate(`/listings/${listing.id}`);
    } catch (err: any) {
      const resp = err.response?.data;
      setError(resp?.message || 'Failed to update listing');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUploadImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!listing || !e.target.files?.[0]) return;
    const file = e.target.files[0];
    setUploadingImage(true);

    try {
      await listingApi.uploadImage(listing.id, file);
      const refreshed = await listingApi.getById(listing.id);
      setListing(refreshed);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to upload image');
    } finally {
      setUploadingImage(false);
    }
  };

  const handleDeleteImage = async (imageId: number) => {
    if (!listing || !window.confirm('Remove this photo?')) return;
    try {
      await listingApi.deleteImage(listing.id, imageId);
      const refreshed = await listingApi.getById(listing.id);
      setListing(refreshed);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to delete image');
    }
  };

  if (loading) {
    return <div className="py-20 text-center text-sm text-gray-500">Loading listing for edit...</div>;
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">
            Edit Listing #{listing?.id}
          </h1>
          <span className="text-xs text-amber-600 bg-amber-50 px-2 py-0.5 rounded font-medium">
            Status: DRAFT
          </span>
        </div>
        <Link to={`/listings/${id}`} className="text-xs text-gray-500 hover:text-gray-900">
          Cancel
        </Link>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6 sm:p-8 shadow-sm space-y-6">
        {error && (
          <div className="bg-rose-50 border border-rose-200 text-rose-700 px-4 py-3 rounded-lg text-xs">
            {error}
          </div>
        )}

        <form onSubmit={handleUpdate} className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Waste Category</label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 bg-white outline-none focus:ring-1 focus:ring-emerald-500"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.parentName ? `${c.parentName} → ${c.name}` : c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Pickup Address</label>
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

          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Listing Title</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Quantity</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Unit</label>
              <input
                type="text"
                required
                value={unit}
                onChange={(e) => setUnit(e.target.value)}
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-1">Expected Price (₹)</label>
              <input
                type="number"
                step="0.01"
                min="0"
                value={expectedPrice}
                onChange={(e) => setExpectedPrice(e.target.value)}
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 outline-none focus:ring-1 focus:ring-emerald-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Description</label>
            <textarea
              rows={4}
              required
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full text-xs rounded-md border border-gray-300 p-3 outline-none focus:ring-1 focus:ring-emerald-500"
            />
          </div>

          <div className="pt-2 flex justify-end space-x-3">
            <button
              type="submit"
              disabled={submitting}
              className="px-5 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold shadow-sm"
            >
              {submitting ? 'Updating...' : 'Save Changes'}
            </button>
          </div>
        </form>

        {/* Image Management Section */}
        <div className="pt-6 border-t border-gray-200 space-y-4">
          <div className="flex justify-between items-center">
            <div>
              <h3 className="font-bold text-xs text-gray-900 uppercase tracking-wider">
                Listing Photos ({listing?.images?.length || 0} / 5)
              </h3>
              <p className="text-[11px] text-gray-400">JPEG, PNG, or WEBP (Max 5MB per photo)</p>
            </div>
            {listing && listing.images.length < 5 && (
              <label className="cursor-pointer px-3 py-1.5 bg-emerald-50 text-emerald-700 rounded-md text-xs font-semibold hover:bg-emerald-100">
                {uploadingImage ? 'Uploading...' : '+ Add Photo'}
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  disabled={uploadingImage}
                  onChange={handleUploadImage}
                  className="hidden"
                />
              </label>
            )}
          </div>

          {listing?.images && listing.images.length > 0 ? (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {listing.images.map((img) => (
                <div key={img.id} className="relative rounded-lg border border-gray-200 overflow-hidden group">
                  <img src={img.fileUrl} alt="" className="w-full h-24 object-cover" />
                  <button
                    type="button"
                    onClick={() => handleDeleteImage(img.id)}
                    className="absolute top-1 right-1 bg-red-600 text-white rounded-full p-1 opacity-80 hover:opacity-100 transition shadow"
                    title="Delete Image"
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div className="p-4 bg-gray-50 rounded-lg text-center text-xs text-gray-400">
              No photos attached yet.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
