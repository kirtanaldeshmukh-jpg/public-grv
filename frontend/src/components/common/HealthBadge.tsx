import React from 'react';
import { AlertTriangle, CheckCircle, ShieldAlert } from 'lucide-react';

interface HealthBadgeProps {
  score?: number | null;
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
  className?: string;
}

export const HealthBadge: React.FC<HealthBadgeProps> = ({
  score = 100,
  size = 'md',
  showLabel = true,
  className = '',
}) => {
  const safeScore = Math.max(0, Math.min(100, score ?? 100));

  let colorConfig = {
    bg: 'bg-emerald-50',
    border: 'border-emerald-200',
    text: 'text-emerald-700',
    icon: CheckCircle,
    label: 'Healthy',
  };

  if (safeScore < 50) {
    colorConfig = {
      bg: 'bg-red-50',
      border: 'border-red-200',
      text: 'text-red-700 font-bold',
      icon: ShieldAlert,
      label: 'Critical Risk',
    };
  } else if (safeScore < 80) {
    colorConfig = {
      bg: 'bg-amber-50',
      border: 'border-amber-200',
      text: 'text-amber-800 font-semibold',
      icon: AlertTriangle,
      label: 'At Risk',
    };
  }

  const Icon = colorConfig.icon;

  const sizeClasses = {
    sm: 'px-1.5 py-0.5 text-[10px] gap-1',
    md: 'px-2.5 py-1 text-xs gap-1.5',
    lg: 'px-3 py-1.5 text-sm gap-2',
  }[size];

  const iconSizes = {
    sm: 'w-3 h-3',
    md: 'w-3.5 h-3.5',
    lg: 'w-4 h-4',
  }[size];

  return (
    <div
      className={`inline-flex items-center rounded-lg border shadow-xs ${colorConfig.bg} ${colorConfig.border} ${colorConfig.text} ${sizeClasses} ${className}`}
      title={`SAKALA Health Score: ${safeScore}/100 - ${colorConfig.label}`}
    >
      <Icon className={`shrink-0 ${iconSizes}`} />
      <span className="font-semibold">{safeScore}</span>
      {showLabel && (
        <span className="opacity-80 text-[11px] hidden sm:inline">
          /100
        </span>
      )}
    </div>
  );
};

