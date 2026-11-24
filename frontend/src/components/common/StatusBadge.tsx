import React from 'react';
import { ListingStatus } from '../../types';

interface StatusBadgeProps {
  status: ListingStatus;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs font-medium';

  switch (status) {
    case 'POSTED':
      return (
        <span className={`inline-flex items-center rounded-md bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-600/20 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 mr-1.5 rounded-full bg-emerald-500"></span>
          POSTED
        </span>
      );
    case 'DRAFT':
      return (
        <span className={`inline-flex items-center rounded-md bg-amber-50 text-amber-700 ring-1 ring-inset ring-amber-600/20 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 mr-1.5 rounded-full bg-amber-500"></span>
          DRAFT
        </span>
      );
    case 'ALLOCATED':
      return (
        <span className={`inline-flex items-center rounded-md bg-blue-50 text-blue-700 ring-1 ring-inset ring-blue-700/10 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 mr-1.5 rounded-full bg-blue-500"></span>
          ALLOCATED
        </span>
      );
    case 'COMPLETED':
      return (
        <span className={`inline-flex items-center rounded-md bg-purple-50 text-purple-700 ring-1 ring-inset ring-purple-700/10 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 mr-1.5 rounded-full bg-purple-500"></span>
          COMPLETED
        </span>
      );
    case 'CANCELLED':
      return (
        <span className={`inline-flex items-center rounded-md bg-rose-50 text-rose-700 ring-1 ring-inset ring-rose-600/10 ${sizeClasses}`}>
          <span className="w-1.5 h-1.5 mr-1.5 rounded-full bg-rose-500"></span>
          CANCELLED
        </span>
      );
    default:
      return (
        <span className={`inline-flex items-center rounded-md bg-gray-50 text-gray-700 ring-1 ring-inset ring-gray-500/10 ${sizeClasses}`}>
          {status}
        </span>
      );
  }
};
