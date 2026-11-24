import React, { useState } from 'react';
import apiClient from '../../api/client';
import { ReportTargetType } from '../../types';

interface ReportDisputeModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetType: ReportTargetType;
  targetId: number;
  targetTitle?: string;
  onSuccess?: () => void;
}

export const ReportDisputeModal: React.FC<ReportDisputeModalProps> = ({
  isOpen,
  onClose,
  targetType,
  targetId,
  targetTitle,
  onSuccess,
}) => {
  const [reason, setReason] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reason.trim() || !description.trim()) {
      setError('Please provide a reason and detailed description');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await apiClient.post('/reports', {
        targetType,
        targetId,
        reason: reason.trim(),
        description: description.trim(),
      });
      if (onSuccess) onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit report');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl relative animate-in fade-in zoom-in-95 duration-150">
        <h3 className="text-lg font-bold text-gray-900 mb-1">Report Issue / File Dispute</h3>
        <p className="text-xs text-gray-500 mb-4">
          Reporting {targetType} #{targetId} {targetTitle ? `("${targetTitle}")` : ''} for administrative review.
        </p>

        {error && (
          <div className="mb-4 p-3 bg-rose-50 text-rose-700 text-xs rounded-lg border border-rose-200">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Reason for Report</label>
            <input
              type="text"
              required
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="e.g., Inaccurate weight, missed pickup, improper material"
              className="w-full text-xs border border-gray-300 rounded-lg p-2.5 focus:ring-2 focus:ring-emerald-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 mb-1">Detailed Explanation</label>
            <textarea
              required
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Provide clear details and facts regarding what occurred..."
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
              className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-medium shadow-sm transition-colors disabled:opacity-50"
            >
              {submitting ? 'Submitting...' : 'Submit Report'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
