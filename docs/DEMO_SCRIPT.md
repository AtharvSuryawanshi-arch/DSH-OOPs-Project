# Demo and video plan (15–20 minutes)

This script is designed for the project demonstration video. It covers the three required workflows,
shows each role, and explains the OOP design and intelligent features while the application is on
screen. Adjust the timing to the group size; every member should introduce themselves and explain at
least one module.

## Before recording

```bash
# From a clean checkout
docker compose down -v            # only if you want a fresh seeded database
docker compose up -d --wait db
cd backend && ./mvnw spring-boot:run
# another terminal
cd frontend && npm run dev
```

Open <http://localhost:5173> and keep a second browser profile or incognito window ready for switching
roles. The seeder creates a rich dataset (13 incidents, 4 technicians, SLA breaches, a duplicate pair,
an at-risk incident and notifications) automatically.

Demo accounts:

- Admin: `admin@itsm.local` / `Admin@123`
- Technician: `priya@itsm.local` / `Tech@123` (also `rohan@itsm.local`, `dev@itsm.local`)
- Employee: `employee@itsm.local` / `Employee@123` (also `vikram@itsm.local`)

Reset after the demo with `docker compose down -v` if you need a clean run.

## Suggested timing

| Time | Segment | Presenter | What is shown |
| --- | --- | --- | --- |
| 0:00–1:30 | Introduction | All | Team names, problem statement, stack, architecture in one slide |
| 1:30–4:00 | Login + role-based experience | Member 1 (UI) | Login, admin dashboard, role differences, responsive layout |
| 4:00–7:30 | Workflow 1 — report to assignment | Member 2 (Backend/domain) | Employee reports P1 payment incident; auto classification, priority rationale, SLA, duplicate detection, auto-assignment, notifications |
| 7:30–11:00 | Workflow 2 — investigation to closure | Member 3 (Technician/domain) | Technician queue, acknowledge, start, internal note, resolve, reporter verify, admin close, feedback, audit timeline |
| 11:00–13:30 | Workflow 3 — correlation, SLA and escalation | Member 2/4 | Duplicate link, at-risk/breached SLA, escalation, admin reports, workload and service analysis |
| 13:30–16:30 | Administration and OOP design | Member 4/5 | Users/skills, services/categories, SLA policies, assignment strategy switch, audit; state/strategy/observer explanation |
| 16:30–18:30 | Testing and security | Member 5 | `mvnw test`/Testcontainers, lint/build, RBAC/401/403, invalid transition, validation |
| 18:30–20:00 | Summary and future work | All | Requirements coverage, limitations, future work, thank you |

## Workflow 1 — Employee report → triage → assignment (about 3.5 minutes)

1. Sign in as **employee@itsm.local / Employee@123**.
2. On the employee dashboard, point out the scoped KPIs and recent incidents.
3. Click **Report an incident**.
4. Fill:
   - Title: “Payment application unavailable after deployment”
   - Description: “Customers cannot complete card payments. The gateway returns 503 and checkout
     requests time out.”
   - Affected service: **Payment Gateway**
   - Impact: **Extreme**, Urgency: **Critical**, Affected users: **650**
5. Click **Analyze this report**. Show:
   - auto-classification **Application** with confidence and rationale;
   - possible duplicate list (the seeded payment incident(s)).
6. Click **Create incident**. On the detail page, explain:
   - reference `INC-...`, P1 priority and stored rationale/score;
   - response target 2 minutes and resolution target 15 minutes (payment-specific SLA), and why the
     service override beats the global P1 policy;
   - auto-assignment to the best technician (skill/load/experience);
   - assignment/created notifications;
   - the duplicate warning if the report matched.
7. Switch to **admin@itsm.local** and open the notification bell / inbox to show the in-app and
   simulated email records. Mention that both channels are stored; email is simulated and logged.

## Workflow 2 — Technician investigation → resolution → verification → closure (about 3.5 minutes)

Use a seeded assigned incident if you do not want to mutate the new P1 (e.g. the cloud-storage
incident) or create a fresh one with auto-assign enabled.

1. Sign in as the assigned technician (use the technician shown on the incident).
2. Open **My queue**; show capacity utilisation, SLA deadlines and the incident list.
3. Open an **Assigned** incident.
4. Click **Acknowledge**. Point out the response timestamp and any response-breach warning.
5. Click **Start investigation**.
6. Add an **internal note** (visible only to technicians/admins). Explain that employees cannot see it.
7. Add a public comment.
8. Click **Resolve incident**, enter resolution notes, and confirm them.
9. Switch to the reporter (employee). Open the incident, show the resolution notes, click **Verify
   resolution**, and optionally leave feedback (1–5 stars + comment).
