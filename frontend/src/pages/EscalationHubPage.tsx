import React from 'react';
import { useTranslation } from 'react-i18next';
import { ShieldAlert, AlertTriangle, ArrowUpRight, Scale } from 'lucide-react';

export const EscalationHubPage: React.FC = () => {
  const { t } = useTranslation();

  const mockEscalations = [
    {
      id: 101,
      trackingNumber: 'RT-2026-104410',
      title: 'Delayed survey of farmland boundaries',
      department: 'Revenue & Land Records',
      fromLevel: 1,
      toLevel: 2,
      reason: 'SAKALA Statutory Breach: 14-day SLA deadline expired. Automatically escalated to Tahsildar / Assistant Commissioner.',
      notifiedOfficial: 'Assistant Commissioner (Sub-Division)',
      escalatedAt: '2026-11-06 09:30 AM',
      acknowledged: false,
    },
    {
      id: 102,
      trackingNumber: 'RT-2026-104201',
      title: 'Unattended primary healthcare vaccine refrigerator outage',
      department: 'Health & Family Welfare',
      fromLevel: 0,
      toLevel: 1,
      reason: 'Critical SLA breach: PHC power failure response overdue by 24 hours. Escalated to Taluk Health Officer.',
      notifiedOfficial: 'Taluk Health Officer (THO)',
      escalatedAt: '2026-11-05 04:15 PM',
      acknowledged: true,
    },
  ];

  return (
    <div className="py-6 space-y-8">
      {/* Statutory Advisory */}
      <div className="bg-amber-50 border-l-4 border-amber-500 p-4 rounded-r-xl">
        <div className="flex items-start gap-3">
          <Scale className="w-5 h-5 text-amber-700 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="text-sm font-bold text-amber-900">
              Statutory Supervisory Oversight Notice
            </h3>
            <p className="text-xs text-amber-800 mt-1 leading-relaxed">
              {t('escalation.notice')}
            </p>
          </div>
        </div>
      </div>

      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <ShieldAlert className="w-6 h-6 text-red-600" />
          <span>{t('escalation.hubTitle')}</span>
        </h1>
        <p className="text-xs text-gray-500 mt-1">{t('escalation.hubSubtitle')}</p>
      </div>

      {/* Escalation Cards */}
      <div className="space-y-4">
        {mockEscalations.map((item) => (
          <div
            key={item.id}
            className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm hover:border-red-300 transition space-y-4"
          >
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <span className="px-2.5 py-1 rounded bg-red-100 text-red-800 text-xs font-bold flex items-center gap-1">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  <span>Tier {item.toLevel} Escalated</span>
                </span>
                <h3 className="text-base font-bold text-primary">{item.trackingNumber}</h3>
                <span className="text-xs text-gray-500">• {item.department}</span>
              </div>
              <span className="text-xs text-gray-500">{item.escalatedAt}</span>
            </div>

            <p className="text-sm font-semibold text-gray-900">{item.title}</p>

            <div className="bg-gray-50 p-3 rounded-lg border border-gray-200 text-xs text-gray-700">
              <span className="font-bold text-gray-900">Statutory Trigger: </span>
              {item.reason}
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 pt-2 text-xs">
              <div className="text-gray-600">
                <span>Notified Supervisory Authority: </span>
                <span className="font-bold text-gray-900">{item.notifiedOfficial}</span>
              </div>

              <div className="flex items-center gap-2">
                {!item.acknowledged ? (
                  <button className="px-3 py-1.5 rounded-lg bg-red-600 hover:bg-red-700 text-white font-semibold transition">
                    Acknowledge Supervisory Intervention
                  </button>
                ) : (
                  <span className="px-3 py-1 rounded bg-emerald-50 text-emerald-700 border border-emerald-200 font-semibold">
                    Acknowledged by Supervisor
                  </span>
                )}
                <button className="px-3 py-1.5 rounded-lg border border-gray-300 hover:bg-gray-50 text-gray-700 font-semibold flex items-center gap-1">
                  <span>View Case Dossier</span>
                  <ArrowUpRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

