import React from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { FilePlus2, Search, ArrowRight, Clock, AlertTriangle, CheckCircle2, ShieldAlert } from 'lucide-react';

export const HomePage: React.FC = () => {
  const { t } = useTranslation();

  const stats = [
    { label: t('home.statSla'), value: '94.8%', icon: Clock, color: 'text-indigo-600 bg-indigo-50' },
    { label: t('home.statResolved'), value: '1,42,890', icon: CheckCircle2, color: 'text-emerald-600 bg-emerald-50' },
    { label: t('home.statEscalated'), value: '412', icon: ShieldAlert, color: 'text-amber-600 bg-amber-50' },
    { label: t('home.statActive'), value: '3,840', icon: AlertTriangle, color: 'text-blue-600 bg-blue-50' },
  ];

  return (
    <div className="space-y-12 py-6">
      {/* Hero Section */}
      <section className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-[#1e1b4b] via-primary-container to-primary text-white p-8 md:p-14 shadow-xl">
        <div className="relative z-10 max-w-3xl space-y-6">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md text-xs font-semibold text-amber-300">
            <span>KARNATAKA SAKALA SERVICES ACT</span>
            <span>•</span>
            <span>GUARANTEED REDRESSAL</span>
          </div>

          <h1 className="text-3xl md:text-5xl font-extrabold tracking-tight leading-tight">
            {t('home.heroTitle')}
          </h1>

          <p className="text-base md:text-lg text-primary-light/90 leading-relaxed">
            {t('home.heroSubtitle')}
          </p>

          <div className="flex flex-wrap gap-4 pt-4">
            <Link
              to="/file"
              className="inline-flex items-center gap-2 px-6 py-3.5 rounded-lg bg-amber-400 hover:bg-amber-300 text-gray-900 font-bold text-sm shadow-lg transition"
            >
              <FilePlus2 className="w-5 h-5" />
              <span>{t('home.fileCta')}</span>
              <ArrowRight className="w-4 h-4" />
            </Link>

            <Link
              to="/track"
              className="inline-flex items-center gap-2 px-6 py-3.5 rounded-lg bg-white/10 hover:bg-white/20 text-white font-semibold text-sm border border-white/20 backdrop-blur-sm transition"
            >
              <Search className="w-5 h-5" />
              <span>{t('home.trackCta')}</span>
            </Link>
          </div>
        </div>
      </section>

      {/* Live SLA & System Metrics */}
      <section>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {stats.map((stat, i) => {
            const Icon = stat.icon;
            return (
              <div
                key={i}
                className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm hover:shadow-md transition flex items-center justify-between"
              >
                <div>
                  <p className="text-xs font-medium text-gray-500">{stat.label}</p>
                  <p className="text-2xl font-bold text-gray-900 mt-1">{stat.value}</p>
                </div>
                <div className={`p-3 rounded-lg ${stat.color}`}>
                  <Icon className="w-6 h-6" />
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Two Citizen Gateways: File & Track */}
      <section className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="bg-white border border-gray-200 rounded-xl p-8 shadow-sm hover:border-primary/50 transition">
          <div className="w-12 h-12 rounded-lg bg-amber-500/10 text-amber-600 flex items-center justify-center mb-4">
            <FilePlus2 className="w-6 h-6" />
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Lodge Public Grievance</h2>
          <p className="text-sm text-gray-600 mb-6 leading-relaxed">
            Submit issues concerning roads, electricity, civic amenities, or revenue records directly to Karnataka authorities with instant timestamp and SLA deadline.
          </p>
          <Link
            to="/file"
            className="inline-flex items-center gap-2 text-primary font-bold text-sm hover:text-primary-dark"
          >
            <span>Proceed to Registration</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        <div className="bg-white border border-gray-200 rounded-xl p-8 shadow-sm hover:border-primary/50 transition">
          <div className="w-12 h-12 rounded-lg bg-primary/10 text-primary flex items-center justify-center mb-4">
            <Search className="w-6 h-6" />
          </div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">Track Existing Grievance</h2>
          <p className="text-sm text-gray-600 mb-6 leading-relaxed">
            Check live statutory status, elapsed SLA days, and verified resolution milestones using your unique tracking number. No account required.
          </p>
          <Link
            to="/track"
            className="inline-flex items-center gap-2 text-primary font-bold text-sm hover:text-primary-dark"
          >
            <span>Track with Reference ID</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </section>
    </div>
  );
};

