import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute';

// All 12 Pages
import { LoginPage } from './pages/LoginPage';
import { HomePage } from './pages/HomePage';
import { FileGrievancePage } from './pages/FileGrievancePage';
import { TrackComplaintPage } from './pages/TrackComplaintPage';
import { OfficerDashboardPage } from './pages/OfficerDashboardPage';
import { ComplaintDetailsPage } from './pages/ComplaintDetailsPage';
import { ReminderCentrePage } from './pages/ReminderCentrePage';
import { EscalationCentrePage } from './pages/EscalationCentrePage';
import { ActionHistoryPage } from './pages/ActionHistoryPage';
import { AdminDashboardPage } from './pages/AdminDashboardPage';
import { DepartmentsPage } from './pages/DepartmentsPage';
import { SettingsPage } from './pages/SettingsPage';
import { AnalyticsPage } from './pages/AnalyticsPage';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
          {/* Public Authentication Route */}
          <Route path="/login" element={<LoginPage />} />

          {/* App Shell Layout */}
          <Route element={<AppLayout />}>
            {/* Public Citizen Pages */}
            <Route path="/" element={<HomePage />} />
            <Route path="/file" element={<FileGrievancePage />} />
            <Route path="/track" element={<TrackComplaintPage />} />

            {/* Official Protected Portals */}
            <Route
              path="/officer"
              element={
                <ProtectedRoute allowedRoles={['OFFICER', 'SUPERVISOR', 'ADMIN']}>
                  <OfficerDashboardPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/admin"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'DATA_ENTRY', 'SUPERVISOR']}>
                  <AdminDashboardPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/complaints"
              element={
                <ProtectedRoute>
                  <AdminDashboardPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/complaints/:id"
              element={
                <ProtectedRoute>
                  <ComplaintDetailsPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/reminders"
              element={
                <ProtectedRoute>
                  <ReminderCentrePage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/escalations"
              element={
                <ProtectedRoute>
                  <EscalationCentrePage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/history"
              element={
                <ProtectedRoute>
                  <ActionHistoryPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/departments"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'SUPERVISOR']}>
                  <DepartmentsPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/settings"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <SettingsPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/analytics"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'SUPERVISOR']}>
                  <AnalyticsPage />
                </ProtectedRoute>
              }
            />

            {/* Catch-all redirect */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ToastProvider>
  </AuthProvider>
);
};

export default App;
