import React, { useState } from 'react';
import apiClient from '../../api/client';
import { ListingDetail } from '../../types';

interface RequestPickupModalProps {
  isOpen: boolean;
  onClose: () => void;
  listing: ListingDetail;
  onSuccess?: () => void;
}

export const RequestPickupModal: React.FC<RequestPickupModalProps> = ({
  isOpen,
  onClose,
  listing,
  onSuccess,
}) => {
  const [proposedPrice, setProposedPrice] = useState<number>(listing.expectedPrice || 0);
  const [note, setNote] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const totalProposed = proposedPrice * listing.quantity;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await apiClient.post(`/listings/${listing.id}/pickup-requests`, {
        proposedPrice,
        note: note.trim(),
      });
      if (onSuccess) onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit pickup request');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
        <h3 className="text-lg font-bold text-gray-900 mb-1">Request Waste Pickup</h3>
        <p className="text-xs text-gray-500 mb-4">
          Submitting request to collector for "{listing.title}"
        </p>

        {error && (
          <div className="mb-4 p-3 bg-rose-50 text-rose-700 text-xs rounded-lg border border-rose-200">
            {error}
          </div>
        )}

        <div className="bg-slate-50 p-3 rounded-lg border border-gray-200 text-xs mb-4 space-y-1">
          <div className="flex justify-between">
            <span className="text-gray-500">Material Quantity:</span>
            <span className="font-semibold text-gray-900">{listing.quantity} {listing.unit}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Expected Price:</span>
            <span className="font-semibold text-emerald-700">₹{listing.expectedPrice} / {listing.unit}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Pickup Location:</span>
            <span className="font-medium text-gray-700">{listing.city}, {listing.state}</span>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">
              Your Proposed Price (₹ per {listing.unit})
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2.5 text-gray-500 text-xs font-semibold">₹</span>
              <input
                type="number"
                step="0.01"
                min="0"
                required
                value={proposedPrice}
                onChange={(e) => setProposedPrice(parseFloat(e.target.value) || 0)}
                className="w-full pl-7 pr-3 py-2 text-xs border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:outline-none"
              />
            </div>
            <div className="flex justify-between text-[11px] text-gray-500 mt-1">
              <span>Total estimated value:</span>
              <span className="font-bold text-gray-900">₹{totalProposed.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Note to Generator (Optional)</label>
            <textarea
              rows={3}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="e.g., Available for collection this Thursday morning with hydraulic scale."
              className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
            />
          </div>

          <div className="flex justify-end space-x-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg text-xs font-medium hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-medium shadow-sm transition-colors disabled:opacity-50"
            >
              {submitting ? 'Submitting...' : 'Send Pickup Request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
