import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Menu, AlertCircle, LogOut, User as UserIcon } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { NotificationDropdown } from './NotificationDropdown';
import { NotificationItem } from '../../types';
import apiClient from '../../api/client';

interface TopBarProps {
  onMenuToggle: () => void;
  notifications?: NotificationItem[];
}

export const TopBar: React.FC<TopBarProps> = ({ onMenuToggle }) => {
  const { i18n, t } = useTranslation();
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [notificationList, setNotificationList] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isDemoMode, setIsDemoMode] = useState<boolean>(true);

  // Poll notifications
  const fetchNotifications = useCallback(async () => {
    if (!user) return;
    try {
      const [notifRes, countRes] = await Promise.allSettled([
        apiClient.get('/notifications'),
        apiClient.get('/notifications/unread-count'),
      ]);

      if (notifRes.status === 'fulfilled' && notifRes.value.data?.data) {
        setNotificationList(notifRes.value.data.data);
      }
      if (countRes.status === 'fulfilled' && countRes.value.data?.data) {
        setUnreadCount(countRes.value.data.data.unreadCount ?? 0);
      }
    } catch {
      // Ignore polling errors
    }
  }, [user]);

  // Check demo mode setting
  useEffect(() => {
    let isMounted = true;
    apiClient
      .get('/settings')
      .then((res) => {
        if (!isMounted) return;
        const list = res.data?.data || [];
        const demoSetting = list.find((s: any) => s.settingKey === 'demo.mode');
        if (demoSetting) {
          setIsDemoMode(demoSetting.settingValue === 'true');
        }
      })
      .catch(() => {});
    return () => {
      isMounted = false;
    };
  }, []);

  // Set up polling interval: 15s in demo mode, 30s outside
  useEffect(() => {
    if (!user) {
      setNotificationList([]);
      setUnreadCount(0);
      return;
    }

    fetchNotifications();
    const intervalMs = isDemoMode ? 15000 : 30000;
    const interval = setInterval(fetchNotifications, intervalMs);

    return () => clearInterval(interval);
  }, [user, isDemoMode, fetchNotifications]);

  const handleMarkRead = async (id: number) => {
    setNotificationList((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
    setUnreadCount((c) => Math.max(0, c - 1));
    try {
      await apiClient.put(`/notifications/${id}/read`);
    } catch {
      // offline fallback
    }
  };

  const handleMarkAllRead = async () => {
    setNotificationList((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);
    try {
      await apiClient.put('/notifications/mark-all-read');
    } catch {
      // offline fallback
    }
  };

  const handleLanguageChange = (lang: string) => {
    i18n.changeLanguage(lang);
  };

  const currentLang = i18n.language?.startsWith('kn') ? 'kn' : 'en';

  return (
    <header className="h-16 bg-white/95 backdrop-blur-md border-b border-gray-200/80 px-4 sm:px-6 flex items-center justify-between gap-4 sticky top-0 z-30 shadow-xs">
      {/* Left: Mobile hamburger & Emergency SOS Banner */}
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <button
          onClick={onMenuToggle}
          className="lg:hidden p-2 rounded-xl text-gray-600 hover:bg-gray-100 hover:text-gray-900 transition-colors"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="hidden xl:flex items-center gap-2 bg-red-50 text-red-700 px-3 py-1.5 rounded-lg border border-red-200/60 max-w-xl truncate">
          <AlertCircle className="w-4 h-4 shrink-0 text-red-600 animate-pulse" />
          <span className="text-[11px] font-bold uppercase tracking-wider text-red-800 shrink-0">
            EMERGENCY SOS:
          </span>
          <span className="text-xs text-red-700 truncate">
            {t('topbar.sosMessage', '3 Taluk drinking water grievances crossed statutory SAKALA SLA window')}
          </span>
        </div>
      </div>

      {/* Right: Language toggle, Notification bell, User info */}
      <div className="flex items-center gap-2.5 sm:gap-4 shrink-0">
        {/* Language Switcher */}
        <div className="bg-gray-100 p-0.5 rounded-lg flex items-center">
          <button
            type="button"
            onClick={() => handleLanguageChange('en')}
            className={`px-2.5 py-1 rounded text-xs font-semibold transition-all ${
              currentLang === 'en'
                ? 'bg-white text-primary-container shadow-xs'
                : 'text-gray-500 hover:text-gray-900'
            }`}
          >
            English
          </button>
          <button
            type="button"
            onClick={() => handleLanguageChange('kn')}
            className={`px-2.5 py-1 rounded text-xs font-semibold transition-all ${
              currentLang === 'kn'
                ? 'bg-white text-primary-container shadow-xs'
                : 'text-gray-500 hover:text-gray-900'
            }`}
          >
            ಕನ್ನಡ
          </button>
        </div>

        {/* Notifications */}
        <NotificationDropdown
          notifications={notificationList}
          unreadCount={unreadCount}
          onMarkRead={handleMarkRead}
          onMarkAllRead={handleMarkAllRead}
        />

        <div className="h-5 w-px bg-gray-200 hidden sm:block" />

        {/* User Account or Login Button */}
        {user ? (
          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:flex flex-col">
              <span className="text-xs font-bold text-gray-900 leading-tight truncate max-w-[150px]">
                {user.fullName || user.username}
              </span>
              <span className="text-[10px] text-gray-500 font-semibold uppercase tracking-wider">
                {user.role?.replace('ROLE_', '')}
              </span>
            </div>

            <div className="w-8 h-8 rounded-full bg-primary-container text-white flex items-center justify-center font-bold text-xs ring-2 ring-indigo-200 shrink-0">
              {(user.fullName || user.username).charAt(0).toUpperCase()}
            </div>

            <button
              onClick={() => {
                logout();
                navigate('/login');
              }}
              title="Sign Out"
              className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        ) : (
          <Link
            to="/login"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary-container text-white text-xs font-semibold hover:bg-primary transition-colors shadow-xs"
          >
            <UserIcon className="w-3.5 h-3.5" />
            <span>{t('auth.login', 'Official Login')}</span>
          </Link>
        )}
      </div>
    </header>
  );
};

