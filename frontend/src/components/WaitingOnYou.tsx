import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, Clock3 } from 'lucide-react'
import { errorMessage, incidentApi } from '../api/client'
import type { Incident, PageResponse } from '../api/types'
import { useAuth } from '../auth/auth-context'
import { useApi } from '../hooks/useApi'
import { Flash } from './ui'

/**
 * "Waiting on you": incidents this user reported that a technician has marked fixed.
 * Works for every role, because anyone can report an incident. Renders nothing when there is
 * nothing to answer.
 */
export function WaitingOnYou() {
  const { user } = useAuth()
  const path = user ? `/incidents?status=RESOLVED&reporterId=${user.id}&size=5` : ''
  const waiting = useApi<PageResponse<Incident>>(path, { pollMs: 15_000 })
  const [busy, setBusy] = useState<string | null>(null)
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)

  const items = waiting.data?.content ?? []
  if (!user || items.length === 0) {
    return message ? <Flash tone={message.tone}>{message.text}</Flash> : null
  }

  async function answer(incident: Incident, fixed: boolean) {
    setBusy(`${incident.id}-${fixed ? 'yes' : 'no'}`)
    setMessage(null)
    try {
      if (fixed) {
        await incidentApi.verify(incident.id)
        setMessage({ tone: 'success', text: `Thanks. ${incident.reference} is confirmed and will be closed by the service desk.` })
      } else {
        await incidentApi.reopen(incident.id)
        setMessage({ tone: 'success', text: `${incident.reference} is back with ${incident.technicianName ?? 'the technician'}.` })
      }
      window.dispatchEvent(new Event('itsm:notifications'))
      waiting.refetch()
    } catch (cause) {
      setMessage({ tone: 'error', text: errorMessage(cause) })
    } finally {
      setBusy(null)
    }
  }

  return (
    <section className="waiting-panel" aria-labelledby="waiting-title">
      <div className="waiting-head">
        <Clock3 size={20} aria-hidden="true" />
        <h2 id="waiting-title">Waiting on you · {items.length}</h2>
      </div>
      {message && <Flash tone={message.tone}>{message.text}</Flash>}
      <ul className="waiting-list">
        {items.map((incident) => (
          <li key={incident.id} className="waiting-item">
            <div className="waiting-copy">
              <span className="waiting-ref">{incident.reference} · {incident.serviceName}</span>
              <Link to={`/incidents/${incident.id}`} className="waiting-title">{incident.title}</Link>
              <p>
                {incident.technicianName ?? 'The technician'} says this is fixed
                {incident.resolutionNotes ? <>: “{incident.resolutionNotes}”</> : '.'}{' '}
                <strong>Is it working for you now?</strong>
              </p>
            </div>
            <div className="waiting-actions">
              <button
                type="button"
                className="button button-success"
                disabled={busy !== null}
                onClick={() => void answer(incident, true)}
              >
                <Check size={16} /> {busy === `${incident.id}-yes` ? 'Saving…' : "Yes, it's fixed"}
              </button>
              <button
                type="button"
                className="button button-secondary"
                disabled={busy !== null}
                onClick={() => void answer(incident, false)}
              >
                {busy === `${incident.id}-no` ? 'Saving…' : 'No, still broken'}
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}
