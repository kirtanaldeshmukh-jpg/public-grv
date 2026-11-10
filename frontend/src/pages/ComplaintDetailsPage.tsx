import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Printer,
  AlertTriangle,
  UserCheck,
  MapPin,
  ShieldCheck,
  Send,
  Bot,
  Paperclip,
  Download,
  X,
  CheckCircle,
  Users,
  RotateCcw,
  FileCheck,
} from 'lucide-react';
import { mockComplaints } from '../mocks';
import { StatusPill } from '../components/common/StatusPill';
import { HealthBadge } from '../components/common/HealthBadge';
import { Timeline } from '../components/common/Timeline';
import { GrievanceMap } from '../components/common/GrievanceMap';
import apiClient from '../api/client';
import { useToast } from '../context/ToastContext';
import { useTranslation } from 'react-i18next';

export const ComplaintDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { toast } = useToast();
  const { t, i18n } = useTranslation();

  const initialComplaint =
    mockComplaints.find((c) => c.id.toString() === id || c.trackingNumber === id) ||
    mockComplaints[0];

  const [complaint, setComplaint] = useState<any>(initialComplaint);
  const [historyItems, setHistoryItems] = useState<any[]>(initialComplaint.history || []);
  const [officers, setOfficers] = useState<any[]>([]);

  // Officer Update Form State
  const [updateStatus, setUpdateStatus] = useState<string>('IN_PROGRESS');
  const [progressNote, setProgressNote] = useState(
    'Desilting unit deployed at 4th cross junction. Main distribution valve SV-08 isolated for inspection. Pipe joint replacement underway.'
  );
  const [isPublic, setIsPublic] = useState(true);
  const [nextFollowUpDate, setNextFollowUpDate] = useState('2026-11-10T10:00');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Assign Officer Modal State
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [selectedOfficerId, setSelectedOfficerId] = useState<string>('');
  const [assignReason, setAssignReason] = useState('Assigned for immediate field inspection and redressal');
  const [isAssigning, setIsAssigning] = useState(false);

  // Submit Resolution Modal State (Prompt 7.3)
  const [showResolveModal, setShowResolveModal] = useState(false);
  const [resolutionSummary, setResolutionSummary] = useState('');
  const [resolutionFile, setResolutionFile] = useState<File | null>(null);
  const [isResolving, setIsResolving] = useState(false);

  const fetchComplaintData = useCallback(async () => {
    if (!id) return;
    try {
      const res = await apiClient.get(`/complaints/${id}`);
      if (res.data?.data) {
        setComplaint(res.data.data);
      }
      const histRes = await apiClient.get(`/complaints/${id}/history`);
      if (histRes.data?.data) {
        setHistoryItems(histRes.data.data);
      }
    } catch {
      // offline or mock fallback
    }
  }, [id]);

  useEffect(() => {
    fetchComplaintData();
    apiClient
      .get('/officers')
      .then((res) => {
        if (res.data?.data) {
          setOfficers(res.data.data);
          if (res.data.data.length > 0) {
            setSelectedOfficerId(String(res.data.data[0].id));
          }
        }
      })
      .catch(() => {});
  }, [fetchComplaintData]);

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!progressNote.trim()) {
      toast.warning('Please enter an action/progress remark.');
      return;
    }
    setIsSubmitting(true);

    try {
      await apiClient.post(`/complaints/${complaint.id}/updates`, {
        status: updateStatus,
        progressNote,
        isPublic,
        nextFollowUpDate: nextFollowUpDate || undefined,
      });
      toast.success('Docket updated, deadline reset & health score recalculated!');
      fetchComplaintData();
    } catch (err: any) {
      // Mock update fallback
      const newEvent = {
        id: Date.now(),
        complaintId: complaint.id,
        actorId: null,
        actorRole: 'OFFICER',
        actorName: 'Er. Mahadevappa (Assigned Officer)',
        action: `STATUS_CHANGE_TO_${updateStatus}`,
        fromStatus: complaint.status,
        toStatus: updateStatus,
        remarks: progressNote,
        isInternal: !isPublic,
        createdAt: new Date().toISOString(),
      };
      setHistoryItems([newEvent, ...historyItems]);
      setComplaint((prev: any) => ({ ...prev, status: updateStatus }));
      toast.success('Docket updated locally!');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedOfficerId) {
      toast.warning('Please select an officer.');
      return;
    }
    setIsAssigning(true);
    try {
      await apiClient.put(`/complaints/${complaint.id}/assign`, {
        officerId: Number(selectedOfficerId),
        reassignReason: assignReason,
      });
      toast.success('Complaint successfully assigned to officer!');
      setShowAssignModal(false);
      fetchComplaintData();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to assign officer.');
    } finally {
      setIsAssigning(false);
    }
  };

  const handleConfirmAiTriage = async () => {
    try {
      await apiClient.post(`/complaints/${complaint.id}/confirm-ai-triage`, {
        confirmedDepartmentId: complaint.departmentId || 1,
        confirmedPriority: complaint.priority || 'HIGH',
      });
      toast.success('AI recommendation confirmed by official!');
      fetchComplaintData();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to confirm AI recommendation.');
    }
  };

  const handleSubmitResolution = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resolutionSummary.trim()) {
      toast.warning('Please enter a resolution summary description.');
      return;
    }
    setIsResolving(true);
    try {
      const formData = new FormData();
      formData.append('summary', resolutionSummary);
      if (resolutionFile) {
        formData.append('files', resolutionFile);
      }

      await apiClient.post(`/complaints/${complaint.id}/resolve`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });

      toast.success('Resolution submitted successfully! Citizen notification dispatched for verification.');
      setShowResolveModal(false);
      fetchComplaintData();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to submit resolution.');
    } finally {
      setIsResolving(false);
    }
  };

  const deadlineStr = complaint.followUpDeadline || complaint.slaDueDate;
  const isEscalated = complaint.escalationLevel > 0;

  return (
    <div className="space-y-6">
      {/* Top Breadcrumb & Statutory Assurance */}
      <div className="flex flex-wrap items-center justify-between gap-3 text-xs text-gray-500">
        <div className="flex items-center gap-2">
          <Link to="/officer" className="hover:text-primary flex items-center gap-1 font-semibold">
            <ArrowLeft className="w-4 h-4" />
            <span>Complaints Roster</span>
          </Link>
          <span>/</span>
          <span className="text-gray-900 font-bold font-mono">{complaint.trackingNumber}</span>
          <span>/</span>
          <span className="text-primary font-medium">{complaint.departmentNameEn}</span>
        </div>

        <div className="flex items-center gap-2 text-teal font-semibold">
          <ShieldCheck className="w-4 h-4" />
          <span>SAKALA Legally Binding Redressal SLA</span>
        </div>
      </div>

      {/* Header & Command Actions */}
      <div className="flex flex-col xl:flex-row xl:items-start justify-between gap-4">
        <div className="space-y-2 max-w-4xl">
          <div className="flex flex-wrap items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full bg-red-600 text-white text-[11px] font-extrabold flex items-center gap-1.5 shadow-2xs">
              <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse"></span>
              {complaint.priority} Priority
            </span>
            <StatusPill status={complaint.status} />
            <span className="bg-gray-100 text-gray-700 text-xs px-2.5 py-0.5 rounded font-mono font-bold">
              ID: {complaint.trackingNumber}
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight leading-snug">
            {complaint.title}
          </h1>
          <p className="text-xs text-gray-500 font-medium">
            Filed under Sakala Services Act 2011 • Ward 08, Bengaluru Urban District Command
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 self-start xl:self-center">
          <button
            type="button"
            onClick={() => window.print()}
            className="px-3.5 py-2 rounded-xl bg-white border border-gray-300 text-gray-700 text-xs font-bold hover:bg-gray-50 flex items-center gap-1.5 transition-colors shadow-2xs"
          >
            <Printer className="w-4 h-4 text-gray-500" />
            <span>Print Dossier</span>
          </button>
          <button
            type="button"
            onClick={() => alert('Section 14 Statutory Notice prepared.')}
            className="px-3.5 py-2 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-bold hover:bg-red-100 flex items-center gap-1.5 transition-colors"
          >
            <AlertTriangle className="w-4 h-4 text-red-600" />
            <span>Issue Sec 14 Notice</span>
          </button>
          <button
            type="button"
            onClick={() => navigate('/departments')}
            className="px-4 py-2 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center gap-1.5 transition-colors shadow-2xs"
          >
            <UserCheck className="w-4 h-4" />
            <span>Reassign Officer</span>
          </button>
          {complaint.status !== 'RESOLVED' && complaint.status !== 'RESOLUTION_SUBMITTED' && (
            <button
              type="button"
              onClick={() => setShowResolveModal(true)}
              className="px-4 py-2 rounded-xl bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 flex items-center gap-1.5 transition-colors shadow-2xs cursor-pointer"
            >
              <CheckCircle className="w-4 h-4" />
              <span>Submit Resolution</span>
            </button>
          )}
        </div>
      </div>

      {/* Prompt 7.3: Citizen Resolution Review Alert Banner */}
      {complaint.status === 'RESOLUTION_SUBMITTED' && (
        <div className="bg-cyan-50 border-2 border-cyan-300 rounded-2xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-cyan-600 text-white flex items-center justify-center shrink-0">
              <FileCheck className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-sm font-bold text-cyan-950">Resolution Submitted — Awaiting Citizen Verification</h3>
                <span className="text-[10px] bg-cyan-200 text-cyan-800 font-bold px-2 py-0.5 rounded-full">CITIZEN SIGN-OFF REQUIRED</span>
              </div>
              <p className="text-xs text-cyan-800 mt-1">
                <strong>Officer Summary:</strong> {complaint.resolutionSummary || 'Resolution evidence submitted for field review.'}
              </p>
              {complaint.resolutionSubmittedAt && (
                <span className="text-[11px] text-cyan-600 block mt-0.5 font-mono">
                  Submitted: {new Date(complaint.resolutionSubmittedAt).toLocaleString()}
                </span>
              )}
            </div>
          </div>
          <div className="text-xs text-cyan-900 bg-cyan-100/80 px-3 py-2 rounded-xl font-medium shrink-0">
            Citizen can confirm or reopen via tracking ID
          </div>
        </div>
      )}

      {/* Prompt 7.3: Reopened Grievance Alert */}
      {complaint.status === 'REOPENED' && (
        <div className="bg-amber-50 border-2 border-amber-300 rounded-2xl p-5 flex items-start gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-600 text-white flex items-center justify-center shrink-0">
            <RotateCcw className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-amber-950">Grievance Reopened by Citizen</h3>
              <span className="text-[10px] bg-amber-200 text-amber-900 font-bold px-2 py-0.5 rounded-full">
                Reopened {complaint.reopenCount || 1} Time(s)
              </span>
            </div>
            <p className="text-xs text-amber-800 mt-1">
              The citizen inspected the field redressal and reported the issue remains unrectified. Supervisor has received escalated notification. Priority field reinspection is mandatory.
            </p>
          </div>
        </div>
      )}

      {/* Health Diagnostic & Penalty Risk Banner */}
      <section className="bg-white border border-red-200 rounded-2xl p-6 shadow-xs relative overflow-hidden">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
          {/* Health Score Gauge */}
          <div className="lg:col-span-3 flex flex-col items-center justify-center p-4 bg-gray-50 rounded-xl text-center border border-gray-200">
            <HealthBadge score={complaint.healthScore || 75} size="lg" />
            <div className="mt-2 text-xs font-bold text-gray-900 flex items-center justify-center gap-1.5">
              <span>Health Index</span>
              {complaint.healthScoreDetail && (
                <span
                  className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full ${
                    complaint.healthScoreDetail.band === 'HEALTHY'
                      ? 'bg-emerald-100 text-emerald-800'
                      : complaint.healthScoreDetail.band === 'ATTENTION'
                      ? 'bg-amber-100 text-amber-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {complaint.healthScoreDetail.band}
                </span>
              )}
            </div>
            <span className="text-[10px] text-gray-500">
              {(complaint.healthScore || 75) < 50 ? 'Critical Attention Required' : 'Healthy Trajectory'}
            </span>
          </div>

          {/* Diagnostic Drivers */}
          <div className="lg:col-span-6 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-gray-900 flex items-center gap-1.5">
                <AlertTriangle className="w-4 h-4 text-red-500" />
                Risk Drivers & SLA Compliance Pulse
              </span>
              <span className="text-[10px] bg-gray-100 text-gray-600 font-mono px-2 py-0.5 rounded">
                AUTONOMOUS ENGINE
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
              <div className="p-2.5 rounded-lg bg-gray-50 border border-gray-100">
                <span className="font-bold text-gray-900 block">SLA Deadline</span>
                <span className="text-[11px] text-gray-500">
                  {deadlineStr ? new Date(deadlineStr).toLocaleString() : 'Active'}
                </span>
              </div>
              <div className="p-2.5 rounded-lg bg-gray-50 border border-gray-100">
                <span className="font-bold text-gray-900 block">Automated Warnings</span>
                <span className="text-[11px] text-gray-500">
                  Cadence active under Sakala
                </span>
              </div>
              <div className="p-2.5 rounded-lg bg-gray-50 border border-gray-100">
                <span className="font-bold text-gray-900 block">Escalation Status</span>
                <span className="text-[11px] text-red-600 font-semibold">
                  {isEscalated ? `Tier-${complaint.escalationLevel} Escalated` : 'Standard Tier-1'}
                </span>
              </div>
              <div className="p-2.5 rounded-lg bg-gray-50 border border-gray-100">
                <span className="font-bold text-gray-900 block">Citizen Privacy</span>
                <span className="text-[11px] text-teal font-semibold">Masked for non-retaliation</span>
              </div>
            </div>

            {/* Health Score Calculation Reasons (Prompt 7.1 & 8.3) */}
            {complaint.healthScoreDetail?.reasons && complaint.healthScoreDetail.reasons.length > 0 && (
              <div className="pt-2 border-t border-gray-100">
                <span className="text-[11px] font-bold text-gray-700 block mb-1">
                  {i18n.language === 'kn' ? 'ಆರೋಗ್ಯ ಸೂಚ್ಯಂಕದ ನಿರ್ಧಾರಕ ಅಂಶಗಳು:' : 'Determinant Factors:'}
                </span>
                <div className="flex flex-wrap gap-1.5">
                  {complaint.healthScoreDetail.reasonItems && complaint.healthScoreDetail.reasonItems.length > 0
                    ? complaint.healthScoreDetail.reasonItems.map((item: any, idx: number) => (
                        <span
                          key={idx}
                          className="text-[10px] bg-white border border-gray-200 text-gray-700 px-2 py-0.5 rounded-md font-medium"
                        >
                          • {String(t(item.key, { defaultValue: item.message, ...(item.params || {}) }))}
                        </span>
                      ))
                    : complaint.healthScoreDetail.reasons.map((r: string, idx: number) => (
                        <span
                          key={idx}
                          className="text-[10px] bg-white border border-gray-200 text-gray-700 px-2 py-0.5 rounded-md font-medium"
                        >
                          • {r}
                        </span>
                      ))}
                </div>
              </div>
            )}
          </div>

          {/* Statutory Penalty Accrual */}
          <div className="lg:col-span-3 p-4 bg-gray-50 border border-gray-200 rounded-xl flex flex-col justify-between h-full space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-[10px] uppercase font-bold text-red-600 tracking-wider">
                Statutory Fine Accrual
              </span>
              <AlertTriangle className="w-4 h-4 text-red-500" />
            </div>
            <div>
              <span className="text-2xl font-extrabold text-gray-900">₹250 <span className="text-xs font-normal text-gray-500">/ day</span></span>
              <p className="text-[10px] text-red-600 mt-0.5">
                Active deduction potential under Sakala Act Sec 24 if breached
              </p>
            </div>
            <div className="text-[10px] text-gray-500 pt-1 border-t border-gray-200">
              Subject to Sakala Appellate Review
            </div>
          </div>
        </div>
      </section>

      {/* Prompt 7.1: Prescriptive Next Best Action Banner */}
      {complaint.nextBestAction && (
        <section className="bg-gradient-to-r from-indigo-900 to-indigo-950 border border-indigo-700 rounded-2xl p-5 text-white shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1.5 max-w-2xl">
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 rounded-md bg-teal text-white text-[10px] font-extrabold uppercase tracking-wider">
                Prescriptive Next Best Action
              </span>
              <span
                className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${
                  complaint.nextBestAction.urgency === 'CRITICAL'
                    ? 'bg-red-500 text-white'
                    : complaint.nextBestAction.urgency === 'HIGH'
                    ? 'bg-amber-400 text-gray-900'
                    : 'bg-indigo-500 text-white'
                }`}
              >
                {complaint.nextBestAction.urgency} Urgency
              </span>
              <span className="text-indigo-200 text-xs">Target Role: {complaint.nextBestAction.targetRole}</span>
            </div>
            <h3 className="text-base font-bold text-white flex items-center gap-2">
              {complaint.nextBestAction.actionKey
                ? String(t(complaint.nextBestAction.actionKey, { defaultValue: complaint.nextBestAction.title }))
                : complaint.nextBestAction.title}
            </h3>
            <p className="text-xs text-indigo-200 leading-relaxed">
              {complaint.nextBestAction.reasonKey
                ? String(t(complaint.nextBestAction.reasonKey, { defaultValue: complaint.nextBestAction.reason, ...(complaint.nextBestAction.params || {}) }))
                : complaint.nextBestAction.reason}
            </p>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {complaint.status !== 'RESOLVED' && complaint.status !== 'RESOLUTION_SUBMITTED' && (
              <button
                type="button"
                onClick={() => setShowResolveModal(true)}
                className="px-4 py-2.5 rounded-xl bg-teal text-white text-xs font-bold hover:bg-teal-600 transition-colors shadow-sm flex items-center gap-1.5 cursor-pointer"
              >
                <CheckCircle className="w-4 h-4" />
                <span>Submit Resolution Proof</span>
              </button>
            )}
          </div>
        </section>
      )}

      {/* Main 2-Column Dossier Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column (7 cols): Narrative, Location, Attachments */}
        <div className="lg:col-span-7 space-y-6">
          {/* Grievance Narrative Card */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="px-2.5 py-1 rounded-md bg-indigo-50 text-primary text-xs font-bold">
                {complaint.category}
              </span>
              <span className="text-xs text-gray-500">
                Logged: {new Date(complaint.createdAt).toLocaleString()}
              </span>
            </div>

            <div className="space-y-1.5">
              <h2 className="text-base font-bold text-gray-900">Complainant Narrative / ನಾಗರಿಕರ ದೂರು ವಿವರ</h2>
              <div className="p-4 rounded-xl bg-gray-50 border border-gray-100 text-sm text-gray-800 leading-relaxed font-normal">
                "{complaint.description}"
              </div>
            </div>

            {/* Citizen Protected Profile */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              <div className="p-3 rounded-xl bg-gray-50 border border-gray-100 space-y-0.5">
                <span className="text-[10px] text-gray-400 font-bold uppercase block">
                  Complainant Name / ಅರ್ಜಿದಾರರು
                </span>
                <span className="text-xs font-bold text-gray-900 block">{complaint.citizenName}</span>
                <span className="text-[10px] text-teal font-semibold">Resident Citizen Record</span>
              </div>
              <div className="p-3 rounded-xl bg-gray-50 border border-gray-100 space-y-0.5">
                <span className="text-[10px] text-gray-400 font-bold uppercase block">
                  Contact Protocol (Masked for Privacy)
                </span>
                <span className="text-xs font-mono font-bold text-gray-900 block">{complaint.citizenPhone}</span>
                <span className="text-[10px] text-gray-500 truncate block">Masked under Privacy Act</span>
              </div>
            </div>
          </div>

          {/* Incident Location & Cadastral Data */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <MapPin className="w-5 h-5 text-primary" />
                <h2 className="text-base font-bold text-gray-900">Incident Location & Cadastral GIS</h2>
              </div>
              <span className="text-xs text-teal font-semibold">Cadastral Accuracy: ±3.5m</span>
            </div>

            <p className="text-xs text-gray-700 leading-relaxed">
              {complaint.locationAddress || 'Vidyaranyapura, Ward 08, Bengaluru'}
            </p>

            <GrievanceMap
              latitude={complaint.latitude ? Number(complaint.latitude) : 12.9784}
              longitude={complaint.longitude ? Number(complaint.longitude) : 77.6408}
              title={complaint.title}
              locationAddress={complaint.locationAddress}
              height="220px"
            />
          </div>

          {/* Attachments */}
          {complaint.attachments && complaint.attachments.length > 0 && (
            <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Paperclip className="w-4 h-4 text-primary" />
                  <h3 className="text-base font-bold text-gray-900">Verified Evidence & Field Attachments</h3>
                </div>
                <span className="text-[10px] text-gray-400 font-mono">SHA-256 HASH VERIFIED</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {complaint.attachments.map((att: any) => (
                  <div key={att.id} className="p-3 rounded-xl bg-gray-50 border border-gray-200 flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <span className="text-xs font-bold text-gray-900 truncate block">{att.originalFilename}</span>
                      <span className="text-[10px] text-gray-500 block">{att.contentType} • {(att.fileSize / 1024).toFixed(0)} KB</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => alert(`Downloading ${att.originalFilename}`)}
                      className="p-1.5 rounded-lg bg-white border border-gray-200 text-primary hover:bg-indigo-50 transition-colors shrink-0"
                    >
                      <Download className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Right Column (5 cols): AI Intelligence, Officer, Cluster */}
        <aside className="lg:col-span-5 space-y-6">
          {/* AI Redressal Guidance */}
          <div className="bg-white border border-indigo-200 rounded-2xl p-6 shadow-xs space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Bot className="w-5 h-5 text-primary" />
                <h3 className="text-sm font-bold text-gray-900">AI Redressal Guidance</h3>
              </div>
              <span className="text-[10px] font-bold bg-indigo-50 text-primary px-2 py-0.5 rounded">
                NIC Rule Engine
              </span>
            </div>

            <div className="p-3 rounded-xl bg-indigo-50/50 border border-indigo-100 text-xs text-gray-700 leading-relaxed">
              <strong>Mandated Immediate Action:</strong> Dispatch emergency secondary desilting suction tanker SV-08 to 4th cross junction; isolate 100mm sluice valve to stop contaminated backpressure, then upload pipe pressure certificate.
            </div>

            <button
              type="button"
              onClick={handleConfirmAiTriage}
              className="w-full py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center justify-center gap-1.5 transition-colors shadow-2xs"
            >
              <span>Confirm AI Triage & Department Allocation</span>
            </button>
          </div>

          {/* Administrative Jurisdiction */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-3">
            <h3 className="text-sm font-bold text-gray-900">Administrative Jurisdiction</h3>
            <div className="p-3 rounded-xl bg-gray-50 border border-gray-100 space-y-1">
              <span className="text-[10px] text-gray-400 font-bold uppercase block">Designated Authority</span>
              <span className="text-xs font-bold text-gray-900 block">{complaint.departmentNameEn}</span>
              <span className="text-[11px] text-gray-500 block">Jurisdictional Field Redressal Desk</span>
            </div>

            <div className="p-3 rounded-xl bg-gray-50 border border-gray-100 flex items-center justify-between">
              <div>
                <span className="text-xs font-bold text-gray-900 block">
                  {complaint.assignedOfficerName || 'Unassigned / ನಿಯೋಜಿಸಲಾಗಿಲ್ಲ'}
                </span>
                <span className="text-[10px] text-gray-500 block">
                  {complaint.assignedOfficerName ? 'Assigned Field Officer' : 'Awaiting official delegation'}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setShowAssignModal(true)}
                className="text-[11px] bg-primary text-white font-bold px-2.5 py-1 rounded-lg hover:bg-primary-hover transition-colors cursor-pointer"
              >
                {complaint.assignedOfficerName ? 'Reassign' : 'Assign Officer'}
              </button>
            </div>
          </div>

          {/* Prompt 7.2: Community Issue Cluster Analysis */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-gray-900 flex items-center gap-1.5">
                <Users className="w-4 h-4 text-teal" />
                Community Issue Cluster
              </span>
              {complaint.communityIssue ? (
                <span className="text-[10px] bg-teal/10 text-teal font-extrabold px-2 py-0.5 rounded-full">
                  Cluster #{complaint.communityIssue.id} • {complaint.communityIssue.complaintCount} Citizens
                </span>
              ) : (
                <span className="text-[10px] bg-gray-100 text-gray-600 font-bold px-2 py-0.5 rounded-full">
                  Isolated Incident
                </span>
              )}
            </div>

            {complaint.communityIssue ? (
              <div className="space-y-2 text-xs">
                <div className="p-3 rounded-xl bg-teal/5 border border-teal/20 space-y-1">
                  <span className="font-bold text-gray-900 block">{complaint.communityIssue.title}</span>
                  <span className="text-[11px] text-gray-600 block">
                    Category: {complaint.communityIssue.category} • Department: {complaint.communityIssue.departmentName}
                  </span>
                  <span className="text-[10px] text-teal font-semibold block">
                    Status: {complaint.communityIssue.status} • Non-destructive duplicate grouping
                  </span>
                </div>

                <div>
                  <span className="text-[11px] font-bold text-gray-700 block mb-1">
                    Co-affected Citizen Tracking IDs ({complaint.communityIssue.linkedTrackingIds?.length || 0}):
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {complaint.communityIssue.linkedTrackingIds?.map((tid: string) => (
                      <button
                        key={tid}
                        type="button"
                        onClick={() => navigate(`/track?id=${tid}`)}
                        className={`font-mono text-[11px] px-2.5 py-1 rounded-lg border transition-colors cursor-pointer ${
                          tid === complaint.trackingNumber || tid === complaint.trackingId
                            ? 'bg-primary text-white border-primary font-bold'
                            : 'bg-gray-50 border-gray-200 text-gray-800 hover:bg-gray-100 font-semibold'
                        }`}
                      >
                        {tid}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <p className="text-[11px] text-gray-500 leading-normal">
                No duplicate complaints detected in the 500m vicinity within the last 30 days. This docket is registered as an isolated civic issue.
              </p>
            )}
          </div>
        </aside>
      </div>

      {/* Bottom Section A: Officer Action & Resolution Update Form */}
      <section className="bg-white border border-gray-200 rounded-2xl p-6 sm:p-8 shadow-xs space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 pb-4">
          <div>
            <h2 className="text-lg font-bold text-gray-900">Log Action & Advance Docket Status / ಕ್ರಮ ಕೈಗೊಳ್ಳಿ</h2>
            <p className="text-xs text-gray-500">
              Official records submitted here reset the follow-up deadline and recalculate the compliance health score.
            </p>
          </div>
          <span className="text-xs font-mono font-bold text-gray-600 bg-gray-100 px-3 py-1 rounded-full">
            OFFICER JURISDICTION PRIVILEGE
          </span>
        </div>

        <form onSubmit={handleUpdate} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">
                Target Resolution Status / ಸ್ಥಿತಿ ಬದಲಾಯಿಸಿ *
              </label>
              <select
                value={updateStatus}
                onChange={(e) => setUpdateStatus(e.target.value)}
                className="w-full bg-gray-50 border border-gray-200 text-gray-900 rounded-xl px-3.5 py-2.5 text-xs font-bold outline-none focus:bg-white focus:border-primary"
              >
                <option value="IN_PROGRESS">In Progress — Work Underway On-Site</option>
                <option value="ASSIGNED">Assigned — Crew Dispatched</option>
                <option value="RESOLVED">Resolved — Rectification Complete</option>
                <option value="REJECTED">Rejected — Out of Municipal Scope</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">
                Mandated Next Follow-Up Check / ಮುಂದಿನ ಪರಿಶೀಲನೆ *
              </label>
              <input
                type="datetime-local"
                value={nextFollowUpDate}
                onChange={(e) => setNextFollowUpDate(e.target.value)}
                className="w-full bg-gray-50 border border-gray-200 text-gray-900 rounded-xl px-3.5 py-2 text-xs outline-none focus:bg-white focus:border-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-gray-700 mb-1">
              Official Progress Note & Field Observations / ಅಧಿಕೃತ ಟಿಪ್ಪಣಿ *
            </label>
            <textarea
              rows={3}
              required
              value={progressNote}
              onChange={(e) => setProgressNote(e.target.value)}
              className="w-full bg-gray-50 border border-gray-200 text-gray-900 rounded-xl p-3.5 text-xs outline-none focus:bg-white focus:border-primary"
              placeholder="State what corrective action was taken on site..."
            />
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="pubFlag"
              checked={isPublic}
              onChange={(e) => setIsPublic(e.target.checked)}
              className="w-4 h-4 rounded text-primary accent-primary"
            />
            <label htmlFor="pubFlag" className="text-xs text-gray-700 font-medium cursor-pointer">
              Visible on Citizen Public Tracking Timeline (Citizens never see internal officer notes or names)
            </label>
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center gap-2 transition-colors shadow-2xs disabled:opacity-50"
            >
              <Send className="w-3.5 h-3.5" />
              <span>{isSubmitting ? 'Updating...' : 'Update Docket & Notify Citizen via SMS'}</span>
            </button>
          </div>
        </form>
      </section>

      {/* Bottom Section B: Action History & Immutable Audit Trail */}
      <section className="bg-white border border-gray-200 rounded-2xl p-6 sm:p-8 shadow-xs space-y-5">
        <div className="flex items-center justify-between border-b border-gray-100 pb-4">
          <h2 className="text-lg font-bold text-gray-900">Action History & Immutable Audit Trail</h2>
          <span className="text-xs text-gray-500 font-mono">SHA-256 SECURED LOG</span>
        </div>

        <Timeline items={historyItems} />
      </section>

      {/* Assign Officer Modal */}
      {showAssignModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-gray-200 space-y-4 animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h3 className="text-sm font-bold text-gray-900 flex items-center gap-2">
                <UserCheck className="w-4 h-4 text-primary" />
                Assign Officer to Grievance #{complaint.trackingNumber}
              </h3>
              <button
                onClick={() => setShowAssignModal(false)}
                className="text-gray-400 hover:text-gray-700"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleAssign} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Select Jurisdictional Officer / ಅಧಿಕಾರಿಯನ್ನು ಆಯ್ಕೆಮಾಡಿ
                </label>
                <select
                  value={selectedOfficerId}
                  onChange={(e) => setSelectedOfficerId(e.target.value)}
                  className="w-full text-xs font-semibold bg-gray-50 border border-gray-200 rounded-xl px-3 py-2.5 focus:bg-white focus:border-primary outline-none"
                  required
                >
                  {officers.map((off) => (
                    <option key={off.id} value={off.id}>
                      {off.fullName} • {off.designation} ({off.departmentNameEn || off.departmentCode})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Delegation Justification Reason / ವಿವರಣೆ
                </label>
                <textarea
                  value={assignReason}
                  onChange={(e) => setAssignReason(e.target.value)}
                  rows={3}
                  className="w-full text-xs bg-gray-50 border border-gray-200 rounded-xl p-3 focus:bg-white focus:border-primary outline-none resize-none"
                  placeholder="Reason for allocation or transfer..."
                  required
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowAssignModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isAssigning}
                  className="px-5 py-2 text-xs font-bold text-white bg-primary hover:bg-primary-hover rounded-xl shadow-xs disabled:opacity-50"
                >
                  {isAssigning ? 'Assigning...' : 'Confirm Assignment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Submit Resolution Modal (Prompt 7.3) */}
      {showResolveModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 space-y-4 shadow-xl">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <CheckCircle className="w-5 h-5 text-emerald-600" />
                <h3 className="text-base font-bold text-gray-900">Submit Resolution Proof</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowResolveModal(false)}
                className="p-1 rounded-lg hover:bg-gray-100 text-gray-400 hover:text-gray-600 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-gray-500">
              Under Prompt 7.3 verified resolution policy, submitting this summary transitions the grievance to <strong>RESOLUTION_SUBMITTED</strong>. The citizen will be requested to inspect the work and confirm closure or request further remediation.
            </p>

            <form onSubmit={handleSubmitResolution} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Resolution Summary & Work Performed * / ಪರಿಹಾರ ವಿವರಣೆ
                </label>
                <textarea
                  value={resolutionSummary}
                  onChange={(e) => setResolutionSummary(e.target.value)}
                  rows={4}
                  className="w-full text-xs bg-gray-50 border border-gray-200 rounded-xl p-3 focus:bg-white focus:border-emerald-600 outline-none resize-none"
                  placeholder="Detail the technical actions taken (e.g. pipe joint replaced, road asphalted, streetlight bulb installed)..."
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Optional Evidence Attachment (Photo / PDF Work Order)
                </label>
                <input
                  type="file"
                  accept="image/*,application/pdf"
                  onChange={(e) => setResolutionFile(e.target.files?.[0] || null)}
                  className="w-full text-xs text-gray-600 file:mr-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-bold file:bg-emerald-50 file:text-emerald-700 hover:file:bg-emerald-100 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowResolveModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-xl cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isResolving}
                  className="px-5 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl shadow-xs disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
                >
                  <CheckCircle className="w-4 h-4" />
                  <span>{isResolving ? 'Submitting...' : 'Submit Resolution'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
