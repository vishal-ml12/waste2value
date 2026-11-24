import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { listingApi, categoryApi } from '../../api/client';
import { Category, ListingSummary, PageResponse } from '../../types';
import { StatusBadge } from '../../components/common/StatusBadge';

export const BrowseWastePage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const [categories, setCategories] = useState<Category[]>([]);
  const [listingsData, setListingsData] = useState<PageResponse<ListingSummary> | null>(null);
  const [loading, setLoading] = useState(true);

  // Filter form states
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [categoryId, setCategoryId] = useState(searchParams.get('categoryId') || '');
  const [city, setCity] = useState(searchParams.get('city') || '');
  const [state, setState] = useState(searchParams.get('state') || '');
  const [minPrice, setMinPrice] = useState(searchParams.get('minPrice') || '');
  const [maxPrice, setMaxPrice] = useState(searchParams.get('maxPrice') || '');
  const [minQuantity, setMinQuantity] = useState(searchParams.get('minQuantity') || '');
  const [maxQuantity, setMaxQuantity] = useState(searchParams.get('maxQuantity') || '');
  const [sortBy, setSortBy] = useState(searchParams.get('sortBy') || 'newest');
  const [page, setPage] = useState(Number(searchParams.get('page')) || 0);

  // Coordinates
  const [latitude, setLatitude] = useState<number | undefined>(
    searchParams.get('latitude') ? Number(searchParams.get('latitude')) : undefined
  );
  const [longitude, setLongitude] = useState<number | undefined>(
    searchParams.get('longitude') ? Number(searchParams.get('longitude')) : undefined
  );
  const [maxDistanceKm, setMaxDistanceKm] = useState<number | undefined>(
    searchParams.get('maxDistanceKm') ? Number(searchParams.get('maxDistanceKm')) : undefined
  );

  useEffect(() => {
    categoryApi.getFlat(true).then(setCategories).catch(console.error);
  }, []);

  const fetchListings = async () => {
    setLoading(true);
    try {
      const res = await listingApi.search({
        keyword: keyword || undefined,
        categoryId: categoryId ? Number(categoryId) : undefined,
        city: city || undefined,
        state: state || undefined,
        minPrice: minPrice ? Number(minPrice) : undefined,
        maxPrice: maxPrice ? Number(maxPrice) : undefined,
        minQuantity: minQuantity ? Number(minQuantity) : undefined,
        maxQuantity: maxQuantity ? Number(maxQuantity) : undefined,
        latitude,
        longitude,
        maxDistanceKm,
        sortBy,
        page,
        size: 9,
      });
      setListingsData(res);
    } catch (err) {
      console.error('Failed to search listings', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchListings();
  }, [page, sortBy]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchListings();
  };

  const handleClearFilters = () => {
    setKeyword('');
    setCategoryId('');
    setCity('');
    setState('');
    setMinPrice('');
    setMaxPrice('');
    setMinQuantity('');
    setMaxQuantity('');
    setLatitude(undefined);
    setLongitude(undefined);
    setMaxDistanceKm(undefined);
    setSortBy('newest');
    setPage(0);
    setSearchParams({});
    listingApi.search({ size: 9, page: 0 }).then(setListingsData);
  };

  const handleUseMyLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setLatitude(pos.coords.latitude);
          setLongitude(pos.coords.longitude);
          setMaxDistanceKm(100);
          setSortBy('distance_asc');
        },
        (err) => {
          alert('Could not retrieve current location: ' + err.message);
        }
      );
    } else {
      alert('Geolocation is not supported by your browser');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
          Marketplace Waste Listings
        </h1>
        <p className="text-sm text-gray-600 mt-1">
          Explore posted recyclable, reusable, and feedstock materials available for pickup.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Filters Sidebar */}
        <aside className="lg:col-span-1">
          <form onSubmit={handleSearchSubmit} className="bg-white rounded-xl border border-gray-200 p-5 space-y-5 shadow-sm">
            <div className="flex justify-between items-center border-b border-gray-100 pb-3">
              <h2 className="font-semibold text-gray-900 text-sm">Filter Search</h2>
              <button
                type="button"
                onClick={handleClearFilters}
                className="text-xs text-emerald-600 hover:text-emerald-700 font-medium"
              >
                Reset
              </button>
            </div>

            {/* Keyword Search */}
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Keyword</label>
              <input
                type="text"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="PET bottles, cardboard, metal..."
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 focus:ring-1 focus:ring-emerald-500 focus:border-emerald-500 outline-none"
              />
            </div>

            {/* Category */}
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Category</label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full text-xs rounded-md border border-gray-300 px-3 py-2 focus:ring-1 focus:ring-emerald-500 focus:border-emerald-500 outline-none bg-white"
              >
                <option value="">All Categories</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.parentName ? `${c.parentName} → ${c.name}` : c.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Location */}
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">City</label>
                <input
                  type="text"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  placeholder="e.g. Mumbai"
                  className="w-full text-xs rounded-md border border-gray-300 px-2 py-1.5 focus:ring-1 focus:ring-emerald-500 outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">State</label>
                <input
                  type="text"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                  placeholder="e.g. MH"
                  className="w-full text-xs rounded-md border border-gray-300 px-2 py-1.5 focus:ring-1 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            {/* Price Range */}
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Price Range (₹)</label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  placeholder="Min"
                  value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)}
                  className="w-full text-xs rounded-md border border-gray-300 px-2 py-1.5 focus:ring-1 focus:ring-emerald-500 outline-none"
                />
                <input
                  type="number"
                  placeholder="Max"
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)}
                  className="w-full text-xs rounded-md border border-gray-300 px-2 py-1.5 focus:ring-1 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            {/* Quantity Range */}
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Quantity Range</label>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  placeholder="Min"
                  value={minQuantity}
                  onChange={(e) => setMinQuantity(e.target.value)}
                  className="w-full text-xs rounded-md border border-gray-300 px-2 py-1.5 focus:ring-1 focus:ring-emerald-500 outline-none"
                />
                <input
                  type="number"
                  placeholder="Max"
                  value={maxQuantity}
                  onChange={(e) => setMaxQuantity(e.target.value)}
                  className="w-full text-xs rounded-md border border-gray-300 px-2 py-1.5 focus:ring-1 focus:ring-emerald-500 outline-none"
                />
              </div>
            </div>

            {/* Geolocation Filter */}
            <div className="pt-2 border-t border-gray-100">
              <div className="flex justify-between items-center mb-1">
                <label className="text-xs font-medium text-gray-700">Distance Search</label>
                <button
                  type="button"
                  onClick={handleUseMyLocation}
                  className="text-[11px] text-emerald-600 hover:text-emerald-700 font-semibold"
                >
                  📍 Use GPS
                </button>
              </div>
              {latitude && longitude && (
                <div className="text-[11px] text-gray-500 bg-gray-50 p-2 rounded mb-2">
                  Near: {latitude.toFixed(3)}, {longitude.toFixed(3)}
                  <input
                    type="number"
                    placeholder="Max km (e.g. 50)"
                    value={maxDistanceKm || ''}
                    onChange={(e) => setMaxDistanceKm(e.target.value ? Number(e.target.value) : undefined)}
                    className="mt-1 w-full text-xs rounded border border-gray-300 px-2 py-1"
                  />
                </div>
              )}
            </div>

            <button
              type="submit"
              className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-medium py-2 rounded-lg text-xs shadow-sm transition"
            >
              Apply Filters
            </button>
          </form>
        </aside>

        {/* Results Area */}
        <main className="lg:col-span-3">
          {/* Sorting and Count Bar */}
          <div className="bg-white rounded-xl border border-gray-200 p-4 mb-6 shadow-sm flex flex-col sm:flex-row justify-between items-center gap-3">
            <div className="text-xs text-gray-600">
              Showing <span className="font-semibold text-gray-900">{listingsData?.content.length || 0}</span> of{' '}
              <span className="font-semibold text-gray-900">{listingsData?.totalElements || 0}</span> available listings
            </div>

            <div className="flex items-center space-x-2">
              <label className="text-xs text-gray-500 font-medium">Sort By:</label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="text-xs border border-gray-300 rounded-md px-2.5 py-1.5 bg-white outline-none focus:ring-1 focus:ring-emerald-500"
              >
                <option value="newest">Newest First</option>
                <option value="price_asc">Price: Low to High</option>
                <option value="price_desc">Price: High to Low</option>
                <option value="quantity_desc">Quantity: High to Low</option>
                {latitude && longitude && <option value="distance_asc">Distance: Nearest First</option>}
              </select>
            </div>
          </div>

          {/* Listings Grid */}
          {loading ? (
            <div className="py-20 text-center text-sm text-gray-500">Searching active marketplace listings...</div>
          ) : !listingsData || listingsData.content.length === 0 ? (
            <div className="bg-white rounded-xl border border-gray-200 p-12 text-center">
              <div className="text-3xl mb-2">🔍</div>
              <h3 className="font-bold text-gray-900 mb-1">No Matching Waste Lots Found</h3>
              <p className="text-xs text-gray-500 max-w-sm mx-auto mb-4">
                Try widening your search terms, changing the category, or clearing location radius filters.
              </p>
              <button
                onClick={handleClearFilters}
                className="px-4 py-2 bg-emerald-50 text-emerald-700 text-xs font-semibold rounded-lg hover:bg-emerald-100 transition"
              >
                Reset All Filters
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
              {listingsData.content.map((listing) => (
                <div
                  key={listing.id}
                  className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex flex-col hover:border-emerald-400 transition"
                >
                  <div className="p-5 flex-1 flex flex-col">
                    <div className="flex justify-between items-start mb-2">
                      <span className="text-[11px] font-semibold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded">
                        {listing.categoryName}
                      </span>
                      <StatusBadge status={listing.status} size="sm" />
                    </div>

                    <h3 className="font-bold text-gray-900 text-base mb-2 line-clamp-2">
                      <Link to={`/listings/${listing.id}`} className="hover:text-emerald-600">
                        {listing.title}
                      </Link>
                    </h3>

                    <div className="text-xs text-gray-500 space-y-1 mt-auto pt-4 border-t border-gray-50">
                      <div className="flex justify-between">
                        <span className="text-gray-400">Location:</span>
                        <span className="font-medium text-gray-700">{listing.city}, {listing.state}</span>
                      </div>
                      {listing.distanceKm !== null && listing.distanceKm !== undefined && (
                        <div className="flex justify-between text-emerald-600 font-medium">
                          <span>Distance:</span>
                          <span>{listing.distanceKm} km away</span>
                        </div>
                      )}
                      <div className="flex justify-between">
                        <span className="text-gray-400">Quantity:</span>
                        <span className="font-semibold text-gray-900">{listing.quantity} {listing.unit}</span>
                      </div>
                      <div className="flex justify-between pt-1">
                        <span className="text-gray-400">Price:</span>
                        <span className="font-bold text-emerald-700">₹{listing.expectedPrice} / {listing.unit}</span>
                      </div>
                    </div>
                  </div>

                  <div className="bg-gray-50 px-5 py-3 border-t border-gray-100 flex justify-between items-center text-xs">
                    <span className="text-gray-500">By {listing.generatorName}</span>
                    <Link
                      to={`/listings/${listing.id}`}
                      className="font-semibold text-emerald-600 hover:text-emerald-700"
                    >
                      View Details →
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Pagination */}
          {listingsData && listingsData.totalPages > 1 && (
            <div className="mt-8 flex justify-center items-center space-x-2">
              <button
                disabled={listingsData.first}
                onClick={() => setPage(page - 1)}
                className="px-3 py-1.5 border border-gray-300 rounded-md text-xs font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Previous
              </button>
              <span className="text-xs text-gray-600 px-3">
                Page {listingsData.page + 1} of {listingsData.totalPages}
              </span>
              <button
                disabled={listingsData.last}
                onClick={() => setPage(page + 1)}
                className="px-3 py-1.5 border border-gray-300 rounded-md text-xs font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};
