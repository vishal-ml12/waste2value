import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { listingApi } from '../../api/client';
import { ListingDetail } from '../../types';
import { StatusBadge } from '../../components/common/StatusBadge';
import { useAuth } from '../../context/AuthContext';
import { RequestPickupModal } from '../../components/common/RequestPickupModal';
import { ReportDisputeModal } from '../../components/common/ReportDisputeModal';
import { SmartMatchingRecommendations } from '../../components/matching/SmartMatchingRecommendations';

export const ListingDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [listing, setListing] = useState<ListingDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeImage, setActiveImage] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [showPickupModal, setShowPickupModal] = useState(false);
  const [showReportModal, setShowReportModal] = useState(false);

  const fetchListing = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await listingApi.getById(Number(id));
      setListing(data);
      if (data.images && data.images.length > 0) {
        setActiveImage(data.images[0].fileUrl);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Listing not found or access denied');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchListing();
  }, [id]);

  const isOwner = user && listing && user.id === listing.generatorId;

  const handlePublish = async () => {
    if (!listing || !window.confirm('Publish this listing to the active marketplace?')) return;
    setActionLoading(true);
    try {
      await listingApi.publish(listing.id);
      fetchListing();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to publish listing');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!listing || !window.confirm('Are you sure you want to cancel this listing?')) return;
    setActionLoading(true);
    try {
      await listingApi.cancel(listing.id);
      fetchListing();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel listing');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-16 text-center text-sm text-gray-500">
        Loading listing details...
      </div>
    );
  }

  if (error || !listing) {
    return (
      <div className="max-w-xl mx-auto px-4 py-16 text-center">
        <div className="bg-white rounded-xl border border-gray-200 p-8 shadow-sm">
          <div className="text-3xl mb-3">⚠️</div>
          <h2 className="text-lg font-bold text-gray-900 mb-2">Listing Unavailable</h2>
          <p className="text-xs text-gray-600 mb-6">{error || 'This listing does not exist or is not available publicly.'}</p>
          <Link
            to="/browse"
            className="inline-flex px-4 py-2 rounded-lg bg-emerald-600 text-white font-medium text-xs hover:bg-emerald-700"
          >
            ← Back to Marketplace
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Breadcrumbs & Back */}
      <div className="flex justify-between items-center text-xs text-gray-500">
        <div className="flex items-center space-x-2">
          <Link to="/browse" className="hover:text-emerald-600">Marketplace</Link>
          <span>/</span>
          {listing.parentCategoryName && (
            <>
              <span>{listing.parentCategoryName}</span>
              <span>/</span>
            </>
          )}
          <span className="font-semibold text-gray-900">{listing.categoryName}</span>
        </div>
        <Link to="/browse" className="text-emerald-600 font-semibold hover:text-emerald-700">
          ← Back to Browse
        </Link>
      </div>

      {/* Main Card */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="p-6 sm:p-8">
          <div className="flex flex-col sm:flex-row justify-between items-start gap-4 pb-6 border-b border-gray-100">
            <div>
              <div className="flex items-center space-x-3 mb-2">
                <span className="text-xs font-semibold text-emerald-800 bg-emerald-50 px-2.5 py-0.5 rounded">
                  {listing.categoryName}
                </span>
                <StatusBadge status={listing.status} />
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
                {listing.title}
              </h1>
            </div>

            {/* Owner Actions */}
            {isOwner && (
              <div className="flex flex-wrap gap-2">
                {listing.status === 'DRAFT' && (
                  <>
                    <button
                      onClick={handlePublish}
                      disabled={actionLoading}
                      className="px-3.5 py-2 rounded-lg bg-emerald-600 text-white text-xs font-semibold hover:bg-emerald-700 transition"
                    >
                      Publish Listing
                    </button>
                    <Link
                      to={`/generator/listings/${listing.id}/edit`}
                      className="px-3.5 py-2 rounded-lg border border-gray-300 text-gray-700 text-xs font-semibold hover:bg-gray-50 transition"
                    >
                      Edit Listing
                    </Link>
                  </>
                )}
                {(listing.status === 'DRAFT' || listing.status === 'POSTED') && (
                  <button
                    onClick={handleCancel}
                    disabled={actionLoading}
                    className="px-3.5 py-2 rounded-lg border border-rose-200 text-rose-600 text-xs font-semibold hover:bg-rose-50 transition"
                  >
                    Cancel Listing
                  </button>
                )}
              </div>
            )}

            {/* Collector Action */}
            {!isOwner && user?.role === 'COLLECTOR' && listing.status === 'POSTED' && (
              <div className="flex gap-2">
                <button
                  onClick={() => setShowPickupModal(true)}
                  className="px-5 py-2.5 rounded-lg bg-emerald-600 text-white text-sm font-bold hover:bg-emerald-700 shadow-sm transition"
                >
                  Request Waste Pickup 🚚
                </button>
                <button
                  onClick={() => setShowReportModal(true)}
                  className="px-3 py-2 rounded-lg border border-gray-300 text-gray-600 text-xs font-medium hover:bg-gray-50"
                >
                  Report
                </button>
              </div>
            )}

            {!isOwner && (!user || user?.role !== 'COLLECTOR') && listing.status === 'POSTED' && (
              <button
                onClick={() => setShowReportModal(true)}
                className="px-3 py-2 rounded-lg border border-gray-300 text-gray-600 text-xs font-medium hover:bg-gray-50"
              >
                Report Listing
              </button>
            )}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 pt-6">
            {/* Left 2 Cols: Details & Description */}
            <div className="lg:col-span-2 space-y-6">
              {/* Image Gallery */}
              {listing.images && listing.images.length > 0 ? (
                <div className="space-y-3">
                  <div className="aspect-video w-full rounded-xl bg-gray-100 overflow-hidden border border-gray-200 flex items-center justify-center">
                    <img
                      src={activeImage || listing.images[0].fileUrl}
                      alt={listing.title}
                      className="w-full h-full object-cover"
                    />
                  </div>
                  {listing.images.length > 1 && (
                    <div className="flex space-x-2 overflow-x-auto pb-2">
                      {listing.images.map((img) => (
                        <button
                          key={img.id}
                          onClick={() => setActiveImage(img.fileUrl)}
                          className={`w-16 h-16 rounded-lg overflow-hidden border-2 flex-shrink-0 ${
                            activeImage === img.fileUrl ? 'border-emerald-500' : 'border-gray-200'
                          }`}
                        >
                          <img src={img.fileUrl} alt="" className="w-full h-full object-cover" />
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <div className="aspect-video w-full rounded-xl bg-gray-50 border border-dashed border-gray-300 flex flex-col items-center justify-center text-gray-400">
                  <span className="text-3xl mb-1">📦</span>
                  <span className="text-xs">No images provided for this listing</span>
                </div>
              )}

              {/* Description */}
              <div>
                <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider mb-2">Description & Notes</h2>
                <div className="bg-gray-50 rounded-xl p-5 text-sm text-gray-700 leading-relaxed border border-gray-100 whitespace-pre-line">
                  {listing.description}
                </div>
              </div>

              {/* Technical Specifications */}
              <div>
                <h2 className="text-sm font-bold text-gray-900 uppercase tracking-wider mb-2">Specifications</h2>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
                  <div className="bg-white border border-gray-200 rounded-lg p-3">
                    <div className="text-[11px] text-gray-500">Available Quantity</div>
                    <div className="text-base font-bold text-gray-900 mt-0.5">{listing.quantity} {listing.unit}</div>
                  </div>
                  <div className="bg-white border border-gray-200 rounded-lg p-3">
                    <div className="text-[11px] text-gray-500">Expected Unit Price</div>
                    <div className="text-base font-bold text-emerald-700 mt-0.5">₹{listing.expectedPrice} / {listing.unit}</div>
                  </div>
                  <div className="bg-white border border-gray-200 rounded-lg p-3">
                    <div className="text-[11px] text-gray-500">Total Lot Value</div>
                    <div className="text-base font-bold text-gray-900 mt-0.5">
                      ₹{(listing.quantity * listing.expectedPrice).toLocaleString()}
                    </div>
                  </div>
                  <div className="bg-white border border-gray-200 rounded-lg p-3">
                    <div className="text-[11px] text-gray-500">Posted On</div>
                    <div className="text-xs font-semibold text-gray-800 mt-1">
                      {new Date(listing.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Right Col: Location & Generator Info */}
            <div className="space-y-6">
              {/* Location Card */}
              <div className="bg-gray-50 rounded-xl border border-gray-200 p-5 space-y-3">
                <div className="flex items-center space-x-2">
                  <span className="text-base">📍</span>
                  <h3 className="font-bold text-gray-900 text-sm">Pickup Location</h3>
                </div>
                <div className="text-xs text-gray-600 space-y-1">
                  <div className="font-medium text-gray-900">{listing.addressLine1}</div>
                  {listing.addressLine2 && <div>{listing.addressLine2}</div>}
                  <div>{listing.city}, {listing.state} — {listing.postalCode}</div>
                </div>

                <div className="pt-3 border-t border-gray-200 text-xs">
                  <div className="text-gray-500 mb-1">GPS Coordinates:</div>
                  <code className="bg-white px-2 py-1 rounded border border-gray-200 text-gray-800 block text-center font-mono">
                    {listing.latitude.toFixed(5)}, {listing.longitude.toFixed(5)}
                  </code>
                  <a
                    href={`https://www.google.com/maps?q=${listing.latitude},${listing.longitude}`}
                    target="_blank"
                    rel="noreferrer"
                    className="block text-center mt-2 text-emerald-600 font-semibold hover:text-emerald-700"
                  >
                    Open in Maps ↗
                  </a>
                </div>
              </div>

              {/* Generator Profile Card */}
              <div className="bg-gray-50 rounded-xl border border-gray-200 p-5 space-y-3">
                <div className="flex items-center space-x-2">
                  <span className="text-base">🏢</span>
                  <h3 className="font-bold text-gray-900 text-sm">Waste Generator</h3>
                </div>
                <div className="text-xs text-gray-700 space-y-1">
                  <div className="font-bold text-gray-900 text-sm">{listing.generatorName}</div>
                  <div className="text-gray-500">Verified Platform Waste Generator</div>
                  {listing.generatorPhone && (
                    <div className="pt-2 text-emerald-700 font-medium">
                      📞 {listing.generatorPhone}
                    </div>
                  )}
                </div>
              </div>

              {/* System Audit Card */}
              <div className="bg-white rounded-xl border border-gray-200 p-4 text-[11px] text-gray-400 space-y-1">
                <div>Listing ID: #{listing.id}</div>
                <div>Optimistic Lock Version: {listing.version}</div>
                <div>Last Updated: {new Date(listing.updatedAt).toLocaleString()}</div>
              </div>
            </div>
          </div>

          {/* Smart Matching Recommendations (Phase 3A) */}
          <div className="pt-6">
            <SmartMatchingRecommendations
              listingId={listing.id}
              listingStatus={listing.status}
              isOwner={Boolean(isOwner)}
            />
          </div>
        </div>
      </div>

      {showPickupModal && (
        <RequestPickupModal
          isOpen={true}
          onClose={() => setShowPickupModal(false)}
          listing={listing}
          onSuccess={() => {
            alert('Pickup request submitted successfully to generator!');
            navigate('/collector/requests');
          }}
        />
      )}

      {showReportModal && (
        <ReportDisputeModal
          isOpen={true}
          onClose={() => setShowReportModal(false)}
          targetType="LISTING"
          targetId={listing.id}
          targetTitle={listing.title}
          onSuccess={() => alert('Report submitted successfully to administrators')}
        />
      )}
    </div>
  );
};
