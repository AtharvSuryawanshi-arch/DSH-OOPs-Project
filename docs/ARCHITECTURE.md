# Architecture

This document explains how I-ITSM is structured, how an incident moves through the system, which
object-oriented patterns are used, and where the important boundaries are.

## 1. Design goals and quality attributes

| Goal | How the architecture supports it |
| --- | --- |
| Maintainability | Feature-oriented packages, a small domain model with behaviour in entities, explicit service boundaries, DTOs at the API edge |
| Extensibility | Strategy interfaces for priority/assignment, state classes for lifecycle, domain events for notifications/audit, configurable catalogue and SLA policies |
| Correctness | State machine rejects illegal transitions, validation at the HTTP edge, business rules in services, optimistic locking on incidents |
| Security | Stateless JWT, BCrypt, method-level roles, per-incident authorization checks, internal-note filtering |
| Observability | Append-only audit trail, Actuator health, structured simulated-email logs, dashboard reports |
| Testability | Pure domain rules (priority, states, SLA math, similarity) isolated from persistence; integration test with a real PostgreSQL container |
| Performance | Indexed search columns, paginated queries, entity graphs for to-one relations, batched lazy loading |

## 2. System context

```mermaid
flowchart LR
    E([Employee]) --> SPA[React 19 SPA]
    T([Technician]) --> SPA
    A([Administrator]) --> SPA

    SPA -->|JSON over HTTP + Bearer JWT| API[Spring Boot 4.1 REST API]
    SPA -->|/api and /actuator proxy in dev| VITE[Vite dev server]

    API --> SEC[Spring Security<br/>JWT filter + RBAC]
    API --> DOMAIN[Incident & SLA domain services]
    DOMAIN --> PG[(PostgreSQL 17<br/>Docker Alpine)]
    DOMAIN --> EVENTS[Spring application events]
    EVENTS --> NOTIFY[In-app notification store]
    EVENTS --> EMAIL[Simulated email adapter<br/>stored + logged]
    SCHED[Scheduled SLA monitor] --> DOMAIN
    ADMIN([Service manager]) -.-> REPORTS[Dashboard & reports]
    REPORTS -.-> API
```

## 3. Container / component view

```mermaid
flowchart TB
    subgraph Browser["Browser"]
        UI[React SPA<br/>role-aware routing]
        CHART[Recharts dashboards]
        CLIENT[Typed API client<br/>JWT in localStorage]
        UI --- CHART
        UI --- CLIENT
    end

    subgraph Backend["Spring Boot application"]
        SEC[Security filter chain]
        WEB[REST controllers]
        INC[Incident service]
        WF[Lifecycle state machine]
        PRI[Priority strategy]
        ASG[Assignment strategies]
        SLA[SLA policies + monitor]
        INT[Intelligence services]
        AUD[Audit service]
        NOT[Notification service]
        REP[Dashboard service]
        CAT[Catalog & settings]
        USR[Auth & user management]
    end

    subgraph Data["Persistence"]
        PG[(PostgreSQL)]
    end

    CLIENT -->|/api/**| SEC
    SEC --> WEB
    WEB --> INC
    WEB --> USR
    WEB --> CAT
    WEB --> REP
    INC --> WF
    INC --> PRI
    INC --> SLA
    INC --> ASG
    INC --> INT
    INC --> AUD
    INC --> NOT
    SLA --> NOT
    SLA --> AUD
    INC --> PG
    USR --> PG
    CAT --> PG
    NOT --> PG
    AUD --> PG
    REP --> PG
```

### Backend package boundaries

