# Demo and video plan (15–20 minutes)

**Concept: "One outage, start to finish."** Instead of touring features, the video follows the
brief's own scenario, *the payment application is down*, through all three required workflows. Each
design pattern is explained at the moment it does its job, and we deliberately try to break the rules
on camera. Every member introduces themselves and presents the part they built.

## Before recording

```bash
docker compose down -v            # fresh, seeded database (removes test data)
docker compose up -d --wait db
cd backend && ./mvnw spring-boot:run
# another terminal
cd frontend && npm run dev
```

On Windows, if the backend fails with "Unable to establish loopback connection", start it from
PowerShell with a different socket folder:

```powershell
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$env:LOCALAPPDATA\JavaSockets"; New-Item -ItemType Directory -Force "$env:LOCALAPPDATA\JavaSockets" | Out-Null; cd backend; .\mvnw.cmd spring-boot:run
```

Open <http://localhost:5173> in **two browser windows side by side** (one normal, one private/incognito
so they can be signed in as different people). The dashboard, incident page and "Waiting on you" panel
refresh themselves every 10–15 seconds, so a step taken in one window appears in the other without
pressing Refresh.

Demo accounts (public, seeded):

| Who | Login | Password |
| --- | --- | --- |
| Neha Sharma, employee | `employee@itsm.local` | `Employee@123` |
| Vikram Rao, employee | `vikram@itsm.local` | `Employee@123` |
| Priya Nair, technician (application, database, cloud) | `priya@itsm.local` | `Tech@123` |
| Rohan / Dev / Sara, technicians | `rohan@` / `dev@` / `sara@itsm.local` | `Tech@123` |
| Aarav Mehta, service manager | `admin@itsm.local` | `Admin@123` |

Choose light or dark mode with the **Dark mode** switch at the bottom of the sidebar before you start,
and use the same mode for the whole recording.

## Running order

| Time | Segment | Presenter | What is on screen |
| --- | --- | --- | --- |
| 0:00 | Cold open | Narrator | The finished audit trail of the outage (record it at the end, show it first) |
| 0:45 | Team | All | Each member: name + the module they built |
| 1:45 | Act 1: Report (Workflow 1) | Member 2 | Neha reports the outage; triage; assignment; strategy switch |
| 5:00 | Act 2: Fix (Workflow 2) | Member 3 | Split screen Priya / Neha; "break it"; still broken → fixed → confirmed → closed |
| 10:00 | Act 3: Storm (Workflow 3) | Member 4 | Several reports; duplicates; SLA risk; escalation; dashboard and reports |
| 13:00 | Under the hood | Member 1 / 5 | Architecture, then each pattern as "where you just saw it" |
| 16:00 | Proof | Member 5 | Test run (44 passing), security checks |
| 17:30 | Close | All | MVP checklist, limitations, next steps |

## Cold open (45 seconds)

Show the **Audit trail** tab of the finished outage incident, scrolling from "created" to "closed".
Line to say: *"Every line on this screen is going to happen in front of you in the next fifteen
minutes."* Then cut to Neha's home screen.

## Act 1: Report → triage → assignment (Workflow 1, about 3 minutes)

1. Sign in as **Neha** (`employee@itsm.local`). Her **Home** shows *her* requests only: "Waiting on
   you", "Being worked on" with a progress bar, and "Recently fixed". Point out that an employee does
   not see operational statistics.
2. Click **New incident** (top bar). Fill in:
   - Title: "Payment application unavailable after deployment"
   - Description: "Customers cannot complete card payments. The gateway returns 503 and checkout
     requests time out."
   - Service: **Payment Gateway**; Impact **Extreme**; Urgency **Critical**; People affected **650**.
3. Click **Analyze this report**: category **Application** with confidence, and the seeded payment
   incidents listed as possible duplicates.
4. Click **Create incident**. On the incident page show:
   - the **Progress** tracker (Reported → Assigned → Working on it → Fixed → Confirmed → Closed);
   - P1 with the stored formula ("Impact × Urgency × Criticality + affected-users bonus");
   - the payment-specific SLA (tighter than the global P1 policy);
   - auto-assignment to **Priya** (skill match: application).
5. **"Why Priya?"** Switch to the code: `AssignmentStrategy` interface and its three implementations
   (`SkillBasedAssignmentStrategy`, `LoadBasedAssignmentStrategy`, `RoundRobinAssignmentStrategy`).
   *"The incident code never changes: it asks the configured strategy."*
6. In the admin window: **Assignment settings** → switch to **Load-based**. Sign in as **Vikram** and
   report a second payment problem: it goes to the least busy technician instead. Switch back to
   Skill-based afterwards. (Rehearse this: who wins load-based depends on the current workload.)
7. Open the **notification bell** as Priya: in-app notification; mention the simulated email channel.

## Act 2: Investigate → resolve → verify → close (Workflow 2, about 5 minutes)

Left window: **Priya**. Right window: **Neha** on her Home screen.

1. Priya opens the incident and clicks **Acknowledge**. Within seconds Neha's progress bar moves to
   "Working on it". *"Two people, two windows, one incident."*
