import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { Boxes, LoaderCircle, Pencil, Plus, Search, Tags } from 'lucide-react'
import { adminApi, errorMessage } from '../../api/client'
import type {
  Category,
  CategoryPayload,
  Service,
  ServicePayload,
} from '../../api/types'
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
} from '../../components/ui'
import { titleCase } from '../../lib/format'

type Tab = 'services' | 'categories'
type ModalKind = 'service' | 'category' | null

const criticalities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

export function AdminCatalogPage() {
  const { user } = useAuth()
  const services = useApi<Service[]>('/admin/services')
  const categories = useApi<Category[]>('/admin/categories')
  const [tab, setTab] = useState<Tab>('services')
  const [search, setSearch] = useState('')
  const [modal, setModal] = useState<ModalKind>(null)
  const [editingService, setEditingService] = useState<Service | null>(null)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)
  const [serviceForm, setServiceForm] = useState<ServicePayload>({
    code: '', name: '', description: '', businessCriticality: 'MEDIUM', active: true,
  })
  const [categoryForm, setCategoryForm] = useState<CategoryPayload>({
    code: '', name: '', description: '', active: true,
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!user) return null
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />

  const serviceList = (services.data ?? []).filter((service) =>
    `${service.name} ${service.code}`.toLowerCase().includes(search.toLowerCase()),
  )
  const categoryList = (categories.data ?? []).filter((category) =>
    `${category.name} ${category.code}`.toLowerCase().includes(search.toLowerCase()),
  )

  function openService(service?: Service) {
    setEditingService(service ?? null)
    setServiceForm(service ? {
      code: service.code,
      name: service.name,
      description: service.description ?? '',
      businessCriticality: service.businessCriticality,
      active: service.active,
    } : { code: '', name: '', description: '', businessCriticality: 'MEDIUM', active: true })
    setError(null)
    setModal('service')
  }

  function openCategory(category?: Category) {
    setEditingCategory(category ?? null)
    setCategoryForm(category ? {
      code: category.code,
      name: category.name,
      description: category.description ?? '',
      active: category.active,
    } : { code: '', name: '', description: '', active: true })
    setError(null)
    setModal('category')
  }

  async function saveService(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      if (editingService) {
        await adminApi.updateService(editingService.id, { ...serviceForm, code: editingService.code })
      } else {
        await adminApi.createService(serviceForm)
      }
      services.refetch()
      setModal(null)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  async function saveCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      if (editingCategory) {
        await adminApi.updateCategory(editingCategory.id, { ...categoryForm, code: editingCategory.code })
      } else {
        await adminApi.createCategory(categoryForm)
      }
      categories.refetch()
      setModal(null)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  if ((services.loading && !services.data) || (categories.loading && !categories.data)) {
    return <PageLoader label="Loading service catalogue…" />
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="Administration"
        title="Services & categories"
        description="Keep the catalogue current. Service criticality feeds priority; categories power classification and technician skills."
        actions={
          <button
            type="button"
            className="button button-primary"
            onClick={() => (tab === 'services' ? openService() : openCategory())}
          >
            <Plus size={16} /> Add {tab === 'services' ? 'service' : 'category'}
          </button>
        }
      />

      {error && <Flash tone="error">{error}</Flash>}

      <section className="panel">
        <div className="catalog-toolbar">
          <div className="segmented-tabs">
            <button type="button" className={tab === 'services' ? 'active' : ''} onClick={() => setTab('services')}>
              <Boxes size={15} /> Services <span>{services.data?.length ?? 0}</span>
            </button>
            <button type="button" className={tab === 'categories' ? 'active' : ''} onClick={() => setTab('categories')}>
              <Tags size={15} /> Categories <span>{categories.data?.length ?? 0}</span>
            </button>
          </div>
          <label className="search-field search-field-small">
            <Search size={16} />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search catalogue…" />
          </label>
        </div>

        {tab === 'services' ? (
          services.error ? (
            <ErrorPanel message={services.error} onRetry={services.refetch} />
          ) : serviceList.length === 0 ? (
            <EmptyState title="No services yet" description="Add the IT services that incidents can affect." icon={Boxes} />
          ) : (
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Service</th>
                    <th>Code</th>
                    <th>Business criticality</th>
                    <th>Status</th>
                    <th aria-label="Actions" />
                  </tr>
                </thead>
                <tbody>
                  {serviceList.map((service) => (
                    <tr key={service.id}>
                      <td>
                        <strong>{service.name}</strong>
                        {service.description && <p className="table-subtext">{service.description}</p>}
                      </td>
                      <td><code>{service.code}</code></td>
                      <td>
                        <Badge tone={service.businessCriticality === 'CRITICAL' ? 'red' : service.businessCriticality === 'HIGH' ? 'amber' : service.businessCriticality === 'MEDIUM' ? 'blue' : 'slate'}>
                          {titleCase(service.businessCriticality)}
                        </Badge>
                      </td>
                      <td><Badge tone={service.active ? 'green' : 'slate'}>{service.active ? 'Active' : 'Inactive'}</Badge></td>
                      <td>
                        <button type="button" className="icon-button" onClick={() => openService(service)} aria-label={`Edit ${service.name}`}>
                          <Pencil size={15} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        ) : categories.error ? (
          <ErrorPanel message={categories.error} onRetry={categories.refetch} />
        ) : categoryList.length === 0 ? (
          <EmptyState title="No categories yet" description="Add incident categories to power classification." icon={Tags} />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Category</th>
                  <th>Code</th>
                  <th>Description</th>
                  <th>Status</th>
                  <th aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {categoryList.map((category) => (
                  <tr key={category.id}>
                    <td><strong>{category.name}</strong></td>
                    <td><code>{category.code}</code></td>
                    <td className="muted">{category.description || '—'}</td>
                    <td><Badge tone={category.active ? 'green' : 'slate'}>{category.active ? 'Active' : 'Inactive'}</Badge></td>
                    <td>
                      <button type="button" className="icon-button" onClick={() => openCategory(category)} aria-label={`Edit ${category.name}`}>
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

      <Modal
        open={modal === 'service'}
        onClose={() => setModal(null)}
        title={editingService ? `Edit ${editingService.name}` : 'Add a service'}
        description="Business criticality influences the calculated priority for every incident on this service."
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModal(null)}>Cancel</button>
            <button type="submit" form="service-form" className="button button-primary" disabled={saving}>
              {saving ? <LoaderCircle className="spin" size={15} /> : null}
              {editingService ? 'Save service' : 'Create service'}
            </button>
          </>
        }
      >
        {error && <Flash tone="error">{error}</Flash>}
        <form id="service-form" className="form-stack" onSubmit={saveService}>
          <div className="form-grid-two">
            <Field label="Service name">
              <input value={serviceForm.name} onChange={(event) => setServiceForm({ ...serviceForm, name: event.target.value })} required maxLength={120} />
            </Field>
            <Field label="Code">
              <input value={serviceForm.code} onChange={(event) => setServiceForm({ ...serviceForm, code: event.target.value.toUpperCase() })} disabled={Boolean(editingService)} required maxLength={40} />
            </Field>
          </div>
          <Field label="Description">
            <textarea value={serviceForm.description} onChange={(event) => setServiceForm({ ...serviceForm, description: event.target.value })} rows={3} maxLength={500} />
          </Field>
          <div className="form-grid-two">
            <Field label="Business criticality">
              <select value={serviceForm.businessCriticality} onChange={(event) => setServiceForm({ ...serviceForm, businessCriticality: event.target.value })}>
                {criticalities.map((criticality) => (
                  <option key={criticality} value={criticality}>{titleCase(criticality)}</option>
                ))}
              </select>
            </Field>
            <label className="checkbox-field checkbox-field-spaced">
              <input type="checkbox" checked={serviceForm.active} onChange={(event) => setServiceForm({ ...serviceForm, active: event.target.checked })} />
              Active and selectable
            </label>
          </div>
        </form>
      </Modal>

      <Modal
        open={modal === 'category'}
        onClose={() => setModal(null)}
        title={editingCategory ? `Edit ${editingCategory.name}` : 'Add a category'}
        description="Categories are used by auto-classification and as technician skills."
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModal(null)}>Cancel</button>
            <button type="submit" form="category-form" className="button button-primary" disabled={saving}>
              {saving ? <LoaderCircle className="spin" size={15} /> : null}
              {editingCategory ? 'Save category' : 'Create category'}
            </button>
          </>
        }
      >
        {error && <Flash tone="error">{error}</Flash>}
        <form id="category-form" className="form-stack" onSubmit={saveCategory}>
          <div className="form-grid-two">
            <Field label="Category name">
              <input value={categoryForm.name} onChange={(event) => setCategoryForm({ ...categoryForm, name: event.target.value })} required maxLength={80} />
            </Field>
            <Field label="Code" hint="Used by the keyword classifier.">
              <input value={categoryForm.code} onChange={(event) => setCategoryForm({ ...categoryForm, code: event.target.value.toUpperCase() })} disabled={Boolean(editingCategory)} required maxLength={40} />
            </Field>
          </div>
          <Field label="Description">
            <textarea value={categoryForm.description} onChange={(event) => setCategoryForm({ ...categoryForm, description: event.target.value })} rows={3} maxLength={255} />
          </Field>
          <label className="checkbox-field">
            <input type="checkbox" checked={categoryForm.active} onChange={(event) => setCategoryForm({ ...categoryForm, active: event.target.checked })} />
            Active and selectable
          </label>
        </form>
      </Modal>
    </div>
  )
}
