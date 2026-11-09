import { useState, type FormEvent } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  CheckCircle2,
  Clock3,
  Copy,
  History,
  LoaderCircle,
  Lock,
  MessageSquare,
  Play,
  RotateCcw,
  Send,
  ShieldAlert,
  Sparkles,
  Star,
  UserCheck,
  Wrench,
  XCircle,
  Pencil,
} from 'lucide-react'
import { errorMessage, incidentApi } from '../api/client'
import type {
  DuplicateCandidate,
  Impact,
  Incident,
  ResolutionSuggestion,
  TechnicianRecommendation,
  Urgency,
} from '../api/types'
import { useAuth } from '../auth/auth-context'
import { useApi } from '../hooks/useApi'
import {
  Avatar,
  Badge,
  EmptyState,
  ErrorPanel,
  Field,
  Flash,
  Modal,
  PageLoader,
  PriorityBadge,
  RoleBadge,
  SectionTitle,
  SlaBadge,
  StatusBadge,
  WarningNote,
} from '../components/ui'
import { formatDateTime, formatDue, isOverdue, timeAgo, titleCase } from '../lib/format'

type ModalKind = 'assign' | 'resolve' | 'close' | 'feedback' | 'priority' | 'cancel' | 'edit' | null

export function IncidentDetailPage() {
  const { id } = useParams()
  const incidentId = Number(id)
  const { user } = useAuth()
  const location = useLocation()

  const detail = useApi<Incident>(`/incidents/${incidentId}`)
  const duplicates = useApi<DuplicateCandidate[]>(`/incidents/${incidentId}/duplicates`)
  const suggestions = useApi<ResolutionSuggestion[]>(`/incidents/${incidentId}/suggested-resolutions`)
  const recommendations = useApi<TechnicianRecommendation[]>(`/incidents/${incidentId}/recommended-technicians`)

  const [tab, setTab] = useState<'conversation' | 'audit'>('conversation')
  const [comment, setComment] = useState('')
  const [internal, setInternal] = useState(false)
  const [commentSending, setCommentSending] = useState(false)
  const [commentError, setCommentError] = useState<string | null>(null)

  const [modal, setModal] = useState<ModalKind>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const [resolveNotes, setResolveNotes] = useState('')
  const [closeNotes, setCloseNotes] = useState('')
  const [cancelReason, setCancelReason] = useState('')
  const [priority, setPriority] = useState('P1')
  const [priorityReason, setPriorityReason] = useState('')
  const [rating, setRating] = useState(5)
  const [feedbackComment, setFeedbackComment] = useState('')
  const [editTitle, setEditTitle] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [editImpact, setEditImpact] = useState<Impact>('MEDIUM')
  const [editUrgency, setEditUrgency] = useState<Urgency>('MEDIUM')
  const [editAffectedUsers, setEditAffectedUsers] = useState(1)
  const [createdNotice, setCreatedNotice] = useState(
    Boolean((location.state as { created?: boolean } | null)?.created),
  )

  if (!Number.isFinite(incidentId)) {
    return <ErrorPanel message="That incident reference is invalid." />
  }

  if (detail.loading && !detail.data) {
    return <PageLoader label="Loading incident…" />
  }

  if (detail.error || !detail.data) {
    return (
      <div className="stack">
        <Link to="/incidents" className="text-link back-link"><ArrowLeft size={15} /> Back to incidents</Link>
        <ErrorPanel message={detail.error ?? 'Incident not found.'} onRetry={detail.refetch} />
      </div>
    )
  }

  const incident = detail.data
  const role = user?.role ?? 'EMPLOYEE'
  const isAdmin = role === 'ADMIN'
  const isTechnician = role === 'TECHNICIAN'
  const isReporter = user?.id === incident.reporterId
  const isAssigned = user?.id === incident.technicianId
  const status = incident.status
  const isClosed = status === 'CLOSED' || status === 'CANCELLED'
  const canAssign = isAdmin && !isClosed && status !== 'RESOLVED' && status !== 'VERIFIED'
  const canVerify = isReporter && status === 'RESOLVED'
  const canReopen = (isReporter || isAdmin) && status === 'RESOLVED'
  const canClose = isAdmin && status === 'VERIFIED'
  const canCancel = isAdmin && ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED'].includes(status)
  const canFeedback = isReporter && ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(status) && !incident.feedbackRating
  const canOverride = isAdmin && !isClosed && status !== 'RESOLVED' && status !== 'VERIFIED'
  const canEdit = (isReporter || isAdmin) && (status === 'NEW' || status === 'CLASSIFIED')
  const canComment = isReporter || isAssigned || isAdmin
  const canInternal = isTechnician || isAdmin

  const technicianAction =
    isTechnician && isAssigned
      ? status === 'ASSIGNED'
        ? { key: 'acknowledge', label: 'Acknowledge', icon: UserCheck }
        : status === 'ACKNOWLEDGED'
          ? { key: 'start', label: 'Start investigation', icon: Play }
          : status === 'IN_PROGRESS'
            ? { key: 'resolve', label: 'Resolve incident', icon: CheckCircle2 }
            : null
      : null

  function refreshAll() {
    detail.refetch()
    duplicates.refetch()
    suggestions.refetch()
    recommendations.refetch()
    window.dispatchEvent(new Event('itsm:notifications'))
  }

  async function execute(label: string, action: () => Promise<unknown>, closeModal = false) {
    setActionLoading(label)
    setActionError(null)
    try {
      await action()
      if (closeModal) {
        setModal(null)
      }
      refreshAll()
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setActionLoading(null)
    }
  }

  function openModal(kind: ModalKind) {
    setActionError(null)
    setModal(kind)
    if (kind === 'priority') {
      setPriority(incident.priority)
      setPriorityReason('')
    }
    if (kind === 'resolve') {
      setResolveNotes(incident.resolutionNotes ?? '')
    }
    if (kind === 'close') {
      setCloseNotes(incident.closureNotes ?? '')
    }
    if (kind === 'feedback') {
      setRating(incident.feedbackRating ?? 5)
      setFeedbackComment(incident.feedbackComment ?? '')
    }
    if (kind === 'cancel') {
      setCancelReason('')
    }
    if (kind === 'edit') {
      setEditTitle(incident.title)
      setEditDescription(incident.description)
      setEditImpact(incident.impact)
      setEditUrgency(incident.urgency)
      setEditAffectedUsers(incident.affectedUsers)
    }
  }

  async function submitComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!comment.trim()) {
      return
    }
    setCommentSending(true)
    setCommentError(null)
    try {
      await incidentApi.comment(incident.id, comment.trim(), internal)
      setComment('')
      setInternal(false)
      detail.refetch()
    } catch (cause) {
      setCommentError(errorMessage(cause))
    } finally {
      setCommentSending(false)
    }
  }

  const comments = incident.comments ?? []
  const audit = incident.audit ?? []
  const sortedDuplicates = duplicates.data ?? []
  const resolutionSuggestions = suggestions.data ?? []
  const technicianSuggestions = recommendations.data ?? []

  return (
    <div className="stack incident-detail">
      <div className="detail-topline">
        <Link to="/incidents" className="text-link back-link"><ArrowLeft size={15} /> Back to incidents</Link>
        <span className="muted small">Last updated {timeAgo(incident.updatedAt)}</span>
      </div>

      {createdNotice && (
        <Flash tone="success">
          <span>Incident <strong>{incident.reference}</strong> created and classified. The assigned technician has been notified.</span>
          <button type="button" className="flash-dismiss" onClick={() => setCreatedNotice(false)} aria-label="Dismiss">
            <XCircle size={16} />
          </button>
        </Flash>
      )}

      {incident.escalationLevel > 0 && !isClosed && status !== 'RESOLVED' && status !== 'VERIFIED' && (
        <div className="escalation-banner">
          <ShieldAlert size={19} />
          <div>
            <strong>Escalated to level {incident.escalationLevel}</strong>
            <span>This incident has not been acknowledged within its response SLA and has been raised to service management.</span>
          </div>
        </div>
      )}

      <header className="incident-header panel">
        <div className="incident-header-main">
          <div className="incident-header-badges">
            <PriorityBadge priority={incident.priority} />
            <StatusBadge status={incident.status} label={incident.statusLabel} />
            <SlaBadge status={incident.slaStatus} />
            {incident.autoClassified && <Badge tone="violet">Auto-classified</Badge>}
            {incident.duplicateOfReference && <Badge tone="amber">Duplicate review</Badge>}
          </div>
          <h1>{incident.title}</h1>
          <div className="incident-subline">
            <span className="incident-ref-large">{incident.reference}</span>
            <span>·</span>
            <span>{incident.serviceName}</span>
            <span>·</span>
            <span>{incident.categoryName}</span>
            <span>·</span>
            <span>Created {formatDateTime(incident.createdAt)}</span>
          </div>
        </div>

        <div className="incident-actions">
          {technicianAction && (
            <button
              type="button"
              className="button button-primary"
              disabled={actionLoading !== null}
              onClick={() => {
                if (technicianAction.key === 'resolve') {
                  openModal('resolve')
                  return
                }
                void execute(
                  technicianAction.key,
                  technicianAction.key === 'acknowledge'
                    ? () => incidentApi.acknowledge(incident.id)
                    : () => incidentApi.start(incident.id),
                )
              }}
            >
              {actionLoading === technicianAction.key ? <LoaderCircle className="spin" size={16} /> : <technicianAction.icon size={16} />}
              {technicianAction.label}
            </button>
          )}
          {canVerify && (
            <button
              type="button"
              className="button button-success"
              disabled={actionLoading !== null}
              onClick={() => void execute('verify', () => incidentApi.verify(incident.id))}
            >
              {actionLoading === 'verify' ? <LoaderCircle className="spin" size={16} /> : <CheckCircle2 size={16} />}
              Verify resolution
            </button>
          )}
          {canFeedback && (
            <button type="button" className="button button-secondary" onClick={() => openModal('feedback')}>
              <Star size={16} /> Leave feedback
            </button>
          )}
          {canReopen && (
            <button
              type="button"
              className="button button-ghost"
              disabled={actionLoading !== null}
              onClick={() => void execute('reopen', () => incidentApi.reopen(incident.id))}
            >
              <RotateCcw size={16} /> Reopen
            </button>
          )}
          {canAssign && (
            <button type="button" className="button button-secondary" onClick={() => openModal('assign')}>
              <UserCheck size={16} /> {incident.technicianId ? 'Reassign' : 'Assign'}
            </button>
          )}
          {canEdit && (
            <button type="button" className="button button-ghost" onClick={() => openModal('edit')}>
              <Pencil size={16} /> Edit details
            </button>
          )}
          {canOverride && (
            <button type="button" className="button button-ghost" onClick={() => openModal('priority')}>
              <Sparkles size={16} /> Priority
            </button>
          )}
          {canClose && (
            <button type="button" className="button button-primary" onClick={() => openModal('close')}>
              <CheckCircle2 size={16} /> Close incident
            </button>
          )}
          {canCancel && (
            <button type="button" className="button button-danger-ghost" onClick={() => openModal('cancel')}>
              <XCircle size={16} /> Cancel
            </button>
          )}
        </div>
      </header>

      {actionError && <Flash tone="error">{actionError}</Flash>}

      <div className="incident-detail-grid">
        <div className="stack">
          <section className="panel">
            <SectionTitle title="What happened" />
            <p className="incident-description">{incident.description}</p>
            <div className="metadata-grid">
              <div><span>Impact</span><strong>{titleCase(incident.impact)}</strong></div>
              <div><span>Urgency</span><strong>{titleCase(incident.urgency)}</strong></div>
              <div><span>Affected users</span><strong>{incident.affectedUsers.toLocaleString()}</strong></div>
              <div><span>Service criticality</span><strong>{titleCase(incident.serviceCriticality)}</strong></div>
              <div><span>Category</span><strong>{incident.categoryName}</strong></div>
              <div><span>Reported by</span><strong>{incident.reporterName}</strong></div>
            </div>
            {incident.priorityRationale && (
              <div className="priority-rationale">
                <div>
                  <Sparkles size={16} />
                  <strong>Priority rationale</strong>
                  <span className="score-pill">Score {incident.priorityScore}</span>
                </div>
                <p>{incident.priorityRationale}</p>
                {incident.autoClassified && incident.classificationConfidence != null && (
                  <span className="muted small">
                    Auto-classified with {Math.round(incident.classificationConfidence * 100)}% confidence. Administrators can override the priority.
                  </span>
                )}
              </div>
            )}
          </section>

          {(incident.duplicateOfReference || sortedDuplicates.length > 0) && (
            <section className="panel duplicate-panel">
              <div className="duplicate-heading">
                <Copy size={18} />
                <div>
                  <h2>Possible duplicate or related incident</h2>
                  <p>Similar reports can be linked so technicians do not investigate the same root cause twice.</p>
                </div>
              </div>
              {incident.duplicateOfReference && (
                <p className="duplicate-original">
                  This incident is linked as a possible duplicate of <strong>{incident.duplicateOfReference}</strong>
                  {incident.duplicateScore != null && ` (${Math.round(incident.duplicateScore * 100)}% similarity)`}.
                </p>
              )}
              {sortedDuplicates.length > 0 && (
                <ul className="duplicate-list">
                  {sortedDuplicates.map((duplicate) => (
                    <li key={duplicate.incidentId}>
                      <Link to={`/incidents/${duplicate.incidentId}`}>
                        <span>{duplicate.reference}</span>
                        <strong>{duplicate.title}</strong>
                        <em>{Math.round(duplicate.score * 100)}% match</em>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          )}

          {incident.resolutionNotes && (
            <section className="panel resolution-panel">
              <div className="resolution-heading">
                <CheckCircle2 size={19} />
                <div>
                  <h2>Resolution</h2>
                  <p>
                    {incident.resolvedAt ? `Resolved ${formatDateTime(incident.resolvedAt)}` : 'Resolution recorded'}
                    {incident.closureNotes ? ` · Closed ${formatDateTime(incident.closedAt)}` : ''}
                  </p>
                </div>
              </div>
              <p className="incident-description">{incident.resolutionNotes}</p>
              {incident.closureNotes && <div className="closure-note"><strong>Closure note</strong><p>{incident.closureNotes}</p></div>}
            </section>
          )}

          <section className="panel activity-panel">
            <div className="activity-tabs" role="tablist" aria-label="Incident activity">
              <button
                type="button"
                role="tab"
                aria-selected={tab === 'conversation'}
                className={tab === 'conversation' ? 'active' : ''}
                onClick={() => setTab('conversation')}
              >
                <MessageSquare size={16} /> Conversation <span>{comments.length}</span>
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={tab === 'audit'}
                className={tab === 'audit' ? 'active' : ''}
                onClick={() => setTab('audit')}
              >
                <History size={16} /> Audit trail <span>{audit.length}</span>
              </button>
            </div>

            {tab === 'conversation' ? (
              <div className="conversation">
                {comments.length === 0 ? (
                  <EmptyState title="No comments yet" description="Updates between the reporter and technician will appear here." icon={MessageSquare} />
                ) : (
                  <ul className="comment-list">
                    {comments.map((entry) => (
                      <li key={entry.id} className={`comment ${entry.internal ? 'comment-internal' : ''}`}>
                        <Avatar name={entry.authorName} size="sm" />
                        <div>
                          <div className="comment-head">
                            <strong>{entry.authorName}</strong>
                            <RoleBadge role={entry.authorRole} />
                            {entry.internal && <Badge tone="amber"><Lock size={11} /> Internal note</Badge>}
                            <span className="muted small">{timeAgo(entry.createdAt)}</span>
                          </div>
                          <p>{entry.message}</p>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}

                {canComment && !isClosed ? (
                  <form className="comment-form" onSubmit={submitComment}>
                    <label className="field">
                      <span className="field-label">{canInternal ? 'Add a comment' : 'Add information'}</span>
                      <textarea
                        value={comment}
                        onChange={(event) => setComment(event.target.value)}
                        placeholder={internal ? 'Internal note visible only to technicians and admins…' : 'Share an update with the incident team…'}
                        rows={3}
                        maxLength={4000}
                        required
                      />
                    </label>
                    <div className="comment-form-footer">
                      {canInternal && (
                        <label className="checkbox-field">
                          <input
                            type="checkbox"
                            checked={internal}
                            onChange={(event) => setInternal(event.target.checked)}
                          />
                          Internal note
                        </label>
                      )}
                      <button type="submit" className="button button-secondary" disabled={commentSending}>
                        {commentSending ? <LoaderCircle className="spin" size={15} /> : <Send size={15} />}
                        {commentSending ? 'Sending…' : 'Post comment'}
                      </button>
                    </div>
                    {commentError && <div className="form-error">{commentError}</div>}
                  </form>
                ) : isClosed ? (
                  <p className="muted small">This incident is closed. Reopen it to continue the conversation.</p>
                ) : null}
              </div>
            ) : (
              <ol className="timeline">
                {audit.map((entry) => (
                  <li key={entry.id}>
                    <span className="timeline-dot" aria-hidden="true" />
                    <div className="timeline-content">
                      <div className="timeline-head">
                        <strong>{titleCase(entry.action)}</strong>
                        <span className="muted small">{formatDateTime(entry.createdAt)}</span>
                      </div>
                      <p>{entry.description}</p>
                      <span className="timeline-actor">
                        {entry.actorName}
                        {entry.fromState && entry.toState && entry.fromState !== entry.toState
                          ? ` · ${titleCase(entry.fromState)} → ${titleCase(entry.toState)}`
                          : ''}
                      </span>
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </section>
        </div>

        <aside className="stack detail-side">
          <section className="panel sla-panel">
            <SectionTitle title="SLA clock" subtitle="Targets set when the incident was classified" />
            <div className="sla-row">
              <div>
                <span><Clock3 size={14} /> Response</span>
                <strong className={!incident.acknowledgedAt && isOverdue(incident.responseDueAt) ? 'text-danger' : ''}>
                  {incident.acknowledgedAt ? 'Responded' : formatDue(incident.responseDueAt)}
                </strong>
                <small>{incident.acknowledgedAt ? formatDateTime(incident.acknowledgedAt) : formatDateTime(incident.responseDueAt)}</small>
              </div>
              <Badge tone={incident.acknowledgedAt ? 'green' : 'slate'}>
                {incident.acknowledgedAt ? 'Acknowledged' : 'Pending'}
              </Badge>
            </div>
            <div className="sla-row">
              <div>
                <span><Clock3 size={14} /> Resolution</span>
                <strong className={!incident.resolvedAt && isOverdue(incident.resolutionDueAt) ? 'text-danger' : ''}>
                  {incident.resolvedAt ? 'Resolved' : formatDue(incident.resolutionDueAt)}
                </strong>
                <small>{incident.resolvedAt ? formatDateTime(incident.resolvedAt) : formatDateTime(incident.resolutionDueAt)}</small>
              </div>
              <Badge tone={incident.resolvedAt ? 'green' : 'slate'}>
                {incident.resolvedAt ? 'Resolved' : 'Open'}
              </Badge>
            </div>
            {(incident.responseBreached || incident.resolutionBreached) && (
              <WarningNote>
                {incident.responseBreached && 'Response target breached. '}
                {incident.resolutionBreached && 'Resolution target breached.'}
              </WarningNote>
            )}
          </section>

          <section className="panel">
            <SectionTitle title="People" />
            <div className="person-block">
              <Avatar name={incident.reporterName} />
              <div>
                <span>Reporter</span>
                <strong>{incident.reporterName}</strong>
                <small>{incident.reporterDepartment || incident.reporterEmail}</small>
              </div>
            </div>
            <div className="person-block">
              <Avatar name={incident.technicianName ?? 'Unassigned'} />
              <div>
                <span>Assigned technician</span>
                <strong>{incident.technicianName ?? 'Awaiting assignment'}</strong>
                <small>{incident.technicianName ? 'Support engineering' : 'An admin can assign a technician'}</small>
              </div>
            </div>
            {canAssign && (
              <button type="button" className="button button-ghost button-block" onClick={() => openModal('assign')}>
                <UserCheck size={16} /> {incident.technicianId ? 'Change technician' : 'Assign technician'}
              </button>
            )}
          </section>

          {incident.feedbackRating != null && (
            <section className="panel feedback-panel">
              <SectionTitle title="Customer feedback" />
              <div className="rating-stars" aria-label={`${incident.feedbackRating} out of 5`}>
                {[1, 2, 3, 4, 5].map((value) => (
                  <Star key={value} size={18} fill={value <= (incident.feedbackRating ?? 0) ? 'currentColor' : 'none'} />
                ))}
              </div>
              {incident.feedbackComment && <p>{incident.feedbackComment}</p>}
            </section>
          )}

          {canInternal && resolutionSuggestions.length > 0 && (
            <section className="panel suggestions-panel">
              <SectionTitle title="Suggested resolutions" subtitle="From similar past incidents" />
              <ul className="suggestion-list">
                {resolutionSuggestions.map((suggestion) => (
                  <li key={suggestion.incidentId}>
                    <div>
                      <Link to={`/incidents/${suggestion.incidentId}`}>{suggestion.reference}</Link>
                      <strong>{suggestion.title}</strong>
                      <p>{suggestion.resolutionNotes}</p>
                      <span className="muted small">{Math.round(suggestion.score * 100)}% text match</span>
                    </div>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <section className="panel facts-panel">
            <SectionTitle title="Incident facts" />
            <dl className="facts-list">
              <div><dt>Created</dt><dd>{formatDateTime(incident.createdAt)}</dd></div>
              <div><dt>Acknowledged</dt><dd>{formatDateTime(incident.acknowledgedAt)}</dd></div>
              <div><dt>Resolved</dt><dd>{formatDateTime(incident.resolvedAt)}</dd></div>
              <div><dt>Closed</dt><dd>{formatDateTime(incident.closedAt)}</dd></div>
              <div><dt>Affected users</dt><dd>{incident.affectedUsers.toLocaleString()}</dd></div>
              <div><dt>Priority score</dt><dd>{incident.priorityScore}</dd></div>
            </dl>
          </section>
        </aside>
      </div>

      <Modal
        open={modal === 'assign'}
        onClose={() => setModal(null)}
        title={incident.technicianId ? 'Reassign technician' : 'Assign a technician'}
        description="Recommendations combine skill fit, current workload, experience and service history."
        wide
      >
        {actionError && <Flash tone="error">{actionError}</Flash>}
        <ul className="recommendation-list">
          {technicianSuggestions.map((technician) => (
            <li key={technician.technicianId} className="recommendation-card">
              <div className="recommendation-main">
                <Avatar name={technician.name} size="sm" />
                <div>
                  <strong>{technician.name}</strong>
                  <span>{technician.skills.join(' · ') || 'No skills recorded'}</span>
                </div>
              </div>
              <div className="recommendation-metrics">
                <span><strong>{technician.activeIncidents}</strong> active</span>
                <span><strong>{technician.maxCapacity ?? '—'}</strong> capacity</span>
                <span><strong>{technician.score}</strong> fit score</span>
                {technician.skillMatch && <Badge tone="green">Skill match</Badge>}
              </div>
              <button
                type="button"
                className="button button-sm button-secondary"
                disabled={!technician.hasCapacity || actionLoading !== null || incident.technicianId === technician.technicianId}
                onClick={() => {
                  void execute(
                    `assign-${technician.technicianId}`,
                    () => incidentApi.assign(incident.id, technician.technicianId),
                    true,
                  )
                }}
              >
                {actionLoading === `assign-${technician.technicianId}` ? <LoaderCircle className="spin" size={14} /> : null}
                {incident.technicianId === technician.technicianId ? 'Assigned' : technician.hasCapacity ? 'Assign' : 'At capacity'}
              </button>
            </li>
          ))}
          {technicianSuggestions.length === 0 && (
            <EmptyState title="No technicians available" description="All technicians may be inactive, unavailable or at capacity." icon={Wrench} />
          )}
        </ul>
      </Modal>

      <Modal
        open={modal === 'resolve'}
        onClose={() => setModal(null)}
        title="Resolve incident"
        description="Describe what fixed the issue so the reporter can verify the resolution and future technicians can learn from it."
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModal(null)}>Cancel</button>
            <button
              type="button"
              className="button button-success"
              disabled={!resolveNotes.trim() || actionLoading !== null}
              onClick={() => void execute('resolve', () => incidentApi.resolve(incident.id, resolveNotes.trim()), true)}
            >
              {actionLoading === 'resolve' ? <LoaderCircle className="spin" size={15} /> : <CheckCircle2 size={15} />}
              Mark resolved
            </button>
          </>
        }
      >
        {actionError && <Flash tone="error">{actionError}</Flash>}
        <Field label="Resolution notes" hint="Include the fix, validation and any follow-up action.">
          <textarea
            value={resolveNotes}
            onChange={(event) => setResolveNotes(event.target.value)}
            rows={6}
            placeholder="What was the root cause and what did you do?"
            maxLength={5000}
          />
        </Field>
      </Modal>

      <Modal
        open={modal === 'close'}
        onClose={() => setModal(null)}
        title="Close incident"
        description="Closure is the final lifecycle step. The reporter has already verified the resolution."
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModal(null)}>Cancel</button>
            <button
              type="button"
              className="button button-primary"
              disabled={actionLoading !== null}
              onClick={() => void execute('close', () => incidentApi.close(incident.id, closeNotes.trim() || undefined), true)}
            >
              {actionLoading === 'close' ? <LoaderCircle className="spin" size={15} /> : <CheckCircle2 size={15} />}
              Close incident
            </button>
          </>
        }
      >
        {actionError && <Flash tone="error">{actionError}</Flash>}
        <Field label="Closure note" hint="Optional context for the audit trail.">
          <textarea
            value={closeNotes}
            onChange={(event) => setCloseNotes(event.target.value)}
            rows={4}
            placeholder="Verified by reporter. No further action required."
            maxLength={5000}
          />
        </Field>
      </Modal>

      <Modal
        open={modal === 'feedback'}
        onClose={() => setModal(null)}
        title="How did we do?"
        description="Your feedback helps the service desk measure quality and improve resolution guidance."
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModal(null)}>Cancel</button>
            <button
              type="button"
              className="button button-primary"
              disabled={actionLoading !== null}
              onClick={() => void execute('feedback', () => incidentApi.feedback(incident.id, rating, feedbackComment.trim()), true)}
            >
              {actionLoading === 'feedback' ? <LoaderCircle className="spin" size={15} /> : <Star size={15} />}
              Submit feedback
            </button>
          </>
        }
      >
        {actionError && <Flash tone="error">{actionError}</Flash>}
        <div className="rating-picker">
          {[1, 2, 3, 4, 5].map((value) => (
            <button
              key={value}
              type="button"
              className={value <= rating ? 'active' : ''}
              onClick={() => setRating(value)}
              aria-label={`${value} star${value === 1 ? '' : 's'}`}
            >
              <Star size={27} fill={value <= rating ? 'currentColor' : 'none'} />
            </button>
          ))}
        </div>
        <Field label="Comment" hint="Optional, but specific feedback is the most useful.">
          <textarea
            value={feedbackComment}
            onChange={(event) => setFeedbackComment(event.target.value)}
            rows={3}
            maxLength={1000}
            placeholder="What went well?"
          />
        </Field>
      </Modal>

      <Modal
        open={modal === 'priority'}
        onClose={() => setModal(null)}
        title="Override priority"
        description="Administrators can override the calculated priority. This updates the SLA targets and is fully audited."
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModal(null)}>Cancel</button>
            <button
              type="button"
              className="button button-primary"
              disabled={actionLoading !== null}
              onClick={() => void execute('priority', () => incidentApi.overridePriority(incident.id, priority, priorityReason.trim() || undefined), true)}
            >
              {actionLoading === 'priority' ? <LoaderCircle className="spin" size={15} /> : <Sparkles size={15} />}
              Apply override
            </button>
          </>
        }
      >
        {actionError && <Flash tone="error">{actionError}</Flash>}
        <Field label="New priority">
          <select value={priority} onChange={(event) => setPriority(event.target.value)}>
            <option value="P1">P1 — Critical</option>
            <option value="P2">P2 — High</option>
            <option value="P3">P3 — Medium</option>
            <option value="P4">P4 — Low</option>
          </select>
        </Field>
        <Field label="Reason" hint="Recorded in the audit trail.">
          <textarea value={priorityReason} onChange={(event) => setPriorityReason(event.target.value)} rows={3} maxLength={500} />
        </Field>
      </Modal>

      <Modal
        open={modal === 'cancel'}
        onClose={() => setModal(null)}
        title="Cancel incident"
        description="Cancellation is final. Use it for duplicates, invalid reports or issues resolved outside the process."
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModal(null)}>Keep incident</button>
            <button
              type="button"
              className="button button-danger"
              disabled={!cancelReason.trim() || actionLoading !== null}
              onClick={() => void execute('cancel', () => incidentApi.cancel(incident.id, cancelReason.trim()), true)}
            >
              {actionLoading === 'cancel' ? <LoaderCircle className="spin" size={15} /> : <XCircle size={15} />}
              Cancel incident
            </button>
          </>
        }
      >
        {actionError && <Flash tone="error">{actionError}</Flash>}
        <Field label="Cancellation reason">
          <textarea value={cancelReason} onChange={(event) => setCancelReason(event.target.value)} rows={4} maxLength={1000} />
        </Field>
      </Modal>

      <Modal
        open={modal === 'edit'}
        onClose={() => setModal(null)}
        title="Edit incident details"
        description="Editing re-runs classification and priority. This is only available before the technician starts work."
        wide
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModal(null)}>Cancel</button>
            <button
              type="button"
              className="button button-primary"
              disabled={!editTitle.trim() || !editDescription.trim() || actionLoading !== null}
              onClick={() => void execute('edit', () => incidentApi.update(incident.id, {
                title: editTitle.trim(),
                description: editDescription.trim(),
                impact: editImpact,
                urgency: editUrgency,
                affectedUsers: Math.max(1, editAffectedUsers),
              }), true)}
            >
              {actionLoading === 'edit' ? <LoaderCircle className="spin" size={15} /> : <Pencil size={15} />}
              Save changes
            </button>
          </>
        }
      >
        {actionError && <Flash tone="error">{actionError}</Flash>}
        <div className="form-stack">
          <Field label="Title">
            <input value={editTitle} onChange={(event) => setEditTitle(event.target.value)} maxLength={200} />
          </Field>
          <Field label="Description">
            <textarea value={editDescription} onChange={(event) => setEditDescription(event.target.value)} rows={5} maxLength={10000} />
          </Field>
          <div className="form-grid-two">
            <Field label="Impact">
              <select value={editImpact} onChange={(event) => setEditImpact(event.target.value as Impact)}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="EXTREME">Extreme</option>
              </select>
            </Field>
            <Field label="Urgency">
              <select value={editUrgency} onChange={(event) => setEditUrgency(event.target.value as Urgency)}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </Field>
          </div>
          <Field label="Affected users">
            <input
              type="number"
              min={1}
              max={1_000_000}
              value={editAffectedUsers}
              onChange={(event) => setEditAffectedUsers(Number(event.target.value))}
            />
          </Field>
        </div>
      </Modal>
    </div>
  )
}