10. Switch to the admin. Open the verified incident and click **Close incident**, add a closure note.
11. Open the **Audit trail** tab and scroll: created → classified → priority → assigned → acknowledged
    → started → comments → resolved → verified → closed → feedback. Each entry has actor, time and
    state change.
12. Open **Notifications** as the technician/reporter to show assignment, status and resolution
    notifications across both channels.

## Workflow 3 — Correlation, SLA risk, escalation and reporting (about 2.5 minutes)

1. Return to the admin dashboard. Show:
   - P1/open/SLA-breach KPI cards;
   - seven-day created vs resolved trend;
   - priority mix;
   - SLA watch list;
   - technician workload against capacity.
2. Open the seeded payment incident with breached response/resolution SLAs.
3. Show the **duplicate/related incident** panel and open the linked report. Explain the similarity
   score and the self-reference in the database.
4. Show the **SLA clock** and warning/breach note. If the monitor has run, the escalation banner shows
   level 1/2/3 for an unacknowledged critical incident. Run **SLA scan** from the Reports/SLA page to
   demonstrate deterministic/idempotent monitoring.
5. Open **Reports**:
   - service analysis (Payment Gateway/Cloud/Email…);
   - category analysis;
   - technician workload table;
   - average response/resolution and breached count.
6. Open **Audit trail** in administration and show system-generated escalation/SLA entries.

## Administration and design segment (about 3 minutes)

1. **Users & skills**: show the user table, role badges, technician skills/capacity. Add a technician
   with a skill (or edit one) and explain the user inheritance model. Demonstrate the self-
   deactivation guard or the active-incident deactivation guard.
2. **Services & categories**: show business criticality and active flags. Explain how a new category
   automatically becomes available to the classifier.
3. **SLA policies**: show global P1–P4 plus the payment-specific P1 override. Open the add-policy
   modal; mention validation (resolution > response).
4. **Assignment settings**: switch from Skill-based to Load-based (and back), explain how the
   candidate scoring works and that the change applies to future classifications without redeploying.
5. **Object-oriented design** (talk over the architecture slide or the code):
   - State pattern for the lifecycle and why illegal transitions are rejected;
   - Strategy pattern for priority and assignment;
   - Observer/domain events for notifications and audit;
   - Factory/repository patterns and the user inheritance hierarchy;
   - composition of comments/audit into incidents and optimistic locking.

## Testing and security segment (about 2 minutes)

1. Run `cd backend && ./mvnw test`; mention 33 unit tests + 1 full-stack Testcontainers integration
   test against a disposable PostgreSQL container.
2. Run `cd frontend && npm run lint && npm run build`; show a clean build.
3. In the browser or terminal, demonstrate:
   - unauthenticated request → 401;
   - employee calling `/api/admin/users` → 403;
   - technician accessing an unassigned incident → 403;
   - invalid transition (assigned → resolved) → 409;
   - blank title → 400 with field errors;
   - unknown endpoint → 404 ProblemDetail.
4. Briefly explain BCrypt, JWT expiry and the internal-note filtering.

## Closing (about 1.5 minutes)

- Restate the three workflows and how the MVP requirements are met.
- Highlight the innovation features: auto-classification, duplicate detection, smart recommendations,
  SLA risk/escalation, suggested resolutions.
- Mention limitations honestly: simulated email, no refresh tokens/rate limiting, in-process monitor,
  single tenant.
- Summarise future work (outbox/real providers, real-time notifications, MFA, analytics at scale,
  dependency graph, CI/CD).

## Recording tips

- Use two browser windows side by side (employee and technician) for hand-offs; keep the URL bar
  visible to show the role switch.
- Zoom the browser to 90–100% at 1080p; hide unrelated tabs and notifications.
- Prepare the demo data before recording; leave the dashboard animations ~1.5 seconds before
  screenshots so charts are fully drawn.
- Do not show the terminal output of real secrets; the demo credentials are intentionally public.
- Keep each workflow under 4 minutes and rehearse the role switches.
- Name the final file according to the course instruction, e.g.
  `1_OOP_S1_2025-2026_Project.mp4`.
