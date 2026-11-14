import { Check, XCircle } from 'lucide-react'
import type { Incident, IncidentStatus } from '../api/types'
import { formatCompactDate } from '../lib/format'
import { LIFECYCLE_STEPS as STEPS, STEP_REACHED as REACHED, nextStepText } from '../lib/lifecycle'

/** Shows where an incident is in its lifecycle (see lib/lifecycle.ts for the step mapping). */
function stepCaption(index: number, incident: Incident): string | null {
  const verifiedAt = incident.audit?.find((entry) => entry.toState === 'VERIFIED')?.createdAt
  switch (index) {
    case 0:
      return formatCompactDate(incident.createdAt)
    case 1:
      return incident.technicianName ?? null
    case 2:
      return incident.acknowledgedAt ? formatCompactDate(incident.acknowledgedAt) : null
    case 3:
      return incident.resolvedAt ? formatCompactDate(incident.resolvedAt) : null
    case 4:
      return verifiedAt ? formatCompactDate(verifiedAt) : null
    case 5:
      return incident.closedAt ? formatCompactDate(incident.closedAt) : null
    default:
      return null
  }
}

export function LifecycleTracker({ incident, viewerIsReporter }: { incident: Incident; viewerIsReporter: boolean }) {
  if (incident.status === 'CANCELLED') {
    return (
      <section className="panel lifecycle lifecycle-cancelled" aria-label="Progress">
        <XCircle size={20} aria-hidden="true" />
        <p>{nextStepText(incident, viewerIsReporter)}</p>
      </section>
    )
  }

  const reached = REACHED[incident.status]
  const finished = incident.status === 'CLOSED'

  return (
    <section className="panel lifecycle" aria-labelledby="lifecycle-title">
      <h2 id="lifecycle-title" className="lifecycle-title">Progress</h2>
      <ol className="lifecycle-steps">
        {STEPS.map((label, index) => {
          const done = index < reached || (finished && index === reached)
          const current = !finished && index === reached
          const state = done ? 'done' : current ? 'current' : 'todo'
          const caption = done || current ? stepCaption(index, incident) : index === reached + 1 ? 'Next' : null
          return (
            <li key={label} className={`lifecycle-step lifecycle-${state}`} aria-current={current ? 'step' : undefined}>
              <span className="lifecycle-marker" aria-hidden="true">
                {done ? <Check size={14} strokeWidth={3} /> : null}
              </span>
              <span className="lifecycle-label">{label}</span>
              {caption && <span className="lifecycle-caption">{caption}</span>}
            </li>
          )
        })}
      </ol>
      <p className="lifecycle-next">{nextStepText(incident, viewerIsReporter)}</p>
    </section>
  )
}

/** Thin six-segment bar for lists and cards. */
export function LifecycleBar({ status }: { status: IncidentStatus }) {
  const reached = REACHED[status]
  const filled = status === 'CLOSED' ? STEPS.length : reached + 1
  return (
    <div
      className="lifecycle-bar"
      role="img"
      aria-label={`Step ${Math.min(filled, STEPS.length)} of ${STEPS.length}: ${STEPS[Math.min(reached, STEPS.length - 1)]}`}
    >
      {STEPS.map((label, index) => (
        <span key={label} className={index < filled ? 'filled' : undefined} />
      ))}
    </div>
  )
}
