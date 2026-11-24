import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export const CollectorDashboardPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-6">
      <div className="bg-white rounded-2xl border border-gray-200 p-8 shadow-sm">
        <div className="flex items-center space-x-3 mb-4">
          <span className="text-3xl">🚛</span>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Collector Portal</h1>
            <span className="text-xs font-semibold text-blue-700 bg-blue-50 px-2 py-0.5 rounded border border-blue-200">
              ROLE_COLLECTOR
            </span>
          </div>
        </div>

        <p className="text-sm text-gray-600 mb-6">
          Welcome, <span className="font-semibold text-gray-900">{user?.firstName} {user?.lastName}</span>. As a verified waste collector, you can explore generator listings in your geographic coverage area and coordinate collection routes.
        </p>

        {/* Phase 1 Features Available */}
        <div className="bg-emerald-50/60 rounded-xl p-5 border border-emerald-200 mb-6">
          <h2 className="text-xs font-bold text-emerald-900 uppercase tracking-wider mb-2">Phase 1 Active Capabilities</h2>
          <ul className="text-xs text-emerald-800 space-y-1.5 list-disc list-inside">
            <li>Search active waste listings filtered by category, city, and state</li>
            <li>Sort listings by GPS distance radius from your depot coordinates</li>
            <li>Inspect precise pickup addresses and GPS maps coordinates</li>
            <li>Review generator contact details and lot volume specifications</li>
          </ul>

          <div className="mt-4">
            <Link
              to="/browse"
              className="inline-flex px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm"
            >
              Explore Available Collection Lots →
            </Link>
          </div>
        </div>

        {/* Phase 2 Preview */}
        <div className="bg-gray-50 rounded-xl p-5 border border-gray-200">
          <h2 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Phase 2 Roadmap Preview</h2>
          <p className="text-xs text-gray-600 mb-3">
            Full collector workflow including bid submission, digital collection manifest, vehicle routing, and pickup confirmation will be activated in Phase 2.
          </p>
          <div className="flex gap-2">
            <span className="text-[11px] bg-white border border-gray-200 px-2.5 py-1 rounded text-gray-600">Route Optimization</span>
            <span className="text-[11px] bg-white border border-gray-200 px-2.5 py-1 rounded text-gray-600">Digital Manifests</span>
            <span className="text-[11px] bg-white border border-gray-200 px-2.5 py-1 rounded text-gray-600">Smart Weighing</span>
          </div>
        </div>
      </div>
    </div>
  );
};
