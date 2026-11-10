import React from 'react';
import { useTranslation } from 'react-i18next';
import { ShieldCheck, Scale, FileText } from 'lucide-react';

export const Footer: React.FC = () => {
  const { t } = useTranslation();

  return (
    <footer className="bg-slate-900 text-slate-300 border-t border-slate-800 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <ShieldCheck className="w-5 h-5 text-emerald-400" />
              <span className="font-bold text-white text-base">ResolveTrack GovPortal</span>
            </div>
            <p className="text-xs text-slate-400 leading-relaxed">
              {t('app.sakalaNotice')}
            </p>
            <p className="text-xs text-amber-400/90 mt-2 font-medium">
              {t('app.portalTagline')}
            </p>
          </div>

          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-200 mb-3 flex items-center gap-1.5">
              <Scale className="w-4 h-4 text-primary-light" />
              Statutory Governance Principles
            </h4>
            <ul className="text-xs text-slate-400 space-y-2">
              <li>• Business logic for deadlines and escalation operates strictly in verified backend domain services.</li>
              <li>• AI assistance is limited to categorization suggestions requiring official human confirmation.</li>
              <li>• Automated escalation triggers supervisory notifications and logs audit history—never decides penal/FIR matters.</li>
              <li>• Citizens' data, internal deliberations, and officer identities are protected by design.</li>
            </ul>
          </div>

          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-200 mb-3 flex items-center gap-1.5">
              <FileText className="w-4 h-4 text-emerald-400" />
              State Helplines & Links
            </h4>
            <p className="text-xs text-slate-400 leading-relaxed mb-3">
              Karnataka Guarantee of Services to Citizens Act (SAKALA) Helpline: <strong>080-44554455</strong>
            </p>
            <div className="text-[11px] text-slate-500">
              © {new Date().getFullYear()} Government of Karnataka • All Rights Reserved.
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
};

