import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Shield,
  Lock,
  User,
  KeyRound,
  CheckCircle,
  AlertCircle,
  ExternalLink,
  PhoneCall,
  Gavel,
  Fingerprint,
} from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { t, i18n } = useTranslation();
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('pass');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const authUser = await login({ username, password });
      const targetPath =
        location.state?.from?.pathname ||
        (authUser.role === 'ADMIN'
          ? '/admin'
          : authUser.role === 'SUPERVISOR'
          ? '/dashboard'
          : '/officer');
      navigate(targetPath, { replace: true });
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          'Authentication failed. Please check credentials or network connection.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleQuickDemoLogin = (demoUser: string) => {
    setUsername(demoUser);
    setPassword('pass');
  };

  const currentLang = i18n.language?.startsWith('kn') ? 'kn' : 'en';

  return (
    <div className="min-h-[calc(100vh-100px)] flex items-center justify-center p-2 sm:p-4">
      <div className="w-full max-w-5xl min-h-[620px] flex flex-col lg:flex-row shadow-2xl rounded-2xl overflow-hidden bg-white border border-gray-200">
        {/* LEFT PANEL: Institutional Vision & Brand Authority */}
        <div className="relative w-full lg:w-1/2 bg-gradient-to-br from-[#1e1b4b] via-indigo-900 to-indigo-950 text-white flex flex-col justify-between p-8 lg:p-12 overflow-hidden">
          {/* Subtle Decorative Geometry */}
          <div className="absolute -top-32 -left-32 w-80 h-80 rounded-full bg-teal-500/10 blur-3xl pointer-events-none" />
          <div className="absolute -bottom-24 -right-24 w-80 h-80 rounded-full bg-indigo-500/20 blur-3xl pointer-events-none" />

          {/* Top Brand Header Block */}
          <div className="relative z-10 space-y-6">
            <div className="flex items-center justify-between flex-wrap gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-white/10 backdrop-blur-md flex items-center justify-center text-teal-300 shadow-xs border border-white/10">
                  <Shield className="w-5 h-5 text-amber-300" />
                </div>
                <div>
                  <div className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                    <span>ResolveTrack</span>
                    <span className="text-[10px] px-2 py-0.5 rounded bg-teal-500/20 text-teal-200 font-semibold uppercase tracking-wider border border-teal-500/30">
                      GovNet
                    </span>
                  </div>
                  <p className="text-[11px] font-medium tracking-wider text-indigo-200 uppercase">
                    Government of Karnataka • ಕರ್ನಾಟಕ ಸರ್ಕಾರ
                  </p>
                </div>
              </div>

              {/* K-SWAN Status Node */}
              <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-xs border border-white/10 text-xs">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-teal-400 opacity-75" />
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-teal-400" />
                </span>
                <span className="text-teal-100 text-[11px] font-medium">K-SWAN Secured</span>
              </div>
            </div>

            {/* Tagline & Bilingual Hero Statement */}
            <div className="pt-4 space-y-2">
              <h1 className="text-2xl sm:text-3xl font-extrabold text-white leading-tight">
                No complaint forgotten.<br />
                <span className="text-teal-300">No follow-up missed.</span>
              </h1>
              <p className="text-sm font-medium text-indigo-200">
                ಯಾವುದೇ ದೂರು ಮರೆಯಲಾಗುವುದಿಲ್ಲ. ಯಾವುದೇ ಅನುಸರಣೆ ತಪ್ಪಿಹೋಗುವುದಿಲ್ಲ.
              </p>
              <p className="text-xs text-indigo-300/80 pt-2 leading-relaxed">
                Automated multi-tier statutory escalation and tamper-evident audit trail guaranteed under Karnataka SAKALA Services Act, 2011.
              </p>
            </div>
          </div>

          {/* SAKALA Institutional Badge */}
          <div className="relative z-10 py-6">
            <div className="bg-white/10 backdrop-blur-md rounded-xl p-4 border border-white/10 flex items-center gap-4 shadow-lg">
              <div className="w-12 h-12 rounded-xl bg-teal-500/20 text-teal-300 flex items-center justify-center shrink-0 border border-teal-500/30">
                <CheckCircle className="w-6 h-6" />
              </div>
              <div className="space-y-0.5">
                <div className="text-[10px] uppercase font-bold tracking-wider text-teal-300">
                  Statutory SLA Guarantee
                </div>
                <h3 className="text-sm font-bold text-white">SAKALA Redressal Framework</h3>
                <p className="text-[11px] text-indigo-200/90 leading-tight">
                  Automated Stage-I to Secretariat auto-breach escalation sequence active.
                </p>
              </div>
            </div>
          </div>

          {/* Bottom Trust Markers */}
          <div className="relative z-10 grid grid-cols-3 gap-2 pt-2 border-t border-white/10 text-[11px] text-indigo-200">
            <div className="flex items-center gap-2">
              <Lock className="w-4 h-4 text-teal-300 shrink-0" />
              <span>256-Bit SSL<br /><strong className="text-white">Encrypted</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <Gavel className="w-4 h-4 text-teal-300 shrink-0" />
              <span>SAKALA Sec 14<br /><strong className="text-white">Enforced</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <Fingerprint className="w-4 h-4 text-teal-300 shrink-0" />
              <span>GovNet SSO<br /><strong className="text-white">Ready</strong></span>
            </div>
          </div>
        </div>

        {/* RIGHT PANEL: Authentication Form */}
        <div className="w-full lg:w-1/2 bg-surface p-6 sm:p-10 lg:p-12 flex flex-col justify-between">
          {/* Top Bar with Helpline and Language Switcher */}
          <div className="flex items-center justify-between flex-wrap gap-3 pb-4">
            <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface-low border border-gray-200 text-gray-600 text-xs shadow-2xs">
              <PhoneCall className="w-3.5 h-3.5 text-teal-600" />
              <span>Helpline: <strong className="text-gray-900 font-bold">1800-425-0012</strong></span>
            </div>

            <div className="bg-gray-200/70 p-0.5 rounded-lg flex items-center text-xs">
              <button
                type="button"
                onClick={() => i18n.changeLanguage('en')}
                className={`px-2.5 py-1 rounded font-semibold transition-all ${
                  currentLang === 'en'
                    ? 'bg-white text-primary-container shadow-xs'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                English
              </button>
              <button
                type="button"
                onClick={() => i18n.changeLanguage('kn')}
                className={`px-2.5 py-1 rounded font-semibold transition-all ${
                  currentLang === 'kn'
                    ? 'bg-white text-primary-container shadow-xs'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                ಕನ್ನಡ
              </button>
            </div>
          </div>

          {/* Form */}
          <div className="my-auto max-w-sm w-full mx-auto space-y-5">
            <div>
              <h2 className="text-xl font-bold text-gray-900 tracking-tight">
                {t('auth.loginTitle', 'Official Gateway Sign-In')}
              </h2>
              <p className="text-xs text-gray-500 mt-1">
                {t('auth.loginSubtitle', 'Access restricted to authorized Karnataka administrative and field officers.')}
              </p>
            </div>

            {error && (
              <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-xs text-red-700 flex items-start gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-600 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  {t('auth.username', 'Government Official ID / Username')}
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                    <User className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    required
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="e.g., admin, officer_rajesh"
                    className="w-full pl-9 pr-3 py-2 text-xs rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-primary-container focus:border-primary-container bg-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">
                  {t('auth.password', 'Password')}
                </label>
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                    <KeyRound className="w-4 h-4" />
                  </div>
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-9 pr-3 py-2 text-xs rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-primary-container focus:border-primary-container bg-white"
                  />
                </div>
              </div>

              {/* Quick Demo Fill Buttons */}
              <div className="pt-1">
                <span className="text-[11px] text-gray-500 font-medium">Quick Demo Profiles:</span>
                <div className="flex items-center gap-1.5 mt-1.5">
                  <button
                    type="button"
                    onClick={() => handleQuickDemoLogin('admin')}
                    className="px-2.5 py-1 rounded-lg bg-indigo-50 border border-indigo-200 text-indigo-700 text-[11px] font-semibold hover:bg-indigo-100 transition-colors"
                  >
                    Admin (IAS)
                  </button>
                  <button
                    type="button"
                    onClick={() => handleQuickDemoLogin('sup_ramesh')}
                    className="px-2.5 py-1 rounded-lg bg-teal-50 border border-teal-200 text-teal-800 text-[11px] font-semibold hover:bg-teal-100 transition-colors"
                  >
                    Supervisor (SP)
                  </button>
                  <button
                    type="button"
                    onClick={() => handleQuickDemoLogin('officer_rajesh')}
                    className="px-2.5 py-1 rounded-lg bg-purple-50 border border-purple-200 text-purple-700 text-[11px] font-semibold hover:bg-purple-100 transition-colors"
                  >
                    Field Officer
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full py-2.5 px-4 rounded-xl bg-primary-container text-white text-xs font-semibold hover:bg-primary transition-all shadow-sm flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
              >
                {isSubmitting ? (
                  <span className="inline-flex items-center gap-2">
                    <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    Authenticating...
                  </span>
                ) : (
                  <span>{t('auth.submit', 'Authenticate via GovNet')}</span>
                )}
              </button>
            </form>
          </div>

          {/* Footer Link to Public Citizen Portal */}
          <div className="pt-6 border-t border-gray-200 text-center">
            <Link
              to="/track"
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary-container hover:underline"
            >
              <span>{t('auth.citizenLoginLink', 'Are you a citizen? Track or file grievance without sign-in')}</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
