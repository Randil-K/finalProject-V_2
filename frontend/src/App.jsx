import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

import { AuthProvider } from './auth/AuthContext.jsx';
import RequireAuth from './components/RequireAuth.jsx';

import Landing from './pages/Landing.jsx';
import Login from './pages/auth/Login.jsx';
import Register from './pages/auth/Register.jsx';
import ForgotPassword from './pages/auth/ForgotPassword.jsx';
import ResetPassword from './pages/auth/ResetPassword.jsx';

import VolunteerShell from './layouts/VolunteerShell.jsx';
import Feed from './pages/volunteer/Feed.jsx';
import ReportDetail from './pages/volunteer/ReportDetail.jsx';
import MoreInfo from './pages/volunteer/MoreInfo.jsx';
import SubmitReport from './pages/volunteer/SubmitReport.jsx';
import Alerts from './pages/volunteer/Alerts.jsx';
import Opportunities from './pages/volunteer/Opportunities.jsx';
import Profile from './pages/volunteer/Profile.jsx';
import UserProfile from './pages/volunteer/UserProfile.jsx';
import Cleanups from './pages/volunteer/Cleanups.jsx';
import HelpCall from './pages/volunteer/HelpCall.jsx';
import CleanupDetail from './pages/volunteer/CleanupDetail.jsx';

import AuthorityShell from './layouts/AuthorityShell.jsx';
import Queue from './pages/authority/Queue.jsx';
import ReportReview from './pages/authority/ReportReview.jsx';
import Projects from './pages/authority/Projects.jsx';
import ProjectDetail from './pages/authority/ProjectDetail.jsx';
import Analytics from './pages/authority/Analytics.jsx';
import ConsoleAlerts from './pages/authority/ConsoleAlerts.jsx';
import Users from './pages/authority/Users.jsx';
import Verifications from './pages/authority/Verifications.jsx';

const CONSOLE_ROLES = ['ADMIN', 'AUTHORITY'];

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          <Route path="/app" element={<VolunteerShell />}>
            <Route index element={<Feed />} />
            <Route path="report/:id" element={<ReportDetail />} />
            <Route
              path="report/:id/more-info"
              element={
                <RequireAuth>
                  <MoreInfo />
                </RequireAuth>
              }
            />
            <Route path="cleanups" element={<Cleanups />} />
            <Route path="cleanups/:id" element={<CleanupDetail />} />
            <Route
              path="help/:alertId"
              element={
                <RequireAuth>
                  <HelpCall />
                </RequireAuth>
              }
            />
            <Route
              path="submit"
              element={
                <RequireAuth>
                  <SubmitReport />
                </RequireAuth>
              }
            />
            <Route
              path="alerts"
              element={
                <RequireAuth>
                  <Alerts />
                </RequireAuth>
              }
            />
            <Route
              path="opportunities"
              element={
                <RequireAuth>
                  <Opportunities />
                </RequireAuth>
              }
            />
            <Route
              path="users/:id"
              element={
                <RequireAuth>
                  <UserProfile />
                </RequireAuth>
              }
            />
            <Route
              path="profile"
              element={
                <RequireAuth>
                  <Profile />
                </RequireAuth>
              }
            />
          </Route>

          <Route
            path="/console"
            element={
              <RequireAuth roles={CONSOLE_ROLES}>
                <AuthorityShell />
              </RequireAuth>
            }
          >
            <Route index element={<Queue />} />
            <Route path="reports/:id" element={<ReportReview />} />
            <Route path="projects" element={<Projects />} />
            <Route path="projects/:id" element={<ProjectDetail />} />
            <Route path="analytics" element={<Analytics />} />
            <Route path="alerts" element={<ConsoleAlerts />} />
            <Route
              path="verifications"
              element={
                <RequireAuth roles={['ADMIN']}>
                  <Verifications />
                </RequireAuth>
              }
            />
            <Route
              path="users"
              element={
                <RequireAuth roles={['ADMIN']}>
                  <Users />
                </RequireAuth>
              }
            />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
