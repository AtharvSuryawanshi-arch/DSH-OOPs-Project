# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

I-ITSM: an ITIL-style incident management system built for an OOP course (CS F213). Spring Boot 4.1 / Java 21 / PostgreSQL 17 backend in `backend/`, React 19 + Vite 8 + TypeScript 7 SPA in `frontend/`. The OOP design patterns are a graded requirement, so new behaviour should fit the existing patterns (state, strategy, factory, observer) rather than bypass them. `docs/` holds the architecture, API, database, test-case and report documents — keep them in sync when changing behaviour they describe.

## Course brief and team decisions

Two reference PDFs live **outside this repo**, in the parent folder's `docs/` (`../docs/` from the repo root):

- `OOP_S1_2026-2027_Project.pdf`: the official course brief (CS F213 / MAC F212). It sets the MVP, the role capabilities, the incident lifecycle, the OOP and design-pattern requirements, and the grading rubric. Submission deadline: 22 Nov 2026.
- `team-decisions-and-todos.pdf`: the team's agreed decisions and open to-dos. Read it before starting a task, because it may already settle the question.

Every change must keep meeting the brief. In particular:

- OOP and design patterns are a major grading criterion. Extend the existing State, Strategy, Factory, Observer and Repository structures instead of adding role or status `if`/`switch` branches around them.
- Use inheritance only for genuine is-a relationships, and represent business rules with interfaces and polymorphism.
- Invalid state transitions must stay impossible.
- Every important action must keep producing an audit record.

When a change affects behaviour the report describes, update `docs/` in this repo to match.

## Commands

From the repo root (Makefile wraps these):

```bash
make db-up          # docker compose up -d --wait db  (postgres:17-alpine on host port ${POSTGRES_PORT:-5432})
make backend        # cd backend && ./mvnw spring-boot:run   -> http://localhost:8080
make frontend       # cd frontend && npm run dev             -> http://localhost:5173 (strictPort)
make test           # backend tests + frontend lint & build
make db-reset       # drop the volume and recreate (demo seed re-runs on next backend start)
```

Backend (`backend/`, use `mvnw.cmd` on Windows cmd/PowerShell):

```bash
./mvnw test                                              # all tests
./mvnw test -Dtest=WeightedPriorityStrategyTest          # one class
./mvnw test -Dtest=IncidentLifecycleTest#methodName      # one method
./mvnw clean package
```

`ItsmWorkflowIntegrationTest` uses Testcontainers, so Docker must be running for the full suite. Unit tests don't need a database.

Frontend (`frontend/`): `npm run lint` (oxlint), `npm run build` (`tsc -b && vite build` — this is the type check), `npm run dev`. There are no frontend unit tests.

Demo logins (seeded when the `users` table is empty): `admin@itsm.local` / `Admin@123`, `priya@itsm.local` / `Tech@123`, `employee@itsm.local` / `Employee@123`.

## Architecture

Backend packages under `com.example.itsm` are feature-oriented (`incident`, `sla`, `user`, `catalog`, `notification`, `audit`, `intelligence`, `dashboard`, `settings`, `security`, `seed`, `common`). Key cross-cutting mechanics:

