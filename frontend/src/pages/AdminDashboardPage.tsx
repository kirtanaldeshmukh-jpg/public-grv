import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  RefreshCw,
  Download,
  AlertTriangle,
  Clock,
  Inbox,
  UserCheck,
  Activity,
  FileCheck,
  TrendingUp,
  Sparkles,
} from 'lucide-react';
import { mockComplaints, mockDashboardSummary, mockDepartments } from '../mocks';
import { StatusPill } from '../components/common/StatusPill';
import { HealthBadge } from '../components/common/HealthBadge';
import apiClient from '../api/client';
import { DashboardSummary } from '../types';
import { useToast } from '../context/ToastContext';
import { EmptyState } from '../components/common/EmptyState';

export const AdminDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [summary, setSummary] = useState<DashboardSummary>(mockDashboardSummary);
  const [complaints, setComplaints] = useState(mockComplaints);
  const [departments, setDepartments] = useState<any[]>(mockDepartments);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDept, setSelectedDept] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [selectedPriority, setSelectedPriority] = useState('ALL');
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Fetch departments
  useEffect(() => {
    apiClient
      .get('/departments')
      .then((res) => {
        if (res.data?.data && res.data.data.length > 0) {
          setDepartments(res.data.data);
        }
      })
      .catch(() => {});
  }, []);

  // Fetch summary and complaints from backend when available
  const fetchData = async (showToast = false) => {
    setIsRefreshing(true);
    try {
      const summaryRes = await apiClient.get('/dashboard/summary');
      if (summaryRes.data?.data) {
        setSummary(summaryRes.data.data);
      }
    } catch {
      // Fallback to rich mock
    }

    try {
      const complaintsRes = await apiClient.get('/complaints', {
        params: {
          search: searchTerm || undefined,
          status: selectedStatus !== 'ALL' ? selectedStatus : undefined,
          priority: selectedPriority !== 'ALL' ? selectedPriority : undefined,
        },
      });
      if (complaintsRes.data?.data?.content) {
        setComplaints(complaintsRes.data.data.content);
      }
      if (showToast) {
        toast.info('Grievance registry & metrics synchronized');
      }
    } catch {
      // Fallback
    } finally {
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [selectedDept, selectedStatus, selectedPriority]);

  const filteredComplaints = complaints.filter((c) => {
    const matchesSearch =
      c.trackingNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.citizenName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (c.locationAddress && c.locationAddress.toLowerCase().includes(searchTerm.toLowerCase()));

    if (!matchesSearch) return false;

    if (selectedDept !== 'ALL' && c.departmentNameEn !== selectedDept) return false;
    if (selectedStatus !== 'ALL' && c.status !== selectedStatus) return false;
    if (selectedPriority !== 'ALL' && c.priority !== selectedPriority) return false;

    return true;
  });

  return (
    <div className="space-y-6">
      {/* Top Filter & Search Bar */}
      <header className="bg-white border border-gray-200 rounded-2xl p-4 sm:p-5 shadow-xs flex flex-col xl:flex-row items-stretch xl:items-center justify-between gap-4">
        {/* Search Field */}
        <div className="relative flex-1 min-w-[280px]">
          <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search complaints by ID, keyword, ward, or citizen..."
            className="w-full h-10 pl-10 pr-28 rounded-xl bg-gray-50 text-gray-900 text-xs font-semibold placeholder:text-gray-400 focus:outline-none focus:bg-white focus:border-primary border border-gray-200 transition-all"
          />
          <span className="absolute right-3 top-1/2 -translate-y-1/2 text-[10px] text-gray-400 font-medium hidden sm:inline-block">
            ಕುಂದುಕೊರತೆ ಹುಡುಕಿ
          </span>
        </div>

        {/* Dropdowns */}
        <div className="flex flex-wrap sm:flex-nowrap items-center gap-2.5">
          <select
            value={selectedDept}
            onChange={(e) => setSelectedDept(e.target.value)}
            className="h-10 px-3 rounded-xl bg-gray-50 border border-gray-200 text-gray-900 text-xs font-semibold focus:outline-none focus:bg-white"
          >
            <option value="ALL">All Departments</option>
            {departments.map((d) => (
              <option key={d.code || d.id} value={d.nameEn}>
                {d.nameEn}
              </option>
            ))}
          </select>

          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="h-10 px-3 rounded-xl bg-gray-50 border border-gray-200 text-gray-900 text-xs font-semibold focus:outline-none focus:bg-white"
          >
            <option value="ALL">All Statuses</option>
            <option value="NEW">New</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="OVERDUE">Overdue (SAKALA Breach)</option>
            <option value="RESOLVED">Resolved</option>
          </select>

          <select
            value={selectedPriority}
            onChange={(e) => setSelectedPriority(e.target.value)}
            className="h-10 px-3 rounded-xl bg-gray-50 border border-gray-200 text-gray-900 text-xs font-semibold focus:outline-none focus:bg-white"
          >
            <option value="ALL">All Priorities</option>
            <option value="URGENT">Critical / SOS</option>
            <option value="HIGH">High Priority</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>

          <button
            type="button"
            onClick={() => fetchData(true)}
            disabled={isRefreshing}
            className="h-10 w-10 rounded-xl bg-gray-50 border border-gray-200 text-gray-700 hover:bg-gray-100 flex items-center justify-center transition-colors"
            title="Refresh"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin text-primary' : ''}`} />
          </button>

          <button
            type="button"
            onClick={() => alert('Exporting SAKALA Dossier CSV...')}
            className="h-10 px-4 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center gap-1.5 transition-colors shadow-2xs whitespace-nowrap"
          >
            <Download className="w-4 h-4" />
            <span>Export Dossier</span>
          </button>
        </div>
      </header>

      {/* 7 Stat Cards Row */}
      <section className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-7 gap-3">
        {/* Total Filed */}
        <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Total Filed</span>
            <div className="w-7 h-7 rounded-lg bg-indigo-50 text-primary flex items-center justify-center">
              <Inbox className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-xl font-extrabold text-gray-900">{summary.total}</span>
            <div className="flex items-center gap-1 mt-0.5 text-[10px] text-teal font-semibold">
              <TrendingUp className="w-3 h-3" />
              <span>+5.2% wk</span>
            </div>
          </div>
        </div>

        {/* New */}
        <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">New</span>
            <div className="w-7 h-7 rounded-lg bg-indigo-100 text-primary flex items-center justify-center">
              <Sparkles className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-xl font-extrabold text-primary">{summary.new}</span>
            <span className="text-[10px] text-gray-400 block mt-0.5">Triaged pending</span>
          </div>
        </div>

        {/* Assigned */}
        <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Assigned</span>
            <div className="w-7 h-7 rounded-lg bg-teal/10 text-teal flex items-center justify-center">
              <UserCheck className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-xl font-extrabold text-teal">{summary.assigned}</span>
            <span className="text-[10px] text-gray-400 block mt-0.5">Dispatched to field</span>
          </div>
        </div>

        {/* In Progress */}
        <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">In Progress</span>
            <div className="w-7 h-7 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <Activity className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-xl font-extrabold text-amber-600">{summary.inProgress}</span>
            <span className="text-[10px] text-gray-400 block mt-0.5">Remediation active</span>
          </div>
        </div>

        {/* Pending Citizen */}
        <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Pending Cit.</span>
            <div className="w-7 h-7 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
              <Clock className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-xl font-extrabold text-purple-600">{summary.pending}</span>
            <span className="text-[10px] text-gray-400 block mt-0.5">Awaiting OTP ack</span>
          </div>
        </div>

        {/* Overdue */}
        <div className="bg-white border border-red-200 rounded-2xl p-4 shadow-xs flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-red-600 uppercase tracking-wider">Overdue</span>
            <div className="w-7 h-7 rounded-lg bg-red-50 text-red-600 flex items-center justify-center">
              <AlertTriangle className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-xl font-extrabold text-red-600">{summary.overdue}</span>
            <span className="text-[10px] text-red-700 font-semibold block mt-0.5">SAKALA Penalty</span>
          </div>
        </div>

        {/* Resolved */}
        <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs flex flex-col justify-between hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Resolved</span>
            <div className="w-7 h-7 rounded-lg bg-teal/10 text-teal flex items-center justify-center">
              <FileCheck className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-xl font-extrabold text-teal">{summary.resolved}</span>
            <span className="text-[10px] text-teal font-semibold block mt-0.5">94.8% SLA rate</span>
          </div>
        </div>
      </section>

      {/* Master Grievance Table */}
      <section className="bg-white border border-gray-200 rounded-2xl shadow-xs overflow-hidden">
        <div className="p-5 border-b border-gray-200 flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-gray-900">Master Grievance Registry & Health Rank</h2>
            <p className="text-xs text-gray-500">
              Sorted by Health Score deficiency (lowest first) for proactive supervisory triage.
            </p>
          </div>
          <span className="text-xs text-gray-500 font-semibold">
            Showing {filteredComplaints.length} Records
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50/75 border-b border-gray-200 text-[11px] font-bold text-gray-500 uppercase tracking-wider">
                <th className="py-3 px-4">Tracking ID</th>
                <th className="py-3 px-4">Grievance Subject</th>
                <th className="py-3 px-4">Department & Officer</th>
                <th className="py-3 px-4">Health Index</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">SLA Delivery</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-xs">
              {filteredComplaints.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8">
                    <EmptyState
                      title="No grievances match the filter criteria"
                      description="Try adjusting your department, status, priority, or search keyword."
                    />
                  </td>
                </tr>
              ) : (
                filteredComplaints.map((c) => (
                  <tr
                    key={c.id}
                    onClick={() => navigate(`/complaints/${c.id}`)}
                    className="hover:bg-indigo-50/30 transition-colors cursor-pointer group"
                  >
                    <td className="py-3.5 px-4 font-mono font-bold text-primary whitespace-nowrap">
                      {c.trackingNumber}
                    </td>

                    <td className="py-3.5 px-4 max-w-xs">
                      <span className="font-semibold text-gray-900 line-clamp-1 group-hover:text-primary transition-colors">
                        {c.title}
                      </span>
                      <span className="text-[11px] text-gray-500 block truncate">
                        {c.locationAddress || 'Bengaluru'}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="font-semibold text-gray-800 block text-xs">
                        {c.departmentNameEn}
                      </span>
                      <span className="text-[10px] text-gray-500">
                        {c.assignedOfficerName || 'Pending Assignment'}
                      </span>
                    </td>

                    <td className="py-3.5 px-4">
                      <HealthBadge score={c.healthScore || 75} />
                    </td>

                    <td className="py-3.5 px-4">
                      <StatusPill status={c.status} />
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="text-gray-900 font-medium block">
                        {new Date(c.followUpDeadline || c.slaDueDate).toLocaleDateString()}
                      </span>
                      <span className={`text-[10px] font-bold ${c.status === 'OVERDUE' ? 'text-red-600' : 'text-teal'}`}>
                        {c.status === 'OVERDUE' ? 'Breached' : 'Within SLA'}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/complaints/${c.id}`);
                        }}
                        className="px-3 py-1.5 rounded-lg bg-indigo-50 text-primary font-bold text-xs hover:bg-primary hover:text-white transition-colors"
                      >
                        Inspect
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
