# Test cases and validation evidence

This document records the planned and executed validation for I-ITSM. It covers normal operations,
boundary conditions, invalid input, exceptions, authorization and business rules, as required by the
project brief.

## 1. How to reproduce

```bash
# Backend unit tests + Testcontainers integration test
cd backend && ./mvnw test

# Frontend static checks and production build
cd frontend && npm run lint && npm run build

# Manual API smoke test against a running stack
BASE=http://localhost:8080
TOKEN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"admin@itsm.local","password":"Admin@123"}' | jq -r .token)
curl -H "Authorization: Bearer $TOKEN" "$BASE/api/dashboard" | jq .totals
```

The Testcontainers test requires a working Docker daemon; it starts a disposable
`postgres:17-alpine` container and never touches the development database.

## 2. Automated test inventory

**Result: 34 tests run, 34 passed, 0 failures** (33 unit tests + 1 full-stack integration test).

| Test class | Tests | Covers |
| --- | --- | --- |
| `WeightedPriorityStrategyTest` | 4 | Formula, P1/P2/P3/P4 bands, affected-user bonus, rationale |
| `IncidentLifecycleTest` | 5 | State transitions allowed/denied, closed/cancelled terminal states, invalid transition exception |
| `AssignmentStrategyTest` | 4 | Skill-based, load-based, round-robin, capacity/unavailable filtering |
| `CategorizationServiceTest` | 3 | Network/auth classification, low-confidence fallback |
| `TextSimilarityTest` | 4 | Identical/unrelated text, overlap coefficient, empty input |
| `SlaPolicyServiceTest` | 2 | Response/resolution/warning deadline arithmetic, service override math |
| `SlaStatusResolverTest` | 5 | On track, at risk, response breach, met, late resolution |
| `JwtServiceTest` | 2 | Token identity/validation, tampered token rejection |
| `IncidentDomainTest` | 4 | Acknowledge, resolve, feedback, duplicate link |
| `ItsmWorkflowIntegrationTest` | 1 | Real PostgreSQL + HTTP API: login, create, auto-classify, auto-assign, ack, start, resolve, verify, close, feedback, role guard, invalid transition |

## 3. Manual API/UI validation performed

The following were exercised against the running application with curl and Playwright:

- Login/registration for all three roles; unauthorized requests return 401.
- Employee creates incidents; auto-category, priority rationale, SLA deadlines and auto-assignment
  are returned immediately.
- Full lifecycle through acknowledge → start → resolve → verify → close → feedback.
- Invalid transition before acknowledgement returns 409.
- Blank title → 400 with field-level errors; unknown service → 404.
- Edit before assignment reclassifies and recalculates priority/SLA.
- Feedback before resolution and duplicate feedback both return 409.
- Admin cancellation, priority override, user creation/update, deactivation guard.
- Employee/technician/admin dashboards, workload, notifications, audit page, reports.
- Duplicate preview and suggested resolutions.
- Assignment strategy switch from skill-based to load-based changed the selected technician on the
  next incident.
- Browser checks at desktop and mobile widths with no console/page errors.

## 4. Test case catalogue

Legend: **A** = automated test; **M** = manual API/browser verification; **R** = code review /
invariant enforced in design.

### Authentication and authorization

| ID | Scenario | Preconditions | Steps | Expected | Status |
| --- | --- | --- | --- | --- | --- |
| TC-AUTH-01 | Valid login as administrator | Seeded database | POST `/api/auth/login` with `admin@itsm.local` / `Admin@123` | 200, JWT and admin profile | Pass (M/A) |
| TC-AUTH-02 | Invalid password | Existing user | POST login with wrong password | 401, ProblemDetail `INVALID_CREDENTIALS`/unauthorized; no token | Pass (M) |
| TC-AUTH-03 | Access protected API without token | Backend running | GET `/api/dashboard` without `Authorization` | 401 JSON ProblemDetail | Pass (M) |
| TC-AUTH-04 | Employee calls admin API | Employee token | GET `/api/admin/users` | 403 forbidden | Pass (M) |
| TC-AUTH-05 | Employee opens another reporter's incident | Employee token | GET `/api/incidents/2` where reporter is different | 403 | Pass (M) |
| TC-AUTH-06 | Technician opens an unassigned incident | Technician token | GET incident not assigned to them | 403 | Pass (M) |
| TC-AUTH-07 | Employee attempts an internal note | Employee token, own incident | POST comment with `internal: true` | 403; internal notes restricted | Pass (M) |
| TC-AUTH-08 | Internal note visibility | Technician created an internal note | Employee opens detail; admin opens detail | Employee response excludes the note; admin/assigned technician sees it | Pass (M) |
| TC-AUTH-09 | Tampered JWT | Any valid token | Change last characters of token; call protected API | 401; token rejected | Pass (A/M) |
| TC-AUTH-10 | Self-deactivation guard | Admin token | PATCH own user with `active:false` | 409 `CANNOT_DEACTIVATE_SELF` | Pass (M) |
| TC-AUTH-11 | Last-admin / technician-with-work guard | Admin token; technician has active incidents | Deactivate technician with active work | 409 `TECHNICIAN_HAS_ACTIVE_INCIDENTS`; data unchanged | Pass (M) |

