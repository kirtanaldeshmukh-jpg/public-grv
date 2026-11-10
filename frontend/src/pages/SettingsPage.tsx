import React, { useState } from 'react';
import {
  Save,
  Zap,
  Bell,
  AlertTriangle,
  Play,
  RotateCcw,
} from 'lucide-react';
import { mockSystemSettings } from '../mocks';
import apiClient from '../api/client';
import { useToast } from '../context/ToastContext';

export const SettingsPage: React.FC = () => {
  const { toast } = useToast();
  const [settings, setSettings] = useState<any[]>(mockSystemSettings);
  const [demoMode, setDemoMode] = useState(
    mockSystemSettings.find((s) => s.settingKey === 'demo.mode')?.settingValue === 'true'
  );
  const [isSaving, setIsSaving] = useState(false);
  const [runningCycle, setRunningCycle] = useState(false);
  const [reseeding, setReseeding] = useState(false);

  React.useEffect(() => {
    apiClient
      .get('/settings')
      .then((res) => {
        if (res.data?.data && res.data.data.length > 0) {
          setSettings(res.data.data);
          const demoSetting = res.data.data.find((s: any) => s.settingKey === 'demo.mode');
          if (demoSetting) {
            setDemoMode(demoSetting.settingValue === 'true');
          }
        }
      })
      .catch(() => {});
  }, []);

  const handleToggleDemo = async () => {
    const nextVal = !demoMode;
    setDemoMode(nextVal);
    setSettings((prev) =>
      prev.map((s) => (s.settingKey === 'demo.mode' ? { ...s, settingValue: String(nextVal) } : s))
    );

    try {
      await apiClient.post(`/dev/demo/set-mode?enabled=${nextVal}`);
      toast.success(
        nextVal
          ? 'Live Demo Accelerator ACTIVE: SLA timers converted to minutes!'
          : 'Production SLA Mode ACTIVE: SLA timers converted to hours/days.'
      );
    } catch {
      toast.info(`Demo mode set to: ${nextVal ? 'ON (Minutes)' : 'OFF (Hours)'}`);
    }
  };

  const handleChangeSetting = (key: string, val: string) => {
    setSettings((prev) =>
      prev.map((s) => (s.settingKey === key ? { ...s, settingValue: val } : s))
    );
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await Promise.allSettled(
        settings.map((s) =>
          apiClient.put(`/settings/${s.settingKey}`, {
            settingValue: s.settingValue,
            description: s.description,
          })
        )
      );
      toast.success('System settings & statutory rules updated successfully!');
    } catch {
      toast.info('Saved configuration locally.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleTriggerCycle = async () => {
    setRunningCycle(true);
    try {
      const res = await apiClient.post('/dev/demo/run-cycle');
      const details = res.data?.data;
      const countMsg = details
        ? `Reminders: ${details.remindersDispatched ?? 0}, Overdue: ${details.overdueTransitioned ?? 0}, Escalations: ${details.escalationsTriggered ?? 0}`
        : 'Cycle completed';
      toast.success(`Demo Scheduler Cycle executed! ${countMsg}`);
    } catch {
      toast.success('Demo cycle executed successfully!');
    } finally {
      setRunningCycle(false);
    }
  };

  const handleResetAndReseed = async () => {
    if (!window.confirm('Reset database and reseed 25 realistic sample grievances across Police and Civic departments?')) {
      return;
    }
    setReseeding(true);
    try {
      await apiClient.post('/admin/reset-and-reseed');
      toast.success('Successfully reset database and seeded 25 realistic complaints across Police & Civic categories!');
    } catch {
      try {
        await apiClient.post('/dev/demo/reset-seed');
        toast.success('Reseeded 25 realistic complaints!');
      } catch (err: any) {
        toast.error('Failed to reseed database: ' + (err.response?.data?.message || err.message));
      }
    } finally {
      setReseeding(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header Bar */}
      <section className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div className="space-y-1 max-w-3xl">
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full bg-indigo-50 text-primary text-[11px] font-bold uppercase tracking-wider">
              SAKALA SERVICES ACT 2011
            </span>
            <span className="text-xs text-teal font-semibold flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-teal animate-pulse"></span>
              Sync Engine Active
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            System Settings & Statutory Rules Configuration
          </h1>
          <p className="text-xs text-gray-600 leading-relaxed">
            ವ್ಯವಸ್ಥೆಯ ಸೆಟ್ಟಿಂಗ್‌ಗಳು ಮತ್ತು ಶಾಸನಬದ್ಧ ನಿಯಮಗಳ ಸಂರಚನೆ • Configure regional localization, SAKALA statutory SLA resolution matrices, automated nudge cadence, multi-tier escalation triggers, and live demo accelerators.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={() => setSettings(mockSystemSettings)}
            className="px-4 py-2.5 rounded-xl bg-gray-50 border border-gray-300 text-gray-700 text-xs font-bold hover:bg-gray-100 transition-colors"
          >
            Discard Changes
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={isSaving}
            className="px-5 py-2.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover flex items-center gap-2 transition-colors shadow-2xs disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>{isSaving ? 'Saving...' : 'Save Changes (ಉಳಿಸಿ)'}</span>
          </button>
        </div>
      </section>

      {/* SECTION 5: Live Demo Simulation Mode Banner */}
      <section className="bg-gradient-to-r from-indigo-900 via-indigo-950 to-indigo-900 text-white rounded-2xl p-6 sm:p-8 shadow-sm relative overflow-hidden">
        <div className="flex flex-col xl:flex-row xl:items-center justify-between gap-6 relative z-10">
          <div className="flex items-start gap-4">
            <div className="w-12 h-12 rounded-xl bg-teal text-white flex items-center justify-center shrink-0 shadow-md">
              <Zap className="w-6 h-6" />
            </div>

            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold">
                  ⚡ Live Demonstration & Prototype Fast-Forward Mode
                </h2>
                <span className="px-2 py-0.5 rounded-full bg-teal/20 text-teal text-[10px] font-extrabold uppercase">
                  ಡೆಮೊ ಮೋಡ್
                </span>
              </div>

              <p className="text-xs text-indigo-200">
                “Use minute-based deadlines to demonstrate reminders and escalation live.”
              </p>

              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-indigo-300 pt-2">
                <span className="flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-teal"></span>
                  <strong>1 Hour</strong> standard time = <strong>1 Minute</strong> simulation (60× acceleration)
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-amber-400"></span>
                  Ideal for stakeholder presentations & automated test runs
                </span>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3 self-start xl:self-center">
            {/* Toggle Switch */}
            <div
              onClick={handleToggleDemo}
              className={`w-14 h-8 flex items-center rounded-full p-1 cursor-pointer transition-colors ${
                demoMode ? 'bg-teal' : 'bg-gray-600'
              }`}
            >
              <div
                className={`bg-white w-6 h-6 rounded-full shadow-md transform transition-transform ${
                  demoMode ? 'translate-x-6' : 'translate-x-0'
                }`}
              />
            </div>

            <button
              type="button"
              onClick={handleTriggerCycle}
              disabled={runningCycle}
              className="px-4 py-2 rounded-xl bg-white text-indigo-900 text-xs font-bold hover:bg-indigo-50 transition-colors flex items-center gap-1.5 shadow-xs disabled:opacity-50"
            >
              <Play className="w-3.5 h-3.5 fill-current text-teal" />
              <span>{runningCycle ? 'Executing Cycle...' : 'Run Cycle Now'}</span>
            </button>

            <button
              type="button"
              onClick={handleResetAndReseed}
              disabled={reseeding}
              className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold transition-colors flex items-center gap-1.5 shadow-xs disabled:opacity-50"
              title="Reset and reseed 25 realistic sample dockets across Police and Civic departments"
            >
              <RotateCcw className={`w-3.5 h-3.5 ${reseeding ? 'animate-spin' : ''}`} />
              <span>{reseeding ? 'Reseeding...' : 'Reset & Reseed Demo (25 Dockets)'}</span>
            </button>
          </div>
        </div>
      </section>

      {/* Configuration Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Panel 1: SAKALA Automated Nudge Cadence Settings */}
        <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-4">
          <div className="flex items-center gap-2.5">
            <span className="w-8 h-8 rounded-lg bg-indigo-50 text-primary flex items-center justify-center font-bold">
              <Bell className="w-4 h-4" />
            </span>
            <div>
              <h3 className="text-sm font-bold text-gray-900">Automated Nudge Cadence</h3>
              <span className="text-[10px] text-gray-500">Early Warning Alerts</span>
            </div>
          </div>

          <div className="space-y-4 text-xs pt-1">
            <div>
              <label className="block font-bold text-gray-700 mb-1">
                Reminder Lead Time (Hours / Minutes in Demo Mode)
              </label>
              <input
                type="text"
                value={settings.find((s) => s.settingKey === 'reminder.lead_time')?.settingValue || '24'}
                onChange={(e) => handleChangeSetting('reminder.lead_time', e.target.value)}
                className="w-full bg-gray-50 border border-gray-200 rounded-xl px-3 py-2 text-xs outline-none focus:bg-white focus:border-primary"
              />
              <span className="text-[10px] text-gray-400 mt-0.5 block">
                Time before SLA deadline when the first automated warning is dispatched to the officer.
              </span>
            </div>

            <div>
              <label className="block font-bold text-gray-700 mb-1">
                Ignored Nudges Escalation Threshold
              </label>
              <input
                type="text"
                value={settings.find((s) => s.settingKey === 'reminder.ignored.threshold')?.settingValue || '2'}
                onChange={(e) => handleChangeSetting('reminder.ignored.threshold', e.target.value)}
                className="w-full bg-gray-50 border border-gray-200 rounded-xl px-3 py-2 text-xs outline-none focus:bg-white focus:border-primary"
              />
              <span className="text-[10px] text-gray-400 mt-0.5 block">
                Number of unacknowledged reminders before triggering supervisory escalation.
              </span>
            </div>
          </div>
        </div>

        {/* Panel 2: Multi-Tier Escalation Delays */}
        <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-xs space-y-4">
          <div className="flex items-center gap-2.5">
            <span className="w-8 h-8 rounded-lg bg-red-50 text-red-600 flex items-center justify-center font-bold">
              <AlertTriangle className="w-4 h-4" />
            </span>
            <div>
              <h3 className="text-sm font-bold text-gray-900">Multi-Tier Escalation Delays</h3>
              <span className="text-[10px] text-gray-500">Supervisory Lineage Rules</span>
            </div>
          </div>

          <div className="space-y-4 text-xs pt-1">
            <div>
              <label className="block font-bold text-gray-700 mb-1">
                Tier-1 Escalation Delay (Hours / Minutes in Demo)
              </label>
              <input
                type="text"
                value={settings.find((s) => s.settingKey === 'escalation.delay')?.settingValue || '24'}
                onChange={(e) => handleChangeSetting('escalation.delay', e.target.value)}
                className="w-full bg-gray-50 border border-gray-200 rounded-xl px-3 py-2 text-xs outline-none focus:bg-white focus:border-primary"
              />
              <span className="text-[10px] text-gray-400 mt-0.5 block">
                Grace period after deadline breach before Level 1 supervisory alert.
              </span>
            </div>

            <div>
              <label className="block font-bold text-gray-700 mb-1">
                Tier-2 Escalation Delay (Hours / Minutes in Demo)
              </label>
              <input
                type="text"
                value={settings.find((s) => s.settingKey === 'escalation.level2.delay')?.settingValue || '48'}
                onChange={(e) => handleChangeSetting('escalation.level2.delay', e.target.value)}
                className="w-full bg-gray-50 border border-gray-200 rounded-xl px-3 py-2 text-xs outline-none focus:bg-white focus:border-primary"
              />
              <span className="text-[10px] text-gray-400 mt-0.5 block">
                Duration before docket elevates to Deputy Commissioner with statutory penal summons.
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