| Package | Responsibility |
| --- | --- |
| `security` | JWT parsing/validation, current user, Spring Security rules, JSON 401/403 handlers |
| `user` | User inheritance, login/registration, admin user management, technician directory |
| `catalog` | IT services, business criticality, categories, active/inactive lifecycle |
| `sla` | Global/service-specific policies, deadline arithmetic, SLA state, scheduled monitor, escalation |
| `incident` | Incident aggregate, REST API, search/filter specifications, DTO mapping, comments |
| `incident.lifecycle` | State pattern: allowed transitions and transition validation |
| `incident.priority` | Priority context/result and weighted strategy |
| `incident.assignment` | Candidate scoring, three assignment strategies, recommendation service |
| `incident.search` | JPA `Specification` filters and pagination query object |
| `audit` | Append-only incident audit records and admin audit API |
| `notification` | Domain events, observers/listeners, notification persistence, inbox API |
| `intelligence` | Keyword classification, duplicate similarity, resolution suggestions |
| `dashboard` | Role-scoped aggregation for KPIs, charts, workload and recent incidents |
| `settings` | Runtime assignment strategy and auto-assign switches |
| `seed` | Realistic demo organisation and incident history |
| `common` | Auditable base entity, exceptions, ProblemDetail advice, page response |

### Frontend structure

| Area | Responsibility |
| --- | --- |
| `api/client.ts` | Single fetch wrapper: JWT header, JSON, ProblemDetail parsing, 401 event |
| `api/types.ts` | Types mirroring API DTOs |
| `auth` | Session restore, login, registration, demo account quick-fill |
| `components` | App shell, role-aware navigation, notification bell, badges, modals, tables |
| `pages` | Dashboard, incident queue, create/report, incident detail, technician queue, notifications, reports |
| `pages/admin` | Users/skills, services/categories, SLA policies, assignment settings, audit |
| `hooks/useApi.ts` | Abort-safe GET hook with refetch and explicit loading/error state |

## 4. Domain model (class diagram)

```mermaid
classDiagram
    direction LR

    class User {
        <<abstract>>
        -Long id
        -String name
        -String email
        -String passwordHash
        -boolean active
        +getRole() Role
        +updateProfile(name, phone)
        +changePasswordHash(hash)
        +setActive(active)
    }

    class Employee {
        -String department
        +getRole() Role
    }

    class Technician {
        -Integer experienceYears
        -Integer maxCapacity
        -Boolean available
        -Set~Category~ skills
        +hasSkill(Category) boolean
        +replaceSkills(Set)
        +updateProfessionalProfile(...)
    }

    class Administrator {
        -String jobTitle
        +getRole() Role
    }

    class Incident {
        -String reference
        -String title
        -String description
        -Impact impact
        -Urgency urgency
        -Priority priority
        -int priorityScore
        -IncidentStatus status
        -Instant responseDueAt
        -Instant resolutionDueAt
        -boolean responseBreached
        -boolean resolutionBreached
        -int escalationLevel
        +create(...)$ Incident
        +applyClassification(...)
        +applySla(SlaDeadlines)
        +assignTechnician(Technician)
        +acknowledge(Instant, boolean)
        +resolve(String, Instant, boolean)
        +close(String, Instant)
        +reopen(boolean)
        +markDuplicate(Incident, double)
        +recordFeedback(int, String)
    }

    class Category {
        -String code
        -String name
        -boolean active
    }

    class ItService {
        -String code
        -String name
        -BusinessCriticality businessCriticality
        -boolean active
    }

    class SlaPolicy {
        -Priority priority
        -Integer responseMinutes
        -Integer resolutionMinutes
        -int warningPercent
        -boolean active
    }

    class IncidentComment {
        -String message
        -boolean internal
        -Instant createdAt
    }

    class IncidentAudit {
        -AuditAction action
        -IncidentStatus fromState
        -IncidentStatus toState
        -String description
        -Instant createdAt
    }

    class Notification {
        -NotificationChannel channel
        -NotificationType type
        -String title
        -String message
        -boolean read
    }

    class PriorityStrategy {
        <<interface>>
        +calculate(PriorityContext) PriorityResult
        +name() String
    }
    class WeightedPriorityStrategy

    class AssignmentStrategy {
        <<interface>>
        +select(Incident, List~AssignmentCandidate~) Optional~AssignmentCandidate~
        +type() AssignmentStrategyType
    }
    class SkillBasedAssignmentStrategy
    class LoadBasedAssignmentStrategy
    class RoundRobinAssignmentStrategy

    class IncidentState {
        <<interface>>
        +status() IncidentStatus
        +allowedTransitions() Set~IncidentStatus~
        +canTransitionTo(IncidentStatus) boolean
    }
    class NewState
    class ClassifiedState
    class AssignedState
    class AcknowledgedState
    class InProgressState
    class ResolvedState
    class VerifiedState
    class ClosedState
    class CancelledState

    User <|-- Employee
    User <|-- Technician
    User <|-- Administrator

    Technician "1" --o "*" Incident : assigned
    User "1" --o "*" Incident : reported
    Incident "*" --> "1" Category
    Incident "*" --> "1" ItService
    SlaPolicy "*" --> "0..1" ItService
    Incident *-- "*" IncidentComment : composition
    Incident *-- "*" IncidentAudit : composition
    Notification "*" --> "1" User : recipient
    Notification "*" --> "0..1" Incident

    PriorityStrategy <|.. WeightedPriorityStrategy
    AssignmentStrategy <|.. SkillBasedAssignmentStrategy
    AssignmentStrategy <|.. LoadBasedAssignmentStrategy
    AssignmentStrategy <|.. RoundRobinAssignmentStrategy

    IncidentState <|.. NewState
    IncidentState <|.. ClassifiedState
    IncidentState <|.. AssignedState
    IncidentState <|.. AcknowledgedState
    IncidentState <|.. InProgressState
    IncidentState <|.. ResolvedState
    IncidentState <|.. VerifiedState
    IncidentState <|.. ClosedState
    IncidentState <|.. CancelledState
```

