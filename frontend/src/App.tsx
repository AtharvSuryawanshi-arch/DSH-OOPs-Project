import type { ReactNode } from 'react'
import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/auth-context'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { AppShell } from './components/AppShell'
import { PageLoader } from './components/ui'
import { DashboardPage } from './pages/DashboardPage'
import { IncidentDetailPage } from './pages/IncidentDetailPage'
import { IncidentsPage } from './pages/IncidentsPage'
import { MyWorkPage } from './pages/MyWorkPage'
import { NewIncidentPage } from './pages/NewIncidentPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { ReportsPage } from './pages/ReportsPage'
import { AdminAuditPage } from './pages/admin/AdminAuditPage'
import { AdminCatalogPage } from './pages/admin/AdminCatalogPage'
import { AdminSettingsPage } from './pages/admin/AdminSettingsPage'
import { AdminSlaPage } from './pages/admin/AdminSlaPage'
import { AdminUsersPage } from './pages/admin/AdminUsersPage'
import type { Role } from './api/types'

function RequireAuth() {
  const { user, loading } = useAuth()
  if (loading) {
    return <PageLoader label="Restoring your session…" />
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }
  return <Outlet />
}

function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (!roles.includes(user.role)) {
    return <Navigate to="/" replace />
  }
  return children
}

function NotFound() {
  return (
    <div className="not-found">
      <span>404</span>
      <h1>Page not found</h1>
      <p>The page you are looking for does not exist in this workspace.</p>
      <a className="button button-primary" href="/">Back to dashboard</a>
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="incidents" element={<IncidentsPage />} />
          <Route
            path="incidents/new"
            element={
              <RequireRole roles={['EMPLOYEE', 'ADMIN']}>
                <NewIncidentPage />
              </RequireRole>
            }
          />
          <Route path="incidents/:id" element={<IncidentDetailPage />} />
          <Route
            path="my-work"
            element={
              <RequireRole roles={['TECHNICIAN']}>
                <MyWorkPage />
              </RequireRole>
            }
          />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route
            path="reports"
            element={
              <RequireRole roles={['ADMIN']}>
                <ReportsPage />
              </RequireRole>
            }
          />
          <Route
            path="admin/users"
            element={
              <RequireRole roles={['ADMIN']}>
                <AdminUsersPage />
              </RequireRole>
            }
          />
          <Route
            path="admin/catalog"
            element={
              <RequireRole roles={['ADMIN']}>
                <AdminCatalogPage />
              </RequireRole>
            }
          />
          <Route
            path="admin/sla"
            element={
              <RequireRole roles={['ADMIN']}>
                <AdminSlaPage />
              </RequireRole>
            }
          />
          <Route
            path="admin/settings"
            element={
              <RequireRole roles={['ADMIN']}>
                <AdminSettingsPage />
              </RequireRole>
            }
          />
          <Route
            path="admin/audit"
            element={
              <RequireRole roles={['ADMIN']}>
                <AdminAuditPage />
              </RequireRole>
            }
          />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Route>
    </Routes>
  )
}
