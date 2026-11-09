import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Bell,
  CheckCheck,
  Copy,
  LoaderCircle,
  MessageSquare,
  ShieldAlert,
  UserCheck,
  type LucideIcon,
} from 'lucide-react'
import { errorMessage, notificationApi } from '../api/client'
import type { Notification } from '../api/types'
import { useApi } from '../hooks/useApi'
import {
  Badge,
  EmptyState,
  ErrorPanel,
  PageHeader,
  PageLoader,
} from '../components/ui'
import { timeAgo } from '../lib/format'

const typeIcons: Record<string, LucideIcon> = {
  SLA_BREACHED: ShieldAlert,
  SLA_WARNING: ShieldAlert,
  ESCALATION: ShieldAlert,
  INCIDENT_ASSIGNED: UserCheck,
  STATUS_CHANGED: CheckCheck,
  COMMENT_ADDED: MessageSquare,
  DUPLICATE_DETECTED: Copy,
  INCIDENT_CREATED: Bell,
}

type Filter = 'all' | 'unread' | 'IN_APP' | 'EMAIL'

export function NotificationsPage() {
  const navigate = useNavigate()
  const notifications = useApi<Notification[]>('/notifications')
  const [filter, setFilter] = useState<Filter>('all')
  const [updating, setUpdating] = useState<number | 'all' | null>(null)
  const [error, setError] = useState<string | null>(null)

  const all = notifications.data ?? []
  const unread = all.filter((notification) => !notification.read)
  const visible = all.filter((notification) => {
    if (filter === 'unread') return !notification.read
    if (filter === 'IN_APP' || filter === 'EMAIL') return notification.channel === filter
    return true
  })

  async function markAll() {
    setUpdating('all')
    setError(null)
    try {
      await notificationApi.markAllRead()
      notifications.refetch()
      window.dispatchEvent(new Event('itsm:notifications'))
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setUpdating(null)
    }
  }

  async function open(notification: Notification) {
    setUpdating(notification.id)
    setError(null)
    try {
      if (!notification.read) {
        await notificationApi.markRead(notification.id)
        notifications.refetch()
        window.dispatchEvent(new Event('itsm:notifications'))
      }
      if (notification.incidentId) {
        navigate(`/incidents/${notification.incidentId}`)
      }
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setUpdating(null)
    }
  }

  if (notifications.loading && !notifications.data) {
    return <PageLoader label="Loading notifications…" />
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="Inbox"
        title="Notifications"
        description="In-app alerts and simulated email notifications for assignments, SLA events and incident updates."
        actions={
          <button
            type="button"
            className="button button-secondary"
            onClick={() => void markAll()}
            disabled={unread.length === 0 || updating !== null}
          >
            {updating === 'all' ? <LoaderCircle className="spin" size={16} /> : <CheckCheck size={16} />}
            Mark all read
          </button>
        }
      />

      <section className="panel">
        <div className="segmented-tabs notification-tabs">
          {([
            ['all', `All ${all.length}`],
            ['unread', `Unread ${unread.length}`],
            ['IN_APP', 'In-app'],
            ['EMAIL', 'Email log'],
          ] as const).map(([value, label]) => (
            <button
              type="button"
              key={value}
              className={filter === value ? 'active' : ''}
              onClick={() => setFilter(value)}
            >
              {label}
            </button>
          ))}
        </div>

        {error && <div className="form-error">{error}</div>}
        {notifications.error ? (
          <ErrorPanel message={notifications.error} onRetry={notifications.refetch} />
        ) : visible.length === 0 ? (
          <EmptyState
            title={filter === 'unread' ? 'You are all caught up' : 'No notifications here'}
            description="Assignment, SLA and status notifications will appear in this inbox."
            icon={Bell}
          />
        ) : (
          <ul className="notification-list">
            {visible.map((notification) => {
              const Icon = typeIcons[notification.type] ?? Bell
              return (
                <li key={notification.id} className={notification.read ? '' : 'notification-unread'}>
                  <button type="button" className="notification-item" onClick={() => void open(notification)}>
                    <span className={`notification-icon notification-${notification.type.toLowerCase()}`}>
                      {updating === notification.id ? <LoaderCircle className="spin" size={18} /> : <Icon size={18} />}
                    </span>
                    <span className="notification-body">
                      <span className="notification-title-row">
                        <strong>{notification.title}</strong>
                        {!notification.read && <span className="unread-dot" aria-label="Unread" />}
                      </span>
                      <span className="notification-message">{notification.message}</span>
                      <span className="notification-meta">
                        <Badge tone={notification.channel === 'EMAIL' ? 'amber' : 'blue'}>
                          {notification.channel === 'EMAIL' ? 'Email (simulated)' : 'In-app'}
                        </Badge>
                        <span>{timeAgo(notification.createdAt)}</span>
                        {notification.incidentReference && <span>{notification.incidentReference}</span>}
                      </span>
                    </span>
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </section>
    </div>
  )
}
