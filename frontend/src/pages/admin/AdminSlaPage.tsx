import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { Clock3, LoaderCircle, Pencil, Plus, ShieldCheck } from 'lucide-react'
import { adminApi, errorMessage } from '../../api/client'
import type { Priority, Service, SlaPolicy, SlaPolicyPayload } from '../../api/types'
import { useAuth } from '../../auth/auth-context'
import { useApi } from '../../hooks/useApi'
import {
  Badge,
  EmptyState,
  ErrorPanel,
  Field,
  Flash,
  Modal,
  PageHeader,
  PageLoader,
  PriorityBadge,
} from '../../components/ui'
import { formatMinutes } from '../../lib/format'

const priorities: Priority[] = ['P1', 'P2', 'P3', 'P4']

export function AdminSlaPage() {
  const { user } = useAuth()
  const policies = useApi<SlaPolicy[]>('/admin/sla-policies')
  const services = useApi<Service[]>('/admin/services')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<SlaPolicy | null>(null)
  const [form, setForm] = useState<SlaPolicyPayload>({
    name: '',
    priority: 'P2',
    serviceId: null,
    responseMinutes: 15,
    resolutionMinutes: 120,
    warningPercent: 80,
    active: true,
  })
  const [saving, setSaving] = useState(false)
  const [scanning, setScanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  if (!user) return null
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />

  function open(policy?: SlaPolicy) {
    setEditing(policy ?? null)
    setForm(policy ? {
      name: policy.name,
      priority: policy.priority,
      serviceId: policy.serviceId ?? null,
      responseMinutes: policy.responseMinutes,
      resolutionMinutes: policy.resolutionMinutes,
      warningPercent: policy.warningPercent,
      active: policy.active,
    } : {
      name: '',
      priority: 'P2',
      serviceId: null,
      responseMinutes: 15,
      resolutionMinutes: 120,
      warningPercent: 80,
      active: true,
    })
    setError(null)
    setModalOpen(true)
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (form.resolutionMinutes <= form.responseMinutes) {
      setError('The resolution target must be longer than the response target.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      if (editing) {
        await adminApi.updateSlaPolicy(editing.id, form)
      } else {
        await adminApi.createSlaPolicy(form)
      }
      policies.refetch()
      setModalOpen(false)
      setMessage('SLA policy saved. New incidents will use the updated targets.')
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  async function runScan() {
    setScanning(true)
    setError(null)
    setMessage(null)
    try {
      await adminApi.scanSla()
      setMessage('SLA scan completed. Warnings, breaches and escalations are up to date.')
      policies.refetch()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setScanning(false)
    }
  }

  if (policies.loading && !policies.data) {
    return <PageLoader label="Loading SLA policies…" />
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="Administration"
        title="SLA policies"
        description="Response and resolution targets per priority. A service-specific policy overrides the global policy for that service."
        actions={
          <>
            <button type="button" className="button button-secondary" onClick={() => void runScan()} disabled={scanning}>
              <ShieldCheck size={16} /> {scanning ? 'Scanning…' : 'Run SLA scan'}
            </button>
            <button type="button" className="button button-primary" onClick={() => open()}>
              <Plus size={16} /> Add policy
            </button>
          </>
        }
      />

      {message && <Flash tone="success">{message}</Flash>}
      {error && <Flash tone="error">{error}</Flash>}

      <section className="panel">
        {policies.error ? (
          <ErrorPanel message={policies.error} onRetry={policies.refetch} />
        ) : !policies.data || policies.data.length === 0 ? (
          <EmptyState title="No SLA policies" description="Create global targets before incidents can be classified." icon={Clock3} />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Policy</th>
                  <th>Priority</th>
                  <th>Scope</th>
                  <th>Response target</th>
                  <th>Resolution target</th>
                  <th>Warning at</th>
                  <th>Status</th>
                  <th aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {policies.data.map((policy) => (
                  <tr key={policy.id}>
                    <td><strong>{policy.name}</strong></td>
                    <td><PriorityBadge priority={policy.priority} /></td>
                    <td>{policy.serviceName ? <Badge tone="violet">{policy.serviceName}</Badge> : <span className="muted">All services</span>}</td>
                    <td><strong>{formatMinutes(policy.responseMinutes)}</strong></td>
                    <td><strong>{formatMinutes(policy.resolutionMinutes)}</strong></td>
                    <td>{policy.warningPercent}% of target</td>
                    <td><Badge tone={policy.active ? 'green' : 'slate'}>{policy.active ? 'Active' : 'Inactive'}</Badge></td>
                    <td>
                      <button type="button" className="icon-button" onClick={() => open(policy)} aria-label={`Edit ${policy.name}`}>
                        <Pencil size={15} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="panel sla-explainer">
        <h2>How SLA clocks work</h2>
        <div className="explainer-grid">
          <div>
            <strong>1. Classify</strong>
            <p>The priority is calculated from impact, urgency, service criticality and affected users.</p>
          </div>
          <div>
            <strong>2. Resolve the policy</strong>
            <p>A service-specific policy wins; otherwise the global policy for that priority applies.</p>
          </div>
          <div>
            <strong>3. Measure</strong>
            <p>Acknowledging stops the response clock. Resolution stops the resolution clock. A monitor records warnings and breaches.</p>
          </div>
        </div>
      </section>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? `Edit ${editing.name}` : 'Add SLA policy'}
        description="Targets are expressed in minutes from incident creation."
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModalOpen(false)}>Cancel</button>
            <button type="submit" form="sla-form" className="button button-primary" disabled={saving}>
              {saving ? <LoaderCircle className="spin" size={15} /> : null}
              {editing ? 'Save policy' : 'Create policy'}
            </button>
          </>
        }
      >
        {error && <Flash tone="error">{error}</Flash>}
        <form id="sla-form" className="form-stack" onSubmit={save}>
          <Field label="Policy name">
            <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required maxLength={120} />
          </Field>
          <div className="form-grid-two">
            <Field label="Priority">
              <select value={form.priority} onChange={(event) => setForm({ ...form, priority: event.target.value as Priority })}>
                {priorities.map((priority) => <option key={priority} value={priority}>{priority}</option>)}
              </select>
            </Field>
            <Field label="Service scope" hint="Leave empty for the global policy.">
              <select
                value={form.serviceId ?? ''}
                onChange={(event) => setForm({ ...form, serviceId: event.target.value ? Number(event.target.value) : null })}
              >
                <option value="">All services</option>
                {services.data?.map((service) => (
                  <option key={service.id} value={service.id}>{service.name}</option>
                ))}
              </select>
            </Field>
          </div>
          <div className="form-grid-two">
            <Field label="Response target (minutes)">
              <input
                type="number"
                min={1}
                value={form.responseMinutes}
                onChange={(event) => setForm({ ...form, responseMinutes: Number(event.target.value) })}
                required
              />
            </Field>
            <Field label="Resolution target (minutes)">
              <input
                type="number"
                min={1}
                value={form.resolutionMinutes}
                onChange={(event) => setForm({ ...form, resolutionMinutes: Number(event.target.value) })}
                required
              />
            </Field>
          </div>
          <Field label="Warning threshold (%)" hint="A warning notification fires when this percentage of the target has elapsed.">
            <input
              type="number"
              min={1}
              max={99}
              value={form.warningPercent}
              onChange={(event) => setForm({ ...form, warningPercent: Number(event.target.value) })}
              required
            />
          </Field>
          <label className="checkbox-field">
            <input type="checkbox" checked={form.active ?? true} onChange={(event) => setForm({ ...form, active: event.target.checked })} />
            Active policy
          </label>
        </form>
      </Modal>
    </div>
  )
}
