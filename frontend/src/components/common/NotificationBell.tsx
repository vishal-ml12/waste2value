import React, { useState, useEffect, useRef } from 'react';
import apiClient from '../../api/client';
import { ApiResponse, NotificationItem, PageResponse } from '../../types';

export const NotificationBell: React.FC = () => {
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const fetchUnreadCount = async () => {
    try {
      const res = await apiClient.get<ApiResponse<{ unreadCount: number }>>('/notifications/unread-count');
      setUnreadCount(res.data.data.unreadCount);
    } catch {
      // ignore
    }
  };

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get<ApiResponse<PageResponse<NotificationItem>>>('/notifications?size=8');
      setNotifications(res.data.data.content);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30000); // Poll every 30s
    return () => clearInterval(interval);
  }, []);

  const handleToggle = () => {
    if (!isOpen) {
      fetchNotifications();
    }
    setIsOpen(!isOpen);
  };

  const handleMarkAsRead = async (id: number) => {
    try {
      await apiClient.patch(`/notifications/${id}/read`);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n))
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch {
      // ignore
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await apiClient.patch('/notifications/read-all');
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, readAt: new Date().toISOString() }))
      );
      setUnreadCount(0);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative inline-block" ref={dropdownRef}>
      <button
        onClick={handleToggle}
        className="p-2 text-gray-600 hover:text-emerald-600 hover:bg-gray-100 rounded-full relative transition-colors focus:outline-none"
        title="Notifications"
        aria-label="Notifications"
      >
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 bg-rose-600 text-white text-[10px] font-bold px-1.5 py-0.2 rounded-full min-w-[16px] text-center shadow-sm">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-xl shadow-xl border border-gray-200 z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-150">
          <div className="px-4 py-3 bg-gray-50 border-b border-gray-100 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span className="font-semibold text-sm text-gray-900">Notifications</span>
              {unreadCount > 0 && (
                <span className="bg-emerald-100 text-emerald-800 text-xs px-2 py-0.5 rounded-full font-medium">
                  {unreadCount} new
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllAsRead}
                className="text-xs text-emerald-600 hover:text-emerald-700 font-medium"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto divide-y divide-gray-100">
            {loading ? (
              <div className="p-6 text-center text-gray-400 text-xs">Loading updates...</div>
            ) : notifications.length === 0 ? (
              <div className="p-8 text-center text-gray-400">
                <p className="text-sm">No notifications yet</p>
                <p className="text-xs text-gray-400 mt-1">Platform events and messages will appear here</p>
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => !n.isRead && handleMarkAsRead(n.id)}
                  className={`p-3 text-xs cursor-pointer transition-colors ${
                    n.isRead ? 'bg-white hover:bg-gray-50' : 'bg-emerald-50/50 hover:bg-emerald-50'
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <span className="font-semibold text-gray-900">{n.title}</span>
                    {!n.isRead && <span className="w-2 h-2 rounded-full bg-emerald-600 shrink-0 mt-1"></span>}
                  </div>
                  <p className="text-gray-600 mt-1 line-clamp-2 leading-relaxed">{n.message}</p>
                  <span className="text-[10px] text-gray-400 mt-1 block">
                    {new Date(n.createdAt).toLocaleDateString(undefined, {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};