### Incident lifecycle

| ID | Scenario | Preconditions | Steps | Expected | Status |
| --- | --- | --- | --- | --- | --- |
| TC-INC-01 | Report incident with smart triage | Employee token, active service | POST valid incident with no category | 201; category auto-detected; priority score/rationale; SLA deadlines; possible auto-assignment | Pass (M/A) |
| TC-INC-02 | Blank title validation | Employee token | POST incident with whitespace title | 400 with `errors.title`; no row created | Pass (M) |
| TC-INC-03 | Unknown service | Employee token | POST incident with `serviceId: 9999` | 404 `NOT_FOUND` | Pass (M) |
| TC-INC-04 | Illegal transition | Assigned incident | Technician calls `/resolve` before acknowledgement | 409 `INVALID_STATE_TRANSITION` | Pass (M/A) |
| TC-INC-05 | Happy-path lifecycle | Newly created assigned incident | Assigned tech acknowledges → starts → resolves | 200 each; statuses ACKNOWLEDGED → IN_PROGRESS → RESOLVED; timestamps and audits present | Pass (M/A) |
| TC-INC-06 | Verification and closure | Resolved incident | Reporter verifies; admin closes | 200; VERIFIED → CLOSED; closure note and audit | Pass (M/A) |
| TC-INC-07 | Edit before assignment | Employee or admin, CLASSIFIED incident | PATCH title/description/impact/urgency/users | 200; priority/SLA recalculated; audit `UPDATED` | Pass (M) |
| TC-INC-08 | Edit after assignment rejected | Reporter, assigned incident | PATCH incident | 409 `INCIDENT_NOT_EDITABLE`; data unchanged | Pass (M) |
| TC-INC-09 | Feedback before resolution | Reporter, IN_PROGRESS incident | POST feedback | 409 `FEEDBACK_NOT_AVAILABLE` | Pass (M) |
| TC-INC-10 | Duplicate feedback | Reporter, closed incident with feedback | POST feedback again | 409 `FEEDBACK_ALREADY_PROVIDED` | Pass (M) |
| TC-INC-11 | Admin cancels early incident | Admin token, ASSIGNED incident | POST `/cancel` with reason | 200; CANCELLED; reason in closure notes; audit | Pass (M) |
| TC-INC-12 | Close before verification is rejected | Admin, RESOLVED incident | POST `/close` | 409 `INCIDENT_NOT_VERIFIED` | Pass (M) |
| TC-INC-13 | Reopen resolution | Reporter, RESOLVED incident | POST `/reopen` | 200; IN_PROGRESS; audit; SLA clock continues | Pass (R) |
| TC-INC-14 | Incident detail access | Reporter, assigned tech, admin | GET detail | Full data for authorized actors only; comments/audit included | Pass (M) |

### Priority and SLA

| ID | Scenario | Preconditions | Steps | Expected | Status |
| --- | --- | --- | --- | --- | --- |
| TC-PRI-01 | Extreme impact on critical service | — | Calculate priority `EXTREME/CRITICAL/CRITICAL` with 500 users | P1, score 74, rationale mentions all three factors | Pass (A) |
| TC-PRI-02 | Low-severity incident | — | Calculate `LOW/LOW/LOW` | P4, score 1 | Pass (A) |
| TC-PRI-03 | Affected-user bonus lifts band | — | Compare HIGH/HIGH/HIGH with 1 vs 500 users | 27/P2 vs 37/P1 | Pass (A) |
| TC-PRI-04 | Service criticality changes priority | — | Same impact/urgency on low vs critical service | Higher criticality produces higher score/priority | Pass (A) |
| TC-SLA-01 | Deadline calculation | P2 policy 15/120, warning 80% | Compute deadlines from creation | Response +15m; resolution +120m; warnings +12m and +96m | Pass (A) |
| TC-SLA-02 | On track | Incident before warning | Resolve SLA status | ON_TRACK | Pass (A) |
| TC-SLA-03 | At risk | Between warning and deadline | Resolve SLA status | AT_RISK | Pass (A) |
| TC-SLA-04 | Response breach | Unacknowledged past response due | Resolve SLA status | BREACHED | Pass (A/M) |
| TC-SLA-05 | Resolution breached after target | Resolved after resolution due | Resolve SLA status | BREACHED; `resolutionBreached` true | Pass (A) |
| TC-SLA-06 | Met | Resolved within both targets | Resolve SLA status | MET | Pass (A) |
| TC-SLA-07 | Service-specific override | Payment P1 policy 2/15 vs global 5/30 | Create payment P1 incident | Due dates use 2/15 and 70% warning | Pass (M) |
| TC-SLA-08 | Monitor idempotence | Seeded breached incidents | POST `/api/admin/sla/scan` twice | No duplicate warning/breach audits/notifications; flags stable | Pass (M) |
| TC-SLA-09 | Auto-escalation | Unacknowledged P1/P2 past response | Let monitor run / trigger scan | Escalation level increases; admin notification and audit | Pass (M) |
| TC-SLA-10 | Policy validation | Admin token | Create policy with resolution ≤ response | 409 `INVALID_SLA_TARGETS` | Pass (M) |

