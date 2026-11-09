import type {
  AuditEntry,
  AuthResponse,
  Category,
  CategoryPayload,
  CreateIncidentPayload,
  CreateUserPayload,
  DashboardResponse,
  DuplicateCandidate,
  Incident,
  IntelligencePreview,
  Notification,
  PageResponse,
  ResolutionSuggestion,
  Service,
  ServicePayload,
  Settings,
  SlaPolicy,
  SlaPolicyPayload,
  TechnicianRecommendation,
  TechnicianWorkload,
  UpdateUserPayload,
  User,
} from './types'

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
const TOKEN_KEY = 'itsm.token'

export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly fields?: Record<string, string>

  constructor(message: string, status: number, code?: string, fields?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fields = fields
  }
}

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function apiGet<T>(path: string, signal?: AbortSignal): Promise<T> {
  return request<T>(path, { signal })
}

export function storeToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

interface RequestOptions {
  method?: string
  body?: unknown
  signal?: AbortSignal
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers()
  const token = getStoredToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE}/api${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    })
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause
    }
    throw new ApiError('Cannot reach the API. Is the backend running?', 0, 'NETWORK_ERROR')
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`
    let code: string | undefined
    let fields: Record<string, string> | undefined
    try {
      const problem = (await response.json()) as {
        detail?: string
        title?: string
        code?: string
        errors?: Record<string, string>
      }
      message = problem.detail ?? problem.title ?? message
      code = problem.code
      fields = problem.errors
    } catch {
      // Keep the status-based fallback message.
    }
    if (response.status === 401 && token) {
      window.dispatchEvent(new Event('itsm:unauthorized'))
    }
    throw new ApiError(message, response.status, code, fields)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

function query(params: Record<string, unknown>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '' || value === false) {
      continue
    }
    search.set(key, String(value))
  }
  const serialized = search.toString()
  return serialized ? `?${serialized}` : ''
}

export function errorMessage(cause: unknown): string {
  if (cause instanceof ApiError) {
    return cause.message
  }
  if (cause instanceof Error) {
    return cause.message
  }
  return 'Something went wrong.'
}

export const authApi = {
  login: (email: string, password: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: { email, password } }),
  register: (payload: {
    name: string
    email: string
    password: string
    phone?: string
    department?: string
  }) => request<AuthResponse>('/auth/register', { method: 'POST', body: payload }),
  me: () => request<User>('/auth/me'),
}

export interface IncidentFilters {
  search?: string
  status?: string
  priority?: string
  categoryId?: number | ''
  serviceId?: number | ''
  technicianId?: number | ''
  slaStatus?: string
  duplicatesOnly?: boolean
  escalatedOnly?: boolean
  page?: number
  size?: number
}

export const incidentApi = {
  list: (filters: IncidentFilters, signal?: AbortSignal) =>
    request<PageResponse<Incident>>(`/incidents${query({ ...filters })}`, { signal }),
  get: (id: number, signal?: AbortSignal) => request<Incident>(`/incidents/${id}`, { signal }),
  create: (payload: CreateIncidentPayload) =>
    request<Incident>('/incidents', { method: 'POST', body: payload }),
  update: (id: number, payload: Partial<CreateIncidentPayload>) =>
    request<Incident>(`/incidents/${id}`, { method: 'PATCH', body: payload }),
  assign: (id: number, technicianId: number) =>
    request<Incident>(`/incidents/${id}/assign`, { method: 'POST', body: { technicianId } }),
  acknowledge: (id: number) =>
    request<Incident>(`/incidents/${id}/acknowledge`, { method: 'POST' }),
  start: (id: number) => request<Incident>(`/incidents/${id}/start`, { method: 'POST' }),
  resolve: (id: number, resolutionNotes: string) =>
    request<Incident>(`/incidents/${id}/resolve`, {
      method: 'POST',
      body: { resolutionNotes },
    }),
  verify: (id: number) => request<Incident>(`/incidents/${id}/verify`, { method: 'POST' }),
  close: (id: number, closureNotes?: string) =>
    request<Incident>(`/incidents/${id}/close`, {
      method: 'POST',
      body: { closureNotes: closureNotes || null },
    }),
  reopen: (id: number) => request<Incident>(`/incidents/${id}/reopen`, { method: 'POST' }),
  cancel: (id: number, reason: string) =>
    request<Incident>(`/incidents/${id}/cancel`, { method: 'POST', body: { reason } }),
  feedback: (id: number, rating: number, comment?: string) =>
    request<Incident>(`/incidents/${id}/feedback`, {
      method: 'POST',
      body: { rating, comment },
    }),
  overridePriority: (id: number, priority: string, reason?: string) =>
    request<Incident>(`/incidents/${id}/priority-override`, {
      method: 'POST',
      body: { priority, reason },
    }),
  comment: (id: number, message: string, internal: boolean) =>
    request(`/incidents/${id}/comments`, { method: 'POST', body: { message, internal } }),
  duplicates: (id: number, signal?: AbortSignal) =>
    request<DuplicateCandidate[]>(`/incidents/${id}/duplicates`, { signal }),
  suggestions: (id: number, signal?: AbortSignal) =>
    request<ResolutionSuggestion[]>(`/incidents/${id}/suggested-resolutions`, { signal }),
  recommendations: (id: number, signal?: AbortSignal) =>
    request<TechnicianRecommendation[]>(`/incidents/${id}/recommended-technicians`, { signal }),
}

export const dashboardApi = {
  get: (signal?: AbortSignal) => request<DashboardResponse>('/dashboard', { signal }),
}

export const notificationApi = {
  list: (signal?: AbortSignal) => request<Notification[]>('/notifications', { signal }),
  unreadCount: (signal?: AbortSignal) =>
    request<{ count: number }>('/notifications/unread-count', { signal }),
  markRead: (id: number) =>
    request<void>(`/notifications/${id}/read`, { method: 'PATCH' }),
  markAllRead: () =>
    request<{ updated: number }>('/notifications/read-all', { method: 'POST' }),
}

export const catalogApi = {
  categories: (signal?: AbortSignal) => request<Category[]>('/catalog/categories', { signal }),
  services: (signal?: AbortSignal) => request<Service[]>('/catalog/services', { signal }),
  slaPolicies: (signal?: AbortSignal) =>
    request<SlaPolicy[]>('/catalog/sla-policies', { signal }),
}

export const intelligenceApi = {
  classify: (title: string, description: string, serviceId?: number) =>
    request<IntelligencePreview>('/intelligence/classify', {
      method: 'POST',
      body: { title, description, serviceId },
    }),
}

export const technicianApi = {
  workload: (signal?: AbortSignal) =>
    request<TechnicianWorkload>('/technicians/me/workload', { signal }),
}

export const adminApi = {
  users: (signal?: AbortSignal) => request<User[]>('/admin/users', { signal }),
  createUser: (payload: CreateUserPayload) =>
    request<User>('/admin/users', { method: 'POST', body: payload }),
  updateUser: (id: number, payload: UpdateUserPayload) =>
    request<User>(`/admin/users/${id}`, { method: 'PATCH', body: payload }),
  allCategories: (signal?: AbortSignal) =>
    request<Category[]>('/admin/categories', { signal }),
  createCategory: (payload: CategoryPayload) =>
    request<Category>('/admin/categories', { method: 'POST', body: payload }),
  updateCategory: (id: number, payload: CategoryPayload) =>
    request<Category>(`/admin/categories/${id}`, { method: 'PATCH', body: payload }),
  allServices: (signal?: AbortSignal) => request<Service[]>('/admin/services', { signal }),
  createService: (payload: ServicePayload) =>
    request<Service>('/admin/services', { method: 'POST', body: payload }),
  updateService: (id: number, payload: ServicePayload) =>
    request<Service>(`/admin/services/${id}`, { method: 'PATCH', body: payload }),
  slaPolicies: (signal?: AbortSignal) =>
    request<SlaPolicy[]>('/admin/sla-policies', { signal }),
  createSlaPolicy: (payload: SlaPolicyPayload) =>
    request<SlaPolicy>('/admin/sla-policies', { method: 'POST', body: payload }),
  updateSlaPolicy: (id: number, payload: SlaPolicyPayload) =>
    request<SlaPolicy>(`/admin/sla-policies/${id}`, { method: 'PATCH', body: payload }),
  settings: (signal?: AbortSignal) => request<Settings>('/admin/settings', { signal }),
  updateSettings: (payload: Partial<Settings>) =>
    request<Settings>('/admin/settings', { method: 'PUT', body: payload }),
  audit: (page: number, size: number, signal?: AbortSignal) =>
    request<PageResponse<AuditEntry>>(`/admin/audit?page=${page}&size=${size}`, { signal }),
  scanSla: () => request<{ status: string }>('/admin/sla/scan', { method: 'POST' }),
}
