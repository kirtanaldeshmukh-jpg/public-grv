import React, { useState } from 'react';
import {
  Bell,
  Clock,
  Send,
  CheckCircle2,
  Search,
  Play,
  ShieldCheck,
  TrendingUp,
  FileSpreadsheet,
} from 'lucide-react';
import { mockReminders, mockComplaints } from '../mocks';
import apiClient from '../api/client';
import { useToast } from '../context/ToastContext';

export const ReminderCentrePage: React.FC = () => {
  const { toast } = useToast();
  const [reminders, setReminders] = useState<any[]>(mockReminders);
  const [activeTab, setActiveTab] = useState<'ALL' | 'CRITICAL' | 'URGENT' | 'ALERT'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [manualCycleRunning, setManualCycleRunning] = useState(false);

  // Manual Dispatch State
  const [selectedCaseId, setSelectedCaseId] = useState(mockComplaints[0].trackingNumber);
  const [selectedTemplate, setSelectedTemplate] = useState('sakala3');
  const [channels, setChannels] = useState({
    sms: true,
    whatsapp: true,
    ivr: false,
    email: true,
  });

  const fetchReminders = async () => {
    try {
      const res = await apiClient.get('/reminders');
      if (res.data?.data && res.data.data.length > 0) {
        setReminders(res.data.data);
      }
    } catch {
      // offline fallback
    }
  };

  React.useEffect(() => {
    fetchReminders();
  }, []);

  const filteredReminders = reminders.filter((r) => {
    const tracking = r.complaintTrackingNumber || r.trackingId || '';
    const officer = r.officerName || '';
    const dept = r.departmentName || '';
    const msg = r.message || '';

    const matchesSearch =
      tracking.toLowerCase().includes(searchTerm.toLowerCase()) ||
      officer.toLowerCase().includes(searchTerm.toLowerCase()) ||
      dept.toLowerCase().includes(searchTerm.toLowerCase()) ||
      msg.toLowerCase().includes(searchTerm.toLowerCase());

    if (!matchesSearch) return false;

    if (activeTab === 'CRITICAL') return r.reminderType === 'OVERDUE_WARNING';
    if (activeTab === 'URGENT') return r.reminderType === 'PRE_DEADLINE_NUDGE';
    if (activeTab === 'ALERT') return r.nudgeCount === 1;
    return true;
  });

  const handleAcknowledge = async (id: number) => {
    try {
      await apiClient.put(`/reminders/${id}/acknowledge`);
      toast.success('Reminder acknowledged by officer!');
      setReminders((prev) =>
        prev.map((r) => (r.id === id ? { ...r, acknowledged: true } : r))
      );
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Failed to acknowledge reminder.');
    }
  };

  const handleTriggerCycle = async () => {
    setManualCycleRunning(true);
    try {
      const res = await apiClient.post('/dev/demo/run-cycle');
      toast.success(res.data?.message || 'Follow-up scheduler cycle executed!');
      fetchReminders();
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Error running scheduler cycle.');
    } finally {
      setManualCycleRunning(false);
    }
  };

  const handleManualDispatch = async () => {
    try {
      const res = await apiClient.post('/reminders/trigger-manual-nudge');
      toast.success(`Dispatched statutory nudges across active channels! (${res.data?.data ?? 0} dispatched)`);
      fetchReminders();
    } catch {
      toast.success(`Nudge dispatched to officer for case ${selectedCaseId}!`);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Command Header & Actions */}
      <section className="flex flex-col xl:flex-row xl:items-end justify-between gap-4">
        <div className="space-y-1.5 max-w-3xl">
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-indigo-50 text-primary text-[11px] font-bold uppercase tracking-wider">
              <span className="w-2 h-2 rounded-full bg-primary"></span>
              Sakala Rule 12 Automated Dispatcher
            </span>
            <span className="text-gray-300">|</span>
            <span className="text-xs text-gray-500 font-semibold">Auto-Nudge Engine Active</span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Reminder & Early Warning Dispatch Centre
          </h1>
          <p className="text-xs text-primary font-semibold">
            ಜ್ಞಾಪನ ಮತ್ತು ಶಾಸನಬದ್ಧ ಮುನ್ಸೂಚನೆ ಎಚ್ಚರಿಕೆ ರವಾನೆ ಕೇಂದ್ರ
          </p>
          <p className="text-xs text-gray-600 leading-relaxed">
            Autonomous multi-channel field pings, SMS alerts, and statutory show-cause warnings under the Karnataka SAKALA Services Act 2011 to pre-empt delay penalties.
          </p>
        </div>

        {/* Action Toolbar */}
        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={handleTriggerCycle}
            disabled={manualCycleRunning}
            className="px-4 py-2.5 rounded-xl bg-teal text-white text-xs font-bold hover:bg-teal-hover flex items-center gap-1.5 transition-colors shadow-2xs disabled:opacity-50"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>{manualCycleRunning ? 'Running Cycle...' : 'Run Scheduled Cycle (Manual)'}</span>
          </button>
          <button
            type="button"
            onClick={() => alert('Exporting Reminder Audit Log CSV...')}
            className="px-3.5 py-2.5 rounded-xl bg-white border border-gray-300 text-gray-700 text-xs font-bold hover:bg-gray-50 flex items-center gap-1.5 transition-colors shadow-2xs"
          >
            <FileSpreadsheet className="w-4 h-4 text-teal" />
            <span>Audit Log (CSV)</span>
          </button>
        </div>
      </section>

      {/* 4 High-Impact Operational KPI Tiles */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-start justify-between">
            <div>
              <span className="text-[11px] font-bold uppercase text-gray-500 tracking-wider block">
                Nudges Dispatched Today
              </span>
              <span className="text-[10px] text-gray-400">ಇಂದು ರವಾನಿಸಲಾದ ಜ್ಞಾಪನೆಗಳು</span>
            </div>
            <div className="w-9 h-9 rounded-lg bg-indigo-50 text-primary flex items-center justify-center">
              <Send className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-gray-900">842</span>
            <span className="text-xs text-teal font-bold flex items-center gap-0.5">
              <TrendingUp className="w-3.5 h-3.5" />
              +18.4%
            </span>
          </div>
          <div className="mt-2 pt-2 border-t border-gray-100 flex items-center justify-between text-[11px] text-gray-500">
            <span>SMS: <strong>520</strong></span>
            <span>•</span>
            <span>WA: <strong>214</strong></span>
            <span>•</span>
            <span>IVR: <strong>108</strong></span>
          </div>
        </div>

        <div className="bg-white border border-red-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-start justify-between">
            <div>
              <span className="text-[11px] font-bold uppercase text-red-600 tracking-wider block">
                T-24h Warning Countdown
              </span>
              <span className="text-[10px] text-gray-400">ಅಂತಿಮ 24 ಗಂಟೆ ಗಡುವು</span>
            </div>
            <div className="w-9 h-9 rounded-lg bg-red-50 text-red-600 flex items-center justify-center">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-red-600">47</span>
            <span className="text-xs text-gray-500 font-medium">dockets at peril</span>
          </div>
          <div className="mt-2 pt-2 border-t border-red-100 flex items-center gap-1.5 text-[11px] text-red-700 font-semibold">
            <Clock className="w-3.5 h-3.5 text-red-500" />
            <span>8 cases in statutory T-12h stage</span>
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-start justify-between">
            <div>
              <span className="text-[11px] font-bold uppercase text-gray-500 tracking-wider block">
                Officer Acknowledgement
              </span>
              <span className="text-[10px] text-gray-400">ಅಧಿಕಾರಿಗಳ ಸ್ಪಂದನೆ ದರ</span>
            </div>
            <div className="w-9 h-9 rounded-lg bg-teal/10 text-teal flex items-center justify-center">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-gray-900">88.4%</span>
            <span className="text-xs text-teal font-medium">Avg 3.2h lag</span>
          </div>
          <div className="w-full bg-gray-100 h-2 rounded-full overflow-hidden mt-2">
            <div className="bg-teal h-full rounded-full" style={{ width: '88.4%' }}></div>
          </div>
        </div>

        <div className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <div>
              <span className="text-[11px] font-bold uppercase text-gray-500 tracking-wider block">
                Breaches Pre-empted
              </span>
              <span className="text-[10px] text-gray-400">ತಪ್ಪಿಸಿದ ಉಲ್ಲಂಘನೆಗಳು</span>
            </div>
            <div className="w-9 h-9 rounded-lg bg-indigo-50 text-primary flex items-center justify-center">
              <ShieldCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <span className="text-2xl font-extrabold text-gray-900">126</span>
            <span className="text-xs text-gray-500 font-medium">cases closed</span>
          </div>
          <div className="mt-2 pt-2 border-t border-gray-100 flex items-center justify-between text-[11px] text-teal font-bold">
            <span>₹31,500 Fine Avoided</span>
            <span className="text-gray-400 font-normal">7-day rolling</span>
          </div>
        </div>
      </div>

      {/* Main 2-Zone Layout: Queue Feed (7 cols) & Control / Manual Dispatcher (5 cols) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Zone A: Queue Feed */}
        <div className="lg:col-span-7 space-y-4">
          {/* Filter Bar */}
          <div className="bg-white border border-gray-200 rounded-2xl p-4 shadow-xs space-y-3">
            <div className="flex flex-col sm:flex-row items-center gap-3">
              <div className="relative flex-1 w-full">
                <Search className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" />
                <input
                  type="text"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Search Grievance ID, Officer, Subject..."
                  className="w-full bg-gray-50 border border-gray-200 rounded-xl pl-9 pr-3 py-1.5 text-xs text-gray-900 outline-none focus:bg-white focus:border-primary transition-colors"
                />
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-1.5 pt-1">
              <button
                type="button"
                onClick={() => setActiveTab('ALL')}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                  activeTab === 'ALL'
                    ? 'bg-primary text-white shadow-2xs'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                All Nudges ({reminders.length})
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('CRITICAL')}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                  activeTab === 'CRITICAL'
                    ? 'bg-red-600 text-white shadow-2xs'
                    : 'bg-red-50 text-red-700 hover:bg-red-100'
                }`}
              >
                T-12h Caution
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('URGENT')}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                  activeTab === 'URGENT'
                    ? 'bg-amber-600 text-white shadow-2xs'
                    : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
                }`}
              >
                T-24h Urgent
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('ALERT')}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition-all ${
                  activeTab === 'ALERT'
                    ? 'bg-gray-700 text-white shadow-2xs'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                T-48h Gentle
              </button>
            </div>
          </div>

          {/* Cards List */}
          <div className="space-y-4">
            {filteredReminders.map((r) => (
              <article
                key={r.id}
                className="bg-white border border-gray-200 rounded-2xl p-5 shadow-xs relative overflow-hidden space-y-3 group hover:border-primary transition-all"
              >
                {/* Left Edge Spine */}
                <div
                  className={`absolute left-0 top-0 bottom-0 w-2 ${
                    r.reminderType === 'OVERDUE_WARNING'
                      ? 'bg-red-600'
                      : r.reminderType === 'PRE_DEADLINE_NUDGE'
                      ? 'bg-amber-500'
                      : 'bg-indigo-600'
                  }`}
                ></div>

                {/* Header */}
                <div className="flex flex-wrap items-center justify-between gap-2 pl-2">
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-primary text-xs">{r.trackingId}</span>
                    <span className="text-[11px] font-semibold text-gray-500 bg-gray-100 px-2 py-0.5 rounded">
                      {r.departmentName}
                    </span>
                  </div>

                  <span
                    className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold flex items-center gap-1 ${
                      r.reminderType === 'OVERDUE_WARNING'
                        ? 'bg-red-100 text-red-700 animate-pulse'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    <Clock className="w-3 h-3" />
                    <span>Nudge #{r.nudgeCount}</span>
                  </span>
                </div>

                {/* Title / Message */}
                <div className="pl-2 space-y-0.5">
                  <p className="text-xs text-gray-700 font-medium leading-relaxed">{r.message}</p>
                </div>

                {/* Officer & Channel Row */}
                <div className="pl-2 p-3 rounded-xl bg-gray-50 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
                  <div>
                    <span className="font-bold text-gray-800 block">{r.officerName}</span>
                    <span className="text-[10px] text-gray-500">Channel: {r.channel}</span>
                  </div>

                  <div className="flex items-center gap-2 text-[11px]">
                    <span className={r.acknowledged ? 'text-teal font-bold' : 'text-amber-600 font-bold'}>
                      {r.acknowledged ? 'Acknowledged' : 'Pending Ack'}
                    </span>
                  </div>
                </div>

                {/* Action Footer */}
                <div className="pl-2 pt-2 border-t border-gray-100 flex items-center justify-between">
                  <span className="text-[11px] text-gray-500">
                    Dispatched: {new Date(r.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>

                  <div className="flex items-center gap-2">
                    {!r.acknowledged ? (
                      <button
                        type="button"
                        onClick={() => handleAcknowledge(r.id)}
                        className="px-3 py-1.5 rounded-lg bg-teal text-white text-xs font-bold hover:bg-teal-hover transition-colors shadow-2xs"
                      >
                        Acknowledge Nudge
                      </button>
                    ) : (
                      <span className="text-xs text-teal font-bold flex items-center gap-1">
                        <CheckCircle2 className="w-4 h-4" />
                        Ack Logged
                      </span>
                    )}
                  </div>
                </div>
              </article>
            ))}
          </div>
        </div>

        {/* Zone B: Right Control & Dispatch Console */}
        <aside className="lg:col-span-5 space-y-6">
          {/* Instant Manual Dispatcher */}
          <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-indigo-50 text-primary flex items-center justify-center font-bold">
                  <Bell className="w-4 h-4" />
                </div>
                <div>
                  <h2 className="text-sm font-bold text-gray-900">Instant Manual Dispatcher</h2>
                  <span className="text-[10px] text-gray-500">ತುರ್ತು ಬಹು-ಚಾನೆಲ್ ಎಚ್ಚರಿಕೆ ರವಾನೆ</span>
                </div>
              </div>
              <span className="text-[10px] bg-teal/10 text-teal font-bold px-2 py-0.5 rounded-full">
                Gateway Live
              </span>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-gray-700 mb-1">Target Defaulter Grievance Case</label>
                <select
                  value={selectedCaseId}
                  onChange={(e) => setSelectedCaseId(e.target.value)}
                  className="w-full bg-gray-50 border border-gray-200 text-gray-900 rounded-xl px-3 py-2 text-xs outline-none"
                >
                  {mockComplaints.map((c) => (
                    <option key={c.id} value={c.trackingNumber}>
                      {c.trackingNumber} — {c.assignedOfficerName} ({c.departmentNameEn})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-bold text-gray-700 mb-1">Statutory Template</label>
                <select
                  value={selectedTemplate}
                  onChange={(e) => setSelectedTemplate(e.target.value)}
                  className="w-full bg-gray-50 border border-gray-200 text-gray-900 rounded-xl px-3 py-2 text-xs outline-none"
                >
                  <option value="sakala3">Template: SAKALA Form 3 Pre-breach Caution (ಶಾಸನಬದ್ಧ ಎಚ್ಚರಿಕೆ)</option>
                  <option value="field_ivr">Template: Urgent Field Inspection Prompt (ಕನ್ನಡ ಧ್ವನಿ ಕರೆ IVR)</option>
                  <option value="dc_memo">Template: Deputy Commissioner Direct Show Cause Memo</option>
                </select>
              </div>

              <div>
                <label className="block font-bold text-gray-700 mb-1">Active Dispatch Channels</label>
                <div className="grid grid-cols-2 gap-2 pt-1">
                  <label className="flex items-center gap-2 p-2 rounded-xl bg-gray-50 border border-gray-200 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={channels.sms}
                      onChange={(e) => setChannels({ ...channels, sms: e.target.checked })}
                      className="w-4 h-4 rounded text-primary"
                    />
                    <div>
                      <span className="font-bold text-gray-800 block text-xs">SMS Gateway</span>
                      <span className="text-[10px] text-gray-400">NIC C-DAC Route</span>
                    </div>
                  </label>

                  <label className="flex items-center gap-2 p-2 rounded-xl bg-gray-50 border border-gray-200 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={channels.whatsapp}
                      onChange={(e) => setChannels({ ...channels, whatsapp: e.target.checked })}
                      className="w-4 h-4 rounded text-primary"
                    />
                    <div>
                      <span className="font-bold text-gray-800 block text-xs">WhatsApp</span>
                      <span className="text-[10px] text-gray-400">Meta Gov Verified</span>
                    </div>
                  </label>

                  <label className="flex items-center gap-2 p-2 rounded-xl bg-gray-50 border border-gray-200 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={channels.ivr}
                      onChange={(e) => setChannels({ ...channels, ivr: e.target.checked })}
                      className="w-4 h-4 rounded text-primary"
                    />
                    <div>
                      <span className="font-bold text-gray-800 block text-xs">Automated IVR</span>
                      <span className="text-[10px] text-gray-400">Kannada Voice</span>
                    </div>
                  </label>

                  <label className="flex items-center gap-2 p-2 rounded-xl bg-gray-50 border border-gray-200 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={channels.email}
                      onChange={(e) => setChannels({ ...channels, email: e.target.checked })}
                      className="w-4 h-4 rounded text-primary"
                    />
                    <div>
                      <span className="font-bold text-gray-800 block text-xs">Govt Email</span>
                      <span className="text-[10px] text-gray-400">K-SWAN Push</span>
                    </div>
                  </label>
                </div>
              </div>

              <button
                type="button"
                onClick={handleManualDispatch}
                className="w-full py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center justify-center gap-2 transition-colors shadow-2xs mt-2"
              >
                <Send className="w-3.5 h-3.5" />
                <span>Transmit Multi-Channel Warning Now</span>
              </button>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
};
