import React from 'react';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: number | string;
  subtitle?: string;
  trend?: {
    value: string;
    isPositive?: boolean;
    label?: string;
  };
  icon?: LucideIcon | React.ComponentType<{ className?: string }>;
  iconBg?: string;
  iconColor?: string;
  highlight?: boolean;
  alert?: boolean;
  onClick?: () => void;
  className?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  trend,
  icon: Icon,
  iconBg = 'bg-primary-light/40',
  iconColor = 'text-primary-container',
  highlight = false,
  alert = false,
  onClick,
  className = '',
}) => {
  return (
    <div
      onClick={onClick}
      className={`rounded-xl p-4 transition-all duration-200 border ${
        alert
          ? 'bg-red-50/50 border-red-200 hover:border-red-300'
          : highlight
          ? 'bg-indigo-50/40 border-indigo-200 hover:border-indigo-300'
          : 'bg-white border-gray-200/80 hover:border-gray-300'
      } shadow-xs hover:shadow-md ${onClick ? 'cursor-pointer' : ''} ${className}`}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="text-[11px] font-semibold text-gray-500 uppercase tracking-wider truncate">
          {title}
        </span>
        {Icon && (
          <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${iconBg} ${iconColor}`}>
            <Icon className="w-4 h-4" />
          </div>
        )}
      </div>

      <div className="mt-2.5">
        <div className={`text-2xl font-bold tracking-tight ${alert ? 'text-red-700' : 'text-gray-900'}`}>
          {value}
        </div>

        {trend && (
          <div className="flex items-center gap-1.5 mt-1 text-[11px]">
            <span className={`font-semibold ${trend.isPositive ? 'text-teal-600' : 'text-rose-600'}`}>
              {trend.value}
            </span>
            {trend.label && <span className="text-gray-400">{trend.label}</span>}
          </div>
        )}

        {subtitle && !trend && (
          <p className="mt-1 text-[11px] text-gray-500 truncate">
            {subtitle}
          </p>
        )}
      </div>
    </div>
  );
};

