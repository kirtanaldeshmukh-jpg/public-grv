import React, { useState } from 'react';
import { Bell, CheckCheck, ExternalLink } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { NotificationItem } from '../../types';

interface NotificationDropdownProps {
  notifications: NotificationItem[];
  unreadCount: number;
  onMarkRead: (id: number) => void;
  onMarkAllRead: () => void;
}

export const NotificationDropdown: React.FC<NotificationDropdownProps> = ({
  notifications,
  unreadCount,
  onMarkRead,
  onMarkAllRead,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const navigate = useNavigate();

  const handleNotificationClick = (n: NotificationItem) => {
    onMarkRead(n.id);
    if (n.trackingId) {
      setIsOpen(false);
      navigate(`/complaints/${n.trackingId}`);
    }
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-xl text-gray-600 hover:bg-gray-100 hover:text-gray-900 transition-colors cursor-pointer"
        title="Notifications"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1 right-1 min-w-[18px] h-[18px] px-1 bg-red-600 text-white rounded-full text-[10px] font-bold flex items-center justify-center animate-pulse">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <>
          <div
            className="fixed inset-0 z-40"
            onClick={() => setIsOpen(false)}
          />
          <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-2xl shadow-xl border border-gray-200 z-50 overflow-hidden flex flex-col">
            <div className="p-3.5 bg-surface-low border-b border-gray-200/80 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-xs text-gray-900">Notifications</span>
                {unreadCount > 0 && (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-100 text-red-700">
                    {unreadCount} unread
                  </span>
                )}
              </div>
              {unreadCount > 0 && (
                <button
                  onClick={onMarkAllRead}
                  className="inline-flex items-center gap-1 text-[11px] font-medium text-primary-container hover:underline"
                >
                  <CheckCheck className="w-3.5 h-3.5" />
                  Mark all read
                </button>
              )}
            </div>

            <div className="max-h-80 overflow-y-auto divide-y divide-gray-100">
              {notifications.length === 0 ? (
                <div className="p-6 text-center text-xs text-gray-400">
                  No notifications to display
                </div>
              ) : (
                notifications.map((n) => (
                  <div
                    key={n.id}
                    onClick={() => handleNotificationClick(n)}
                    className={`p-3 text-xs transition-colors hover:bg-gray-50 cursor-pointer flex flex-col gap-1 ${
                      !n.read ? 'bg-indigo-50/20' : ''
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className={`font-semibold ${!n.read ? 'text-gray-900' : 'text-gray-600'}`}>
                        {n.title}
                      </span>
                      <span className="text-[10px] text-gray-400 shrink-0">
                        {n.createdAt ? new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                      </span>
                    </div>
                    <p className="text-[11px] text-gray-500 line-clamp-2">
                      {n.message}
                    </p>
                    {n.trackingId && (
                      <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-primary-container mt-0.5">
                        <ExternalLink className="w-2.5 h-2.5" />
                        Grievance {n.trackingId}
                      </span>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
};
