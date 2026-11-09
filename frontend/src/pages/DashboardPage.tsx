import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { Link } from 'react-router-dom'
import {
  AlertTriangle,
  ArrowUpRight,
  CircleDot,
  RefreshCw,
  Siren,
  Ticket,
  TimerOff,
  Wrench,
} from 'lucide-react'
import type { DashboardResponse, Incident, PageResponse, Priority } from '../api/types'
import { useAuth } from '../auth/auth-context'
import { useApi } from '../hooks/useApi'
import {
  Avatar,
  EmptyState,
  ErrorPanel,
  PageHeader,
  PageLoader,
  PriorityBadge,
  ProgressBar,
  SectionTitle,
  SlaBadge,
  StatCard,
  StatusBadge,
} from '../components/ui'
import { formatMinutes, greeting, timeAgo } from '../lib/format'

const priorityColors: Record<Priority, string> = {
  P1: '#dc2626',
  P2: '#ea580c',
  P3: '#2563eb',
  P4: '#64748b',
}

const dateTick = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' })

export function DashboardPage() {
  const { user } = useAuth()
  const dashboard = useApi<DashboardResponse>('/dashboard')
  const risk = useApi<PageResponse<Incident>>('/incidents?slaStatus=AT_RISK&size=4&escalatedOnly=false')

  if (dashboard.loading && !dashboard.data) {
    return <PageLoader label="Loading service desk overview…" />
  }

  if (dashboard.error || !dashboard.data) {
    return (
      <div className="stack">
        <PageHeader title="Service desk overview" description="Live operational view across incidents and SLAs." />
        <ErrorPanel message={dashboard.error ?? 'Dashboard data is unavailable.'} onRetry={dashboard.refetch} />
      </div>
    )
  }

  const data = dashboard.data
  const firstName = user?.name.split(' ')[0] ?? 'there'
  const isAdmin = user?.role === 'ADMIN'
  const scopeLabel = isAdmin ? 'across the organisation' : user?.role === 'TECHNICIAN' ? 'in your queue' : 'in your reports'

  const priorityData = data.byPriority.map((item) => ({
    name: item.key,
    label: item.label,
    value: item.count,
  }))

  const recent = data.recentIncidents
  const atRisk = risk.data?.content ?? []
  const workload = data.technicianWorkload

  return (
    <div className="stack">
      <PageHeader
        eyebrow={`${greeting()}, ${firstName}`}
        title="Service desk overview"
        description={`Live incident health ${scopeLabel}. Priorities, targets and workload update as the desk works.`}
        actions={
          <>
            <button type="button" className="button button-ghost" onClick={dashboard.refetch}>
              <RefreshCw size={16} /> Refresh
            </button>
            {user?.role === 'TECHNICIAN' ? (
              <Link to="/my-work" className="button button-primary">
                <Wrench size={16} /> Open my queue
              </Link>
            ) : (
              <Link to="/incidents/new" className="button button-primary">
                <Ticket size={16} /> Report an incident
              </Link>
            )}
          </>
        }
      />

      <div className="stat-grid">
        <StatCard
          label="Total incidents"
          value={data.totals.total}
          hint={isAdmin ? 'all time' : 'you can see'}
          icon={Ticket}
          tone="indigo"
        />
        <StatCard
          label="Open now"
          value={data.totals.open}
          hint={`${data.totals.unassigned} awaiting assignment`}
          icon={CircleDot}
          tone="blue"
        />
        <StatCard
          label="Critical open"
          value={data.totals.critical}
          hint="P1 priority"
          icon={Siren}
          tone="red"
        />
        <StatCard
          label="SLA breaches"
          value={data.totals.slaBreaches}
          hint={`Avg resolution ${formatMinutes(data.totals.averageResolutionMinutes)}`}
          icon={TimerOff}
          tone="amber"
        />
      </div>

      <div className="dashboard-grid">
        <section className="panel panel-span-2">
          <SectionTitle
            title="Incident flow"
            subtitle="Created vs resolved over the last seven days"
            action={<span className="panel-kicker">{data.scope === 'ADMIN' ? 'Organisation' : 'Your scope'}</span>}
          />
          <div className="chart-box chart-box-large">
            <ResponsiveContainer width="100%" height={280}>
              <AreaChart data={data.trend} margin={{ top: 10, right: 12, left: -18, bottom: 0 }}>
                <defs>
                  <linearGradient id="createdFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#4f46e5" stopOpacity={0.28} />
                    <stop offset="100%" stopColor="#4f46e5" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="resolvedFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#10b981" stopOpacity={0.22} />
                    <stop offset="100%" stopColor="#10b981" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="4 4" stroke="#e7eaf0" vertical={false} />
                <XAxis
                  dataKey="date"
                  tickFormatter={(value: string) => dateTick.format(new Date(`${value}T00:00:00Z`))}
                  axisLine={false}
                  tickLine={false}
                  tick={{ fontSize: 11, fill: '#8a90a2' }}
                />
                <YAxis allowDecimals={false} axisLine={false} tickLine={false} tick={{ fontSize: 11, fill: '#8a90a2' }} />
                <Tooltip
                  contentStyle={{ borderRadius: 12, border: '1px solid #e7eaf0', fontSize: 12 }}
                  labelFormatter={(value) => dateTick.format(new Date(`${value}T00:00:00Z`))}
                />
                <Area type="monotone" dataKey="created" name="Created" stroke="#4f46e5" strokeWidth={2} fill="url(#createdFill)" />
                <Area type="monotone" dataKey="resolved" name="Resolved" stroke="#10b981" strokeWidth={2} fill="url(#resolvedFill)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="panel">
          <SectionTitle title="Priority mix" subtitle="Current distribution" />
          <div className="chart-box">
            <ResponsiveContainer width="100%" height={210}>
              <PieChart>
                <Pie
                  data={priorityData}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={58}
                  outerRadius={86}
                  paddingAngle={4}
                  stroke="none"
                >
                  {priorityData.map((entry) => (
                    <Cell key={entry.name} fill={priorityColors[entry.name as Priority]} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e7eaf0', fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <ul className="legend-list">
            {priorityData.map((item) => (
              <li key={item.name}>
                <span className="legend-dot" style={{ background: priorityColors[item.name as Priority] }} />
                <span>{item.label}</span>
                <strong>{item.value}</strong>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <div className="dashboard-grid">
        <section className="panel panel-span-2">
          <SectionTitle
            title="Recent incidents"
            subtitle="Newest activity in your scope"
            action={<Link className="text-link" to="/incidents">View all <ArrowUpRight size={14} /></Link>}
          />
          {recent.length === 0 ? (
            <EmptyState title="No incidents yet" description="Reported incidents will appear here." icon={Ticket} />
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Incident</th>
                    <th>Priority</th>
                    <th>Status</th>
                    <th>SLA</th>
                    <th>Owner</th>
                    <th>Updated</th>
                  </tr>
                </thead>
                <tbody>
                  {recent.map((incident) => (
                    <tr key={incident.id}>
                      <td>
                        <Link className="incident-cell" to={`/incidents/${incident.id}`}>
                          <span className="incident-ref">{incident.reference}</span>
                          <span className="incident-title">{incident.title}</span>
                        </Link>
                      </td>
                      <td><PriorityBadge priority={incident.priority} /></td>
                      <td><StatusBadge status={incident.status} label={incident.statusLabel} /></td>
                      <td><SlaBadge status={incident.slaStatus} /></td>
                      <td>
                        {incident.technicianName ? (
                          <span className="person-inline"><Avatar name={incident.technicianName} size="sm" />{incident.technicianName}</span>
                        ) : (
                          <span className="muted">Unassigned</span>
                        )}
                      </td>
                      <td className="muted">{timeAgo(incident.updatedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <div className="stack">
          <section className="panel">
            <SectionTitle
              title="SLA watch"
              subtitle="At-risk incidents"
              action={<Link className="text-link" to="/incidents?slaStatus=AT_RISK">View</Link>}
            />
            {atRisk.length === 0 ? (
              <div className="sla-clear">
                <span><AlertTriangle size={18} /></span>
                <p>No incidents are currently inside the warning window.</p>
              </div>
            ) : (
              <ul className="watch-list">
                {atRisk.map((incident) => (
                  <li key={incident.id}>
                    <Link to={`/incidents/${incident.id}`}>
                      <span className="watch-ref">{incident.reference}</span>
                      <strong>{incident.title}</strong>
                      <span className="watch-meta">
                        <SlaBadge status={incident.slaStatus} /> {incident.priority}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="panel">
            <SectionTitle title="Desk performance" subtitle="Rolling averages" />
            <div className="mini-metrics">
              <div>
                <span>Avg response</span>
                <strong>{formatMinutes(data.totals.averageResponseMinutes)}</strong>
              </div>
              <div>
                <span>Avg resolution</span>
                <strong>{formatMinutes(data.totals.averageResolutionMinutes)}</strong>
              </div>
              <div>
                <span>Resolved</span>
                <strong>{data.totals.resolved}</strong>
              </div>
            </div>
          </section>
        </div>
      </div>

      {isAdmin && workload.length > 0 && (
        <section className="panel">
          <SectionTitle title="Technician workload" subtitle="Active incidents against capacity" />
          <div className="workload-grid">
            {workload.map((technician) => (
              <div className="workload-card" key={technician.id}>
                <div className="workload-card-head">
                  <span className="person-inline">
                    <Avatar name={technician.name} size="sm" />
                    <strong>{technician.name}</strong>
                  </span>
                  <span className={`availability ${technician.available ? 'available' : 'away'}`}>
                    {technician.available ? 'Available' : 'Away'}
                  </span>
                </div>
                <ProgressBar value={technician.activeIncidents} max={technician.maxCapacity ?? 1} tone={technician.activeIncidents >= (technician.maxCapacity ?? 1) ? 'red' : 'indigo'} />
                <p className="workload-meta">
                  {technician.activeIncidents} active / {technician.maxCapacity ?? '—'} capacity
                </p>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
