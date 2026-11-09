import { useEffect, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import {
  AlertTriangle,
  Check,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  LoaderCircle,
  X,
  type LucideIcon,
} from 'lucide-react'
import type { Priority, Role, SlaStatus } from '../api/types'
import { titleCase } from '../lib/format'

type Tone = 'slate' | 'blue' | 'indigo' | 'violet' | 'amber' | 'red' | 'green' | 'cyan'

export function Badge({
  children,
  tone = 'slate',
  dot = false,
}: {
  children: ReactNode
  tone?: Tone
  dot?: boolean
}) {
  return (
    <span className={`badge badge-${tone}`}>
      {dot && <span className="badge-dot" aria-hidden="true" />}
      {children}
    </span>
  )
}

const priorityTones: Record<Priority, Tone> = {
  P1: 'red',
  P2: 'amber',
  P3: 'blue',
  P4: 'slate',
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <Badge tone={priorityTones[priority] ?? 'slate'}>
      <span className="priority-code">{priority}</span>
    </Badge>
  )
}

const statusTones: Record<string, Tone> = {
  NEW: 'slate',
  CLASSIFIED: 'blue',
  ASSIGNED: 'indigo',
  ACKNOWLEDGED: 'cyan',
  IN_PROGRESS: 'amber',
  RESOLVED: 'green',
  VERIFIED: 'violet',
  CLOSED: 'slate',
  CANCELLED: 'slate',
}

export function StatusBadge({ status, label }: { status: string; label?: string }) {
  return (
    <Badge tone={statusTones[status] ?? 'slate'} dot>
      {label ?? titleCase(status)}
    </Badge>
  )
}

const slaTones: Record<SlaStatus, Tone> = {
  ON_TRACK: 'green',
  AT_RISK: 'amber',
  BREACHED: 'red',
  MET: 'blue',
}

export function SlaBadge({ status }: { status: SlaStatus }) {
  return (
    <Badge tone={slaTones[status] ?? 'slate'} dot>
      {status === 'ON_TRACK' ? 'On track' : titleCase(status)}
    </Badge>
  )
}

export function RoleBadge({ role }: { role: Role }) {
  const tone: Tone = role === 'ADMIN' ? 'violet' : role === 'TECHNICIAN' ? 'blue' : 'slate'
  return <Badge tone={tone}>{titleCase(role)}</Badge>
}

export function Avatar({ name, size = 'md' }: { name: string; size?: 'sm' | 'md' | 'lg' }) {
  const parts = name.split(' ').filter(Boolean)
  const initials = parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
  return (
    <span className={`avatar avatar-${size}`} aria-hidden="true">
      {initials}
    </span>
  )
}

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string
  title: string
  description?: string
  actions?: ReactNode
}) {
  return (
    <header className="page-header">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p className="page-description">{description}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  )
}

export function StatCard({
  label,
  value,
  hint,
  icon: Icon,
  tone = 'indigo',
}: {
  label: string
  value: string | number
  hint?: string
  icon: LucideIcon
  tone?: Tone
}) {
  return (
    <div className={`stat-card stat-${tone}`}>
      <div className="stat-icon">
        <Icon size={20} />
      </div>
      <div>
        <p className="stat-label">{label}</p>
        <p className="stat-value">{value}</p>
        {hint && <p className="stat-hint">{hint}</p>}
      </div>
    </div>
  )
}

export function ProgressBar({
  value,
  max,
  tone = 'indigo',
}: {
  value: number
  max: number
  tone?: Tone
}) {
  const percentage = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0
  return (
    <div
      className={`progress progress-${tone}`}
      role="progressbar"
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={max}
    >
      <span style={{ width: `${percentage}%` }} />
    </div>
  )
}

export function Spinner({ label }: { label?: string }) {
  return (
    <span className="spinner-wrap">
      <LoaderCircle className="spin" size={18} />
      {label && <span>{label}</span>}
    </span>
  )
}

export function PageLoader({ label = 'Loading workspace…' }: { label?: string }) {
  return (
    <div className="page-loader">
      <LoaderCircle className="spin" size={24} />
      <span>{label}</span>
    </div>
  )
}

export function ErrorPanel({
  message,
  onRetry,
}: {
  message: string
  onRetry?: () => void
}) {
  return (
    <div className="error-panel" role="alert">
      <CircleAlert size={20} />
      <div>
        <strong>Something went wrong</strong>
        <p>{message}</p>
      </div>
      {onRetry && (
        <button type="button" className="button button-ghost button-sm" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  )
}

export function EmptyState({
  title,
  description,
  icon: Icon = Check,
  action,
}: {
  title: string
  description?: string
  icon?: LucideIcon
  action?: ReactNode
}) {
  return (
    <div className="empty-state">
      <span className="empty-state-icon">
        <Icon size={22} />
      </span>
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action && <div className="empty-state-action">{action}</div>}
    </div>
  )
}

export function Modal({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  wide = false,
}: {
  open: boolean
  onClose: () => void
  title: string
  description?: string
  children: ReactNode
  footer?: ReactNode
  wide?: boolean
}) {
  useEffect(() => {
    if (!open) {
      return
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    document.body.classList.add('modal-open')
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.classList.remove('modal-open')
    }
  }, [open, onClose])

  if (!open) {
    return null
  }

  return createPortal(
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div
        className={`modal ${wide ? 'modal-wide' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modal-header">
          <div>
            <h2 id="modal-title">{title}</h2>
            {description && <p>{description}</p>}
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close dialog">
            <X size={18} />
          </button>
        </header>
        <div className="modal-body">{children}</div>
        {footer && <footer className="modal-footer">{footer}</footer>}
      </div>
    </div>,
    document.body,
  )
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
}: {
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}) {
  return (
    <div className="pagination">
      <span>
        Page {page + 1} of {Math.max(totalPages, 1)} · {totalElements} records
      </span>
      <div>
        <button
          type="button"
          className="icon-button"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
          aria-label="Previous page"
        >
          <ChevronLeft size={17} />
        </button>
        <button
          type="button"
          className="icon-button"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
          aria-label="Next page"
        >
          <ChevronRight size={17} />
        </button>
      </div>
    </div>
  )
}

export function Flash({
  tone = 'info',
  children,
}: {
  tone?: 'info' | 'success' | 'error' | 'warning'
  children: ReactNode
}) {
  return <div className={`flash flash-${tone}`}>{children}</div>
}

export function SectionTitle({
  title,
  subtitle,
  action,
}: {
  title: string
  subtitle?: string
  action?: ReactNode
}) {
  return (
    <div className="section-title">
      <div>
        <h2>{title}</h2>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {action}
    </div>
  )
}

export function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <div className="brand-lockup">
      <span className="brand-glyph">
        <Check size={18} strokeWidth={3} />
      </span>
      {!compact && (
        <span>
          <strong>I-ITSM</strong>
          <small>Incident workspace</small>
        </span>
      )}
    </div>
  )
}

export function IconText({ icon: Icon, children }: { icon: LucideIcon; children: ReactNode }) {
  return (
    <span className="icon-text">
      <Icon size={15} />
      {children}
    </span>
  )
}

export function WarningNote({ children }: { children: ReactNode }) {
  return (
    <div className="warning-note">
      <AlertTriangle size={17} />
      <span>{children}</span>
    </div>
  )
}

export function Field({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: ReactNode
}) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {hint && <span className="field-hint">{hint}</span>}
    </label>
  )
}
