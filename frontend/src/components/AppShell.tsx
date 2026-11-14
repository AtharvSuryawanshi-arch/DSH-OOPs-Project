import { useState } from 'react'
import { NavLink, Outlet, Link, useLocation } from 'react-router-dom'
import {
  BarChart3,
  Bell,
  Boxes,
  BriefcaseBusiness,
  House,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Plus,
  ScrollText,
  SlidersHorizontal,
  Sun,
  Ticket,
  Timer,
  Users,
  X,
  type LucideIcon,
} from 'lucide-react'
import { useAuth } from '../auth/auth-context'
import { Avatar, Logo, RoleBadge } from './ui'
import { NotificationBell } from './NotificationBell'
import { useTheme } from '../lib/theme'

interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  end?: boolean
}

interface NavGroup {
  label: string
  items: NavItem[]
}

function groupsFor(role: string): NavGroup[] {
  const workspace: NavItem[] = [
    { to: '/', label: role === 'EMPLOYEE' ? 'Home' : 'Dashboard', icon: role === 'EMPLOYEE' ? House : LayoutDashboard, end: true },
    { to: '/incidents', label: role === 'EMPLOYEE' ? 'My requests' : 'Incidents', icon: Ticket },
    // Every role can report an incident with their own account.
    { to: '/incidents/new', label: 'Report incident', icon: Plus },
  ]

  if (role === 'TECHNICIAN') {
    workspace.push({ to: '/my-work', label: 'My queue', icon: BriefcaseBusiness })
  }

  const groups: NavGroup[] = [
    { label: 'Workspace', items: workspace },
    { label: 'Inbox', items: [{ to: '/notifications', label: 'Notifications', icon: Bell }] },
  ]

  if (role === 'ADMIN') {
    groups.push({
      label: 'Administration',
      items: [
        { to: '/reports', label: 'Reports', icon: BarChart3 },
        { to: '/admin/users', label: 'Users & skills', icon: Users },
        { to: '/admin/catalog', label: 'Services & categories', icon: Boxes },
        { to: '/admin/sla', label: 'SLA policies', icon: Timer },
        { to: '/admin/settings', label: 'Assignment settings', icon: SlidersHorizontal },
        { to: '/admin/audit', label: 'Audit trail', icon: ScrollText },
      ],
    })
  }

  return groups
}

function pageTitle(pathname: string, groups: NavGroup[]): string {
  if (/^\/incidents\/\d+$/.test(pathname)) {
    return 'Incident detail'
  }
  if (pathname === '/incidents/new') {
    return 'Report incident'
  }
  const match = groups.flatMap((group) => group.items).find((item) => item.to === pathname)
  if (match) {
    return match.label
  }
  if (pathname.startsWith('/admin/')) {
    return 'Administration'
  }
  return 'Workspace'
}

export function AppShell() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const { theme, toggle: toggleTheme } = useTheme()
  const groups = groupsFor(user?.role ?? 'EMPLOYEE')

  if (!user) {
    return null
  }

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-brand">
          <Link to="/" onClick={() => setMenuOpen(false)}>
            <Logo />
          </Link>
          <button
            type="button"
            className="icon-button sidebar-close"
            onClick={() => setMenuOpen(false)}
            aria-label="Close navigation"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {groups.map((group) => (
            <div className="nav-group" key={group.label}>
              <p className="nav-group-label">{group.label}</p>
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) => `nav-link ${isActive ? 'nav-link-active' : ''}`}
                  onClick={() => setMenuOpen(false)}
                >
                  <item.icon size={18} />
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user">
            <Avatar name={user.name} size="sm" />
            <div>
              <strong>{user.name}</strong>
              <span>{user.role === 'TECHNICIAN' ? user.department || 'Support engineer' : user.email}</span>
            </div>
          </div>
          <button
            type="button"
            className="theme-toggle"
            role="switch"
            aria-checked={theme === 'dark'}
            onClick={toggleTheme}
          >
            {theme === 'dark' ? <Moon size={16} aria-hidden="true" /> : <Sun size={16} aria-hidden="true" />}
            <span>Dark mode</span>
            <span className="theme-toggle-track" aria-hidden="true"><span /></span>
          </button>
          <div className="sidebar-user-actions">
            <RoleBadge role={user.role} />
            <button type="button" className="icon-button icon-button-dark" onClick={logout} aria-label="Sign out">
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>

      {menuOpen && (
        <button
          type="button"
          className="sidebar-scrim"
          onClick={() => setMenuOpen(false)}
          aria-label="Close navigation"
        />
      )}

      <div className="workspace">
        <header className="topbar">
          <div className="topbar-left">
            <button
              type="button"
              className="icon-button mobile-menu"
              onClick={() => setMenuOpen(true)}
              aria-label="Open navigation"
            >
              <Menu size={20} />
            </button>
            <div className="topbar-context">
              <span className="topbar-crumbs">I-ITSM / {pageTitle(location.pathname, groups)}</span>
              <span className="topbar-role">{user.role === 'EMPLOYEE' ? 'Employee' : user.role === 'TECHNICIAN' ? 'Support engineer' : 'Service manager'}</span>
            </div>
          </div>
          <div className="topbar-actions">
            <Link to="/incidents/new" className="button button-primary button-sm topbar-cta">
              <Plus size={16} /> New incident
            </Link>
            <NotificationBell />
            <Link to="/notifications" className="topbar-avatar" aria-label="View profile notifications">
              <Avatar name={user.name} size="sm" />
            </Link>
          </div>
        </header>

        <main className="main">
          <div className="content">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