- **Incident lifecycle = State pattern** (`incident/lifecycle`). `IncidentState` has one method per action (`classify`, `assign`, `acknowledge`, `start`, `resolve`, `verify`, `reopen`, `close`, `cancel`); `AbstractIncidentState` refuses all of them with `InvalidStateTransitionException` (→ 409), and each concrete state overrides only the actions it allows, updating the incident and calling `moveTo`. `IncidentWorkflowService` is the context: services call `workflowService.verify(incident)` etc. and never check `if (status == …)` themselves. There is no generic "set status" endpoint — each action has its own endpoint, audit entry and permission check. Adding an action means: a method on `IncidentState` + refusal default in `AbstractIncidentState` + overrides in the states that allow it, a delegate in `IncidentWorkflowService`, the service method (permissions, audit, events), the controller endpoint, and `frontend/src/lib/lifecycle.ts` if the reporter-facing steps change.
- **Entities own their state changes.** `Incident` fields are private and mutated through domain methods (`applyClassification`, `applySla`, `assignTechnician`, `resolve`, …); controllers/services never set status or timestamps directly. Incidents use `@Version` optimistic locking.
- **Incident creation pipeline** (`IncidentService.create`, single transaction): validate service → `CategorizationService` auto-classify → `WeightedPriorityStrategy` (score = impact × urgency × service criticality + affected-users bonus) → `SlaPolicyService` deadlines → save NEW→CLASSIFIED → audit → `DuplicateDetectionService` → publish event → `AssignmentService.autoAssign` using the strategy stored in `system_settings`.
- **Duplicate detection**: `IncidentRepository.findDuplicateCandidates` filters by status, last 30 days and exclusion in SQL and caps at 500 newest rows; only the text similarity runs in Java (`DuplicateDetectionService`).
- **Strategies**: `PriorityStrategy` and `AssignmentStrategy` (skill-based, load-based, round-robin, selected via `AssignmentStrategyFactory` from runtime settings). Recommendations and strategies share the candidate scoring in `TechnicianCandidateService`.
- **Observer via Spring events**: services publish events from `NotificationEvents`; `NotificationEventListener` (`@EventListener`, synchronous, same transaction) decides recipients and wording, then calls `NotificationDispatcher`, which delivers through every `NotificationSender` bean (`InAppNotificationSender`, `EmailNotificationSender` → `EmailGateway`, simulated by `LoggingEmailGateway`). A new channel = a new `NotificationSender` class + a `NotificationChannel` enum value + a Flyway migration widening `chk_notification_channel` + the frontend channel label. Side effects belong in listeners, not controllers.
- **SLA monitor**: `SlaMonitorService` is `@Scheduled` (interval `app.sla.monitor-interval-ms`) and idempotent via persisted warning/breach flags on the incident; also triggerable via `POST /api/admin/sla/scan`.
- **Security**: stateless HS384 JWT (`JwtAuthenticationFilter`, JJWT). `@PreAuthorize` is the coarse role gate; per-incident ownership is decided in one place, `IncidentAccessPolicy.canView`, used by `IncidentService` and by duplicate detection (matches the viewer may not open are returned with `accessible: false`, no id or title): any role can report, everyone sees what they reported, technicians also see what is assigned to them, admins see all; only the reporter can verify or leave feedback (reporter or admin can reopen); a technician is never assigned an incident they reported (`TechnicianCandidateService` filter + `AssignmentService` guard). New incident endpoints must call those checks.
- **Users**: `User` is abstract with `SINGLE_TABLE` inheritance (`Employee`, `Technician`, `Administrator`, `user_type` discriminator); create via `UserFactory`.
- **Errors**: domain exceptions in `common` (`BusinessRuleException`, `ResourceNotFoundException`, `InvalidStateTransitionException`) are mapped to RFC 9457 `ProblemDetail` by `ApiExceptionHandler`.
- **Persistence**: Flyway migrations in `src/main/resources/db/migration` with `ddl-auto: validate` — any entity change needs a new `V{n}__*.sql` migration (don't edit `V1`). `open-in-view` is off, so lazy relations must be loaded within the service transaction. Time is injected via a `Clock` bean (`config/ClockConfig`) — services use it instead of `Instant.now()` so tests stay deterministic.
- Uses Jackson 3 (`tools.jackson.*` packages), not `com.fasterxml`.

Frontend: `src/api/client.ts` is the single fetch wrapper (JWT from localStorage key `itsm.token`, ProblemDetail → `ApiError`, 401 event triggers logout); `src/api/types.ts` mirrors backend DTOs and must be updated alongside them. `hooks/useApi.ts` is the abort-safe GET hook; pass `{ pollMs }` for quiet background refresh (dashboard, incident detail and "Waiting on you" poll so two windows in different roles see each other's steps). Styling: all colours are CSS variables in `src/index.css` (palette "Ink & Teal", IBM Plex Sans/Mono), redefined for dark mode under `prefers-color-scheme` and `:root[data-theme='dark']`; never hard-code colours in `styles.css` or components (Recharts reads them through `useChartColors()` in `lib/theme.ts`, which also owns the light/dark switch stored in `localStorage` key `itsm.theme`). Employees land on `EmployeeHomePage`; other roles get `DashboardPage`. The Vite dev server proxies `/api` and `/actuator` to `VITE_PROXY_TARGET` (default `http://localhost:8080`); setting `VITE_API_BASE_URL` bypasses the proxy and requires the backend `CORS_ALLOWED_ORIGINS` to include the frontend origin.

## Conventions

`.editorconfig`: LF line endings, 2-space indent, 4-space for Java, tabs in the Makefile.
