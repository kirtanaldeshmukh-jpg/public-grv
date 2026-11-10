import React from 'react';
import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  LayoutDashboard,
  FileText,
  Clock,
  AlertTriangle,
  History,
  Building2,
  Settings,
  BarChart3,
  Shield,
  Search,
  PlusCircle,
  Home,
  X,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface SidebarProps {
  onClose?: () => void;
  className?: string;
}

export const Sidebar: React.FC<SidebarProps> = ({ onClose, className = '' }) => {
  const { t } = useTranslation();
  const { user, hasRole } = useAuth();

  const isAdmin = hasRole(['ADMIN', 'DATA_ENTRY']);
  const isSupervisor = hasRole('SUPERVISOR');
  const isOfficer = hasRole('OFFICER');

  // Navigation Links based on role
  const navItems = [
    // Staff & Official Command
    {
      to: isAdmin ? '/admin' : isSupervisor ? '/dashboard' : '/officer',
      labelKey: 'nav.dashboard',
      fallback: 'Dashboard',
      icon: LayoutDashboard,
      show: !!user,
    },
    {
      to: '/complaints',
      labelKey: 'nav.complaints',
      fallback: 'Complaints',
      icon: FileText,
      show: !!user,
    },
    {
      to: '/reminders',
      labelKey: 'nav.reminders',
      fallback: 'Reminder Centre',
      icon: Clock,
      show: !!user && (isAdmin || isSupervisor || isOfficer),
    },
    {
      to: '/escalations',
      labelKey: 'nav.escalations',
      fallback: 'Escalation Centre',
      icon: AlertTriangle,
      show: !!user && (isAdmin || isSupervisor || isOfficer),
    },
    {
      to: '/history',
      labelKey: 'nav.history',
      fallback: 'Action History',
      icon: History,
      show: !!user,
    },
    {
      to: '/departments',
      labelKey: 'nav.departments',
      fallback: 'Departments',
      icon: Building2,
      show: !!user && (isAdmin || isSupervisor),
    },
    {
      to: '/analytics',
      labelKey: 'nav.analytics',
      fallback: 'Analytics',
      icon: BarChart3,
      show: !!user && (isAdmin || isSupervisor),
    },
    {
      to: '/settings',
      labelKey: 'nav.settings',
      fallback: 'Settings',
      icon: Settings,
      show: !!user && isAdmin,
    },

    // Citizen / Public Links
    {
      to: '/',
      labelKey: 'nav.home',
      fallback: 'Public Portal',
      icon: Home,
      show: !user,
    },
    {
      to: '/track',
      labelKey: 'nav.track',
      fallback: 'Track Grievance',
      icon: Search,
      show: true,
    },
    {
      to: '/file',
      labelKey: 'nav.file',
      fallback: 'File Grievance',
      icon: PlusCircle,
      show: true,
    },
  ];

  return (
    <aside
      className={`w-72 bg-surface-low border-r border-gray-200/80 flex flex-col justify-between py-6 px-4 shadow-xs select-none ${className}`}
    >
      <div className="flex flex-col gap-5">
        {/* Header / Logo */}
        <div className="flex items-center justify-between px-2">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-primary-container text-white flex items-center justify-center font-bold text-lg shadow-sm">
              <Shield className="w-5 h-5 text-amber-300" />
            </div>
            <div className="flex flex-col">
              <span className="font-bold text-sm tracking-tight text-primary-container">
                ResolveTrack
              </span>
              <span className="text-[10px] text-gray-500 font-medium uppercase tracking-wider">
                Govt. of Karnataka
              </span>
            </div>
          </div>
          {onClose && (
            <button
              onClick={onClose}
              className="lg:hidden p-1.5 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-100"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Command Zone Tag */}
        {user && (
          <div className="mx-1 px-3 py-2 rounded-xl bg-white border border-gray-200/70 shadow-xs flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-teal-600 animate-pulse" />
              <span className="text-xs font-semibold text-gray-800">
                {isAdmin ? 'State Apex Command' : isSupervisor ? 'District Command' : 'Taluk Action Unit'}
              </span>
            </div>
            <span className="text-[10px] font-bold bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-full border border-indigo-100">
              Zone 04
            </span>
          </div>
        )}

        {/* Nav Links */}
        <nav className="flex flex-col gap-1 mt-1">
          {navItems
            .filter((item) => item.show)
            .map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={onClose}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-all ${
                      isActive
                        ? 'bg-primary-container text-white shadow-xs font-semibold'
                        : 'text-gray-600 hover:bg-white hover:text-gray-900'
                    }`
                  }
                >
                  <Icon className="w-4 h-4 shrink-0" />
                  <span className="truncate">{t(item.labelKey, item.fallback)}</span>
                </NavLink>
              );
            })}
        </nav>
      </div>

      {/* SLA Target & Footer */}
      <div className="flex flex-col gap-3 px-2 pt-4 border-t border-gray-200/60">
        <div className="p-3 rounded-xl bg-white border border-gray-200/70 shadow-xs flex flex-col gap-1.5">
          <div className="flex items-center justify-between text-[11px]">
            <span className="text-gray-500 font-medium">SAKALA SLA Compliance</span>
            <span className="text-teal-700 font-bold">94.8%</span>
          </div>
          <div className="w-full bg-gray-100 h-1.5 rounded-full overflow-hidden">
            <div className="bg-teal-600 h-full rounded-full w-[94.8%]" />
          </div>
        </div>

        <div className="flex items-center justify-between text-[10px] text-gray-400 px-1">
          <span>Portal v3.4.1</span>
          <span>NIC Karnataka</span>
        </div>
      </div>
    </aside>
  );
};

