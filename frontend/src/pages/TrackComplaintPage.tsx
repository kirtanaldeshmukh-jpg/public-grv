import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Search,
  Download,
  Share2,
  ShieldCheck,
  Clock,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  Camera,
  Phone,
  Mail,
  Send,
  Flag,
  PhoneCall,
  Activity,
  CheckCircle,
  RotateCcw,
  Users,
  X,
} from 'lucide-react';
import apiClient from '../api/client';
import { StatusPill } from '../components/common/StatusPill';
import { HealthBadge } from '../components/common/HealthBadge';
import { useToast } from '../context/ToastContext';

export const TrackComplaintPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const { toast } = useToast();

  const queryId = searchParams.get('id') || '';
  const [searchInput, setSearchInput] = useState(queryId || 'RSK-2026-001');
  const [loading, setLoading] = useState(false);
  const [reminderSent, setReminderSent] = useState(false);
  const [flagRaised, setFlagRaised] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Prompt 7.3 & 7.2 state
  const [resolutionSummary, setResolutionSummary] = useState<string | null>(null);
  const [resolutionSubmittedAt, setResolutionSubmittedAt] = useState<string | null>(null);
  const [canConfirmResolution, setCanConfirmResolution] = useState(false);
  const [reopenCount, setReopenCount] = useState(0);
  const [communityIssue, setCommunityIssue] = useState<any | null>(null);

  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [isConfirming, setIsConfirming] = useState(false);
  const [isRejecting, setIsRejecting] = useState(false);

  // Active case state
  const [caseData, setCaseData] = useState({
    trackingNumber: 'RSK-2025-104',
    departmentNameEn: 'BWSSB • Div IV Yelahanka',
    departmentNameKn: 'ಜಲಮಂಡಳಿ • ವಿಭಾಗ ೪ ಯಲಹಂಕ',
    title: 'Drinking Water Pipeline Burst & Contamination on 8th Main Road, Vidyaranyapura, Bengaluru',
    titleKn: 'ಕುಡಿಯುವ ನೀರಿನ ಪೈಪ್‌ಲೈನ್ ಒಡೆದು ರಸ್ತೆಯಲ್ಲಿ ನೀರು ಸೋರಿಕೆ ಹಾಗೂ ಕಲುಷಿತ ನೀರು ಪೂರೈಕೆ ದೂರು.',
    status: 'IN_PROGRESS',
    healthScore: 84,
    filedAt: '12 Oct 2025, 09:30 AM',
    slaDeadline: '14 Oct 2025, 05:00 PM',
    remainingTime: '18 Hours 15 Mins Remaining',
    progressPercent: 65,
    officerDesignation: 'Assistant Executive Engineer',
    officerOffice: 'Div IV, Yelahanka Sub-Division',
    deskPhone: '+91 80 2294 5144',
    officeEmail: 'aee.div4.yel@bwssb.karnataka.gov.in',
    milestones: [
      {
        step: 1,
        title: '1. Grievance Lodged & Registered',
        time: '12 Oct 2025, 09:30 AM',
        desc: 'Registered via IPGRMS Public Mobile App with GPS Tagged coordinates (13.0827° N, 77.5458° E). SAKALA Token generated automatically.',
        sig: 'Digital Signature ID: KAR-SVR-098827-REC',
        completed: true,
      },
      {
        step: 2,
        title: '2. Verified & Routed to Jurisdiction Officer',
        time: '12 Oct 2025, 11:15 AM',
        desc: 'Assigned to Assistant Executive Engineer, Maintenance Div 4, Yelahanka Sub-Division. Case accepted within SLA window.',
        sig: 'Jurisdiction Nodal Desk Verified',
        completed: true,
      },
      {
        step: 3,
        title: '3. Field Inspection & Heavy Material Dispatch',
        time: '13 Oct 2025, 08:45 AM',
        desc: 'Site excavation team with JCB unit dispatched from Sahakar Nagar depot. Replacement 160mm ductile iron sleeve pipe en route. Sluice valve isolated on 7th Cross junction to prevent further road flooding.',
        sig: 'Job Card #JC-BWSSB-2025-441 • Crew: 6 Technicians on site',
        active: true,
      },
      {
        step: 4,
        title: '4. Pipeline Welding, Flush & Water Quality Testing',
        time: 'Expected: 14 Oct 2025, 04:00 PM',
        desc: 'Post-repair chlorine test and turbidity check to guarantee potability before resuming residential tap supply.',
        pending: true,
      },
      {
        step: 5,
        title: '5. Citizen Final Verification & SAKALA Sign-off',
        time: 'Expected: 14 Oct 2025, 05:00 PM',
        desc: 'The complainant provides confirmation via OTP verification to formally close the docket in state monitoring servers.',
        pending: true,
      },
    ],
  });

  const handleTrack = useCallback(async (e?: React.FormEvent, customId?: string) => {
    if (e) e.preventDefault();
    const targetId = (customId || searchInput).trim();
    if (!targetId) return;

    setLoading(true);
    setErrorMessage(null);
    try {
      const res = await apiClient.get(`/public/track/${encodeURIComponent(targetId)}`);
      if (res.data?.data) {
        const d = res.data.data;
        setCaseData((prev) => ({
          ...prev,
          trackingNumber: d.trackingId || d.trackingNumber || targetId,
          departmentNameEn: d.departmentName || prev.departmentNameEn,
          status: d.status || prev.status,
          slaDeadline: d.followUpDeadline ? new Date(d.followUpDeadline).toLocaleString() : prev.slaDeadline,
          healthScore: d.isResolved ? 100 : prev.healthScore,
        }));
        setResolutionSummary(d.resolutionSummary || null);
        setResolutionSubmittedAt(d.resolutionSubmittedAt || null);
        setCanConfirmResolution(d.canConfirmResolution || d.status === 'RESOLUTION_SUBMITTED');
        setReopenCount(d.reopenCount || 0);
        setCommunityIssue(d.communityIssue || null);
        toast.success(`Dossier loaded for tracking token: ${targetId}`);
      }
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Complaint tracking ID not found in registry.';
      setErrorMessage(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }, [searchInput, toast]);

  useEffect(() => {
    if (queryId) {
      setSearchInput(queryId);
      apiClient.get(`/public/track/${encodeURIComponent(queryId.trim())}`)
        .then((res) => {
          if (res.data?.data) {
            const d = res.data.data;
            setCaseData((prev) => ({
              ...prev,
              trackingNumber: d.trackingId || d.trackingNumber || queryId,
              departmentNameEn: d.departmentName || prev.departmentNameEn,
              status: d.status || prev.status,
              slaDeadline: d.followUpDeadline ? new Date(d.followUpDeadline).toLocaleString() : prev.slaDeadline,
              healthScore: d.isResolved ? 100 : prev.healthScore,
            }));
            setResolutionSummary(d.resolutionSummary || null);
            setResolutionSubmittedAt(d.resolutionSubmittedAt || null);
            setCanConfirmResolution(d.canConfirmResolution || d.status === 'RESOLUTION_SUBMITTED');
            setReopenCount(d.reopenCount || 0);
            setCommunityIssue(d.communityIssue || null);
          }
        })
        .catch(() => {
          setErrorMessage('Complaint tracking ID not found in registry.');
        });
    }
  }, [queryId]);

  const handleConfirmResolution = async () => {
    setIsConfirming(true);
    try {
      const targetId = caseData.trackingNumber;
      await apiClient.post(`/public/complaints/${encodeURIComponent(targetId)}/confirm-resolution`, {
        feedback: 'Citizen confirmed field redressal is satisfactory.',
      });
      toast.success('Grievance marked as RESOLVED! Thank you for verifying.');
      handleTrack(undefined, targetId);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to confirm resolution.');
    } finally {
      setIsConfirming(false);
    }
  };

  const handleRejectResolution = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rejectReason.trim()) {
      toast.warning('Please explain why the issue is not resolved.');
      return;
    }
    setIsRejecting(true);
    try {
      const targetId = caseData.trackingNumber;
      await apiClient.post(`/public/complaints/${encodeURIComponent(targetId)}/reject-resolution`, {
        reason: rejectReason,
      });
      toast.info('Grievance has been REOPENED. Supervisor has been alerted for escalated review.');
      setShowRejectModal(false);
      setRejectReason('');
      handleTrack(undefined, targetId);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to reject resolution.');
    } finally {
      setIsRejecting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Tracking Control Bar */}
      <section className="bg-white border border-gray-200 rounded-2xl p-5 sm:p-6 shadow-xs">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 flex-1">
            <div>
              <span className="text-[11px] font-bold text-primary uppercase tracking-wider block">
                Karnataka IPGRMS • SAKALA Act Track
              </span>
              <h1 className="text-xl sm:text-2xl font-extrabold text-gray-900">Live Case Dossier</h1>
            </div>

            <form onSubmit={handleTrack} className="flex-1 max-w-md w-full">
              <div className="flex items-center bg-gray-50 border border-gray-200 rounded-xl px-3 py-1.5 focus-within:bg-white focus-within:border-primary transition-colors">
                <Search className="w-4 h-4 text-gray-400 mr-2 shrink-0" />
                <input
                  type="text"
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="Grievance ID (e.g. RSK-2025-104)"
                  className="w-full bg-transparent border-0 outline-none text-sm text-gray-900 font-semibold placeholder-gray-400"
                />
                <button
                  type="submit"
                  disabled={loading}
                  className="ml-2 px-3.5 py-1.5 bg-primary text-white text-xs font-bold rounded-lg hover:bg-primary-hover transition-colors flex items-center gap-1 shrink-0"
                >
                  <span>{loading ? '...' : 'Track'}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
              {errorMessage && (
                <div className="mt-2 p-2.5 bg-red-50 border border-red-200 text-red-700 rounded-xl text-xs font-semibold flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
                  <span>{errorMessage}</span>
                </div>
              )}
            </form>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <button
              type="button"
              onClick={() => window.print()}
              className="flex items-center gap-1.5 px-3.5 py-2 bg-gray-50 border border-gray-200 text-gray-700 text-xs font-bold rounded-xl hover:bg-gray-100 transition-colors"
            >
              <Download className="w-4 h-4 text-primary" />
              <span>Receipt PDF</span>
            </button>
            <button
              type="button"
              onClick={() => {
                navigator.clipboard?.writeText(window.location.href);
                alert('Tracking link copied to clipboard!');
              }}
              className="flex items-center gap-1.5 px-3.5 py-2 bg-teal text-white text-xs font-bold rounded-xl hover:bg-teal-hover transition-colors shadow-2xs"
            >
              <Share2 className="w-4 h-4" />
              <span>Share Status</span>
            </button>
          </div>
        </div>
      </section>

      {/* Grievance Header Summary Card */}
      <section className="bg-white border border-gray-200 rounded-2xl p-6 sm:p-8 shadow-xs relative overflow-hidden">
        <div className="absolute left-0 top-0 bottom-0 w-2.5 bg-primary"></div>

        <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-6">
          <div className="space-y-3 flex-1">
            <div className="flex flex-wrap items-center gap-2.5">
              <span className="px-2.5 py-1 rounded-md bg-indigo-50 text-primary text-xs font-bold tracking-wide">
                {caseData.departmentNameEn}
              </span>
              <span className="px-2.5 py-1 rounded-md bg-gray-100 text-gray-700 text-xs font-mono font-bold">
                {caseData.trackingNumber}
              </span>
              <span className="text-xs text-gray-500 flex items-center gap-1">
                <Clock className="w-3.5 h-3.5 text-gray-400" />
                Filed: {caseData.filedAt}
              </span>
            </div>

            <h2 className="text-xl sm:text-2xl font-bold text-gray-900 leading-snug">
              {caseData.title}
            </h2>
            <p className="text-sm text-gray-600">
              {caseData.titleKn}
            </p>
          </div>

          <div className="flex flex-row lg:flex-col items-end justify-between lg:justify-start gap-3 min-w-[240px]">
            <StatusPill status={caseData.status as any} />
            <div className="bg-gray-50 border border-gray-200 p-3 rounded-xl w-full flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <HealthBadge score={caseData.healthScore} />
                <div>
                  <span className="text-xs font-bold text-gray-900 block">Health Score</span>
                  <span className="text-[11px] text-teal font-medium">Active Crew Deployed</span>
                </div>
              </div>
              <Activity className="w-4 h-4 text-teal" />
            </div>
          </div>
        </div>

        {/* SAKALA SLA Banner Ribbon */}
        <div className="mt-6 pt-4 border-t border-gray-100 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2.5 text-sm">
            <Clock className="w-4 h-4 text-primary" />
            <span className="font-semibold text-gray-700">Guaranteed SAKALA SLA Delivery Date:</span>
            <span className="font-bold text-primary">{caseData.slaDeadline}</span>
          </div>
          <div className="flex items-center gap-1.5 bg-teal/10 text-teal px-3 py-1 rounded-full text-xs font-bold">
            <Clock className="w-3.5 h-3.5" />
            <span>{caseData.remainingTime}</span>
          </div>
        </div>
      </section>

      {/* Prompt 7.3: Citizen Resolution Sign-off Banner */}
      {(caseData.status === 'RESOLUTION_SUBMITTED' || canConfirmResolution) && (
        <section className="bg-gradient-to-r from-emerald-50 via-teal-50 to-cyan-50 border-2 border-emerald-400 rounded-2xl p-6 sm:p-7 shadow-sm space-y-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="w-12 h-12 rounded-2xl bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-xs">
                <CheckCircle className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-base font-extrabold text-gray-900">
                    Official Resolution Submitted — Verification Required / ಪರಿಹಾರ ದೃಢೀಕರಣ
                  </h3>
                  <span className="bg-emerald-600 text-white text-[10px] font-extrabold px-2.5 py-0.5 rounded-full uppercase tracking-wider">
                    Action Required
                  </span>
                </div>
                <p className="text-xs text-gray-700 leading-relaxed">
                  The jurisdictional field officer has reported completion of the redressal work for this docket. Under Sakala citizen governance rights, please verify if the issue on-site is resolved.
                </p>
                {resolutionSubmittedAt && (
                  <span className="text-[11px] text-gray-500 font-mono block">
                    Resolution Submitted: {new Date(resolutionSubmittedAt).toLocaleString()}
                  </span>
                )}
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2.5 shrink-0">
              <button
                type="button"
                onClick={handleConfirmResolution}
                disabled={isConfirming}
                className="px-5 py-2.5 rounded-xl bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-colors shadow-sm flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
              >
                <CheckCircle className="w-4 h-4" />
                <span>{isConfirming ? 'Confirming...' : 'Confirm Resolution (Resolved)'}</span>
              </button>
              <button
                type="button"
                onClick={() => setShowRejectModal(true)}
                className="px-4 py-2.5 rounded-xl bg-red-50 border border-red-300 text-red-700 text-xs font-bold hover:bg-red-100 transition-colors flex items-center gap-1.5 cursor-pointer"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Issue Not Resolved (Reopen)</span>
              </button>
            </div>
          </div>

          {resolutionSummary && (
            <div className="p-4 rounded-xl bg-white border border-emerald-200/80 space-y-1">
              <span className="text-[11px] font-bold text-gray-900 uppercase tracking-wider block">
                Officer Redressal Remarks / ಅಧಿಕಾರಿಯ ಪರಿಹಾರ ವರದಿ:
              </span>
              <p className="text-xs text-gray-800 leading-relaxed">
                "{resolutionSummary}"
              </p>
            </div>
          )}
        </section>
      )}

      {/* Prompt 7.3: Reopened Grievance Status Banner */}
      {caseData.status === 'REOPENED' && (
        <section className="bg-amber-50 border-2 border-amber-300 rounded-2xl p-5 flex items-start gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-600 text-white flex items-center justify-center shrink-0">
            <RotateCcw className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-bold text-amber-950">Grievance Reopened Under Escalation</h3>
              <span className="text-[10px] bg-amber-200 text-amber-900 font-bold px-2 py-0.5 rounded-full">
                Reopened {reopenCount || 1} Time(s)
              </span>
            </div>
            <p className="text-xs text-amber-800 mt-1">
              You marked this issue as unresolved. The department supervisor has been alerted, and statutory re-inspection is actively in progress.
            </p>
          </div>
        </section>
      )}

      {/* Prompt 7.2: Linked Community Issue Notice */}
      {communityIssue && (
        <section className="bg-teal/5 border border-teal/20 rounded-2xl p-4 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <Users className="w-5 h-5 text-teal shrink-0" />
            <div>
              <span className="text-xs font-bold text-gray-900 block">
                Linked Community Issue #{communityIssue.id}: {communityIssue.title}
              </span>
              <span className="text-[11px] text-gray-600">
                {communityIssue.complaintCount} residents in your vicinity have co-reported this issue. Each citizen retains their own tracking ID for individual notifications.
              </span>
            </div>
          </div>
          <span className="text-[10px] bg-teal text-white font-extrabold px-2.5 py-1 rounded-full uppercase shrink-0">
            Community Cluster
          </span>
        </section>
      )}

      {/* Dual Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column (8 cols): Milestone Stepper & Photographic Evidence */}
        <div className="lg:col-span-8 space-y-6">
          {/* Milestone Stepper */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 sm:p-8 shadow-xs">
            <div className="flex items-center justify-between mb-6">
              <div>
                <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider block">
                  SAKALA Statutory Audit
                </span>
                <h3 className="text-lg font-bold text-gray-900">Resolution Milestone Tracker</h3>
              </div>
              <span className="px-2.5 py-1 rounded-md bg-gray-100 text-gray-600 text-xs font-semibold">
                5 Stages Architecture
              </span>
            </div>

            <div className="relative pl-6 space-y-7 before:absolute before:left-3 before:top-2 before:bottom-3 before:w-0.5 before:bg-gray-200">
              {caseData.milestones.map((m, idx) => (
                <div key={idx} className="relative flex items-start gap-4">
                  {m.completed ? (
                    <div className="absolute -left-6 mt-0.5 w-6 h-6 rounded-full bg-teal text-white flex items-center justify-center shadow-xs">
                      <CheckCircle2 className="w-4 h-4" />
                    </div>
                  ) : m.active ? (
                    <div className="absolute -left-6 mt-0.5 w-6 h-6 rounded-full bg-primary text-white flex items-center justify-center ring-4 ring-indigo-100 shadow-xs animate-pulse">
                      <Clock className="w-3.5 h-3.5" />
                    </div>
                  ) : (
                    <div className="absolute -left-6 mt-0.5 w-6 h-6 rounded-full bg-gray-200 text-gray-400 flex items-center justify-center text-[10px] font-bold">
                      {m.step}
                    </div>
                  )}

                  <div
                    className={`flex-1 p-4 rounded-xl border transition-all ${
                      m.active
                        ? 'bg-indigo-50/50 border-indigo-200 shadow-2xs'
                        : 'bg-gray-50/50 border-gray-100'
                    }`}
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 mb-1">
                      <span className={`text-sm font-bold ${m.active ? 'text-primary' : 'text-gray-900'}`}>
                        {m.title}
                      </span>
                      <span className="text-[11px] text-gray-500 font-medium">{m.time}</span>
                    </div>
                    <p className="text-xs text-gray-600 leading-relaxed">{m.desc}</p>
                    {m.sig && (
                      <div className="mt-2 text-[11px] font-semibold text-teal flex items-center gap-1">
                        <ShieldCheck className="w-3.5 h-3.5" />
                        <span>{m.sig}</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* On-Ground Photographic & Work Evidence */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 sm:p-8 shadow-xs">
            <div className="flex items-center justify-between mb-5">
              <div>
                <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider block">
                  Verification Proofs
                </span>
                <h3 className="text-lg font-bold text-gray-900">On-Ground Media & Transparency Evidence</h3>
              </div>
              <span className="px-2.5 py-1 rounded-md bg-teal/10 text-teal text-xs font-bold">
                Geo-Verified Photos
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="bg-gray-50 border border-gray-200 rounded-xl overflow-hidden">
                <div className="h-44 bg-gray-200 relative flex items-center justify-center text-gray-400">
                  <Camera className="w-10 h-10 text-gray-400" />
                  <span className="absolute top-2 left-2 bg-black/60 text-white text-[10px] font-bold px-2 py-0.5 rounded">
                    Original Citizen Upload
                  </span>
                  <span className="absolute bottom-2 right-2 bg-black/60 text-white text-[10px] px-2 py-0.5 rounded">
                    12 Oct 2025, 09:28 AM
                  </span>
                </div>
                <div className="p-3.5 space-y-1">
                  <span className="text-xs font-bold text-gray-900 block">Citizen Site Documentation</span>
                  <p className="text-[11px] text-gray-600 leading-normal">
                    Location: 8th Main, Vidyaranyapura. High pressure freshwater leak causing road sub-base erosion.
                  </p>
                </div>
              </div>

              <div className="bg-gray-50 border border-gray-200 rounded-xl overflow-hidden">
                <div className="h-44 bg-indigo-50 relative flex items-center justify-center text-primary">
                  <Activity className="w-10 h-10 text-primary" />
                  <span className="absolute top-2 left-2 bg-primary text-white text-[10px] font-bold px-2 py-0.5 rounded">
                    Live Job Card Evidence (Crew)
                  </span>
                  <span className="absolute bottom-2 right-2 bg-black/60 text-white text-[10px] px-2 py-0.5 rounded">
                    13 Oct 2025, 09:10 AM
                  </span>
                </div>
                <div className="p-3.5 space-y-1">
                  <span className="text-xs font-bold text-gray-900 block">Repair Work Commenced</span>
                  <p className="text-[11px] text-gray-600 leading-normal">
                    Uploaded by Junior Engineer. Safety trench barriers installed, road diversion marked.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column (4 cols): Safety Net, Officer Info & Hierarchy */}
        <aside className="lg:col-span-4 space-y-6">
          {/* Citizen Escalation Safety Net Card */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-4">
            <div className="flex items-center gap-2.5">
              <span className="w-8 h-8 rounded-lg bg-indigo-50 text-primary flex items-center justify-center">
                <ShieldCheck className="w-5 h-5" />
              </span>
              <h4 className="font-bold text-gray-900 text-sm">SAKALA Safety Net</h4>
            </div>

            <div className="bg-gray-50 border border-gray-100 rounded-xl p-3.5 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-gray-500 font-medium">Automatic Escalation Trigger</span>
                <span className="font-bold text-primary">In 18h 15m</span>
              </div>
              <div className="w-full bg-gray-200 h-2 rounded-full overflow-hidden">
                <div className="bg-primary h-full rounded-full" style={{ width: '65%' }}></div>
              </div>
              <p className="text-[11px] text-gray-600 leading-snug pt-1">
                If not resolved by <strong className="text-gray-900 font-semibold">{caseData.slaDeadline}</strong>, this complaint automatically escalates to <strong>Level 2: Chief Engineer BWSSB</strong> with statutory notice.
              </p>
            </div>

            <div className="space-y-2 pt-1">
              <button
                type="button"
                onClick={() => {
                  setReminderSent(true);
                  setTimeout(() => setReminderSent(false), 4000);
                }}
                className="w-full py-2.5 px-3 bg-primary text-white text-xs font-bold rounded-xl hover:bg-primary-hover flex items-center justify-center gap-1.5 transition-colors shadow-2xs"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{reminderSent ? 'Reminder Dispatched to Officer!' : 'Send Polite Reminder to Crew'}</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setFlagRaised(true);
                  setTimeout(() => setFlagRaised(false), 4000);
                }}
                className="w-full py-2.5 px-3 bg-gray-50 border border-red-200 text-red-600 text-xs font-bold rounded-xl hover:bg-red-50 flex items-center justify-center gap-1.5 transition-colors"
              >
                <Flag className="w-3.5 h-3.5" />
                <span>{flagRaised ? 'Early Warning Logged' : 'Raise Early Flag (Worsening Leak)'}</span>
              </button>
            </div>
            <p className="text-[10px] text-gray-400 text-center">
              Complies with Section 4(1) of Karnataka Sakala Services Act, 2011.
            </p>
          </div>

          {/* Assigned Redressal Official Profile */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">
                Accountability Dossier
              </span>
              <span className="px-2 py-0.5 rounded-full bg-teal/10 text-teal text-[10px] font-bold">
                On Duty
              </span>
            </div>

            <div className="space-y-1">
              <span className="text-sm font-bold text-gray-900 block">
                {caseData.officerDesignation}
              </span>
              <span className="text-xs text-gray-500 block">
                {caseData.officerOffice}
              </span>
              <span className="text-[11px] text-teal font-semibold block">
                Citizen Privacy Shield Enforced (Designation & Public Desk Only)
              </span>
            </div>

            <div className="bg-gray-50 border border-gray-100 rounded-xl p-3 space-y-2 text-xs">
              <div className="flex items-center gap-2 text-gray-700">
                <Phone className="w-3.5 h-3.5 text-gray-400" />
                <span>{caseData.deskPhone}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-700 truncate">
                <Mail className="w-3.5 h-3.5 text-gray-400" />
                <span className="truncate">{caseData.officeEmail}</span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => alert('Callback request logged. An official desk executive will reach out.')}
              className="w-full py-2 bg-teal text-white text-xs font-bold rounded-xl hover:bg-teal-hover flex items-center justify-center gap-1.5 transition-colors shadow-2xs"
            >
              <PhoneCall className="w-3.5 h-3.5" />
              <span>Request Officer Callback</span>
            </button>
          </div>

          {/* Hierarchy Escalation Chain */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-3">
            <h4 className="font-bold text-gray-900 text-sm">Hierarchy Escalation Chain</h4>
            <div className="space-y-3 text-xs">
              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-indigo-100 text-primary font-bold flex items-center justify-center text-[10px] shrink-0 mt-0.5">
                  L1
                </span>
                <div>
                  <strong className="text-gray-900 block font-semibold">Assistant Executive Engineer (Current)</strong>
                  <span className="text-gray-500 text-[11px]">Resolution window: 48 Hours</span>
                </div>
              </div>

              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-gray-100 text-gray-600 font-bold flex items-center justify-center text-[10px] shrink-0 mt-0.5">
                  L2
                </span>
                <div>
                  <strong className="text-gray-800 block font-semibold">Executive Engineer / Commissioner</strong>
                  <span className="text-gray-500 text-[11px]">Automatic escalation upon SLA breach</span>
                </div>
              </div>

              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-gray-100 text-gray-600 font-bold flex items-center justify-center text-[10px] shrink-0 mt-0.5">
                  L3
                </span>
                <div>
                  <strong className="text-gray-800 block font-semibold">Sakala Appellate Authority</strong>
                  <span className="text-gray-500 text-[11px]">Statutory inquiry and ₹20/day compensatory docket</span>
                </div>
              </div>
            </div>
          </div>
        </aside>
      </div>

      {/* Prompt 7.3: Citizen Rejection Modal */}
      {showRejectModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <RotateCcw className="w-5 h-5 text-red-600" />
                <h3 className="text-base font-bold text-gray-900">Reopen Grievance</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowRejectModal(false)}
                className="p-1 rounded-lg hover:bg-gray-100 text-gray-400 hover:text-gray-600 cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-gray-600">
              Please explain why the department's redressal is unsatisfactory. This explanation will be logged in the permanent audit trail and escalated directly to the departmental supervisor.
            </p>

            <form onSubmit={handleRejectResolution} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Reason Issue is Not Resolved * / ಕಾರಣ
                </label>
                <textarea
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  rows={4}
                  className="w-full text-xs bg-gray-50 border border-gray-200 rounded-xl p-3 focus:bg-white focus:border-red-500 outline-none resize-none"
                  placeholder="e.g., Pothole was only partially filled and loose gravel remains, Streetlight still flickers at night..."
                  required
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowRejectModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-gray-600 hover:bg-gray-100 rounded-xl cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isRejecting}
                  className="px-5 py-2 text-xs font-bold text-white bg-red-600 hover:bg-red-700 rounded-xl shadow-xs disabled:opacity-50 cursor-pointer flex items-center gap-1.5"
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>{isRejecting ? 'Reopening...' : 'Reopen Grievance'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