2. **Try to break it (state rules).** The UI only offers valid buttons, so call the API directly
   (terminal on screen):

   ```bash
   API=http://localhost:8080/api
   TECH=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"priya@itsm.local","password":"Tech@123"}' | jq -r .token)
   ID=<incident id from the URL>
   # Resolve before starting work → 409 "You cannot resolve an incident that is Acknowledged."
   curl -s -X POST $API/incidents/$ID/resolve -H "Authorization: Bearer $TECH" \
     -H 'Content-Type: application/json' -d '{"resolutionNotes":"too early"}' | jq .detail
   ```

   Switch to `AcknowledgedState.java`: it only implements `start()` (and reassign / cancel); every
   other action falls through to `AbstractIncidentState`, which refuses it. *"There is no if-statement
   about status anywhere in the service. The state object decides."*
3. Priya clicks **Start investigation**, adds an **internal note** (employees cannot see it) and a
   public comment, then **Resolve incident** with notes.
4. Neha's Home shows **"Waiting on you"**: *"Priya says this is fixed… Is it working for you now?"*
   Neha clicks **No, still broken**. The tracker goes back to *Working on it* and Priya's window
   updates. Show `ResolvedState.reopen()`.
5. **Try to break it (permissions).** As the admin, try to confirm Neha's fix through the API:

   ```bash
   ADMIN=$(curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"admin@itsm.local","password":"Admin@123"}' | jq -r .token)
   curl -s -X POST $API/incidents/$ID/verify -H "Authorization: Bearer $ADMIN" | jq .detail
   # → 403 "Only the reporter can perform this action."
   ```

   *"Even the IT manager cannot confirm someone else's fix. Role checks on the endpoint answer 'what
   kind of user are you?'; the service answers 'is this incident yours?'."*
6. Priya resolves again. Neha clicks **Yes, it's fixed** → *Confirmed*. The admin opens the incident
   and clicks **Close incident**. Neha leaves a 5-star rating with **Rate the fix**.
7. Open the **Audit trail** tab: every step with actor, time, previous and new state. This is the
   screen from the cold open.

## Act 3: The storm — duplicates, SLA risk, escalation (Workflow 3, about 3 minutes)

1. In quick succession, report two more payment problems as Vikram and as Rohan (a technician can
   report too, and is never assigned his own incident). The **Possible duplicate** panel links them
   with a similarity score.
2. Admin **Dashboard**: open/critical/breach cards, created-vs-resolved trend, priority mix, the
   **SLA watch** list and technician workload against capacity.
3. Open the seeded breached payment incident: SLA clock, breach note, escalation banner.
4. **Reports** → **Run SLA scan**: warnings, breaches and escalations are applied idempotently (run it
   twice; nothing is duplicated). Show service, category and technician analysis.

## Under the hood (about 3 minutes)

One architecture slide (`docs/ARCHITECTURE.md`, section 3), then each pattern tied to a moment already
shown:

| Pattern | Where the viewer saw it | File to show |
| --- | --- | --- |
| State | Resolve-too-early refused; "still broken" went back a step | `incident/lifecycle/ResolvedState.java`, `AbstractIncidentState.java` |
| Strategy | Switching skill-based → load-based changed who got the incident | `incident/assignment/AssignmentStrategy.java` |
| Observer | One status change produced in-app + email notifications and audit | `notification/NotificationEventListener.java` |
| Strategy + Adapter (notifications) | The same message arrived in the bell and as a (simulated) email | `NotificationSender.java`, `NotificationDispatcher.java`, `EmailNotificationSender.java` → `EmailGateway.java` |
| Factory | Users, notifications, strategies and states are created in one place | `UserFactory`, `IncidentStateFactory` |
| Inheritance | Employee / Technician / Administrator are real "is-a" users | `user/User.java` |
| Repository | No SQL in services | `incident/IncidentRepository.java` |

Mention polymorphism in one sentence: the same `assign()` call assigns from *Classified* but only
reassigns from *Assigned* or *In progress* (`ClassifiedState` vs `AssignedState`).

## Proof: tests and security (about 1.5 minutes)

```bash
cd backend && ./mvnw test        # 44 tests: 40 unit + 4 full-stack integration (real PostgreSQL)
cd frontend && npm run lint && npm run build
```

Name two tests on screen: `IncidentLifecycleTest.eachStateDecidesWhatAnActionDoes` and
`ItsmWorkflowIntegrationTest.technicianCanReportOwnIncidentButIsNeverAssignedToIt`. Mention the 60+
documented cases in `docs/TEST_CASES.md`.

## Close (about 1.5 minutes)

- Show the brief's MVP list with each item ticked (users, incident management, priority, two+
  assignment strategies, SLA, two notification channels, audit, five+ filters, intelligent features,
  persistence).
- Honest limitations: simulated email, in-process SLA monitor, no refresh tokens or rate limiting.
- Next steps: real email/SMS adapters, auto-close of unconfirmed fixes, real-time push.

## Recording tips

- Record each act separately and join them; rehearse the window switches.
- 1080p, browser zoom 90–100%, cursor highlighting on, unrelated tabs and notifications hidden.
- Reset the database before the final take (`docker compose down -v`), since test runs leave incidents
  behind.
- Keep the demo credentials on screen only as shown above; never show real secrets.
- Name the file as the course instructs, e.g. `1_OOP_S1_2026-2027_Project.mp4`, and upload before
  **22 Nov 2026**.
