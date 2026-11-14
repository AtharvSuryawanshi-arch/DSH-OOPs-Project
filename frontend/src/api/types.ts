export type Role = 'EMPLOYEE' | 'TECHNICIAN' | 'ADMIN'

export type Priority = 'P1' | 'P2' | 'P3' | 'P4'
export type IncidentStatus =
  | 'NEW'
  | 'CLASSIFIED'
  | 'ASSIGNED'
  | 'ACKNOWLEDGED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'VERIFIED'
  | 'CLOSED'
  | 'CANCELLED'
export type SlaStatus = 'ON_TRACK' | 'AT_RISK' | 'BREACHED' | 'MET'
export type Impact = 'LOW' | 'MEDIUM' | 'HIGH' | 'EXTREME'
export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type AssignmentStrategyType = 'SKILL_BASED' | 'LOAD_BASED' | 'ROUND_ROBIN'

export interface Skill {
  id: number
  name: string
}

export interface User {
  id: number
  name: string
  email: string
  role: Role
  phone?: string | null
  active: boolean
  department?: string | null
  jobTitle?: string | null
  experienceYears?: number | null
  maxCapacity?: number | null
  available?: boolean | null
  skills: Skill[]
  createdAt: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  user: User
}

export interface Comment {
  id: number
  authorId: number
  authorName: string
  authorRole: Role
  message: string
  internal: boolean
  createdAt: string
}

export interface AuditEntry {
  id: number
  incidentId: number
  action: string
  actorName: string
  fromState?: string | null
  toState?: string | null
  description: string
  createdAt: string
  incidentReference: string
}

export interface Incident {
  id: number
  reference: string
  title: string
  description: string
  status: IncidentStatus
  statusLabel: string
  priority: Priority
  priorityLabel: string
  priorityScore: number
  priorityRationale?: string | null
  impact: Impact
  urgency: Urgency
  categoryId: number
  categoryName: string
  serviceId: number
  serviceName: string
  serviceCriticality: string
  reporterId: number
  reporterName: string
  reporterEmail: string
  reporterDepartment?: string | null
  technicianId?: number | null
  technicianName?: string | null
  affectedUsers: number
  autoClassified: boolean
  classificationConfidence?: number | null
  createdAt: string
  updatedAt: string
  acknowledgedAt?: string | null
  resolvedAt?: string | null
  closedAt?: string | null
  responseDueAt?: string | null
  resolutionDueAt?: string | null
  slaStatus: SlaStatus
  responseBreached: boolean
  resolutionBreached: boolean
  escalationLevel: number
  duplicateOfReference?: string | null
  duplicateScore?: number | null
  resolutionNotes?: string | null
  closureNotes?: string | null
  feedbackRating?: number | null
  feedbackComment?: string | null
  version: number
  comments?: Comment[]
  audit?: AuditEntry[]
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Category {
  id: number
  code: string
  name: string
  description?: string | null
  active: boolean
  createdAt?: string
}

export interface Service {
  id: number
  code: string
  name: string
  description?: string | null
  businessCriticality: string
  active: boolean
}

export interface SlaPolicy {
  id: number
  name: string
  priority: Priority
  serviceId?: number | null
  serviceName?: string | null
  responseMinutes: number
  resolutionMinutes: number
  warningPercent: number
  active: boolean
}

export interface Notification {
  id: number
  channel: 'IN_APP' | 'EMAIL'
  type: string
  title: string
  message: string
  read: boolean
  createdAt: string
  incidentId?: number | null
  incidentReference?: string | null
}

export interface DashboardCount {
  key: string
  label: string
  count: number
}

export interface DashboardResponse {
  scope: Role
  totals: {
    total: number
    open: number
    critical: number
    resolved: number
    slaBreaches: number
    unassigned: number
    averageResponseMinutes: number
    averageResolutionMinutes: number
  }
  byStatus: DashboardCount[]
  byPriority: DashboardCount[]
  byCategory: DashboardCount[]
  byService: DashboardCount[]
  technicianWorkload: Array<{
    id: number
    name: string
    activeIncidents: number
    maxCapacity: number | null
    available: boolean
  }>
  trend: Array<{ date: string; created: number; resolved: number }>
  recentIncidents: Incident[]
  generatedAt: string
}

export interface TechnicianWorkload {
  technicianId: number
  technicianName: string
  activeIncidents: number
  maxCapacity: number | null
  available: boolean
  assignedOpen: number
  resolvedByTechnician: number
  workItems: Incident[]
  generatedAt: string
}

export interface TechnicianRecommendation {
  technicianId: number
  name: string
  skills: string[]
  activeIncidents: number
  maxCapacity: number | null
  skillMatch: boolean
  serviceHistory: number
  score: number
  available: boolean
  hasCapacity: boolean
}

export interface DuplicateCandidate {
  /** Null when the viewer may not open the matching incident. */
  incidentId: number | null
  reference: string
  /** Null when the viewer may not open the matching incident. */
  title: string | null
  status: IncidentStatus
  priority: Priority
  score: number
  createdAt: string
  accessible: boolean
}

export interface ResolutionSuggestion {
  incidentId: number
  reference: string
  title: string
  resolutionNotes: string
  resolvedAt?: string | null
  score: number
}

export interface ClassificationSuggestion {
  categoryId: number | null
  categoryName: string | null
  confidence: number
  rationale: string
}

export interface IntelligencePreview {
  classification: ClassificationSuggestion
  possibleDuplicates: DuplicateCandidate[]
}

export interface Settings {
  assignmentStrategy: AssignmentStrategyType
  autoAssign: boolean
}

export interface CreateIncidentPayload {
  title: string
  description: string
  serviceId: number
  categoryId?: number | null
  impact: Impact
  urgency: Urgency
  affectedUsers: number
}

export interface CreateUserPayload {
  name: string
  email: string
  password: string
  role: Role
  phone?: string
  department?: string
  jobTitle?: string
  experienceYears?: number
  maxCapacity?: number
  skillCategoryIds?: number[]
}

export interface UpdateUserPayload {
  name?: string
  phone?: string
  active?: boolean
  department?: string
  jobTitle?: string
  experienceYears?: number
  maxCapacity?: number
  available?: boolean
  skillCategoryIds?: number[]
  password?: string
}

export interface CategoryPayload {
  code: string
  name: string
  description?: string
  active?: boolean
}

export interface ServicePayload {
  code: string
  name: string
  description?: string
  businessCriticality: string
  active?: boolean
}

export interface SlaPolicyPayload {
  name: string
  priority: Priority
  serviceId?: number | null
  responseMinutes: number
  resolutionMinutes: number
  warningPercent: number
  active?: boolean
}
