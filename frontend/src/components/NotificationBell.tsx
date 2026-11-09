import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { notificationApi } from '../api/client'

export function NotificationBell() {
  const [count, setCount] = useState(0)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const response = await notificationApi.unreadCount()
        if (!cancelled) {
          setCount(response.count)
        }
      } catch {
        // The header should not break if the API is temporarily unavailable.
      }
    }

    void load()
    const interval = window.setInterval(() => void load(), 30_000)
    const refresh = () => void load()
    window.addEventListener('itsm:notifications', refresh)

    return () => {
      cancelled = true
      window.clearInterval(interval)
      window.removeEventListener('itsm:notifications', refresh)
    }
  }, [])

  return (
    <Link to="/notifications" className="notification-bell" aria-label={`Notifications (${count} unread)`}>
      <Bell size={19} />
      {count > 0 && <span className="notification-count">{count > 99 ? '99+' : count}</span>}
    </Link>
  )
}
