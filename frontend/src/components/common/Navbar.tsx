import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { NotificationBell } from './NotificationBell';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const isActive = (path: string) => location.pathname === path;

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex">
            {/* Logo */}
            <Link to="/" className="flex items-center space-x-2">
              <div className="w-9 h-9 rounded-lg bg-emerald-600 flex items-center justify-center text-white font-bold text-lg shadow-sm">
                ♻
              </div>
              <div className="flex flex-col">
                <span className="font-bold text-lg text-gray-900 tracking-tight leading-tight">
                  Waste<span className="text-emerald-600">2</span>Value
                </span>
                <span className="text-[10px] text-gray-500 uppercase tracking-widest font-semibold">
                  Marketplace
                </span>
              </div>
            </Link>

            {/* Desktop Navigation Links */}
            <div className="hidden lg:ml-8 lg:flex lg:space-x-2 items-center">
              <Link
                to="/browse"
                className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                  isActive('/browse')
                    ? 'text-emerald-700 bg-emerald-50'
                    : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                }`}
              >
                Browse Marketplace
              </Link>

              {/* Generator Links */}
              {isAuthenticated && user?.role === 'GENERATOR' && (
                <>
                  <Link
                    to="/generator/dashboard"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/generator/dashboard')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Dashboard
                  </Link>
                  <Link
                    to="/generator/listings"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/generator/listings')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    My Listings
                  </Link>
                  <Link
                    to="/generator/requests"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/generator/requests')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Pickup Requests
                  </Link>
                  <Link
                    to="/generator/addresses"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/generator/addresses')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Addresses
                  </Link>
                </>
              )}

              {/* Collector Links */}
              {isAuthenticated && user?.role === 'COLLECTOR' && (
                <>
                  <Link
                    to="/collector/dashboard"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/collector/dashboard')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Collector Operations
                  </Link>
                  <Link
                    to="/collector/requests"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/collector/requests')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    My Pickups
                  </Link>
                  <Link
                    to="/collector/deliveries"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/collector/deliveries')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Deliveries
                  </Link>
                  <Link
                    to="/collector/profile"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/collector/profile')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Vehicle Profile
                  </Link>
                </>
              )}

              {/* Processor Links */}
              {isAuthenticated && user?.role === 'PROCESSOR' && (
                <>
                  <Link
                    to="/processor/dashboard"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/processor/dashboard')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Plant Dashboard
                  </Link>
                  <Link
                    to="/processor/capabilities"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/processor/capabilities')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Capabilities
                  </Link>
                  <Link
                    to="/processor/receipts"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/processor/receipts')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Material Receipts
                  </Link>
                  <Link
                    to="/processor/profile"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/processor/profile')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Facility Info
                  </Link>
                </>
              )}

              {/* Admin Links */}
              {isAuthenticated && user?.role === 'ADMIN' && (
                <>
                  <Link
                    to="/admin/dashboard"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/admin/dashboard')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Admin Hub
                  </Link>
                  <Link
                    to="/admin/verifications"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/admin/verifications')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Verifications
                  </Link>
                  <Link
                    to="/admin/categories"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/admin/categories')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Categories
                  </Link>
                  <Link
                    to="/admin/users"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/admin/users')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Users
                  </Link>
                  <Link
                    to="/admin/reports"
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                      isActive('/admin/reports')
                        ? 'text-emerald-700 bg-emerald-50'
                        : 'text-gray-700 hover:text-emerald-600 hover:bg-gray-50'
                    }`}
                  >
                    Disputes
                  </Link>
                </>
              )}
            </div>
          </div>

          {/* Right Action / Auth Buttons */}
          <div className="hidden sm:flex sm:items-center sm:space-x-3">
            {isAuthenticated && user ? (
              <div className="flex items-center space-x-3">
                {user.role === 'GENERATOR' && (
                  <Link
                    to="/generator/listings/new"
                    className="inline-flex items-center px-3 py-1.5 border border-transparent text-xs font-semibold rounded-md shadow-sm text-white bg-emerald-600 hover:bg-emerald-700 transition"
                  >
                    + New Listing
                  </Link>
                )}

                {/* Notifications Bell */}
                <NotificationBell />

                <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold bg-gray-100 text-gray-800 border border-gray-200">
                  {user.role}
                </span>

                <Link
                  to="/profile"
                  className="text-xs font-semibold text-gray-700 hover:text-emerald-600 flex items-center space-x-1"
                >
                  <span>{user.firstName}</span>
                </Link>

                <button
                  onClick={handleLogout}
                  className="px-2.5 py-1 text-xs font-medium text-gray-600 hover:text-gray-900 border border-gray-300 rounded hover:bg-gray-50 transition"
                >
                  Sign Out
                </button>
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <Link
                  to="/login"
                  className="text-xs font-semibold text-gray-700 hover:text-emerald-600 px-3 py-1.5"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="inline-flex items-center px-3.5 py-1.5 border border-transparent text-xs font-semibold rounded-md shadow-sm text-white bg-emerald-600 hover:bg-emerald-700 transition"
                >
                  Register
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};
