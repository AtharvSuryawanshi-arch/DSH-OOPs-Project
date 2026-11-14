import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Copy,
  LoaderCircle,
  Sparkles,
  WandSparkles,
} from 'lucide-react'
import { errorMessage, incidentApi, intelligenceApi } from '../api/client'
import type { Category, Impact, IntelligencePreview, Service, Urgency } from '../api/types'
import { useApi } from '../hooks/useApi'
import { Field, Flash, PageHeader, PriorityBadge, WarningNote } from '../components/ui'
import { DuplicateList } from '../components/DuplicateList'
import { titleCase } from '../lib/format'

const impacts: Array<{ value: Impact; label: string; description: string; tone: string }> = [
  { value: 'LOW', label: 'Low', description: 'A single user or minor inconvenience', tone: 'low' },
  { value: 'MEDIUM', label: 'Medium', description: 'A team or a few users are blocked', tone: 'medium' },
  { value: 'HIGH', label: 'High', description: 'A department or major workflow is blocked', tone: 'high' },
  { value: 'EXTREME', label: 'Extreme', description: 'Organisation-wide or revenue-impacting', tone: 'extreme' },
]

const urgencies: Array<{ value: Urgency; label: string; description: string }> = [
  { value: 'LOW', label: 'Low', description: 'Can wait; no immediate business impact' },
  { value: 'MEDIUM', label: 'Medium', description: 'Workaround available for now' },
  { value: 'HIGH', label: 'High', description: 'Business work is actively disrupted' },
  { value: 'CRITICAL', label: 'Critical', description: 'Immediate action is required' },
]