**Inheritance note:** `User` is a genuine is-a hierarchy (`Employee`, `Technician`, `Administrator`
are all users but have different capabilities). It is persisted with `SINGLE_TABLE` inheritance and a
`user_type` discriminator, which keeps authentication queries simple. Technician-specific fields are
nullable for other user types, a deliberate trade-off documented in the database design.

**Composition vs association:** comments and audit entries are strongly owned by an incident
(deleting the incident cascades), while reporter/technician/service/category are associations to
independent aggregates. Duplicate linking is a self-association (`duplicateOf`).

## 5. Use cases by actor

```mermaid
flowchart LR
    subgraph Actors
        E([Employee])
        T([Technician])
        A([Administrator])
    end

    subgraph System["I-ITSM"]
        R[Register / sign in]
        CR[Report incident]
        VW[Track my incidents]
        CM[Add information / comments]
        FB[Verify resolution / feedback]
        Q[View my queue]
        ACK[Acknowledge / start work]
        RES[Investigate / resolve]
        WL[View workload]
        UM[Manage users & skills]
        CAT[Configure services & categories]
        SLA[Configure SLA policies]
        ASG[Configure assignment strategy]
        MON[Monitor incidents & SLA]
        REP[View reports]
        AUD[View audit trail]
        NTF[Notifications]
    end

    E --> R
    E --> CR
    E --> VW
    E --> CM
    E --> FB
    T --> R
    T --> Q
    T --> ACK
    T --> RES
    T --> WL
    T --> CM
    A --> R
    A --> UM
    A --> CAT
    A --> SLA
    A --> ASG
    A --> MON
    A --> REP
    A --> AUD
    A --> NTF
    E --> NTF
    T --> NTF
```

## 6. Incident lifecycle state machine

```mermaid
stateDiagram-v2
    [*] --> NEW : report
    NEW --> CLASSIFIED : auto-classify + priority + SLA
    CLASSIFIED --> ASSIGNED : strategy or admin assignment
    ASSIGNED --> ACKNOWLEDGED : technician acknowledges
    ACKNOWLEDGED --> IN_PROGRESS : investigation starts
    IN_PROGRESS --> RESOLVED : resolution notes
    RESOLVED --> VERIFIED : reporter verifies
    VERIFIED --> CLOSED : administrator closes
    RESOLVED --> IN_PROGRESS : reporter reopens
    NEW --> CANCELLED : admin cancels
    CLASSIFIED --> CANCELLED
    ASSIGNED --> CANCELLED
    ACKNOWLEDGED --> CANCELLED
    CLOSED --> [*]
    CANCELLED --> [*]
```

