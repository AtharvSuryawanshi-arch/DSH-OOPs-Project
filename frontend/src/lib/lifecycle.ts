import type { Incident, IncidentStatus } from '../api/types'

/**
 * The lifecycle in words a reporter understands. The steps mirror the backend state classes:
 * NEW/CLASSIFIED → ASSIGNED → ACKNOWLEDGED/IN_PROGRESS → RESOLVED → VERIFIED → CLOSED.
 */
export const LIFECYCLE_STEPS = ['Reported', 'Assigned', 'Working on it', 'Fixed', 'Confirmed', 'Closed'] as const

/** Index of the step each status has reached. */
export const STEP_REACHED: Record<IncidentStatus, number> = {
  NEW: 0,
  CLASSIFIED: 0,
  ASSIGNED: 1,
  ACKNOWLEDGED: 2,
  IN_PROGRESS: 2,
  RESOLVED: 3,
  VERIFIED: 4,
  CLOSED: 5,
  CANCELLED: 0,
}

/** One plain-language sentence about what happens next. */
export function nextStepText(incident: Incident, viewerIsReporter: boolean): string {
  const tech = incident.technicianName ?? 'A technician'
  switch (incident.status) {
    case 'NEW':
    case 'CLASSIFIED':
      return 'The service desk is finding the right technician.'
    case 'ASSIGNED':
      return `${tech} will pick this up next.`
    case 'ACKNOWLEDGED':
      return `${tech} has picked this up.`
    case 'IN_PROGRESS':
      return `${tech} is working on it.`
    case 'RESOLVED':
      return viewerIsReporter
        ? `${tech} says this is fixed. Please confirm whether it works for you.`
        : `Fixed. Waiting for ${incident.reporterName} to confirm.`
    case 'VERIFIED':
      return 'The fix is confirmed. The service desk will close the request.'
    case 'CLOSED':
      return 'Closed. Nothing more to do.'
    case 'CANCELLED':
      return 'This request was cancelled by the service desk.'
  }
}
