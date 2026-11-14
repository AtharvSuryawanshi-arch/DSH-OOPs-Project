# REST API reference

Base URL: `http://localhost:8080/api`  
Authentication: `Authorization: Bearer <jwt>` on every endpoint except login/registration.  
Content type: `application/json`.  
Errors: RFC 9457 `application/problem+json` with `title`, `status`, `detail`, `code` and optional
`errors` (field validation map).

```json
{
  "type": "about:blank",
  "title": "Business rule violation",
  "status": 409,
  "detail": "Incident cannot move from ASSIGNED to RESOLVED.",
  "code": "INVALID_STATE_TRANSITION",
  "instance": "/api/incidents/42/resolve"
}
```

## Authentication

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Public | Returns `{ token, tokenType, user }` |
| `POST` | `/auth/register` | Public | Self-registers an **Employee** and returns a token |
| `GET` | `/auth/me` | Authenticated | Current user profile |

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@itsm.local","password":"Admin@123"}'
```

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "name": "Aarav Mehta",
    "email": "admin@itsm.local",
    "role": "ADMIN",
    "active": true,
    "skills": [],
    "createdAt": "2026-11-09T14:54:09Z"
  }
}
```

JWT claims include subject (email), user id, name and role. Tokens expire after
`JWT_EXPIRATION_MINUTES` (default 480).

## Incidents