export function NewIncidentPage() {
  const navigate = useNavigate()
  const services = useApi<Service[]>('/catalog/services')
  const categories = useApi<Category[]>('/catalog/categories')

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [impact, setImpact] = useState<Impact>('MEDIUM')
  const [urgency, setUrgency] = useState<Urgency>('MEDIUM')
  const [affectedUsers, setAffectedUsers] = useState(1)
  const [preview, setPreview] = useState<IntelligencePreview | null>(null)
  const [analyzing, setAnalyzing] = useState(false)
  const [analysisError, setAnalysisError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const selectedService = services.data?.find((service) => String(service.id) === serviceId)

  async function analyze() {
    if (!title.trim() || !description.trim()) {
      setAnalysisError('Add a title and description first so the triage engine has something to work with.')
      return
    }
    setAnalyzing(true)
    setAnalysisError(null)
    try {
      const result = await intelligenceApi.classify(
        title.trim(),
        description.trim(),
        serviceId ? Number(serviceId) : undefined,
      )
      setPreview(result)
    } catch (cause) {
      setAnalysisError(errorMessage(cause))
    } finally {
      setAnalyzing(false)
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!serviceId) {
      setError('Choose the affected service before submitting.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const incident = await incidentApi.create({
        title: title.trim(),
        description: description.trim(),
        serviceId: Number(serviceId),
        categoryId: categoryId ? Number(categoryId) : null,
        impact,
        urgency,
        affectedUsers: Math.max(1, affectedUsers),
      })
      navigate(`/incidents/${incident.id}`, { state: { created: true } })
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="stack">
      <Link to="/incidents" className="text-link back-link"><ArrowLeft size={15} /> Back to incidents</Link>
      <PageHeader
        eyebrow="New report"
        title="Report an incident"
        description="Tell us what is broken. The service desk will classify it, set the priority and route it to the right technician."
      />

      <form className="new-incident-layout" onSubmit={handleSubmit}>
        <section className="panel form-panel">
          <div className="form-section">
            <div className="form-section-heading">
              <span>01</span>
              <div>
                <h2>Describe the problem</h2>
                <p>Concrete details help the desk reproduce and resolve the issue faster.</p>
              </div>
            </div>
            <Field label="Incident title" hint="A short, specific summary — for example “Payment gateway returns 503”.">
              <input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="What is going wrong?"
                maxLength={200}
                required
              />
            </Field>
            <Field label="What happened?" hint="Include error messages, when it started and what you have already tried.">
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Describe the symptoms, affected users and any relevant context…"
                rows={7}
                maxLength={10000}
                required
              />
            </Field>
          </div>

          <div className="form-section">
            <div className="form-section-heading">
              <span>02</span>
              <div>
                <h2>Scope and service</h2>
                <p>Which service is affected, and how many people are impacted?</p>
              </div>
            </div>
            <div className="form-grid-two">
              <Field label="Affected service">
                <select value={serviceId} onChange={(event) => setServiceId(event.target.value)} required>
                  <option value="">Select a service…</option>
                  {services.data?.map((service) => (
                    <option key={service.id} value={service.id}>
                      {service.name} · {titleCase(service.businessCriticality)}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Category" hint="Leave on auto-detect to let the triage engine classify the report.">
                <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
                  <option value="">Auto-detect category</option>
                  {categories.data?.map((category) => (
                    <option key={category.id} value={category.id}>{category.name}</option>
                  ))}
                </select>
              </Field>
            </div>
            <div className="form-grid-two">
              <Field label="People affected" hint="A rough number is enough; it contributes to the priority score.">
                <input
                  type="number"
                  min={1}
                  max={1_000_000}
                  value={affectedUsers}
                  onChange={(event) => setAffectedUsers(Number(event.target.value))}
                  required
                />
              </Field>
              <div className="service-hint">
                <span>Service criticality</span>
                <strong>{selectedService ? selectedService.businessCriticality : 'Select a service'}</strong>
                <p>Critical services raise the calculated priority even for a small user count.</p>
              </div>
            </div>
          </div>

          <div className="form-section">
            <div className="form-section-heading">
              <span>03</span>
              <div>
                <h2>Impact and urgency</h2>
                <p>Priority is calculated from these answers, the service criticality and affected users.</p>
              </div>
            </div>

            <div className="choice-group">
              <span className="field-label">Business impact</span>
              <div className="choice-grid">
                {impacts.map((option) => (
                  <button
                    type="button"
                    key={option.value}
                    className={`choice-card choice-${option.tone} ${impact === option.value ? 'choice-selected' : ''}`}
                    onClick={() => setImpact(option.value)}
                    aria-pressed={impact === option.value}
                  >
                    <strong>{option.label}</strong>
                    <span>{option.description}</span>
                  </button>
                ))}
              </div>
            </div>

            <div className="choice-group">
              <span className="field-label">Urgency</span>
              <div className="choice-grid choice-grid-compact">
                {urgencies.map((option) => (
                  <button
                    type="button"
                    key={option.value}
                    className={`choice-card ${urgency === option.value ? 'choice-selected' : ''}`}
                    onClick={() => setUrgency(option.value)}
                    aria-pressed={urgency === option.value}
                  >
                    <strong>{option.label}</strong>
                    <span>{option.description}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>

          {error && <Flash tone="error">{error}</Flash>}

          <div className="form-actions">
            <Link to="/incidents" className="button button-ghost">Cancel</Link>
            <button type="submit" className="button button-primary" disabled={submitting}>
              {submitting ? (
                <>
                  <LoaderCircle className="spin" size={17} /> Creating incident…
                </>
              ) : (
                <>
                  Create incident <ArrowRight size={17} />
                </>
              )}
            </button>
          </div>
        </section>

        <aside className="stack triage-column">
          <section className="panel triage-panel">
            <div className="triage-heading">
              <span><WandSparkles size={18} /></span>
              <div>
                <h2>Smart triage</h2>
                <p>Rule-based intelligence, explained.</p>
              </div>
            </div>
            <p className="triage-copy">
              The assistant reads the report, suggests a category and checks for incidents that may describe the
              same underlying problem. Analysis is advisory — an administrator can always override it.
            </p>
            <button type="button" className="button button-secondary button-block" onClick={analyze} disabled={analyzing}>
              {analyzing ? (
                <>
                  <LoaderCircle className="spin" size={16} /> Analysing…
                </>
              ) : (
                <>
                  <Sparkles size={16} /> Analyze this report
                </>
              )}
            </button>

            {analysisError && <Flash tone="warning">{analysisError}</Flash>}

            {preview ? (
              <div className="triage-results">
                <div className="suggestion-card">
                  <div className="suggestion-title">
                    <span><CheckCircle2 size={16} /></span>
                    <strong>{preview.classification.categoryName ?? 'Uncategorised'}</strong>
                  </div>
                  <div className="confidence-row">
                    <span>Confidence</span>
                    <strong>{Math.round(preview.classification.confidence * 100)}%</strong>
                  </div>
                  <div className="confidence-bar">
                    <span style={{ width: `${Math.round(preview.classification.confidence * 100)}%` }} />
                  </div>
                  <p>{preview.classification.rationale}</p>
                </div>

                <div className="duplicate-block">
                  <div className="triage-subheading">
                    <Copy size={15} /> Possible duplicates
                  </div>
                  {preview.possibleDuplicates.length === 0 ? (
                    <p className="muted small">No close matches in the recent incident history.</p>
                  ) : (
                    <DuplicateList duplicates={preview.possibleDuplicates} newTab />
                  )}
                </div>
              </div>
            ) : (
              <div className="triage-placeholder">
                <Sparkles size={22} />
                <p>Run the analysis to preview the category and spot possible duplicates before submitting.</p>
              </div>
            )}
          </section>

          <section className="panel priority-note">
            <h3>How priority is set</h3>
            <div className="formula">
              <span>Impact</span>
              <span>×</span>
              <span>Urgency</span>
              <span>×</span>
              <span>Service criticality</span>
            </div>
            <p>Affected users add a bonus. The score maps to P1–P4 and determines the SLA targets.</p>
            <div className="priority-scale">
              {(['P1', 'P2', 'P3', 'P4'] as const).map((priority) => (
                <span key={priority}><PriorityBadge priority={priority} /></span>
              ))}
            </div>
            <WarningNote>
              P1 incidents have a 5-minute response target and 30-minute resolution target by default. Payment
              incidents use a tighter service-specific policy.
            </WarningNote>
          </section>
        </aside>
      </form>
    </div>
  )
}
