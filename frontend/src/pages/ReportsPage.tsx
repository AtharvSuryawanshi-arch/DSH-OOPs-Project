import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import {
  Activity,
  ArrowUpRight,
  RefreshCw,
  ShieldCheck,
  TimerOff,
  Users,
} from 'lucide-react'
import { adminApi, errorMessage } from '../api/client'
import type { DashboardResponse, Priority } from '../api/types'
import { useAuth } from '../auth/auth-context'
import { useApi } from '../hooks/useApi'
import {
  Badge,
  ErrorPanel,
  Flash,
  PageHeader,
  PageLoader,
  ProgressBar,
  SectionTitle,
  StatCard,
} from '../components/ui'
import { formatMinutes } from '../lib/format'

const priorityColors: Record<Priority, string> = {
  P1: '#dc2626',
  P2: '#ea580c',
  P3: '#2563eb',
  P4: '#64748b',
}

export function ReportsPage() {
  const { user } = useAuth()
  const dashboard = useApi<DashboardResponse>('/dashboard')
  const [scanning, setScanning] = useState(false)
  const [scanMessage, setScanMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  if (!user) {
    return null
  }
  if (user.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  if (dashboard.loading && !dashboard.data) {
    return <PageLoader label="Building reports…" />
  }
  if (dashboard.error || !dashboard.data) {
    return (
      <div className="stack">
        <PageHeader title="Operational reports" description="Service desk performance and SLA analytics." />
        <ErrorPanel message={dashboard.error ?? 'Report data unavailable.'} onRetry={dashboard.refetch} />
      </div>
    )
  }

  const data = dashboard.data
  const maxService = Math.max(...data.byService.map((item) => item.count), 1)
  const priorityData = data.byPriority.map((item) => ({ ...item, color: priorityColors[item.key as Priority] }))

  async function runScan() {
    setScanning(true)
    setError(null)
    setScanMessage(null)
    try {
      await adminApi.scanSla()
      setScanMessage('SLA monitor completed. Deadlines, warnings and escalations are up to date.')
      dashboard.refetch()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setScanning(false)
    }
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="Administration"
        title="Operational reports"
        description="A service-manager view of incident volume, SLA performance and technician utilisation."
        actions={
          <button type="button" className="button button-secondary" onClick={() => void runScan()} disabled={scanning}>
            <ShieldCheck size={16} /> {scanning ? 'Running scan…' : 'Run SLA scan'}
          </button>
        }
      />

      {scanMessage && <Flash tone="success">{scanMessage}</Flash>}
      {error && <Flash tone="error">{error}</Flash>}

      <div className="stat-grid">
        <StatCard label="Total incidents" value={data.totals.total} hint="all time" icon={Activity} tone="indigo" />
        <StatCard label="Open incidents" value={data.totals.open} hint={`${data.totals.critical} critical`} icon={ArrowUpRight} tone="blue" />
        <StatCard label="SLA breaches" value={data.totals.slaBreaches} hint="response or resolution" icon={TimerOff} tone="red" />
        <StatCard label="Resolved" value={data.totals.resolved} hint="verification / closure queue" icon={ShieldCheck} tone="green" />
      </div>

      <div className="dashboard-grid">
        <section className="panel panel-span-2">
          <SectionTitle title="Incident flow" subtitle="Created and resolved over seven days" />
          <div className="chart-box chart-box-large">
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data.trend} margin={{ top: 10, right: 12, left: -18, bottom: 0 }}>
                <CartesianGrid strokeDasharray="4 4" stroke="#e7eaf0" vertical={false} />
                <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#8a90a2' }} axisLine={false} tickLine={false} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: '#8a90a2' }} axisLine={false} tickLine={false} />
                <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e7eaf0', fontSize: 12 }} />
                <Bar dataKey="created" name="Created" fill="#4f46e5" radius={[5, 5, 0, 0]} />
                <Bar dataKey="resolved" name="Resolved" fill="#10b981" radius={[5, 5, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </section>

        <section className="panel">
          <SectionTitle title="Priority mix" subtitle="Current backlog" />
          <div className="chart-box">
            <ResponsiveContainer width="100%" height={210}>
              <PieChart>
                <Pie data={priorityData} dataKey="count" nameKey="key" innerRadius={58} outerRadius={86} paddingAngle={4} stroke="none">
                  {priorityData.map((entry) => <Cell key={entry.key} fill={entry.color} />)}
                </Pie>
                <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e7eaf0', fontSize: 12 }} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <ul className="legend-list">
            {priorityData.map((item) => (
              <li key={item.key}>
                <span className="legend-dot" style={{ background: item.color }} />
                <span>{item.label}</span>
                <strong>{item.count}</strong>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <div className="reports-grid">
        <section className="panel">
          <SectionTitle title="Service analysis" subtitle="Incidents by affected service" />
          <ul className="bar-list">
            {data.byService.map((service) => (
              <li key={service.key}>
                <div className="bar-list-head">
                  <span>{service.label}</span>
                  <strong>{service.count}</strong>
                </div>
                <div className="bar-track"><span style={{ width: `${Math.round((service.count / maxService) * 100)}%` }} /></div>
              </li>
            ))}
          </ul>
        </section>

        <section className="panel">
          <SectionTitle title="Category analysis" subtitle="Classification mix" />
          <ul className="bar-list bar-list-blue">
            {data.byCategory.map((category) => (
              <li key={category.key}>
                <div className="bar-list-head">
                  <span>{category.label}</span>
                  <strong>{category.count}</strong>
                </div>
                <div className="bar-track"><span style={{ width: `${Math.round((category.count / Math.max(...data.byCategory.map((item) => item.count), 1)) * 100)}%` }} /></div>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <div className="dashboard-grid">
        <section className="panel panel-span-2">
          <SectionTitle title="Technician workload" subtitle="Active incidents against configured capacity" action={<Badge tone="blue"><Users size={12} /> {data.technicianWorkload.length} technicians</Badge>} />
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Technician</th>
                  <th>Active</th>
                  <th>Capacity</th>
                  <th>Utilisation</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {data.technicianWorkload.map((technician) => {
                  const percentage = technician.maxCapacity ? Math.round((technician.activeIncidents / technician.maxCapacity) * 100) : 0
                  return (
                    <tr key={technician.id}>
                      <td><strong>{technician.name}</strong></td>
                      <td>{technician.activeIncidents}</td>
                      <td>{technician.maxCapacity ?? '—'}</td>
                      <td className="table-progress">
                        <ProgressBar value={technician.activeIncidents} max={technician.maxCapacity ?? 1} tone={percentage >= 100 ? 'red' : percentage >= 75 ? 'amber' : 'green'} />
                        <span>{percentage}%</span>
                      </td>
                      <td>
                        <Badge tone={technician.available ? 'green' : 'amber'}>
                          {technician.available ? 'Available' : 'Away'}
                        </Badge>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel">
          <SectionTitle title="SLA health" subtitle="Rolling averages" />
          <div className="metric-stack">
            <div>
              <span>Average response</span>
              <strong>{formatMinutes(data.totals.averageResponseMinutes)}</strong>
            </div>
            <div>
              <span>Average resolution</span>
              <strong>{formatMinutes(data.totals.averageResolutionMinutes)}</strong>
            </div>
            <div>
              <span>Unassigned backlog</span>
              <strong>{data.totals.unassigned}</strong>
            </div>
            <div>
              <span>Breached incidents</span>
              <strong className="text-danger">{data.totals.slaBreaches}</strong>
            </div>
          </div>
          <button type="button" className="button button-ghost button-block" onClick={dashboard.refetch}>
            <RefreshCw size={15} /> Refresh report
          </button>
        </section>
      </div>
    </div>
  )
}
