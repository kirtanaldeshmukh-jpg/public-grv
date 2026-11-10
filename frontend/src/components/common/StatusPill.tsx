import React from 'react';
import { useTranslation } from 'react-i18next';
import { ComplaintStatus } from '../../types';

interface StatusPillProps {
  status: ComplaintStatus | string;
  size?: 'sm' | 'md';
  showDot?: boolean;
  className?: string;
}

export const StatusPill: React.FC<StatusPillProps> = ({
  status,
  size = 'md',
  showDot = true,
  className = '',
}) => {
  const { t } = useTranslation();
  const normalized = (status || '').toUpperCase().replace('-', '_');

  const config: Record<
    string,
    { bg: string; text: string; border: string; dot: string; labelKey: string }
  > = {
    NEW: {
      bg: 'bg-blue-50',
      text: 'text-blue-700',
      border: 'border-blue-200',
      dot: 'bg-blue-600',
      labelKey: 'status.new',
    },
    SUBMITTED: {
      bg: 'bg-blue-50',
      text: 'text-blue-700',
      border: 'border-blue-200',
      dot: 'bg-blue-600',
      labelKey: 'status.submitted',
    },
    ASSIGNED: {
      bg: 'bg-purple-50',
      text: 'text-purple-700',
      border: 'border-purple-200',
      dot: 'bg-purple-600',
      labelKey: 'status.assigned',
    },
    TRIAGED: {
      bg: 'bg-indigo-50',
      text: 'text-indigo-700',
      border: 'border-indigo-200',
      dot: 'bg-indigo-600',
      labelKey: 'status.triaged',
    },
    IN_PROGRESS: {
      bg: 'bg-teal-50',
      text: 'text-teal-800',
      border: 'border-teal-200',
      dot: 'bg-teal-600',
      labelKey: 'status.inProgress',
    },
    OVERDUE: {
      bg: 'bg-red-50',
      text: 'text-red-700 font-semibold',
      border: 'border-red-200',
      dot: 'bg-red-600 animate-pulse',
      labelKey: 'status.overdue',
    },
    ESCALATED: {
      bg: 'bg-rose-950 text-rose-100',
      text: 'text-rose-100 font-bold',
      border: 'border-rose-700',
      dot: 'bg-rose-400 animate-ping',
      labelKey: 'status.escalated',
    },
    RESOLUTION_SUBMITTED: {
      bg: 'bg-cyan-50',
      text: 'text-cyan-800 font-medium',
      border: 'border-cyan-300',
      dot: 'bg-cyan-500 animate-pulse',
      labelKey: 'status.resolutionSubmitted',
    },
    REOPENED: {
      bg: 'bg-amber-100',
      text: 'text-amber-900 font-semibold',
      border: 'border-amber-400',
      dot: 'bg-amber-600 animate-bounce',
      labelKey: 'status.reopened',
    },
    RESOLVED: {
      bg: 'bg-emerald-50',
      text: 'text-emerald-700',
      border: 'border-emerald-200',
      dot: 'bg-emerald-600',
      labelKey: 'status.resolved',
    },
    REJECTED: {
      bg: 'bg-gray-100',
      text: 'text-gray-700',
      border: 'border-gray-200',
      dot: 'bg-gray-500',
      labelKey: 'status.rejected',
    },
  };

  const style = config[normalized] || {
    bg: 'bg-gray-100',
    text: 'text-gray-700',
    border: 'border-gray-200',
    dot: 'bg-gray-400',
    labelKey: 'status.default',
  };

  const sizeClasses =
    size === 'sm'
      ? 'px-2 py-0.5 text-[11px] gap-1'
      : 'px-2.5 py-1 text-xs gap-1.5';

  const label = t(style.labelKey, normalized.replace('_', ' '));

  return (
    <span
      className={`inline-flex items-center rounded-full border font-medium ${style.bg} ${style.text} ${style.border} ${sizeClasses} ${className}`}
    >
      {showDot && (
        <span
          className={`w-1.5 h-1.5 rounded-full shrink-0 ${style.dot}`}
        />
      )}
      <span className="truncate">{label}</span>
    </span>
  );
};

