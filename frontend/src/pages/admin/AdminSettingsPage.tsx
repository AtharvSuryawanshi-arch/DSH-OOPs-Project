import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { Check, LoaderCircle, RefreshCcw, Split, UsersRound, Workflow } from 'lucide-react'
import { adminApi, errorMessage } from '../../api/client'
import type { AssignmentStrategyType, Settings } from '../../api/types'
import { useAuth } from '../../auth/auth-context'
import { useApi } from '../../hooks/useApi'
import {
  Badge,
  ErrorPanel,
  Flash,
  PageHeader,
  PageLoader,
  SectionTitle,
} from '../../components/ui'

const strategies: Array<{
  value: AssignmentStrategyType
  title: string
  description: string
  icon: typeof Workflow
  detail: string
}> = [
  {
    value: 'SKILL_BASED',
    title: 'Skill-based',
    description: 'Best technical match, balanced by workload.',
    icon: Workflow,
    detail: 'Checks the incident category against technician skills first, then factors in current load, experience and history on the affected service.',
  },
  {
    value: 'LOAD_BASED',
    title: 'Load-based',
    description: 'Least busy qualified technician first.',
    icon: UsersRound,
    detail: 'Prioritises free capacity so work is spread evenly across the desk. Useful when most technicians can handle the category.',
  },
  {
    value: 'ROUND_ROBIN',
    title: 'Round-robin',
    description: 'Rotates assignments across available technicians.',
    icon: RefreshCcw,
    detail: 'Cycles through technicians with remaining capacity. Predictable and fair for generalist queues.',
  },
]

export function AdminSettingsPage() {
  const { user } = useAuth()
  const settings = useApi<Settings>('/admin/settings')
  const [saved, setSaved] = useState<string | null>(null)

  if (!user) return null
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />

  if (settings.loading && !settings.data) {
    return <PageLoader label="Loading assignment settings…" />
  }
  if (settings.error || !settings.data) {
    return (
      <div className="stack">
        <PageHeader title="Assignment settings" description="Configure how incidents are routed to technicians." />
        <ErrorPanel message={settings.error ?? 'Settings unavailable.'} onRetry={settings.refetch} />
      </div>
    )
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="Administration"
        title="Assignment settings"
        description="Choose the strategy used when a classified incident is auto-assigned. Administrators can always reassign manually."
        actions={<Badge tone="indigo"><Split size={12} /> Strategy engine</Badge>}
      />

      {saved && <Flash tone="success">{saved}</Flash>}

      <SettingsForm
        key={`${settings.data.assignmentStrategy}-${settings.data.autoAssign}`}
        settings={settings.data}
        onSaved={(message) => {
          setSaved(message)
          settings.refetch()
        }}
      />

      <section className="panel">
        <SectionTitle title="How routing works" subtitle="The strategy is evaluated at classification time" />
        <div className="process-steps">
          <div><span>1</span><strong>Classify</strong><p>Priority, category and SLA are calculated.</p></div>
          <div><span>2</span><strong>Build candidates</strong><p>Active technicians with remaining capacity are scored.</p></div>
          <div><span>3</span><strong>Select</strong><p>The configured strategy picks the best technician.</p></div>
          <div><span>4</span><strong>Notify</strong><p>The technician and reporter receive in-app and simulated email alerts.</p></div>
        </div>
      </section>
    </div>
  )
}

function SettingsForm({
  settings,
  onSaved,
}: {
  settings: Settings
  onSaved: (message: string) => void
}) {
  const [strategy, setStrategy] = useState<AssignmentStrategyType>(settings.assignmentStrategy)
  const [autoAssign, setAutoAssign] = useState(settings.autoAssign)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function save() {
    setSaving(true)
    setError(null)
    try {
      await adminApi.updateSettings({ assignmentStrategy: strategy, autoAssign })
      onSaved('Assignment settings saved. The new strategy applies to future classifications.')
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <section className="panel">
        <SectionTitle title="Assignment strategy" subtitle="At least two strategies are implemented and fully interchangeable." />
        {error && <Flash tone="error">{error}</Flash>}
        <div className="strategy-grid">
          {strategies.map((option) => (
            <button
              type="button"
              key={option.value}
              className={`strategy-card ${strategy === option.value ? 'strategy-selected' : ''}`}
              onClick={() => setStrategy(option.value)}
              aria-pressed={strategy === option.value}
            >
              <span className="strategy-icon"><option.icon size={20} /></span>
              <div>
                <strong>{option.title}</strong>
                <span>{option.description}</span>
                <p>{option.detail}</p>
              </div>
              {strategy === option.value && (
                <span className="strategy-check"><Check size={15} /></span>
              )}
            </button>
          ))}
        </div>

        <label className="switch-row">
          <div>
            <strong>Automatically assign on classification</strong>
            <span>When enabled, a newly classified incident is routed immediately. When disabled it stays in the admin queue.</span>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={autoAssign}
            className={`switch ${autoAssign ? 'switch-on' : ''}`}
            onClick={() => setAutoAssign((current) => !current)}
          >
            <span />
          </button>
        </label>

        <div className="form-actions">
          <button type="button" className="button button-primary" onClick={() => void save()} disabled={saving}>
            {saving ? <LoaderCircle className="spin" size={16} /> : <Check size={16} />}
            {saving ? 'Saving…' : 'Save settings'}
          </button>
        </div>
      </section>
    </>
  )
}