Transitions are implemented with the **State pattern** (`IncidentState` and one class per status).
`IncidentWorkflowService` asks the current state whether a transition is allowed and throws
`InvalidStateTransitionException` (HTTP 409) otherwise. The API never exposes a generic
“set status” endpoint; each action has an explicit endpoint, audit entry and permission check.

## 7. Runtime sequence: employee reports an incident

```mermaid
sequenceDiagram
    autonumber
    actor E as Employee
    participant UI as React form
    participant C as IncidentController
    participant S as IncidentService
    participant CAT as CatalogService
    participant CLS as CategorizationService
    participant P as WeightedPriorityStrategy
    participant SLA as SlaPolicyService
    participant DB as PostgreSQL
    participant DUP as DuplicateDetectionService
    participant AS as AssignmentService
    participant EV as ApplicationEventPublisher
    participant N as NotificationEventListener

    E->>UI: title, description, service, impact, urgency, affected users
    UI->>C: POST /api/incidents
    C->>S: create(request)
    S->>CAT: validate active service
    S->>CLS: suggest category (if auto)
    S->>P: calculate priority context
    P-->>S: PriorityResult (score + rationale)
    S->>SLA: resolve policy + compute deadlines
    S->>DB: save incident (NEW → CLASSIFIED)
    S->>DB: audit created/classified/priority
    S->>DUP: similarity against recent incidents
    DUP-->>S: possible duplicates
    S->>EV: IncidentCreated
    EV-->>N: observer
    N->>DB: in-app + simulated email notifications
    S->>AS: autoAssign(incident)
    AS->>AS: score candidates with configured strategy
    AS->>DB: assignment + audit
    AS->>EV: IncidentAssigned
    EV-->>N: notify technician + reporter
    S-->>C: IncidentResponse
    C-->>UI: 201 Created
```

All of the above happens in one transaction. Notification writes and audit rows roll back together
with the incident if any step fails.

## 8. Runtime sequence: SLA monitor

```mermaid
sequenceDiagram
    autonumber
    participant SCH as @Scheduled monitor
    participant DB as PostgreSQL
    participant INC as Incident aggregate
    participant AUD as AuditService
    participant EV as Event publisher
    participant N as Notifications

    loop every 30 seconds
        SCH->>DB: unresolved incidents
        loop each incident
            SCH->>INC: response warning due?
            SCH->>INC: resolution warning due?
            SCH->>INC: response breach?
            SCH->>INC: resolution breach?
            SCH->>INC: unacknowledged P1/P2 escalation?
            alt a threshold/breach is newly crossed
                SCH->>AUD: append SLA audit
                SCH->>EV: SlaWarning / SlaBreached / Escalated
                EV-->>N: notify technician, reporter, admins
            end
        end
    end
```

Flags (`responseWarningSent`, `responseBreached`, etc.) make every action idempotent: the monitor can
run every 30 seconds without duplicating notifications or audits. `SlaMonitorService.scan()` is also
callable through the admin endpoint for demonstrations.

## 9. Assignment and recommendation flow

```mermaid
flowchart LR
    I[Incident category + service] --> C[Candidate builder]
    C --> S[Skills match]
    C --> L[Current active load]
    C --> X[Experience]
    C --> H[Resolved history on service]
    S & L & X & H --> SC[Candidate score]
    SC --> F{Configured strategy}
    F -->|Skill-based| SB[Highest skill + fit]
    F -->|Load-based| LB[Least active]
    F -->|Round-robin| RB[Next in rotation]
    SB & LB & RB --> A[Assign or recommend]
    A --> AUD[Audit + notification]
```

The recommendations API exposes every candidate's score, skill match, active load, capacity and
availability so an administrator can make an informed manual choice.

## 10. Design patterns used

