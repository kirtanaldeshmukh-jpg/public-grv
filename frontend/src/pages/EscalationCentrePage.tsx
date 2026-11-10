import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  Gavel,
  Search,
  Download,
  PlusCircle,
  ArrowRight,
} from 'lucide-react';
import { mockEscalations } from '../mocks';
import apiClient from '../api/client';
import { useToast } from '../context/ToastContext';

export const EscalationCentrePage: React.FC = () => {
  const navigate = useNavigate();
  const { toast } = useToast();

  const [activeTier, setActiveTier] = useState<'ALL' | 'TIER_1' | 'TIER_2' | 'TIER_3'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [escalations, setEscalations] = useState<any[]>(mockEscalations);
  const [isScanning, setIsScanning] = useState(false);

  const fetchEscalations = async () => {
    try {
      const res = await apiClient.get('/escalations');
      if (res.data?.data && res.data.data.length > 0) {
        setEscalations(res.data.data);
      }
    } catch {
      // offline fallback
    }
  };

  React.useEffect(() => {
    fetchEscalations();
  }, []);

  const filtered = escalations.filter((esc) => {
    const tracking = esc.complaintTrackingNumber || esc.trackingId || '';
    const dept = esc.departmentName || '';
    const reason = esc.triggerReason || '';
    const officer = esc.escalatedToOfficerName || '';

    const matchesSearch =
      tracking.toLowerCase().includes(searchTerm.toLowerCase()) ||
      dept.toLowerCase().includes(searchTerm.toLowerCase()) ||
      reason.toLowerCase().includes(searchTerm.toLowerCase()) ||
      officer.toLowerCase().includes(searchTerm.toLowerCase());

    if (!matchesSearch) return false;

    const level = esc.toLevel || esc.escalationLevel || 1;
    if (activeTier === 'TIER_1') return level === 1;
    if (activeTier === 'TIER_2') return level === 2;
    if (activeTier === 'TIER_3') return level >= 3;
    return true;
  });

  const handleIssueNotice = (trackingNum: string) => {
    toast.warning(`Section 14 Statutory Charge Memo issued for docket ${trackingNum}.`);
  };

  const handleTriggerScan = async () => {
    setIsScanning(true);
    try {
      const res = await apiClient.post('/escalations/trigger-scan');
      toast.success(`Statutory escalation scan completed! Processed: ${res.data?.data ?? 0}`);
      fetchEscalations();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Error executing escalation scan');
    } finally {
      setIsScanning(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Block */}
      <section className="bg-white border border-gray-200 rounded-2xl p-6 sm:p-8 shadow-xs flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="space-y-1.5 max-w-3xl">
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full bg-indigo-50 text-primary text-[11px] font-bold uppercase tracking-wider">
              Karnataka Grievance Redressal Act • SAKALA Compliant
            </span>
            <span className="text-xs text-teal font-semibold">Zone-04 Command</span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Grievance Central Repository & Multi-Tier Escalation Hub
          </h1>
          <p className="text-xs text-primary font-semibold">
            ಸಾರ್ವಜನಿಕ ಕುಂದುಕೊರತೆಗಳ ಕೇಂದ್ರೀಕೃತ ದಾಖಲೆ ಮತ್ತು ಶಾಸನಬದ್ಧ ಮೇಲ್ಮನವಿ ಕೇಂದ್ರ
          </p>
          <p className="text-xs text-gray-600 leading-relaxed">
            Real-time case docket, statutory SLA countdowns, and administrative escalation lineage. Cases automatically elevate when reminders are ignored or deadlines breach.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={handleTriggerScan}
            disabled={isScanning}
            className="px-3.5 py-2.5 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs font-bold hover:bg-red-100 flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-50"
          >
            <Gavel className="w-4 h-4 text-red-600" />
            <span>{isScanning ? 'Scanning...' : 'Trigger Statutory Scan'}</span>
          </button>
          <button
            type="button"
            onClick={() => alert('Exporting Gazette Dossier CSV...')}
            className="px-3.5 py-2.5 rounded-xl bg-gray-50 border border-gray-200 text-gray-700 text-xs font-bold hover:bg-gray-100 flex items-center gap-1.5 transition-colors"
          >
            <Download className="w-4 h-4 text-primary" />
            <span>Export Gazette Dossier (CSV)</span>
          </button>
          <button
            type="button"
            onClick={() => navigate('/file')}
            className="px-4 py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center gap-1.5 transition-colors shadow-2xs"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Register Suo Motu Docket</span>
          </button>
        </div>
      </section>

      {/* 3-Tier Escalation Protocol System Guide */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Tier 1 */}
        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs relative overflow-hidden flex flex-col justify-between">
          <div className="absolute top-0 left-0 bottom-0 w-2 bg-teal"></div>
          <div className="pl-2 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] uppercase font-bold text-teal bg-teal/10 px-2 py-0.5 rounded">
                Tier 01 • Taluk / Ward
              </span>
              <span className="text-[11px] text-gray-500 font-semibold">SLA: 7 Days</span>
            </div>
            <h3 className="text-sm font-bold text-gray-900">Ward / Taluk Section Officer</h3>
            <p className="text-xs text-gray-500 leading-normal">
              Primary verification, site survey & ground rectification stage.
            </p>
          </div>
          <div className="pl-2 pt-3 mt-3 border-t border-gray-100 text-[11px] text-teal font-semibold flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-teal"></span>
            <span>Active Capacity: 2,410 cases redressal window</span>
          </div>
        </div>

        {/* Tier 2 */}
        <div className="bg-white border border-amber-200 rounded-2xl p-5 shadow-xs relative overflow-hidden flex flex-col justify-between">
          <div className="absolute top-0 left-0 bottom-0 w-2 bg-amber-500"></div>
          <div className="pl-2 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] uppercase font-bold text-amber-800 bg-amber-100 px-2 py-0.5 rounded">
                Tier 02 • Sub-Division
              </span>
              <span className="text-[11px] text-amber-700 font-semibold">+48h Breach Notice</span>
            </div>
            <h3 className="text-sm font-bold text-gray-900">Asst. Exec. Engineer / Tahsildar</h3>
            <p className="text-xs text-gray-500 leading-normal">
              Auto-escalated on Level 1 SLA expiration with supervisory audit.
            </p>
          </div>
          <div className="pl-2 pt-3 mt-3 border-t border-amber-100 text-[11px] text-amber-800 font-semibold flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-amber-500"></span>
            <span>215 Cases entering grace window (Warning)</span>
          </div>
        </div>

        {/* Tier 3 */}
        <div className="bg-white border border-red-200 rounded-2xl p-5 shadow-xs relative overflow-hidden flex flex-col justify-between">
          <div className="absolute top-0 left-0 bottom-0 w-2 bg-red-600"></div>
          <div className="pl-2 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] uppercase font-bold text-red-700 bg-red-100 px-2 py-0.5 rounded">
                Tier 03 • Secretariat / DC
              </span>
              <span className="text-[11px] text-red-600 font-semibold">Statutory Fine / KCS</span>
            </div>
            <h3 className="text-sm font-bold text-gray-900">Deputy Commissioner / SAKALA Appellate</h3>
            <p className="text-xs text-gray-500 leading-normal">
              Direct summon, penalty charge sheet under Section 12-B.
            </p>
          </div>
          <div className="pl-2 pt-3 mt-3 border-t border-red-100 text-[11px] text-red-700 font-semibold flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-red-500 animate-ping"></span>
            <span>142 Active cases under DC direct purview</span>
          </div>
        </div>
      </div>

      {/* Search & Filter Bar */}
      <section className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs space-y-3">
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by Docket #, Officer, Department, or Reason..."
              className="w-full bg-gray-50 border border-gray-200 rounded-xl pl-9 pr-3 py-1.5 text-xs text-gray-900 outline-none focus:bg-white focus:border-primary transition-colors"
            />
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-1.5 pt-1">
          <button
            type="button"
            onClick={() => setActiveTier('ALL')}
            className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
              activeTier === 'ALL'
                ? 'bg-primary text-white shadow-2xs'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            All Active Breaches ({escalations.length})
          </button>
          <button
            type="button"
            onClick={() => setActiveTier('TIER_1')}
            className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
              activeTier === 'TIER_1'
                ? 'bg-teal text-white shadow-2xs'
                : 'bg-teal/10 text-teal hover:bg-teal/20'
            }`}
          >
            Tier 1 (Ward Escalated)
          </button>
          <button
            type="button"
            onClick={() => setActiveTier('TIER_2')}
            className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
              activeTier === 'TIER_2'
                ? 'bg-amber-600 text-white shadow-2xs'
                : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
            }`}
          >
            Tier 2 (Supervisory AEE)
          </button>
          <button
            type="button"
            onClick={() => setActiveTier('TIER_3')}
            className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
              activeTier === 'TIER_3'
                ? 'bg-red-600 text-white shadow-2xs'
                : 'bg-red-50 text-red-700 hover:bg-red-100'
            }`}
          >
            Tier 3 (Statutory Fine)
          </button>
        </div>
      </section>

      {/* Escalation Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {filtered.map((item) => (
          <article
            key={item.id}
            className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs relative overflow-hidden space-y-3 group hover:border-primary transition-all flex flex-col justify-between"
          >
            <div
              className={`absolute left-0 top-0 bottom-0 w-2 ${
                item.escalationLevel === 3
                  ? 'bg-red-600'
                  : item.escalationLevel === 2
                  ? 'bg-amber-500'
                  : 'bg-teal'
              }`}
            ></div>

            <div className="pl-2 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="font-mono font-bold text-primary text-xs">{item.trackingId}</span>
                  <span className="text-[10px] bg-gray-100 text-gray-700 font-semibold px-2 py-0.5 rounded">
                    {item.departmentName}
                  </span>
                </div>

                <span
                  className={`text-[10px] font-extrabold px-2.5 py-0.5 rounded-full flex items-center gap-1 ${
                    item.escalationLevel === 3
                      ? 'bg-red-100 text-red-700'
                      : 'bg-amber-100 text-amber-800'
                  }`}
                >
                  <Gavel className="w-3 h-3" />
                  <span>Tier {item.escalationLevel} Breach</span>
                </span>
              </div>

              <div>
                <p className="text-xs text-red-600 font-medium mt-0.5 flex items-center gap-1">
                  <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                  <span>{item.triggerReason}</span>
                </p>
                {item.notes && (
                  <p className="text-[11px] text-gray-600 mt-1 italic">
                    Note: {item.notes}
                  </p>
                )}
              </div>

              <div className="p-3 rounded-xl bg-gray-50 border border-gray-100 text-xs space-y-1">
                <div className="flex items-center justify-between">
                  <span className="text-gray-500 text-[11px]">Escalated From</span>
                  <span className="font-bold text-gray-800">{item.escalatedFromRole}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-500 text-[11px]">Escalated To</span>
                  <span className="font-semibold text-primary">{item.escalatedToOfficerName || item.escalatedToRole}</span>
                </div>
              </div>
            </div>

            <div className="pl-2 pt-3 border-t border-gray-100 flex items-center justify-between">
              <span className="text-[10px] text-gray-400">
                Triggered on: {new Date(item.triggerTimestamp).toLocaleDateString()}
              </span>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => handleIssueNotice(item.trackingId)}
                  className="px-3 py-1.5 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs font-bold hover:bg-red-100 transition-colors"
                >
                  Issue Notice
                </button>
                <button
                  type="button"
                  onClick={() => navigate(`/complaints/${item.complaintId}`)}
                  className="px-3 py-1.5 rounded-lg bg-primary text-white text-xs font-bold hover:bg-primary-hover transition-colors flex items-center gap-1 shadow-2xs"
                >
                  <span>Command Center</span>
                  <ArrowRight className="w-3 h-3" />
                </button>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
};
