import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { LoaderCircle, Pencil, Plus, Search, UserCheck, UserX } from 'lucide-react'
import { adminApi, errorMessage } from '../../api/client'
import type { Category, Role, User } from '../../api/types'
import { useAuth } from '../../auth/auth-context'
import { useApi } from '../../hooks/useApi'
import {
  Avatar,
  Badge,
  EmptyState,
  ErrorPanel,
  Field,
  Flash,
  Modal,
  PageHeader,
  PageLoader,
  RoleBadge,
} from '../../components/ui'

interface UserForm {
  name: string
  email: string
  password: string
  role: Role
  phone: string
  department: string
  jobTitle: string
  experienceYears: number
  maxCapacity: number
  available: boolean
  active: boolean
  skillCategoryIds: number[]
}

const emptyForm: UserForm = {
  name: '',
  email: '',
  password: '',
  role: 'EMPLOYEE',
  phone: '',
  department: '',
  jobTitle: '',
  experienceYears: 0,
  maxCapacity: 8,
  available: true,
  active: true,
  skillCategoryIds: [],
}

export function AdminUsersPage() {
  const { user: currentUser } = useAuth()
  const users = useApi<User[]>('/admin/users')
  const categories = useApi<Category[]>('/admin/categories')
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [form, setForm] = useState<UserForm>(emptyForm)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!currentUser) {
    return null
  }
  if (currentUser.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  const allUsers = users.data ?? []
  const visible = allUsers.filter((user) => {
    if (roleFilter && user.role !== roleFilter) return false
    if (!search.trim()) return true
    const haystack = `${user.name} ${user.email} ${user.department ?? ''} ${user.jobTitle ?? ''}`.toLowerCase()
    return haystack.includes(search.trim().toLowerCase())
  })

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setError(null)
    setModalOpen(true)
  }

  function openEdit(user: User) {
    setEditing(user)
    setForm({
      name: user.name,
      email: user.email,
      password: '',
      role: user.role,
      phone: user.phone ?? '',
      department: user.department ?? '',
      jobTitle: user.jobTitle ?? '',
      experienceYears: user.experienceYears ?? 0,
      maxCapacity: user.maxCapacity ?? 8,
      available: user.available ?? true,
      active: user.active,
      skillCategoryIds: user.skills.map((skill) => skill.id),
    })
    setError(null)
    setModalOpen(true)
  }

  function update<K extends keyof UserForm>(key: K, value: UserForm[K]) {
    setForm((previous) => ({ ...previous, [key]: value }))
  }

  function toggleSkill(id: number) {
    setForm((previous) => ({
      ...previous,
      skillCategoryIds: previous.skillCategoryIds.includes(id)
        ? previous.skillCategoryIds.filter((skillId) => skillId !== id)
        : [...previous.skillCategoryIds, id],
    }))
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      if (editing) {
        await adminApi.updateUser(editing.id, {
          name: form.name.trim(),
          phone: form.phone.trim() || undefined,
          active: form.active,
          department: form.role === 'EMPLOYEE' ? form.department.trim() : undefined,
          jobTitle: form.role === 'ADMIN' ? form.jobTitle.trim() : undefined,
          experienceYears: form.role === 'TECHNICIAN' ? form.experienceYears : undefined,
          maxCapacity: form.role === 'TECHNICIAN' ? form.maxCapacity : undefined,
          available: form.role === 'TECHNICIAN' ? form.available : undefined,
          skillCategoryIds: form.role === 'TECHNICIAN' ? form.skillCategoryIds : undefined,
          password: form.password || undefined,
        })
      } else {
        if (form.password.length < 8) {
          setError('Password must be at least 8 characters.')
          setSaving(false)
          return
        }
        await adminApi.createUser({
          name: form.name.trim(),
          email: form.email.trim(),
          password: form.password,
          role: form.role,
          phone: form.phone.trim() || undefined,
          department: form.role === 'EMPLOYEE' ? form.department.trim() : undefined,
          jobTitle: form.role === 'ADMIN' ? form.jobTitle.trim() : undefined,
          experienceYears: form.role === 'TECHNICIAN' ? form.experienceYears : undefined,
          maxCapacity: form.role === 'TECHNICIAN' ? form.maxCapacity : undefined,
          skillCategoryIds: form.role === 'TECHNICIAN' ? form.skillCategoryIds : undefined,
        })
      }
      users.refetch()
      setModalOpen(false)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  async function toggleActive(user: User) {
    try {
      await adminApi.updateUser(user.id, { active: !user.active })
      users.refetch()
    } catch (cause) {
      setError(errorMessage(cause))
    }
  }

  if (users.loading && !users.data) {
    return <PageLoader label="Loading users…" />
  }

  return (
    <div className="stack">
      <PageHeader
        eyebrow="Administration"
        title="Users & skills"
        description="Manage employees, technicians and administrators, including technician skills and capacity."
        actions={
          <button type="button" className="button button-primary" onClick={openCreate}>
            <Plus size={16} /> Add user
          </button>
        }
      />

      {error && <Flash tone="error">{error}</Flash>}

      <section className="panel">
        <div className="table-toolbar">
          <label className="search-field search-field-small">
            <Search size={16} />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search people…" />
          </label>
          <select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value)} aria-label="Filter by role">
            <option value="">All roles</option>
            <option value="EMPLOYEE">Employees</option>
            <option value="TECHNICIAN">Technicians</option>
            <option value="ADMIN">Administrators</option>
          </select>
          <span className="muted small">{visible.length} account{visible.length === 1 ? '' : 's'}</span>
        </div>

        {users.error ? (
          <ErrorPanel message={users.error} onRetry={users.refetch} />
        ) : visible.length === 0 ? (
          <EmptyState title="No matching users" description="Try a different search or role filter." icon={UserCheck} />
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Person</th>
                  <th>Role</th>
                  <th>Team / skills</th>
                  <th>Workload</th>
                  <th>Status</th>
                  <th aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {visible.map((user) => (
                  <tr key={user.id}>
                    <td>
                      <span className="person-inline">
                        <Avatar name={user.name} size="sm" />
                        <span className="stack-tight">
                          <strong>{user.name}</strong>
                          <span className="muted small">{user.email}</span>
                        </span>
                      </span>
                    </td>
                    <td><RoleBadge role={user.role} /></td>
                    <td>
                      {user.role === 'TECHNICIAN' ? (
                        <div className="skill-chips">
                          {user.skills.length === 0 ? <span className="muted">No skills</span> : user.skills.map((skill) => (
                            <Badge key={skill.id} tone="blue">{skill.name}</Badge>
                          ))}
                        </div>
                      ) : user.department ? (
                        user.department
                      ) : user.jobTitle ? (
                        user.jobTitle
                      ) : (
                        <span className="muted">—</span>
                      )}
                    </td>
                    <td>
                      {user.role === 'TECHNICIAN'
                        ? `${user.experienceYears ?? 0}y exp · ${user.maxCapacity ?? '—'} cap`
                        : <span className="muted">—</span>}
                    </td>
                    <td>
                      <Badge tone={user.active ? 'green' : 'slate'}>{user.active ? 'Active' : 'Inactive'}</Badge>
                    </td>
                    <td>
                      <div className="row-actions">
                        <button type="button" className="icon-button" onClick={() => openEdit(user)} aria-label={`Edit ${user.name}`}>
                          <Pencil size={15} />
                        </button>
                        <button
                          type="button"
                          className="icon-button"
                          onClick={() => void toggleActive(user)}
                          disabled={user.id === currentUser.id}
                          aria-label={user.active ? `Deactivate ${user.name}` : `Activate ${user.name}`}
                          title={user.id === currentUser.id ? 'You cannot deactivate your own account' : undefined}
                        >
                          {user.active ? <UserX size={15} /> : <UserCheck size={15} />}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editing ? `Edit ${editing.name}` : 'Add a user'}
        description={editing ? 'Update profile, access and technician settings.' : 'Create an employee, technician or administrator account.'}
        wide
        footer={
          <>
            <button type="button" className="button button-ghost" onClick={() => setModalOpen(false)}>Cancel</button>
            <button type="submit" form="user-form" className="button button-primary" disabled={saving}>
              {saving ? <LoaderCircle className="spin" size={15} /> : null}
              {editing ? 'Save changes' : 'Create user'}
            </button>
          </>
        }
      >
        {error && <Flash tone="error">{error}</Flash>}
        <form id="user-form" className="form-stack" onSubmit={handleSubmit}>
          <div className="form-grid-two">
            <Field label="Full name">
              <input value={form.name} onChange={(event) => update('name', event.target.value)} required maxLength={120} />
            </Field>
            <Field label="Email" hint={editing ? 'Email is immutable in this demo.' : undefined}>
              <input
                type="email"
                value={form.email}
                onChange={(event) => update('email', event.target.value)}
                disabled={Boolean(editing)}
                required
                maxLength={160}
              />
            </Field>
          </div>

          <div className="form-grid-two">
            <Field label="Role">
              <select
                value={form.role}
                onChange={(event) => update('role', event.target.value as Role)}
                disabled={Boolean(editing)}
              >
                <option value="EMPLOYEE">Employee</option>
                <option value="TECHNICIAN">Technician</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </Field>
            <Field label="Phone">
              <input value={form.phone} onChange={(event) => update('phone', event.target.value)} maxLength={40} />
            </Field>
          </div>

          <Field label={editing ? 'Reset password' : 'Temporary password'} hint={editing ? 'Leave blank to keep the current password.' : 'At least 8 characters.'}>
            <input
              type="password"
              value={form.password}
              onChange={(event) => update('password', event.target.value)}
              minLength={editing ? undefined : 8}
              maxLength={72}
              placeholder={editing ? '••••••••' : 'Set an initial password'}
              required={!editing}
            />
          </Field>

          {editing && (
            <label className="checkbox-field">
              <input type="checkbox" checked={form.active} onChange={(event) => update('active', event.target.checked)} />
              Account active
            </label>
          )}

          {form.role === 'EMPLOYEE' && (
            <Field label="Department">
              <input value={form.department} onChange={(event) => update('department', event.target.value)} maxLength={120} />
            </Field>
          )}

          {form.role === 'ADMIN' && (
            <Field label="Job title">
              <input value={form.jobTitle} onChange={(event) => update('jobTitle', event.target.value)} maxLength={120} />
            </Field>
          )}

          {form.role === 'TECHNICIAN' && (
            <>
              <div className="form-grid-two">
                <Field label="Experience (years)">
                  <input
                    type="number"
                    min={0}
                    max={60}
                    value={form.experienceYears}
                    onChange={(event) => update('experienceYears', Number(event.target.value))}
                  />
                </Field>
                <Field label="Maximum capacity">
                  <input
                    type="number"
                    min={1}
                    max={50}
                    value={form.maxCapacity}
                    onChange={(event) => update('maxCapacity', Number(event.target.value))}
                  />
                </Field>
              </div>
              <div className="form-grid-two">
                <label className="checkbox-field">
                  <input type="checkbox" checked={form.available} onChange={(event) => update('available', event.target.checked)} />
                  Available for assignment
                </label>
              </div>
              <div className="field">
                <span className="field-label">Skill categories</span>
                <div className="checkbox-grid">
                  {categories.data?.map((category) => (
                    <label key={category.id} className="checkbox-field">
                      <input
                        type="checkbox"
                        checked={form.skillCategoryIds.includes(category.id)}
                        onChange={() => toggleSkill(category.id)}
                      />
                      {category.name}
                    </label>
                  ))}
                </div>
              </div>
            </>
          )}
        </form>
      </Modal>
    </div>
  )
}