Role scoping is automatic on `GET /incidents`: every user sees the incidents they reported,
technicians also see incidents assigned to them, and admins see everything.

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/incidents` | Any authenticated user | Report an incident; classifies, prioritizes, attaches SLA, detects duplicates and optionally auto-assigns (never to the reporter) |
| `GET` | `/incidents` | Authenticated | Paginated, filtered, role-scoped queue |
| `GET` | `/incidents/{id}` | Reporter / assigned technician / Admin | Full detail, comments and audit trail |
| `PATCH` | `/incidents/{id}` | Reporter or Admin, before assignment | Edit title/description/impact/urgency/affected users and re-classify |
| `POST` | `/incidents/{id}/assign` | Admin | Assign/reassign a technician (not the incident's own reporter: 409 `SELF_ASSIGNMENT_NOT_ALLOWED`) |
| `POST` | `/incidents/{id}/acknowledge` | Assigned technician | `ASSIGNED → ACKNOWLEDGED`; checks response SLA |
| `POST` | `/incidents/{id}/start` | Assigned technician | `ACKNOWLEDGED → IN_PROGRESS` |
| `POST` | `/incidents/{id}/resolve` | Assigned technician | `IN_PROGRESS → RESOLVED` with resolution notes |
| `POST` | `/incidents/{id}/verify` | Reporter (any role) | `RESOLVED → VERIFIED` |
| `POST` | `/incidents/{id}/close` | Admin | `VERIFIED → CLOSED` with optional closure note |
| `POST` | `/incidents/{id}/reopen` | Reporter (any role) or Admin | `RESOLVED → IN_PROGRESS` |
| `POST` | `/incidents/{id}/cancel` | Admin | Cancels early-lifecycle incidents with a reason |
| `POST` | `/incidents/{id}/feedback` | Reporter (any role) | 1–5 rating + optional comment, once, after resolution |
| `POST` | `/incidents/{id}/priority-override` | Admin | Override priority with reason and recalculate SLA |
| `POST` | `/incidents/{id}/comments` | Authorized viewer | Public comment or internal note (internal = tech/admin only) |
| `GET` | `/incidents/{id}/audit` | Authorized viewer | Chronological audit entries |
| `GET` | `/incidents/{id}/duplicates` | Authorized viewer | Similar incidents with a match score. Matches the caller may not open are returned with `accessible: false` and without `incidentId` or `title` |
| `GET` | `/incidents/{id}/suggested-resolutions` | Authorized viewer | Similar resolved incidents and their resolution notes |
| `GET` | `/incidents/{id}/recommended-technicians` | Authorized viewer | Ranked candidates with skill/load/experience scores |

### Create request

```json
{
  "title": "Payment gateway unavailable",
  "description": "Customers cannot complete card payments; checkout returns 503.",
  "serviceId": 1,
  "categoryId": null,
  "impact": "EXTREME",
  "urgency": "CRITICAL",
  "affectedUsers": 650
}
```

`categoryId` may be omitted to use auto-classification. The response includes the generated
reference, category, priority score/rationale, SLA deadlines, assigned technician and duplicate
reference. P1 incidents are auto-assigned to the best available technician when auto-assign is
enabled.

### List query parameters

| Parameter | Example | Description |
| --- | --- | --- |
| `search` | `payment` | Title, description or reference |
| `status` | `IN_PROGRESS` | Lifecycle state |
| `priority` | `P1` | Priority |
| `categoryId` | `8` | Category |
| `serviceId` | `1` | Affected service |
| `technicianId` | `2` | Technician (narrows the caller's visible incidents) |
| `reporterId` | `6` | Reporter (narrows the caller's visible incidents) |
| `slaStatus` | `AT_RISK` | `ON_TRACK`, `AT_RISK`, `BREACHED`, `MET` |
| `from` / `to` | `2026-11-09T00:00:00Z` | Created-at date range (ISO-8601) |
| `duplicatesOnly` | `true` | Incidents linked to a possible duplicate |
| `escalatedOnly` | `true` | Escalation level > 0 |
| `page` | `0` | Zero-based page |
| `size` | `20` | Page size (max 100) |

Response:

```json
{
  "content": [
    {
      "id": 10,
      "reference": "INC-2026-001010",
      "title": "Payment application unavailable after deployment",
      "status": "IN_PROGRESS",
      "statusLabel": "In progress",
      "priority": "P1",
      "priorityLabel": "Critical",
      "priorityScore": 74,
      "slaStatus": "BREACHED",
      "technicianName": "Priya Nair",
      "resolutionDueAt": "2026-11-09T15:12:00Z",
      "duplicateOfReference": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 16,
  "totalPages": 1
}
```

### Lifecycle transition matrix

| From | Allowed next |
| --- | --- |
| `NEW` | `CLASSIFIED`, `CANCELLED` |
| `CLASSIFIED` | `ASSIGNED`, `CANCELLED` |
| `ASSIGNED` | `ACKNOWLEDGED`, `CANCELLED` |
| `ACKNOWLEDGED` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED` |
| `RESOLVED` | `VERIFIED`, `IN_PROGRESS` (reopen) |
| `VERIFIED` | `CLOSED` |
| `CLOSED` | none |
| `CANCELLED` | none |

## Technician directory and workload

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/technicians` | Admin, Technician | Technician accounts, skills, capacity and availability |
| `GET` | `/technicians/me/workload` | Technician | Active queue, capacity, resolved count and work items |

## Notifications

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/notifications` | Authenticated | Latest 100 in-app + email notifications |
| `GET` | `/notifications/unread-count` | Authenticated | `{ "count": n }` |
| `PATCH` | `/notifications/{id}/read` | Owner | Marks one notification read |
| `POST` | `/notifications/read-all` | Authenticated | Marks all unread notifications read |

Notifications are produced by incident events and SLA monitor events. Both `IN_APP` and `EMAIL`
channels are stored; email is simulated and printed to the server log.

## Dashboard and reports

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/dashboard` | Authenticated | Role-scoped totals, status/priority mix, category/service analysis, seven-day trend, recent incidents, technician workload (admin) |

Example totals:

```json
{
  "scope": "ADMIN",
  "totals": {
    "total": 16,
    "open": 11,
    "critical": 3,
    "resolved": 5,
    "slaBreaches": 7,
    "unassigned": 2,
    "averageResponseMinutes": 6.1,
    "averageResolutionMinutes": 112.0
  }
}
```

## Catalogue

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/catalog/services` | Authenticated | Active services |
| `GET` | `/catalog/categories` | Authenticated | Active categories |
| `GET` | `/catalog/sla-policies` | Authenticated | Active SLA policies |

## Intelligence

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/intelligence/classify` | Authenticated | Preview category suggestion + duplicate candidates for a draft report |

```json
{
  "title": "Payment transactions failing for some checkout attempts",
  "description": "Some checkout attempts fail with a timeout while others succeed.",
  "serviceId": 1
}
```

Response includes `classification.categoryName`, `confidence`, `rationale`, and
`possibleDuplicates[]` with match scores (same `accessible` rule as `/incidents/{id}/duplicates`: other people's incidents appear without title).

## Admin

All admin endpoints require `ROLE_ADMIN`.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/admin/users` | All users |
| `POST` | `/admin/users` | Create employee/technician/admin with role-specific fields |
| `PATCH` | `/admin/users/{id}` | Update profile, active state, technician capacity/skills, optional password reset |
| `GET/POST` | `/admin/categories` | List/create categories |
| `PATCH` | `/admin/categories/{id}` | Update name/description/active state |
| `GET/POST` | `/admin/services` | List/create services |
| `PATCH` | `/admin/services/{id}` | Update name/description/criticality/active state |
| `GET/POST` | `/admin/sla-policies` | List/create SLA policies |
| `PATCH` | `/admin/sla-policies/{id}` | Update targets/scope/active state |
| `GET` | `/admin/settings` | Assignment strategy + auto-assign |
| `PUT` | `/admin/settings` | Change strategy and/or auto-assign |
| `GET` | `/admin/audit?page=0&size=25` | Global audit trail |
| `POST` | `/admin/sla/scan` | Run the SLA monitor synchronously (demo/ops) |

### Create user example

```json
{
  "name": "Maya Iyer",
  "email": "maya@itsm.local",
  "password": "Maya@1234",
  "role": "TECHNICIAN",
  "experienceYears": 4,
  "maxCapacity": 5,
  "skillCategoryIds": [3, 7]
}
```

### Update user example

```json
{
  "available": false,
  "maxCapacity": 4,
  "skillCategoryIds": [3]
}
```

Only provided fields change. Email is immutable; role changes are intentionally not supported
because the user subtypes carry different data. Deactivating a technician with active incidents or
the last active administrator is rejected.

### SLA policy example

```json
{
  "name": "P1 critical — payment gateway",
  "priority": "P1",
  "serviceId": 1,
  "responseMinutes": 2,
  "resolutionMinutes": 15,
  "warningPercent": 70,
  "active": true
}
```

## Health

| Method | Path | Access |
| --- | --- | --- |
| `GET` | `/actuator/health` | Public |

Shows database connectivity and application readiness. Details are exposed in development; set
`management.endpoint.health.show-details=never` for production.

## End-to-end curl example

```bash
BASE=http://localhost:8080

TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"employee@itsm.local","password":"Employee@123"}' | jq -r .token)

INCIDENT=$(curl -s -X POST "$BASE/api/incidents" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "title":"Wi-Fi keeps dropping on the third floor",
    "description":"The corporate Wi-Fi network disconnects repeatedly on the third floor.",
    "serviceId":4,
    "impact":"MEDIUM",
    "urgency":"HIGH",
    "affectedUsers":30
  }')

echo "$INCIDENT" | jq '{id, reference, status, priority, technicianName, slaStatus}'
```
