import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Shield, Globe, Landmark, LogIn, LogOut } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { t, i18n } = useTranslation();
  const location = useLocation();

  const currentLang = i18n.language;
  const toggleLanguage = () => {
    const nextLang = currentLang.startsWith('kn') ? 'en' : 'kn';
    i18n.changeLanguage(nextLang);
  };

  const authData = localStorage.getItem('resolvetrack_auth');
  const user = authData ? JSON.parse(authData) : null;

  const handleLogout = () => {
    localStorage.removeItem('resolvetrack_auth');
    window.location.href = '/';
  };

  const navLinks = [
    { to: '/', label: t('nav.home') },
    { to: '/file', label: t('nav.fileGrievance') },
    { to: '/track', label: t('nav.trackGrievance') },
    { to: '/officer', label: t('nav.officerPortal') },
    { to: '/escalations', label: t('nav.escalationHub') },
  ];

  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
      {/* Top Gov Banner */}
      <div className="bg-primary-container text-white py-1 px-4 text-xs">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Landmark className="w-3.5 h-3.5 text-yellow-300" />
            <span className="font-medium tracking-wide">
              {t('app.govTitle')} • {t('app.name')}
            </span>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1.5 text-secondary-container">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              <span className="text-[11px] font-semibold">{t('app.kswanNotice')}</span>
            </div>
            <button
              onClick={toggleLanguage}
              className="flex items-center gap-1 bg-white/10 hover:bg-white/20 px-2 py-0.5 rounded text-[11px] transition"
            >
              <Globe className="w-3 h-3" />
              <span>{t('nav.language')}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Main Navigation */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Portal identity */}
          <Link to="/" className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-lg bg-primary text-white flex items-center justify-center shadow-md">
              <Shield className="w-6 h-6 text-yellow-300" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-lg text-primary tracking-tight">ResolveTrack</span>
                <span className="text-[10px] uppercase font-bold px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 border border-amber-300">
                  SAKALA
                </span>
              </div>
              <p className="text-[11px] text-gray-500 font-medium">Govt. of Karnataka Redressal Portal</p>
            </div>
          </Link>

          {/* Links */}
          <nav className="hidden md:flex items-center space-x-1">
            {navLinks.map((link) => {
              const active = location.pathname === link.to;
              return (
                <Link
                  key={link.to}
                  to={link.to}
                  className={`px-3 py-2 rounded-md text-sm font-medium transition ${
                    active
                      ? 'bg-primary-light/40 text-primary font-semibold'
                      : 'text-gray-600 hover:text-primary hover:bg-gray-50'
                  }`}
                >
                  {link.label}
                </Link>
              );
            })}
          </nav>

          {/* User Auth or Sign In button */}
          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-3">
                <span className="text-xs font-medium text-gray-700 bg-gray-100 px-2.5 py-1 rounded">
                  {user.fullName || user.username} ({user.role?.replace('ROLE_', '')})
                </span>
                <button
                  onClick={handleLogout}
                  className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 rounded-md transition"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  <span>{t('nav.signOut')}</span>
                </button>
              </div>
            ) : (
              <Link
                to="/login"
                className="inline-flex items-center gap-1.5 px-4 py-2 rounded-md bg-primary text-white text-xs font-semibold hover:bg-primary-dark transition shadow-sm"
              >
                <LogIn className="w-4 h-4" />
                <span>{t('nav.signIn')}</span>
              </Link>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