### Assignment and intelligence

| ID | Scenario | Preconditions | Steps | Expected | Status |
| --- | --- | --- | --- | --- | --- |
| TC-ASG-01 | Skill-based assignment | Skill technicians with capacity | Select best candidate | Category match wins over a less-busy mismatch | Pass (A) |
| TC-ASG-02 | Load-based assignment | Two qualified technicians | Select with LOAD_BASED | Least active technician chosen | Pass (A/M) |
| TC-ASG-03 | Round-robin | Two available technicians | Select four times | Alternates across candidates | Pass (A) |
| TC-ASG-04 | Capacity/unavailable filtering | One at capacity, one unavailable | Run all strategies | No selection; admin sees no capacity | Pass (A) |
| TC-ASG-05 | Live strategy switch | Admin | PUT settings to LOAD_BASED; create incident | New incident follows load-based rule | Pass (M) |
| TC-ASG-06 | Recommendation scoring | Incident with category | GET recommended-technicians | Ranked list with skill/load/experience/history, skill match flags | Pass (M) |
| TC-INT-01 | Auto-classification | Categories configured | Preview "VPN Wi-Fi drops" / "password reset" | Network / Authentication with confidence > 0.5 | Pass (A/M) |
| TC-INT-02 | Classification fallback | No keyword matches | Preview generic text | Other with low confidence and manual-review rationale | Pass (A) |
| TC-INT-03 | Duplicate detection | Existing similar open incident | Preview/post a matching report | Similarity listed; strong match auto-linked and audit/notification created | Pass (M) |
| TC-INT-04 | Suggested resolutions | Similar resolved incident | GET suggested-resolutions | Prior resolution notes returned with match score | Pass (M) |
| TC-INT-05 | Duplicate review UI | Linked incident | Open detail | Duplicate banner and related incident links | Pass (M) |

### Search, reporting, audit and notifications

| ID | Scenario | Preconditions | Steps | Expected | Status |
| --- | --- | --- | --- | --- | --- |
| TC-SRCH-01 | Combined filters | Multiple incidents | GET `/incidents?search=payment&status=IN_PROGRESS&priority=P1&serviceId=1&slaStatus=BREACHED` | Only matching incidents; pagination metadata correct | Pass (M) |
| TC-SRCH-02 | Date range filter | Incidents across days | GET with `from`/`to` | Only incidents in range | Pass (M) |
| TC-SRCH-03 | SLA filter | At-risk/breached incidents | Filter by each SLA state | Counts and rows match resolver state | Pass (M) |
| TC-REP-01 | Role-scoped dashboard | All roles | GET `/dashboard` as employee/technician/admin | Employee sees own reports; technician assigned; admin organisation-wide; admin workstation sections populated | Pass (M) |
| TC-REP-02 | Report aggregates | Seeded data | Open Reports | Totals, trends, priority mix, service/category analysis, technician workload match API data | Pass (M) |
| TC-AUD-01 | Audit completeness | Full workflow executed | GET incident audit and admin audit | Created, classified, priority, assigned, ack, start, resolve, verify, close, comments, SLA, feedback all recorded with actor/time | Pass (M) |
| TC-NOT-01 | Two notification channels | Incident event | GET `/notifications` | Both IN_APP and EMAIL rows; email rows logged by backend | Pass (M) |
| TC-NOT-02 | Mark read | Unread notifications | PATCH one, POST read-all | Unread count decreases; all become read; bell refreshes | Pass (M) |
| TC-ADM-01 | User creation and duplicate email | Admin | POST user; repeat same email | 201 with role fields; 409 on duplicate | Pass (M) |
| TC-ADM-02 | Catalogue CRUD | Admin | Create/update category and service | Persisted; active/inactive respected in catalog | Pass (M) |
| TC-SEC-01 | Validation and ProblemDetail consistency | Any invalid request | Force 400/401/403/404/409 | JSON `title/status/detail/code`; field errors for validation | Pass (M/A) |

## 5. Edge cases and negative paths intentionally covered

- Whitespace-only title/description and password length limits.
- Missing/invalid service IDs.
- Illegal lifecycle transitions (assigned → resolved, new → closed).
- Duplicate feedback.
- Cross-tenant ownership traversal (employee/technician trying another incident).
- Internal-note leakage prevention.
- Deactivated users cannot log in or be assigned.
- Technician at capacity is not selected.
- Global vs service-specific SLA policy precedence.
- SLA warning/breach flags prevent repeated side-effects.
- Admin cannot remove the last active admin or self-deactivate.
- Unknown endpoints return 404 ProblemDetail instead of a stack trace.

## 6. Known gaps

The project intentionally keeps external integrations simulated. There is no load/performance test,
no email-provider integration test, and no browser automation committed to CI. The Testcontainers
integration test provides a real database/API round trip, and the UI was validated with Playwright
during development. These gaps are acceptable for the course scope and listed in the report's future
work.
