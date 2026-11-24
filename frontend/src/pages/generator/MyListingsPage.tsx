import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listingApi } from '../../api/client';
import { ListingSummary, ListingStatus, PageResponse } from '../../types';
import { StatusBadge } from '../../components/common/StatusBadge';

export const MyListingsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [pageData, setPageData] = useState<PageResponse<ListingSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  // Image upload modal state
  const [uploadModalListingId, setUploadModalListingId] = useState<number | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);

  const fetchListings = async () => {
    setLoading(true);
    try {
      const data = await listingApi.getMyListings(statusFilter || undefined, page, 10);
      setPageData(data);
    } catch (err) {
      console.error('Failed to load my listings', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchListings();
  }, [statusFilter, page]);

  const handlePublish = async (id: number) => {
    if (!window.confirm('Publish this listing to the active marketplace?')) return;
    try {
      await listingApi.publish(id);
      fetchListings();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to publish');
    }
  };

  const handleCancel = async (id: number) => {
    if (!window.confirm('Are you sure you want to cancel this listing?')) return;
    try {
      await listingApi.cancel(id);
      fetchListings();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel');
    }
  };

  const handleUploadImage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadModalListingId || !selectedFile) return;

    setUploading(true);
    try {
      await listingApi.uploadImage(uploadModalListingId, selectedFile);
      alert('Image attached successfully!');
      setUploadModalListingId(null);
      setSelectedFile(null);
      fetchListings();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to upload image');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Title & Actions */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            My Waste Listings
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Manage your draft, posted, and historical waste inventory
          </p>
        </div>
        <Link
          to="/generator/listings/new"
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
        >
          + Create New Listing
        </Link>
      </div>

      {/* Filter Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex space-x-6">
          {[
            { label: 'All Lots', value: '' },
            { label: 'Drafts', value: 'DRAFT' },
            { label: 'Posted (Active)', value: 'POSTED' },
            { label: 'Completed', value: 'COMPLETED' },
            { label: 'Cancelled', value: 'CANCELLED' },
          ].map((tab) => (
            <button
              key={tab.value}
              onClick={() => {
                setStatusFilter(tab.value);
                setPage(0);
              }}
              className={`py-3 px-1 border-b-2 font-medium text-xs transition ${
                statusFilter === tab.value
                  ? 'border-emerald-600 text-emerald-700 font-bold'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Listings Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="py-16 text-center text-sm text-gray-500">Loading your listings...</div>
        ) : !pageData || pageData.content.length === 0 ? (
          <div className="p-12 text-center">
            <div className="text-3xl mb-2">📋</div>
            <h3 className="font-bold text-gray-900 mb-1">No listings found</h3>
            <p className="text-xs text-gray-500 mb-4">
              {statusFilter
                ? `You have no listings in '${statusFilter}' status.`
                : "You haven't created any waste listings yet."}
            </p>
            <Link
              to="/generator/listings/new"
              className="inline-flex px-4 py-2 bg-emerald-600 text-white rounded-lg text-xs font-semibold hover:bg-emerald-700"
            >
              Create First Listing
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-left text-xs">
              <thead className="bg-gray-50 text-gray-500 font-medium">
                <tr>
                  <th className="px-6 py-3">Title</th>
                  <th className="px-6 py-3">Category</th>
                  <th className="px-6 py-3">Quantity</th>
                  <th className="px-6 py-3">Expected Price</th>
                  <th className="px-6 py-3">Location</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {pageData.content.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50 transition">
                    <td className="px-6 py-4 font-semibold text-gray-900">
                      <Link to={`/listings/${item.id}`} className="hover:text-emerald-600">
                        {item.title}
                      </Link>
                    </td>
                    <td className="px-6 py-4 text-gray-600">{item.categoryName}</td>
                    <td className="px-6 py-4 font-medium text-gray-900">{item.quantity} {item.unit}</td>
                    <td className="px-6 py-4 font-semibold text-emerald-700">₹{item.expectedPrice}</td>
                    <td className="px-6 py-4 text-gray-500">{item.city}, {item.state}</td>
                    <td className="px-6 py-4">
                      <StatusBadge status={item.status} size="sm" />
                    </td>
                    <td className="px-6 py-4 text-right space-x-2 font-medium">
                      <Link
                        to={`/listings/${item.id}`}
                        className="text-emerald-600 hover:text-emerald-800"
                      >
                        View
                      </Link>

                      {item.status === 'DRAFT' && (
                        <>
                          <button
                            onClick={() => handlePublish(item.id)}
                            className="text-emerald-700 hover:underline font-bold"
                          >
                            Publish
                          </button>
                          <Link
                            to={`/generator/listings/${item.id}/edit`}
                            className="text-gray-600 hover:text-gray-900"
                          >
                            Edit
                          </Link>
                          <button
                            onClick={() => setUploadModalListingId(item.id)}
                            className="text-blue-600 hover:underline"
                          >
                            + Photo
                          </button>
                          <button
                            onClick={() => handleCancel(item.id)}
                            className="text-rose-600 hover:underline"
                          >
                            Cancel
                          </button>
                        </>
                      )}

                      {item.status === 'POSTED' && (
                        <>
                          <button
                            onClick={() => setUploadModalListingId(item.id)}
                            className="text-blue-600 hover:underline"
                          >
                            + Photo
                          </button>
                          <button
                            onClick={() => handleCancel(item.id)}
                            className="text-rose-600 hover:underline"
                          >
                            Cancel
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {pageData && pageData.totalPages > 1 && (
          <div className="p-4 border-t border-gray-100 flex justify-between items-center text-xs">
            <button
              disabled={pageData.first}
              onClick={() => setPage(page - 1)}
              className="px-3 py-1.5 border border-gray-300 rounded bg-white disabled:opacity-50"
            >
              Previous
            </button>
            <span>
              Page {pageData.page + 1} of {pageData.totalPages}
            </span>
            <button
              disabled={pageData.last}
              onClick={() => setPage(page + 1)}
              className="px-3 py-1.5 border border-gray-300 rounded bg-white disabled:opacity-50"
            >
              Next
            </button>
          </div>
        )}
      </div>

      {/* Upload Image Modal */}
      {uploadModalListingId && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-sm w-full p-6 shadow-xl space-y-4">
            <h3 className="font-bold text-sm text-gray-900">Upload Image for Listing #{uploadModalListingId}</h3>
            <p className="text-xs text-gray-500">Attach photos of the recyclable waste lot. JPEG, PNG, or WEBP (Max 5MB).</p>

            <form onSubmit={handleUploadImage} className="space-y-4">
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                required
                onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
                className="w-full text-xs text-gray-500 file:mr-2 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100"
              />

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => {
                    setUploadModalListingId(null);
                    setSelectedFile(null);
                  }}
                  className="px-3 py-1.5 rounded-lg border border-gray-300 text-xs font-semibold text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={uploading || !selectedFile}
                  className="px-3 py-1.5 rounded-lg bg-emerald-600 text-white text-xs font-semibold hover:bg-emerald-700 disabled:opacity-50"
                >
                  {uploading ? 'Uploading...' : 'Upload Image'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
