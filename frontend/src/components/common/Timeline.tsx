import React from 'react';
import { StatusPill } from './StatusPill';
import { UserCheck, Clock, Lock, Globe } from 'lucide-react';

export interface TimelineEvent {
  id: string | number;
  action: string;
  fromStatus?: string | null;
  toStatus?: string | null;
  actorName?: string;
  actorRole?: string;
  remarks?: string;
  details?: string;
  timestamp?: string;
  createdAt?: string;
  isInternal?: boolean;
  isPublic?: boolean;
}

interface TimelineProps {
  events?: TimelineEvent[];
  items?: any[];
  isCitizenView?: boolean;
  className?: string;
}

export const Timeline: React.FC<TimelineProps> = ({
  events,
  items,
  isCitizenView = false,
  className = '',
}) => {
  const rawList = events || items || [];

  if (rawList.length === 0) {
    return (
      <div className="py-6 text-center text-xs text-gray-400">
        No timeline events recorded yet.
      </div>
    );
  }

  // If citizen view, filter out internal confidential records
  const displayEvents = isCitizenView
    ? rawList.filter((e) => !e.isInternal && e.isPublic !== false)
    : rawList;

  return (
    <div className={`relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-gray-200 ${className}`}>
      {displayEvents.map((evt, idx) => {
        const timeStr = evt.timestamp || evt.createdAt || '';
        const detailStr = evt.remarks || evt.details || '';
        const isInternalNote = evt.isInternal || evt.isPublic === false;

        return (
          <div key={evt.id || idx} className="relative flex items-start gap-4">
            {/* Timeline Node Bullet */}
            <div className="absolute -left-6 mt-1 w-5 h-5 rounded-full bg-white border-2 border-primary flex items-center justify-center shadow-xs">
              <span className="w-2 h-2 rounded-full bg-primary"></span>
            </div>

            <div className="flex-1 bg-gray-50/70 border border-gray-100 rounded-xl p-4 shadow-2xs space-y-2">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-gray-900">{evt.action}</span>
                  {evt.toStatus && <StatusPill status={evt.toStatus as any} />}
                </div>

                <div className="flex items-center gap-2">
                  {isInternalNote ? (
                    <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                      <Lock className="w-3 h-3" />
                      <span>Internal Only</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-teal bg-teal/10 px-2 py-0.5 rounded-full">
                      <Globe className="w-3 h-3" />
                      <span>Public Timeline</span>
                    </span>
                  )}
                  <span className="text-[11px] text-gray-400 flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    {timeStr ? new Date(timeStr).toLocaleString() : ''}
                  </span>
                </div>
              </div>

              {detailStr && (
                <p className="text-xs text-gray-600 leading-relaxed">{detailStr}</p>
              )}

              {evt.actorName && !isCitizenView && (
                <div className="pt-2 border-t border-gray-200/60 flex items-center gap-1.5 text-[11px] text-gray-500">
                  <UserCheck className="w-3.5 h-3.5 text-primary" />
                  <span>
                    Logged by <strong className="text-gray-800">{evt.actorName}</strong>
                    {evt.actorRole && ` (${evt.actorRole})`}
                  </span>
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
