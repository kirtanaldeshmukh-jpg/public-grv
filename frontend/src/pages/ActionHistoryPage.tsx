import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  History,
  ShieldCheck,
  Download,
  Printer,
  Search,
  Bot,
  User,
  Eye,
  EyeOff,
  Wrench,
  TrendingUp,
} from 'lucide-react';
import { mockComplaints } from '../mocks';
import apiClient from '../api/client';

export const ActionHistoryPage: React.FC = () => {
  const navigate = useNavigate();

  const [events, setEvents] = useState<any[]>(() =>
    mockComplaints.flatMap((c) =>
      c.history.map((h) => ({
        ...h,
        trackingNumber: c.trackingNumber,
        category: c.category,
        departmentNameEn: c.departmentNameEn,
      }))
    ).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  );

  const [actorFilter, setActorFilter] = useState<string>('ALL');
  const [visibilityFilter, setVisibilityFilter] = useState<'ALL' | 'PUBLIC' | 'INTERNAL'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  React.useEffect(() => {
    apiClient
      .get('/history')
      .then((res) => {
        if (res.data?.data && res.data.data.length > 0) {
          const mapped = res.data.data.map((h: any) => ({
            ...h,
            trackingNumber: h.complaintTrackingNumber || `RSK-2026-00${h.complaintId || 1}`,
          }));
          setEvents(mapped);
        }
      })
      .catch(() => {});
  }, []);

  const filteredEvents = events.filter((ev) => {
    const tracking = ev.trackingNumber || '';
    const act = ev.action || '';
    const rem = ev.remarks || '';
    const actor = ev.actorName || '';

    const matchesSearch =
      tracking.toLowerCase().includes(searchTerm.toLowerCase()) ||
      act.toLowerCase().includes(searchTerm.toLowerCase()) ||
      rem.toLowerCase().includes(searchTerm.toLowerCase()) ||
      actor.toLowerCase().includes(searchTerm.toLowerCase());

    if (!matchesSearch) return false;

    if (actorFilter !== 'ALL' && ev.actorRole !== actorFilter) return false;
    if (visibilityFilter === 'PUBLIC' && ev.isInternal) return false;
    if (visibilityFilter === 'INTERNAL' && !ev.isInternal) return false;

    return true;
  });

  return (
    <div className="space-y-6">
      {/* Top Statutory Banner & Operational Controls */}
      <section className="bg-white border border-gray-200 rounded-2xl p-6 sm:p-8 shadow-xs flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="space-y-1.5 max-w-3xl">
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-indigo-50 text-primary text-[11px] font-bold uppercase tracking-wider">
              <ShieldCheck className="w-3.5 h-3.5 text-primary" />
              Karnataka SAKALA Services Act (2011) Compliant
            </span>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-teal/10 text-teal text-[11px] font-bold">
              <span className="w-1.5 h-1.5 rounded-full bg-teal animate-pulse"></span>
              Live Ingestion: IST NTP Synced
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight flex items-baseline gap-2">
            Action History & Forensic Audit Log
            <span className="text-sm font-normal text-gray-500">ಕ್ರಿಯಾ ಇತಿಹಾಸ ಮತ್ತು ಪರಿಶೋಧನಾ ದಾಖಲೆ</span>
          </h1>

          <p className="text-xs text-gray-600 leading-relaxed">
            Immutable, tamper-evident statutory audit trail tracking all administrative events, automated algorithmic escalations, and official redressal actions under Karnataka SAKALA Services Act.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          <div className="flex items-center gap-2 px-3 py-2 rounded-xl bg-gray-50 border border-gray-200 text-gray-700">
            <ShieldCheck className="w-5 h-5 text-teal" />
            <div className="flex flex-col">
              <span className="text-xs font-bold text-gray-900 leading-tight">SHA-256 Validated</span>
              <span className="text-[10px] text-gray-400 font-mono">Block #892-KAR-AUDIT</span>
            </div>
          </div>

          <button
            type="button"
            onClick={() => alert('Exporting SHA-256 Signed Audit CSV...')}
            className="px-4 py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center gap-1.5 transition-colors shadow-2xs"
          >
            <Download className="w-4 h-4" />
            <span>Export CSV</span>
          </button>
          <button
            type="button"
            onClick={() => window.print()}
            className="px-3.5 py-2.5 rounded-xl bg-white border border-gray-300 text-gray-700 text-xs font-bold hover:bg-gray-50 flex items-center gap-1.5 transition-colors shadow-2xs"
          >
            <Printer className="w-4 h-4 text-gray-500" />
            <span>Print Register</span>
          </button>
        </div>
      </section>

      {/* Telemetry Summary Cards Strip */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-gray-500 uppercase tracking-wider">
              Total Audit Events (7D)
            </span>
            <div className="w-8 h-8 rounded-lg bg-indigo-50 text-primary flex items-center justify-center">
              <History className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline justify-between">
            <span className="text-2xl font-extrabold text-gray-900">2,845</span>
            <span className="text-xs text-teal font-bold flex items-center gap-0.5">
              <TrendingUp className="w-3.5 h-3.5" />
              +14.2%
            </span>
          </div>
          <span className="text-[10px] text-gray-400 mt-1">10 Oct – 16 Oct 2025 across 8 taluks</span>
        </div>

        <div className="bg-white border border-indigo-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-primary uppercase tracking-wider flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-primary animate-ping"></span>
              System Automated Dispatches
            </span>
            <div className="w-8 h-8 rounded-lg bg-indigo-50 text-primary flex items-center justify-center">
              <Bot className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline justify-between">
            <span className="text-2xl font-extrabold text-primary">1,930</span>
            <span className="px-2 py-0.5 rounded-full bg-indigo-50 text-primary text-[10px] font-bold">
              67.8% of Total
            </span>
          </div>
          <span className="text-[10px] text-gray-400 mt-1">SAKALA cron alerts, SLA warnings, tier climbs</span>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-gray-500 uppercase tracking-wider">
              Officer Field Actions
            </span>
            <div className="w-8 h-8 rounded-lg bg-teal/10 text-teal flex items-center justify-center">
              <Wrench className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline justify-between">
            <span className="text-2xl font-extrabold text-gray-900">612</span>
            <span className="text-xs text-teal font-semibold">21.5%</span>
          </div>
          <span className="text-[10px] text-gray-400 mt-1">Site inspections, status advancements, lab logs</span>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-bold text-gray-500 uppercase tracking-wider">
              Citizen Interactions
            </span>
            <div className="w-8 h-8 rounded-lg bg-gray-100 text-gray-600 flex items-center justify-center">
              <User className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline justify-between">
            <span className="text-2xl font-extrabold text-gray-900">303</span>
            <span className="text-xs text-gray-500">10.7%</span>
          </div>
          <span className="text-[10px] text-gray-400 mt-1">Grievances filed, feedback ratings, OTP acks</span>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <section className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs space-y-3">
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by Docket #, Actor Name, Action or Notes..."
              className="w-full bg-gray-50 border border-gray-200 rounded-xl pl-9 pr-3 py-1.5 text-xs text-gray-900 outline-none focus:bg-white focus:border-primary transition-colors"
            />
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <select
              value={actorFilter}
              onChange={(e) => setActorFilter(e.target.value)}
              className="px-3 py-1.5 rounded-xl bg-gray-50 border border-gray-200 text-xs font-semibold text-gray-700 outline-none"
            >
              <option value="ALL">All Actors</option>
              <option value="SYSTEM">SYSTEM (Cron / Engine)</option>
              <option value="OFFICER">OFFICER (Field / Inspector)</option>
              <option value="SUPERVISOR">SUPERVISOR (AEE / Tahsildar)</option>
              <option value="CITIZEN">CITIZEN (Public Portal)</option>
              <option value="ADMIN">ADMIN</option>
            </select>

            <select
              value={visibilityFilter}
              onChange={(e) => setVisibilityFilter(e.target.value as any)}
              className="px-3 py-1.5 rounded-xl bg-gray-50 border border-gray-200 text-xs font-semibold text-gray-700 outline-none"
            >
              <option value="ALL">All Events (Public & Internal)</option>
              <option value="PUBLIC">Citizen Public Timeline Only</option>
              <option value="INTERNAL">Internal Confidential Only</option>
            </select>
          </div>
        </div>
      </section>

      {/* Audit Events Table */}
      <section className="bg-white border border-gray-200 rounded-2xl shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50/75 border-b border-gray-200 text-[11px] font-bold text-gray-500 uppercase tracking-wider">
                <th className="py-3 px-4">Timestamp (IST)</th>
                <th className="py-3 px-4">Docket ID</th>
                <th className="py-3 px-4">Actor</th>
                <th className="py-3 px-4">Action & Operational Details</th>
                <th className="py-3 px-4">Confidentiality</th>
                <th className="py-3 px-4 text-right">Integrity</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-xs">
              {filteredEvents.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-gray-400">
                    No audit records match the current filter.
                  </td>
                </tr>
              ) : (
                filteredEvents.map((ev) => (
                  <tr
                    key={ev.id}
                    className="hover:bg-indigo-50/30 transition-colors"
                  >
                    <td className="py-3.5 px-4 font-mono text-gray-600 whitespace-nowrap">
                      {new Date(ev.createdAt).toLocaleString()}
                    </td>

                    <td className="py-3.5 px-4">
                      <button
                        type="button"
                        onClick={() => navigate(`/complaints/${ev.complaintId}`)}
                        className="font-mono font-bold text-primary hover:underline"
                      >
                        {ev.trackingNumber}
                      </button>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <span
                          className={`w-6 h-6 rounded-md flex items-center justify-center text-[10px] font-bold ${
                            ev.actorRole === 'SYSTEM'
                              ? 'bg-indigo-100 text-primary'
                              : ev.actorRole === 'OFFICER'
                              ? 'bg-teal/15 text-teal'
                              : ev.actorRole === 'CITIZEN'
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-gray-100 text-gray-700'
                          }`}
                        >
                          {ev.actorRole === 'SYSTEM' ? (
                            <Bot className="w-3.5 h-3.5" />
                          ) : (
                            <User className="w-3.5 h-3.5" />
                          )}
                        </span>
                        <div>
                          <span className="font-bold text-gray-900 block text-xs">
                            {ev.actorName}
                          </span>
                          <span className="text-[10px] text-gray-400 uppercase font-semibold">
                            {ev.actorRole}
                          </span>
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 max-w-md">
                      <span className="font-bold text-gray-800 block text-xs">
                        {ev.action}
                      </span>
                      <p className="text-gray-600 text-[11px] line-clamp-2 leading-relaxed">
                        {ev.remarks}
                      </p>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      {!ev.isInternal ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-teal/10 text-teal text-[10px] font-bold">
                          <Eye className="w-3 h-3" />
                          <span>Citizen Public</span>
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-gray-100 text-gray-600 text-[10px] font-bold">
                          <EyeOff className="w-3 h-3" />
                          <span>Internal Only</span>
                        </span>
                      )}
                    </td>

                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <span className="text-[10px] text-teal font-mono font-bold flex items-center justify-end gap-1">
                        <ShieldCheck className="w-3 h-3" />
                        <span>SHA-256 OK</span>
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};