| Pattern | Where | Why |
| --- | --- | --- |
| **State** | `incident.lifecycle.*` | Encapsulates allowed transitions and prevents invalid lifecycle jumps |
| **Strategy** | `PriorityStrategy`, `AssignmentStrategy` + implementations | Priority algorithm and technician selection are independently replaceable |
| **Factory** | `UserFactory`, `NotificationFactory`, `AssignmentStrategyFactory`, `IncidentStateFactory` | Centralises creation and removes switch statements from services |
| **Observer / event-driven** | Spring `ApplicationEventPublisher` + `NotificationEventListener` | Notifications and audit side-effects are decoupled from core workflow |
| **Repository** | Spring Data JPA repositories | Persistence abstraction; domain services do not write SQL |
| **Builder** | `DemoDataSeeder.SeedBuilder` | Builds realistic incidents with many optional lifecycle fields |
| **Adapter (simulated)** | `EMAIL` channel in notification listener | A real provider can replace the log-based adapter without touching domain code |
| **Template method** | `AbstractIncidentState` | Shares status/transition storage across concrete state classes |
| **DTO / Mapper** | Request/response records + `IncidentMapper` | Keeps entities out of API contracts and hides internal fields |

Other OOP concepts:

- **Encapsulation** — entity fields are private; state-changing operations are methods; controllers
  never set status or timestamps directly.
- **Abstraction** — `PriorityStrategy`, `AssignmentStrategy`, `IncidentState` expose only meaningful
  behaviour.
- **Polymorphism** — `getRole()` is overridden by each user subtype; strategies are selected at
  runtime.
- **Composition** — incidents own comments/audits; services compose repositories, strategies and
  event publishers.
- **Exception handling** — domain-specific exceptions mapped to HTTP 400/401/403/404/409 with a
  consistent ProblemDetail body.

## 11. Security architecture

```mermaid
sequenceDiagram
    actor U as Browser
    participant F as JwtAuthenticationFilter
    participant UDS as AppUserDetailsService
    participant SC as SecurityContext
    participant C as Controller
    participant S as Service

    U->>F: Authorization: Bearer <token>
    F->>F: parse + verify HS384 signature/expiry
    F->>UDS: load user by email
    UDS-->>F: UserPrincipal + ROLE_*
    F->>SC: UsernamePasswordAuthenticationToken
    F->>C: continue filter chain
    C->>S: service call
    S->>S: ownership / role check
    S-->>C: DTO or exception
```

- Public routes: login, employee registration, health.
- Method security (`@PreAuthorize`) is the coarse gate; service-level checks enforce per-incident
  ownership and reporter/technician nuances.
- Internal comments are filtered in `IncidentService.detailResponse` for employees.
- JWT secret and expiration are externalised; the default is development-only.

## 12. Transactions, concurrency and consistency

- Spring `@Transactional` wraps incident creation, transitions, comments and admin changes.
- Incidents use `@Version` optimistic locking; a concurrent update receives a conflict rather than
  silently overwriting another actor's work.
- Domain events are synchronous within the transaction, so notification/audit writes are consistent
  with the incident change. (A production system could move email to an outbox for reliability.)
- SLA monitor work is idempotent and guarded by persisted flags.
- Reference numbers come from a PostgreSQL sequence; uniqueness is also enforced by a unique
  constraint.

## 13. Scalability path

The current architecture is a modular monolith, appropriate for a course project and an internal
service desk. If usage grows:

1. Replace full-table dashboard aggregation with database aggregate queries/materialised views and
   cache role-scoped totals.
2. Move SLA scanning and email delivery to a worker/queue (outbox + consumer) so request latency is
   unaffected.
3. Add refresh-token rotation, rate limiting and an API gateway.
4. Partition incidents by tenant/date and add tenant boundaries if the system becomes multi-tenant.
5. Extract notifications into a service if multiple clients (mobile/push/chat) are added.
6. Add OpenTelemetry traces/metrics; the Actuator and audit trail already provide a starting point.

The strategy/state abstractions and event decoupling are intentionally designed so these changes do
not require rewriting the domain workflow.
