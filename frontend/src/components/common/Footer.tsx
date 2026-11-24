import React from 'react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-white border-t border-gray-200 mt-auto">
      <div className="max-w-7xl mx-auto py-12 px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          <div className="col-span-1 md:col-span-2">
            <div className="flex items-center space-x-2 mb-3">
              <div className="w-8 h-8 rounded-lg bg-emerald-600 flex items-center justify-center text-white font-bold text-sm">
                ♻
              </div>
              <span className="font-bold text-lg text-gray-900 tracking-tight">
                Waste<span className="text-emerald-600">2</span>Value
              </span>
            </div>
            <p className="text-sm text-gray-600 max-w-sm mb-4">
              Building a circular economy by connecting waste generators with certified collectors and processing facilities to transform waste into sustainable value.
            </p>
            <div className="text-xs text-gray-500 font-medium space-y-1">
              <p className="flex items-center">
                <span className="text-emerald-600 font-semibold mr-1.5">Model:</span>
                Generators → Collectors → Processors → Value
              </p>
              <p>Platform Architecture: Clean Java 21 / Spring Boot 3 & React 18 Monolith MVP</p>
            </div>
          </div>

          <div>
            <h3 className="text-xs font-semibold text-gray-500 tracking-wider uppercase mb-3">
              Marketplace
            </h3>
            <ul className="space-y-2 text-sm text-gray-600">
              <li><Link to="/browse" className="hover:text-emerald-600">Browse All Listings</Link></li>
              <li><Link to="/browse?category=1" className="hover:text-emerald-600">Recycled Plastics</Link></li>
              <li><Link to="/browse?category=2" className="hover:text-emerald-600">Industrial Paper</Link></li>
              <li><Link to="/browse?category=3" className="hover:text-emerald-600">Metals & Scraps</Link></li>
              <li><Link to="/browse?category=5" className="hover:text-emerald-600">E-Waste</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-xs font-semibold text-gray-500 tracking-wider uppercase mb-3">
              Platform & Roles
            </h3>
            <ul className="space-y-2 text-sm text-gray-600">
              <li><Link to="/register" className="hover:text-emerald-600">For Waste Generators</Link></li>
              <li><Link to="/register" className="hover:text-emerald-600">For Waste Collectors</Link></li>
              <li><Link to="/register" className="hover:text-emerald-600">For Materials Processors</Link></li>
              <li><a href="/swagger-ui.html" target="_blank" rel="noreferrer" className="hover:text-emerald-600">API Documentation (Swagger)</a></li>
            </ul>
          </div>
        </div>

        <div className="border-t border-gray-100 pt-6 flex flex-col sm:flex-row justify-between items-center text-xs text-gray-500">
          <p>© {new Date().getFullYear()} Waste2Value Platform. Built for Production Portfolio.</p>
          <div className="mt-2 sm:mt-0 flex space-x-4">
            <span className="text-emerald-600 font-medium">Phase 1 Complete</span>
            <span>•</span>
            <span>MySQL 8 + Flyway</span>
            <span>•</span>
            <span>Stateless JWT</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
