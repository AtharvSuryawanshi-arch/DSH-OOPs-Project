import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ScrollText } from 'lucide-react'
import type { AuditEntry, PageResponse } from '../../api/types'
import { useAuth } from '../../auth/auth-context'
import { useApi } from '../../hooks/useApi'
import {
  Avatar,
  Badge,
  EmptyState,
  ErrorPanel,
  PageHeader,
  PageLoader,
  Pagination,
} from '../../components/ui'
import { formatDateTime, titleCase } from '../../lib/format'

const actionTones: Record<string, 'blue' | 'green' | 'amber' | 'red' | 'violet' | 'slate'> = {
  INCIDENT_CREATED: 'blue',
  CLASSIFIED: 'blue',
  PRIORITY_CALCULATED: 'slate',
  PRIORITY_OVERRIDDEN: 'violet',
  ASSIGNED: 'violet',
  REASSIGNED: 'violet',
  ACKNOWLEDGED: 'blue',
  WORK_STARTED: 'blue',
  COMMENT_ADDED: 'slate',
  UPDATED: 'slate',
  RESOLVED: 'green',
  VERIFIED: 'green',
  CLOSED: 'green',
  CANCELLED: 'red',
  REOPENED: 'amber',
  SLA_WARNING: 'amber',
  SLA_BREACHED: 'red',
  ESCALATED: 'red',
  DUPLICATE_FLAGGED: 'amber',
  FEEDBACK_RECEIVED: 'green',
}

export function AdminAuditPage() {
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const audit = useApi<PageResponse<AuditEntry>>(`/admin/audit?page=${page}&size=25`)

  if (!user) return null
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />

  if (audit.loading && !audit.data) {
    return <PageLoader label="Loading audit trail…" />
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="Administration"
        title="Audit trail"
        description="Every important action on every incident, in order. Audit entries are append-only and include the actor and state change."
      />

      <section className="panel">
        {audit.error ? (
          <ErrorPanel message={audit.error} onRetry={audit.refetch} />
        ) : !audit.data || audit.data.content.length === 0 ? (
          <EmptyState title="No audit entries" description="Actions on incidents will appear here." icon={ScrollText} />
        ) : (
          <>
            <div className="table-wrap">
              <table className="data-table audit-table">
                <thead>
                  <tr>
                    <th>When</th>
                    <th>Actor</th>
                    <th>Action</th>
                    <th>Incident</th>
                    <th>Detail</th>
                  </tr>
                </thead>
                <tbody>
                  {audit.data.content.map((entry) => (
                    <tr key={entry.id}>
                      <td className="muted nowrap">{formatDateTime(entry.createdAt)}</td>
                      <td>
                        <span className="person-inline">
                          <Avatar name={entry.actorName} size="sm" />
                          {entry.actorName}
                        </span>
                      </td>
                      <td>
                        <Badge tone={actionTones[entry.action] ?? 'slate'}>{titleCase(entry.action)}</Badge>
                      </td>
                      <td>
                        <Link className="incident-ref" to={`/incidents/${entry.incidentId}`}>{entry.incidentReference}</Link>
                      </td>
                      <td>
                        <span>{entry.description}</span>
                        {entry.fromState && entry.toState && entry.fromState !== entry.toState && (
                          <span className="audit-state-change">
                            {titleCase(entry.fromState)} → {titleCase(entry.toState)}
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={audit.data.page}
              totalPages={audit.data.totalPages}
              totalElements={audit.data.totalElements}
              onPageChange={setPage}
            />
          </>
        )}
      </section>
    </div>
  )
}
