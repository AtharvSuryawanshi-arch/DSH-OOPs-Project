import { Link } from 'react-router-dom'
import { Plus, Star, Ticket } from 'lucide-react'
import type { Incident, PageResponse } from '../api/types'
import { useAuth } from '../auth/auth-context'
import { useApi } from '../hooks/useApi'
import { EmptyState, ErrorPanel, PageHeader, PageLoader, SectionTitle, StatusBadge } from '../components/ui'
import { LifecycleBar } from '../components/LifecycleTracker'
import { nextStepText } from '../lib/lifecycle'
import { WaitingOnYou } from '../components/WaitingOnYou'
import { formatCompactDate, greeting, timeAgo } from '../lib/format'

const IN_FLIGHT = new Set(['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS'])
const DONE = new Set(['VERIFIED', 'CLOSED'])

function timingText(incident: Incident): { text: string; tone: 'ok' | 'warn' | 'late' } {
  if (incident.slaStatus === 'BREACHED') {
    return { text: 'Taking longer than our target. It has been flagged to the service desk.', tone: 'late' }
  }
  if (incident.slaStatus === 'AT_RISK') {
    return { text: `Close to the target time (${formatCompactDate(incident.resolutionDueAt)}).`, tone: 'warn' }
  }
  return {
    text: incident.resolutionDueAt ? `On track. Target fix by ${formatCompactDate(incident.resolutionDueAt)}.` : 'On track.',
    tone: 'ok',
  }
}

/** The employee's home: what needs them, what is being worked on, and what was fixed recently. */
export function EmployeeHomePage() {
  const { user } = useAuth()
  // Employees only ever see incidents they reported, so this is "my requests".
  const mine = useApi<PageResponse<Incident>>('/incidents?size=50', { pollMs: 15_000 })

  if (mine.loading && !mine.data) {
    return <PageLoader label="Loading your requests…" />
  }

  const firstName = user?.name.split(' ')[0] ?? 'there'
  const incidents = mine.data?.content ?? []
  const inFlight = incidents.filter((incident) => IN_FLIGHT.has(incident.status))
  const done = incidents.filter((incident) => DONE.has(incident.status)).slice(0, 5)

  return (
    <div className="stack">
      <PageHeader
        eyebrow={`${greeting()}, ${firstName}`}
        title="Your IT requests"
        description="Everything you have reported, and anything that needs your answer."
      />

      {mine.error && <ErrorPanel message={mine.error} onRetry={mine.refetch} />}

      <WaitingOnYou />

      <section className="panel">
        <SectionTitle
          title={`Being worked on · ${inFlight.length}`}
          action={<Link className="text-link" to="/incidents">All my requests</Link>}
        />
        {inFlight.length === 0 ? (
          <EmptyState
            title="Nothing open right now"
            description="If something stops working, report it and the service desk will route it to the right technician."
            icon={Ticket}
            action={
              <Link to="/incidents/new" className="button button-primary">
                <Plus size={16} /> Report a problem
              </Link>
            }
          />
        ) : (
          <ul className="request-list">
            {inFlight.map((incident) => {
              const timing = timingText(incident)
              return (
                <li key={incident.id}>
                  <Link to={`/incidents/${incident.id}`} className="request-card">
                    <div className="request-card-head">
                      <div>
                        <span className="request-ref">{incident.reference} · {incident.serviceName}</span>
                        <strong className="request-title">{incident.title}</strong>
                      </div>
                      <StatusBadge status={incident.status} label={incident.statusLabel} />
                    </div>
                    <LifecycleBar status={incident.status} />
                    <div className="request-meta">
                      <span>{nextStepText(incident, true)}</span>
                      <span className={`request-timing request-timing-${timing.tone}`}>{timing.text}</span>
                    </div>
                  </Link>
                </li>
              )
            })}
          </ul>
        )}
      </section>

      {done.length > 0 && (
        <section className="panel">
          <SectionTitle title="Recently fixed" />
          <ul className="done-list">
            {done.map((incident) => (
              <li key={incident.id}>
                <div>
                  <Link to={`/incidents/${incident.id}`} className="request-title">{incident.title}</Link>
                  <span className="muted">
                    {incident.statusLabel} {timeAgo(incident.closedAt ?? incident.updatedAt)}
                    {incident.technicianName ? ` · fixed by ${incident.technicianName}` : ''}
                  </span>
                </div>
                {incident.feedbackRating ? (
                  <span className="muted">You rated it {incident.feedbackRating} / 5</span>
                ) : (
                  <Link to={`/incidents/${incident.id}`} className="button button-secondary button-sm">
                    <Star size={15} /> Rate the fix
                  </Link>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
