import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Filter, Plus, Search, Ticket, X } from 'lucide-react'
import type { Category, Incident, PageResponse, Service } from '../api/types'
import { useAuth } from '../auth/auth-context'
import { useApi } from '../hooks/useApi'
import {
  Avatar,
  EmptyState,
  ErrorPanel,
  PageHeader,
  PageLoader,
  Pagination,
  PriorityBadge,
  SlaBadge,
  StatusBadge,
} from '../components/ui'
import { formatCompactDate, timeAgo } from '../lib/format'

function buildPath(filters: {
  search: string
  status: string
  priority: string
  categoryId: string
  serviceId: string
  slaStatus: string
  page: number
}) {
  const params = new URLSearchParams()
  if (filters.search.trim()) params.set('search', filters.search.trim())
  if (filters.status) params.set('status', filters.status)
  if (filters.priority) params.set('priority', filters.priority)
  if (filters.categoryId) params.set('categoryId', filters.categoryId)
  if (filters.serviceId) params.set('serviceId', filters.serviceId)
  if (filters.slaStatus) params.set('slaStatus', filters.slaStatus)
  params.set('page', String(filters.page))
  params.set('size', '10')
  return `/incidents?${params.toString()}`
}

export function IncidentsPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [priority, setPriority] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [slaStatus, setSlaStatus] = useState(searchParams.get('slaStatus') ?? '')
  const [page, setPage] = useState(0)

  const categories = useApi<Category[]>('/catalog/categories')
  const services = useApi<Service[]>('/catalog/services')
  const path = buildPath({ search, status, priority, categoryId, serviceId, slaStatus, page })
  const incidents = useApi<PageResponse<Incident>>(path)

  const isAdmin = user?.role === 'ADMIN'
  const canReport = Boolean(user)
  const hasFilters = Boolean(search || status || priority || categoryId || serviceId || slaStatus)

  function resetPageAnd(action: () => void) {
    setPage(0)
    action()
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    resetPageAnd(() => setSearch(searchDraft))
  }

  function clearFilters() {
    setSearchDraft('')
    setSearch('')
    setStatus('')
    setPriority('')
    setCategoryId('')
    setServiceId('')
    setSlaStatus('')
    setPage(0)
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow={isAdmin ? 'Organisation-wide queue' : user?.role === 'TECHNICIAN' ? 'Assigned to you or reported by you' : 'Your reports'}
        title={isAdmin ? 'Incident queue' : user?.role === 'TECHNICIAN' ? 'My incidents' : 'My incidents'}
        description={
          isAdmin
            ? 'Search, filter and triage every incident in the organisation.'
            : user?.role === 'TECHNICIAN'
              ? 'Incidents assigned to you plus any you reported yourself, with SLA state and ownership.'
              : 'Track the incidents you have reported and their resolution progress.'
        }
        actions={
          canReport ? (
            <Link to="/incidents/new" className="button button-primary">
              <Plus size={16} /> Report incident
            </Link>
          ) : undefined
        }
      />

      <section className="panel">
        <form className="filter-bar" onSubmit={handleSearch}>
          <label className="search-field">
            <Search size={17} />
            <input
              value={searchDraft}
              onChange={(event) => setSearchDraft(event.target.value)}
              placeholder="Search title, description or INC number…"
              aria-label="Search incidents"
            />
            {searchDraft && (
              <button
                type="button"
                className="input-trailing-button"
                onClick={() => {
                  setSearchDraft('')
                  resetPageAnd(() => setSearch(''))
                }}
                aria-label="Clear search"
              >
                <X size={15} />
              </button>
            )}
          </label>

          <div className="filter-controls">
            <span className="filter-label"><Filter size={14} /> Filters</span>
            <select
              value={status}
              onChange={(event) => resetPageAnd(() => setStatus(event.target.value))}
              aria-label="Filter by status"
            >
              <option value="">All statuses</option>
              <option value="NEW">New</option>
              <option value="CLASSIFIED">Classified</option>
              <option value="ASSIGNED">Assigned</option>
              <option value="ACKNOWLEDGED">Acknowledged</option>
              <option value="IN_PROGRESS">In progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="VERIFIED">Verified</option>
              <option value="CLOSED">Closed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
            <select
              value={priority}
              onChange={(event) => resetPageAnd(() => setPriority(event.target.value))}
              aria-label="Filter by priority"
            >
              <option value="">All priorities</option>
              <option value="P1">P1 — Critical</option>
              <option value="P2">P2 — High</option>
              <option value="P3">P3 — Medium</option>
              <option value="P4">P4 — Low</option>
            </select>
            <select
              value={categoryId}
              onChange={(event) => resetPageAnd(() => setCategoryId(event.target.value))}
              aria-label="Filter by category"
            >
              <option value="">All categories</option>
              {categories.data?.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
            <select
              value={serviceId}
              onChange={(event) => resetPageAnd(() => setServiceId(event.target.value))}
              aria-label="Filter by service"
            >
              <option value="">All services</option>
              {services.data?.map((service) => (
                <option key={service.id} value={service.id}>{service.name}</option>
              ))}
            </select>
            <select
              value={slaStatus}
              onChange={(event) => resetPageAnd(() => setSlaStatus(event.target.value))}
              aria-label="Filter by SLA status"
            >
              <option value="">Any SLA</option>
              <option value="ON_TRACK">On track</option>
              <option value="AT_RISK">At risk</option>
              <option value="BREACHED">Breached</option>
              <option value="MET">Met</option>
            </select>
            <button type="submit" className="button button-secondary button-sm">Apply</button>
            {hasFilters && (
              <button type="button" className="button button-ghost button-sm" onClick={clearFilters}>
                Clear
              </button>
            )}
          </div>
        </form>

        {incidents.error ? (
          <ErrorPanel message={incidents.error} onRetry={incidents.refetch} />
        ) : incidents.loading && !incidents.data ? (
          <PageLoader label="Loading incidents…" />
        ) : !incidents.data || incidents.data.content.length === 0 ? (
          <EmptyState
            title={hasFilters ? 'No incidents match these filters' : 'No incidents yet'}
            description={hasFilters ? 'Try broadening the search or clearing one of the filters.' : 'Reported incidents will appear here.'}
            icon={Ticket}
            action={hasFilters ? <button className="button button-secondary button-sm" onClick={clearFilters}>Clear filters</button> : undefined}
          />
        ) : (
          <>
            <div className={`table-wrap ${incidents.loading ? 'table-loading' : ''}`}>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Incident</th>
                    <th>Priority</th>
                    <th>Status</th>
                    <th>Service</th>
                    <th>Reporter</th>
                    <th>Technician</th>
                    <th>SLA</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {incidents.data.content.map((incident) => (
                    <tr key={incident.id} onClick={() => navigate(`/incidents/${incident.id}`)} className="clickable-row">
                      <td>
                        <div className="incident-cell">
                          <span className="incident-ref">{incident.reference}</span>
                          <span className="incident-title">{incident.title}</span>
                          {incident.duplicateOfReference && <span className="inline-note">Possible duplicate</span>}
                        </div>
                      </td>
                      <td><PriorityBadge priority={incident.priority} /></td>
                      <td><StatusBadge status={incident.status} label={incident.statusLabel} /></td>
                      <td>{incident.serviceName}</td>
                      <td>{incident.reporterName}</td>
                      <td>
                        {incident.technicianName ? (
                          <span className="person-inline"><Avatar name={incident.technicianName} size="sm" />{incident.technicianName}</span>
                        ) : (
                          <span className="muted">Unassigned</span>
                        )}
                      </td>
                      <td><SlaBadge status={incident.slaStatus} /></td>
                      <td className="muted" title={formatCompactDate(incident.createdAt)}>{timeAgo(incident.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={incidents.data.page}
              totalPages={incidents.data.totalPages}
              totalElements={incidents.data.totalElements}
              onPageChange={setPage}
            />
          </>
        )}
      </section>
    </div>
  )
}
