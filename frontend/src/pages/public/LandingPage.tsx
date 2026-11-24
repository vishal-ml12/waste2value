import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { categoryApi, listingApi } from '../../api/client';
import { Category, ListingSummary } from '../../types';
import { StatusBadge } from '../../components/common/StatusBadge';

export const LandingPage: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [featuredListings, setFeaturedListings] = useState<ListingSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [cats, listingsRes] = await Promise.all([
          categoryApi.getFlat(true),
          listingApi.search({ size: 3 }),
        ]);
        setCategories(cats.slice(0, 8));
        setFeaturedListings(listingsRes.content);
      } catch (err) {
        console.error('Failed to load landing page data', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  return (
    <div className="space-y-16 py-8">
      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-white rounded-2xl border border-gray-200 p-8 sm:p-12 shadow-sm text-center max-w-4xl mx-auto">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-semibold mb-6">
            <span>🌿</span>
            <span>Production-Grade Circular Waste Economy</span>
          </div>

          <h1 className="text-3xl sm:text-5xl font-extrabold text-gray-900 tracking-tight leading-tight mb-4">
            Turn Industrial & Commercial Waste into <span className="text-emerald-600">Sustainable Value</span>
          </h1>

          <p className="text-base sm:text-lg text-gray-600 max-w-2xl mx-auto mb-8">
            Waste2Value connects verified <strong>Waste Generators</strong>, certified <strong>Collectors</strong>, and specialized <strong>Processors</strong> to streamline recovery, recycling, and circular value creation.
          </p>

          <div className="flex flex-col sm:flex-row justify-center items-center gap-4">
            <Link
              to="/browse"
              className="w-full sm:w-auto px-6 py-3 rounded-lg bg-emerald-600 text-white font-semibold text-sm hover:bg-emerald-700 shadow-sm transition"
            >
              Explore Waste Listings
            </Link>
            <Link
              to="/register"
              className="w-full sm:w-auto px-6 py-3 rounded-lg border border-gray-300 bg-white text-gray-700 font-semibold text-sm hover:bg-gray-50 transition"
            >
              Join as Generator or Collector
            </Link>
          </div>
        </div>
      </section>

      {/* The Circular Chain Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-10">
          <h2 className="text-2xl font-bold text-gray-900 tracking-tight">The Waste2Value Workflow</h2>
          <p className="text-sm text-gray-600 mt-1">A transparent, traceable lifecycle connecting every stage of circular recovery</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm relative">
            <div className="text-xs font-bold text-emerald-600 uppercase tracking-wider mb-2">Step 1</div>
            <div className="text-2xl mb-2">🏭</div>
            <h3 className="font-semibold text-gray-900 mb-1">Waste Generators</h3>
            <p className="text-xs text-gray-600">Enterprises catalog recyclable materials with quantity, unit specifications, and pickup location coordinates.</p>
          </div>

          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm relative">
            <div className="text-xs font-bold text-emerald-600 uppercase tracking-wider mb-2">Step 2</div>
            <div className="text-2xl mb-2">🚛</div>
            <h3 className="font-semibold text-gray-900 mb-1">Certified Collectors</h3>
            <p className="text-xs text-gray-600">Discover nearby waste lots using GPS distance filters and coordinate batch pickups.</p>
          </div>

          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm relative">
            <div className="text-xs font-bold text-emerald-600 uppercase tracking-wider mb-2">Step 3</div>
            <div className="text-2xl mb-2">⚙️</div>
            <h3 className="font-semibold text-gray-900 mb-1">Material Processors</h3>
            <p className="text-xs text-gray-600">Sort, wash, shred, and reprocess raw waste streams into standardized recycled feedstocks.</p>
          </div>

          <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm relative">
            <div className="text-xs font-bold text-emerald-600 uppercase tracking-wider mb-2">Step 4</div>
            <div className="text-2xl mb-2">🌱</div>
            <h3 className="font-semibold text-gray-900 mb-1">Sustainable Value</h3>
            <p className="text-xs text-gray-600">Closed-loop circular products re-enter manufacturing supply chains with verified audit trails.</p>
          </div>
        </div>
      </section>

      {/* Featured Categories */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-end mb-6">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 tracking-tight">Active Categories</h2>
            <p className="text-sm text-gray-600 mt-1">Explore standardized waste materials traded on the platform</p>
          </div>
          <Link to="/browse" className="text-xs font-semibold text-emerald-600 hover:text-emerald-700">
            View All →
          </Link>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {categories.map((cat) => (
            <Link
              key={cat.id}
              to={`/browse?categoryId=${cat.id}`}
              className="bg-white p-4 rounded-xl border border-gray-200 hover:border-emerald-500 hover:shadow-sm transition block"
            >
              <div className="font-semibold text-sm text-gray-900 mb-1">{cat.name}</div>
              <div className="text-xs text-gray-500 line-clamp-1">{cat.description || `Standardized ${cat.name}`}</div>
              <div className="mt-3 text-[11px] font-medium text-emerald-700 bg-emerald-50 inline-block px-2 py-0.5 rounded">
                Unit: {cat.unit}
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* Recent Listings */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-end mb-6">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 tracking-tight">Recent Available Listings</h2>
            <p className="text-sm text-gray-600 mt-1">Directly available from generators for collection</p>
          </div>
          <Link to="/browse" className="text-xs font-semibold text-emerald-600 hover:text-emerald-700">
            Browse All ({featuredListings.length}) →
          </Link>
        </div>

        {loading ? (
          <div className="py-12 text-center text-gray-500 text-sm">Loading marketplace listings...</div>
        ) : featuredListings.length === 0 ? (
          <div className="bg-white rounded-xl border border-gray-200 p-8 text-center text-sm text-gray-500">
            No active listings found. Be the first generator to list materials!
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {featuredListings.map((item) => (
              <div key={item.id} className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex flex-col">
                <div className="p-5 flex-1">
                  <div className="flex justify-between items-start mb-2">
                    <span className="text-xs font-medium text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded">
                      {item.categoryName}
                    </span>
                    <StatusBadge status={item.status} size="sm" />
                  </div>
                  <h3 className="font-bold text-gray-900 text-base mb-2 line-clamp-1">
                    {item.title}
                  </h3>
                  <div className="text-xs text-gray-500 space-y-1 mb-4">
                    <div className="flex items-center space-x-1">
                      <span>📍</span>
                      <span>{item.city}, {item.state}</span>
                    </div>
                    <div className="flex items-center space-x-1">
                      <span>⚖️</span>
                      <span>{item.quantity} {item.unit} available</span>
                    </div>
                  </div>
                  <div className="text-sm font-semibold text-gray-900 pt-2 border-t border-gray-100 flex justify-between items-center">
                    <span>Expected Price:</span>
                    <span className="text-emerald-700">₹{item.expectedPrice} / {item.unit}</span>
                  </div>
                </div>
                <div className="bg-gray-50 px-5 py-3 border-t border-gray-100 text-right">
                  <Link
                    to={`/listings/${item.id}`}
                    className="text-xs font-semibold text-emerald-600 hover:text-emerald-800"
                  >
                    View Details & Location →
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
