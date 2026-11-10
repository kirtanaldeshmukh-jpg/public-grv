import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Clock,
  RefreshCw,
  Send,
  Search,
  ShieldAlert,
  Gavel,
  BellRing,
  Wrench,
  TrendingUp,
  FileText,
  ChevronRight,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { mockComplaints } from '../mocks';
import { StatusPill } from '../components/common/StatusPill';
import { HealthBadge } from '../components/common/HealthBadge';
import apiClient from '../api/client';
import { useToast } from '../context/ToastContext';

export const OfficerDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [activeTab, setActiveTab] = useState<'ALL' | 'OVERDUE' | 'REMINDERS' | 'IN_PROGRESS'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [complaints, setComplaints] = useState<any[]>(mockComplaints);
  const [summary, setSummary] = useState<any>(null);
  const [recommendedGroups, setRecommendedGroups] = useState<any[]>([]);

  const fetchOfficerData = async (showToast = false) => {
    try {
      const [complaintsRes, summaryRes, recRes] = await Promise.allSettled([
        apiClient.get('/complaints'),
        apiClient.get('/dashboard/summary'),
        apiClient.get('/dashboard/recommended-actions'),
      ]);

      if (complaintsRes.status === 'fulfilled' && complaintsRes.value.data?.data?.content) {
        setComplaints(complaintsRes.value.data.data.content);
      }
      if (summaryRes.status === 'fulfilled' && summaryRes.value.data?.data) {
        setSummary(summaryRes.value.data.data);
      }
      if (recRes.status === 'fulfilled' && recRes.value.data?.data) {
        const d = recRes.value.data.data;
        setRecommendedGroups(Array.isArray(d) ? d : (d.officerGroups || []));
      }
      if (showToast) {
        toast.info('Officer caseload and docket telemetry updated');
      }
    } catch {
      // Offline fallback
    }
  };

  useEffect(() => {
    fetchOfficerData();
  }, [user]);

  const filteredComplaints = complaints.filter((c) => {
    const matchesSearch =
      c.trackingNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (c.locationAddress && c.locationAddress.toLowerCase().includes(searchTerm.toLowerCase()));

    if (!matchesSearch) return false;

    if (activeTab === 'OVERDUE') return c.status === 'OVERDUE';
    if (activeTab === 'REMINDERS') return (c.escalationLevel && c.escalationLevel > 0) || c.status === 'OVERDUE';
    if (activeTab === 'IN_PROGRESS') return c.status === 'IN_PROGRESS' || c.status === 'ASSIGNED';
    return true;
  });

  const overdueCount = summary?.overdueCount ?? complaints.filter((c) => c.status === 'OVERDUE').length;
  const inProgressCount = summary?.inProgressCount ?? complaints.filter((c) => c.status === 'IN_PROGRESS' || c.status === 'ASSIGNED').length;
  const remindersCount = complaints.filter((c) => (c.escalationLevel && c.escalationLevel > 0) || c.status === 'OVERDUE').length;

  const recOverdueTotal = recommendedGroups.reduce((acc, g) => acc + (g.overdueCount || 0), 0) || overdueCount;
  const recApproachingTotal = recommendedGroups.reduce((acc, g) => acc + (g.approachingDeadlineCount || 0), 0) || remindersCount;
  const recVerificationTotal = recommendedGroups.reduce((acc, g) => acc + (g.pendingCitizenVerificationCount || 0), 0);
  const directActions = recommendedGroups.flatMap((g) => g.actions || []);

  const handleSync = () => {
    fetchOfficerData(true);
  };

  return (
    <div className="space-y-6">
      {/* Header Executive Greeting & Shift Summary */}
      <header className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-[11px] uppercase font-bold text-teal tracking-wider">
              Officer Control Console
            </span>
            <span className="w-1 h-1 rounded-full bg-gray-300"></span>
            <span className="text-xs text-gray-500">Karnataka Sakala IPGRMS</span>
          </div>

          <div className="flex flex-wrap items-baseline gap-2">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
              Good Morning, {user?.fullName || 'Er. Mahadevappa'}
            </h1>
            <span className="text-lg font-medium text-primary">/ ಶುಭೋದಯ</span>
          </div>

          <div className="flex flex-wrap items-center gap-2 pt-1">
            <span className="px-3 py-1 rounded-full bg-gray-100 text-gray-800 text-xs font-semibold">
              BWSSB Yelahanka Division (Ward 08) • Senior Inspector
            </span>
            <span className="px-3 py-1 rounded-full bg-red-50 border border-red-200 text-red-700 text-xs font-bold flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-red-500 animate-pulse"></span>
              <span>{complaints.length} Assigned • {overdueCount} SAKALA Critical</span>
            </span>
          </div>
        </div>

        {/* Quick Action Tray */}
        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={handleSync}
            className="px-4 py-2.5 rounded-xl bg-white border border-gray-300 text-gray-700 text-xs font-bold hover:bg-gray-50 flex items-center gap-2 transition-colors shadow-2xs"
          >
            <RefreshCw className="w-4 h-4 text-teal" />
            <span>Sync Field Log</span>
          </button>
          <button
            type="button"
            onClick={() => alert('Sub-Divisional Notice broadcast to all field inspectors.')}
            className="px-4 py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center gap-2 transition-colors shadow-2xs"
          >
            <Send className="w-4 h-4" />
            <span>Issue Sub-Divisional Notice</span>
          </button>
        </div>
      </header>

      {/* Highlight Banner: Today's Recommended Actions (SAKALA Mandated) */}
      <section className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs relative overflow-hidden">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 text-primary flex items-center justify-center font-bold">
              <Clock className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-gray-900">Today's Recommended Actions</h2>
                <span className="text-xs text-gray-500">/ ಇಂದಿನ ಶಿಫಾರಸು ಕ್ರಮಗಳು</span>
              </div>
              <p className="text-xs text-gray-500">
                Statutory compliance directives prioritised by Karnataka Guarantee of Services to Citizens (SAKALA) framework.
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <div className="flex items-center gap-1.5 bg-red-50 border border-red-200 text-red-700 px-3 py-1.5 rounded-xl text-xs font-bold">
              <Gavel className="w-4 h-4 text-red-600" />
              <span>{recOverdueTotal} Overdue</span>
            </div>
            {recVerificationTotal > 0 && (
              <div className="flex items-center gap-1.5 bg-cyan-50 border border-cyan-200 text-cyan-800 px-3 py-1.5 rounded-xl text-xs font-bold">
                <Clock className="w-4 h-4 text-cyan-600" />
                <span>{recVerificationTotal} Awaiting Citizen Sign-off</span>
              </div>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Card 1: Overdue */}
          <div className="bg-red-50/60 border border-red-200 rounded-xl p-4 flex flex-col justify-between">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-gray-900 flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-red-600 text-white text-[11px] font-extrabold flex items-center justify-center">1</span>
                  Resolve Overdue
                </span>
                <span className="bg-red-600 text-white text-[10px] font-extrabold px-2 py-0.5 rounded-full">
                  {recOverdueTotal} Dockets
                </span>
              </div>
              <p className="text-xs text-gray-600 leading-relaxed">
                Breaching SAKALA statutory limits; immediate nodal signoff or emergency contractor dispatch required.
              </p>
            </div>
            <div className="flex items-center justify-between pt-3 mt-3 border-t border-red-200/60">
              <span className="text-[11px] font-bold text-red-700 flex items-center gap-1">
                <ShieldAlert className="w-3.5 h-3.5" />
                Tier-3 Risk
              </span>
              <button
                type="button"
                onClick={() => setActiveTab('OVERDUE')}
                className="px-2.5 py-1 rounded-lg bg-red-600 text-white text-xs font-bold hover:bg-red-700 transition-colors flex items-center gap-1 cursor-pointer"
              >
                <span>Take Action</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Card 2: Reminders */}
          <div className="bg-amber-50/60 border border-amber-200 rounded-xl p-4 flex flex-col justify-between">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-gray-900 flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-amber-600 text-white text-[11px] font-extrabold flex items-center justify-center">2</span>
                  Approaching Deadlines
                </span>
                <span className="bg-amber-600 text-white text-[10px] font-extrabold px-2 py-0.5 rounded-full">
                  {recApproachingTotal} Approaching
                </span>
              </div>
              <p className="text-xs text-gray-600 leading-relaxed">
                Automated lead-time warnings sent by scheduler. Submit progress note or proof of completion.
              </p>
            </div>
            <div className="flex items-center justify-between pt-3 mt-3 border-t border-amber-200/60">
              <span className="text-[11px] font-bold text-amber-800 flex items-center gap-1">
                <BellRing className="w-3.5 h-3.5" />
                Lead-Time Warning
              </span>
              <button
                type="button"
                onClick={() => setActiveTab('REMINDERS')}
                className="px-2.5 py-1 rounded-lg bg-amber-600 text-white text-xs font-bold hover:bg-amber-700 transition-colors flex items-center gap-1 cursor-pointer"
              >
                <span>Acknowledge</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Card 3: Active Repairs */}
          <div className="bg-indigo-50/60 border border-indigo-200 rounded-xl p-4 flex flex-col justify-between">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-gray-900 flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-full bg-primary text-white text-[11px] font-extrabold flex items-center justify-center">3</span>
                  Active Caseload
                </span>
                <span className="bg-primary text-white text-[10px] font-extrabold px-2 py-0.5 rounded-full">
                  {inProgressCount} Active
                </span>
              </div>
              <p className="text-xs text-gray-600 leading-relaxed">
                Log on-site progress, update citizen timeline, and submit resolution proofs when completed.
              </p>
            </div>
            <div className="flex items-center justify-between pt-3 mt-3 border-t border-indigo-200/60">
              <span className="text-[11px] font-bold text-primary flex items-center gap-1">
                <Wrench className="w-3.5 h-3.5" />
                In Progress
              </span>
              <button
                type="button"
                onClick={() => setActiveTab('IN_PROGRESS')}
                className="px-2.5 py-1 rounded-lg bg-primary text-white text-xs font-bold hover:bg-primary-hover transition-colors flex items-center gap-1 cursor-pointer"
              >
                <span>Log Progress</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* Dynamic Individual Recommended Actions from NextBestAction Engine */}
        {directActions.length > 0 && (
          <div className="mt-5 pt-4 border-t border-gray-100 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-gray-900 uppercase tracking-wider flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-primary animate-ping"></span>
                Prescriptive Next Best Actions ({directActions.length})
              </span>
              <span className="text-[11px] text-gray-500">Prioritized by SLA urgency & compliance score</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
              {directActions.slice(0, 6).map((action: any, idx: number) => (
                <div
                  key={`${action.complaintId}-${idx}`}
                  className="p-3.5 rounded-xl border border-gray-200 bg-gray-50/70 hover:bg-white hover:border-primary/40 hover:shadow-xs transition-all flex flex-col justify-between"
                >
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-mono text-[11px] font-bold text-primary">
                        {action.trackingId}
                      </span>
                      <span
                        className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full ${
                          action.urgency === 'CRITICAL'
                            ? 'bg-red-100 text-red-700'
                            : action.urgency === 'HIGH'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-indigo-50 text-indigo-700'
                        }`}
                      >
                        {action.urgency}
                      </span>
                    </div>

                    <h3 className="text-xs font-bold text-gray-900 line-clamp-1">
                      {action.actionTitle || action.title}
                    </h3>
                    <p className="text-[11px] text-gray-600 line-clamp-2">
                      {action.reason}
                    </p>
                  </div>

                  <div className="pt-2.5 mt-2 border-t border-gray-100 flex items-center justify-between">
                    <span className="text-[10px] text-gray-400 font-mono">
                      Target: {action.targetRole}
                    </span>
                    <button
                      type="button"
                      onClick={() => navigate(`/complaints/${action.complaintId}`)}
                      className="px-2.5 py-1 rounded-lg bg-primary text-white text-[11px] font-bold hover:bg-primary-hover transition-colors flex items-center gap-1 cursor-pointer"
                    >
                      <span>Act Now</span>
                      <ChevronRight className="w-3 h-3" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </section>

      {/* 4 Stat Cards Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-indigo-50 text-primary flex items-center justify-center shrink-0">
            <FileText className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-gray-500 font-medium block">Total Assigned Cases</span>
            <span className="text-2xl font-extrabold text-gray-900">{complaints.length}</span>
            <span className="text-[10px] text-teal font-semibold block mt-0.5">Active under jurisdiction</span>
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-teal/10 text-teal flex items-center justify-center shrink-0">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-gray-500 font-medium block">SLA Compliance Rate</span>
            <span className="text-2xl font-extrabold text-teal">92.4%</span>
            <span className="text-[10px] text-gray-500 block mt-0.5">Target: 90% (SAKALA)</span>
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
            <BellRing className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-gray-500 font-medium block">Active Warnings / Nudges</span>
            <span className="text-2xl font-extrabold text-amber-600">{remindersCount}</span>
            <span className="text-[10px] text-amber-700 font-semibold block mt-0.5">Response required</span>
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-gray-500 font-medium block">Avg Resolution Velocity</span>
            <span className="text-2xl font-extrabold text-emerald-600">1.8 Days</span>
            <span className="text-[10px] text-emerald-700 font-semibold block mt-0.5">Benchmark: 3.0 Days</span>
          </div>
        </div>
      </div>

      {/* Action Queue Section */}
      <section className="bg-white border border-gray-200 rounded-2xl shadow-xs overflow-hidden">
        {/* Table Toolbar & Tabs */}
        <div className="p-5 border-b border-gray-200 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-1.5">
            <button
              type="button"
              onClick={() => setActiveTab('ALL')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all ${
                activeTab === 'ALL'
                  ? 'bg-primary text-white shadow-2xs'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              All Active ({complaints.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('OVERDUE')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all ${
                activeTab === 'OVERDUE'
                  ? 'bg-red-600 text-white shadow-2xs'
                  : 'bg-red-50 text-red-700 hover:bg-red-100'
              }`}
            >
              Overdue ({overdueCount})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('REMINDERS')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all ${
                activeTab === 'REMINDERS'
                  ? 'bg-amber-600 text-white shadow-2xs'
                  : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
              }`}
            >
              Nudges ({remindersCount})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('IN_PROGRESS')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all ${
                activeTab === 'IN_PROGRESS'
                  ? 'bg-indigo-600 text-white shadow-2xs'
                  : 'bg-indigo-50 text-indigo-700 hover:bg-indigo-100'
              }`}
            >
              In Progress ({inProgressCount})
            </button>
          </div>

          <div className="flex items-center gap-3">
            <div className="relative">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search by ID, title, ward..."
                className="bg-gray-50 border border-gray-200 rounded-xl pl-9 pr-3 py-1.5 text-xs text-gray-900 outline-none focus:bg-white focus:border-primary transition-colors w-60"
              />
            </div>
          </div>
        </div>

        {/* Complaints Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50/75 border-b border-gray-200 text-[11px] font-bold text-gray-500 uppercase tracking-wider">
                <th className="py-3 px-4">Tracking ID</th>
                <th className="py-3 px-4">Grievance Subject</th>
                <th className="py-3 px-4">Ward / Location</th>
                <th className="py-3 px-4">Health</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">SLA Deadline</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-xs">
              {filteredComplaints.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-gray-400">
                    No complaints match the current filter.
                  </td>
                </tr>
              ) : (
                filteredComplaints.map((c) => (
                  <tr
                    key={c.id}
                    onClick={() => navigate(`/complaints/${c.id}`)}
                    className="hover:bg-indigo-50/30 transition-colors cursor-pointer group"
                  >
                    <td className="py-3.5 px-4 font-mono font-bold text-primary">
                      {c.trackingNumber}
                    </td>
                    <td className="py-3.5 px-4 max-w-xs">
                      <span className="font-semibold text-gray-900 line-clamp-1 group-hover:text-primary transition-colors">
                        {c.title}
                      </span>
                      <span className="text-[11px] text-gray-500 block truncate">
                        {c.category}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-gray-600 truncate max-w-[180px]">
                      {c.locationAddress || 'Vidyaranyapura'}
                    </td>
                    <td className="py-3.5 px-4">
                      <HealthBadge score={c.healthScore || 75} />
                    </td>
                    <td className="py-3.5 px-4">
                      <StatusPill status={c.status} />
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="text-gray-900 font-medium block">
                        {new Date(c.followUpDeadline || c.slaDueDate).toLocaleDateString()}
                      </span>
                      <span className={`text-[10px] font-bold ${c.status === 'OVERDUE' ? 'text-red-600' : 'text-teal'}`}>
                        {c.status === 'OVERDUE' ? 'SLA Breached' : 'Within SLA'}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/complaints/${c.id}`);
                        }}
                        className="px-3 py-1.5 rounded-lg bg-indigo-50 text-primary font-bold text-xs hover:bg-primary hover:text-white transition-colors"
                      >
                        Inspect & Resolve
                      </button>
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
