import { Link, Navigate } from 'react-router-dom'
import { ArrowUpRight, BriefcaseBusiness, CheckCircle2, Clock3, Gauge, Wrench } from 'lucide-react'
import type { TechnicianWorkload } from '../api/types'
import { useAuth } from '../auth/auth-context'
import { useApi } from '../hooks/useApi'
import {
  EmptyState,
  ErrorPanel,
  PageHeader,
  PageLoader,
  PriorityBadge,
  ProgressBar,
  SlaBadge,
  StatCard,
  StatusBadge,
} from '../components/ui'
import { formatDue, timeAgo } from '../lib/format'

export function MyWorkPage() {
  const { user } = useAuth()
  const workload = useApi<TechnicianWorkload>('/technicians/me/workload')

  if (!user) {
    return null
  }
  if (user.role !== 'TECHNICIAN') {
    return <Navigate to="/" replace />
  }

  if (workload.loading && !workload.data) {
    return <PageLoader label="Loading your queue…" />
  }
  if (workload.error || !workload.data) {
    return (
      <div className="stack">
        <PageHeader title="My queue" description="Your assigned incidents and current workload." />
        <ErrorPanel message={workload.error ?? 'Workload unavailable.'} onRetry={workload.refetch} />
      </div>
    )
  }

  const data = workload.data
  const capacity = data.maxCapacity ?? 0
  const utilization = capacity > 0 ? Math.round((data.activeIncidents / capacity) * 100) : 0
  const nextDue = data.workItems
    .map((item) => item.resolutionDueAt)
    .filter((value): value is string => Boolean(value))
    .sort((left, right) => new Date(left).getTime() - new Date(right).getTime())[0]

  return (
    <div className="stack">
      <PageHeader
        eyebrow="Support engineer workspace"
        title={`${data.technicianName.split(' ')[0]}'s queue`}
        description="Acknowledge, investigate and resolve the incidents assigned to you. SLA clocks keep running."
        actions={
          <span className={`availability-large ${data.available ? 'available' : 'away'}`}>
            <span className="pulse-dot" /> {data.available ? 'Available for assignment' : 'Currently unavailable'}
          </span>
        }
      />

      <div className="stat-grid">
        <StatCard
          label="Active workload"
          value={`${data.activeIncidents} / ${capacity || '—'}`}
          hint={`${utilization}% capacity used`}
          icon={BriefcaseBusiness}
          tone={utilization >= 100 ? 'red' : utilization >= 75 ? 'amber' : 'indigo'}
        />
        <StatCard label="Assigned open" value={data.assignedOpen} hint="unresolved incidents" icon={Wrench} tone="blue" />
        <StatCard label="Resolved by you" value={data.resolvedByTechnician} hint="all time" icon={CheckCircle2} tone="green" />
        <StatCard
          label="Next deadline"
          value={nextDue ? formatDue(nextDue) : 'Clear'}
          hint={nextDue ? 'Resolution target' : 'No active deadlines'}
          icon={Gauge}
          tone={nextDue ? 'amber' : 'green'}
        />
      </div>

      <section className="panel">
        <div className="workload-header">
          <div>
            <h2>Capacity</h2>
            <p>Assignments are capped at your configured capacity.</p>
          </div>
          <span className="muted"><strong>{data.activeIncidents}</strong> active / {capacity} capacity</span>
        </div>
        <ProgressBar value={data.activeIncidents} max={capacity || 1} tone={utilization >= 100 ? 'red' : 'indigo'} />
        {nextDue && (
          <p className="muted small workload-deadline">
            <Clock3 size={13} /> Next SLA deadline {formatDue(nextDue)}
          </p>
        )}
      </section>

      <section className="panel">
        <div className="section-title">
          <div>
            <h2>Assigned incidents</h2>
            <p>Sorted by the nearest SLA deadline.</p>
          </div>
          <Link to="/incidents" className="text-link">All incidents <ArrowUpRight size={14} /></Link>
        </div>

        {data.workItems.length === 0 ? (
          <EmptyState
            title="Your queue is clear"
            description="Nice work. New assignments will appear here when the service desk routes them to you."
            icon={CheckCircle2}
          />
        ) : (
          <ul className="work-queue">
            {data.workItems.map((incident) => (
              <li key={incident.id}>
                <Link to={`/incidents/${incident.id}`} className="work-queue-card">
                  <div className="work-queue-main">
                    <div className="work-queue-badges">
                      <PriorityBadge priority={incident.priority} />
                      <StatusBadge status={incident.status} label={incident.statusLabel} />
                      <SlaBadge status={incident.slaStatus} />
                    </div>
                    <h3>{incident.title}</h3>
                    <p>{incident.reference} · {incident.serviceName} · {incident.reporterName}</p>
                  </div>
                  <div className="work-queue-meta">
                    <strong className={incident.slaStatus === 'BREACHED' ? 'text-danger' : ''}>{formatDue(incident.resolutionDueAt)}</strong>
                    <span>Resolution target</span>
                    <span className="muted small">Updated {timeAgo(incident.updatedAt)}</span>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
