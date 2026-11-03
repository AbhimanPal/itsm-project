# CLAUDE.md — I-ITSM (CS F213 OOP Project)

Read this file fully at the start of every session. It holds every decision made so far.
The full problem statement is in `docs/OOP_S1_2026-2027_Project.pdf` — read it when a requirement is unclear.

## Who we are and how to help us

- Two-person team, BITS Pilani Hyderabad, CS F213 (Object-Oriented Programming), Semester I 2026-27.
- The project is worth **half the course grade**. The recommended team size is 3–5, so we have to be efficient.
- **We are Java beginners.** So far we have covered basics, loops, and classes/objects. Inheritance, interfaces, abstract classes, enums, collections and exceptions are new to us.
- How to help:
  - Explain every change in simple, beginner-friendly words: what you changed, why, and which OOP concept it shows.
  - Make small, step-by-step changes. Don't do big rewrites or build many features at once.
  - Don't add new frameworks or libraries without asking first.
  - When we say "let me try first", give hints or review our code instead of writing it.
  - We must be able to explain every line in our demo video, so never add code we haven't had explained.
  - Tell us how to run and check each change. We run the app from IntelliJ (green ▶ on `ItsmApplication`, red ■ to stop) and open http://localhost:8080 in the browser.
  - We use Spring Boot **4.1**. Most tutorials online are for 3.x, so check the version when copied code doesn't work.
- **Way of working since 29 Oct:** Person A builds the whole app with Claude Code, and Claude writes the code to finish fast. Build one working slice at a time, test it in the browser, and explain each new file afterwards. Person A must still understand each file well enough to explain it on video.

## What we're building

**I-ITSM: Intelligent IT Service Management & Incident Resolution System**, a web app.
An employee reports an IT problem. The system:

1. Classifies it.
2. Calculates its priority (P1–P4).
3. Applies an SLA (deadline).
4. Assigns a technician.
5. Notifies people.
6. Tracks the lifecycle.
7. Logs everything.
8. Shows reports.

**Deadline: 22 Nov 2026 (hard). We aim to submit on 21 Nov.** We submit:

- **One PDF report.**
- **The source code on GitHub**, with the link added to the class spreadsheet.
- **A 15–20 min video demo.** Both members are on camera, each introducing themselves and explaining modules.
- File naming per the PDF: `<GroupNo>_OOP_S1_2025-2026_Project`. This looks like a typo for 2026-2027, and we still need to ask the IC.

### Marks (120 total)

| Criterion | Marks |
|---|---|
| Functionality (MVP) | 60 |
| Technical depth | 15 |
| System design | 10 |
| User experience | 10 |
| Innovation | 10 |
| Docs + presentation | 10 |
| Testing | 5 |

Our priority order: **MVP working first → clean OOP design → one innovation feature.**

Technical depth explicitly rewards "use of APIs" and "integration with third-party services". Our answer is a real email channel (see Notifications).

### Mandatory features (MVP)

**Three roles, each with different screens:**

- **Employee:** register/login, report an incident, view and track their incidents, add info, reply to technician questions, see the resolution, give feedback.
- **Technician:** see assigned incidents, acknowledge, investigate, add notes, update status, ask for info, resolve, see own workload.
- **Admin:** manage users, technicians, IT services, SLA policies and categories; choose the assignment strategy; monitor incidents and SLA violations; view reports and audit logs.

Each role's UI must look different, with clear navigation links between pages (PDF p. 17).

**Incident fields:** ID, reporter, category, description, affected service, impact, urgency, users affected, priority, created time, status, assigned technician, SLA, investigation notes, resolution, closure info.

**Categories:** hardware, software, network, database, cloud, security, authentication, application, storage. Store them as data, not a hardcoded enum, so new ones need no code change.

**Automatic priority:** P1–P4, from impact, urgency, business criticality, users affected and the affected service. The algorithm and its rationale must be documented. See the formula under "Decisions made".

**Technician assignment:** at least 2 strategies (e.g. skill-based and load-based). Admin can switch between them without code changes. Technicians need skills (a set of categories), an availability flag and a max number of active incidents.

**SLA:** different services may have different SLAs (PDF p. 7). An `SlaPolicy` is a row of data: service (or service tier) + priority → response minutes + resolution minutes. Admin can edit them. Defaults:

| Priority | Response | Resolution |
|---|---|---|
| P1 | 5 min | 30 min |
| P2 | 15 min | 2 h |
| P3 | 1 h | 8 h |
| P4 | 4 h | 24 h |

- **Response time** = time from creation until ACKNOWLEDGED. **Resolution time** = time from creation until RESOLVED.
- Warn when 80% of the time has passed. Detect and record violations.
- A background scheduler checks this every minute.
- Assumption (state it in the report): the SLA clock does **not** pause while waiting for the employee's info.

**Lifecycle:**

- Main path: NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED.
- IN_PROGRESS ↔ AWAITING_INFO, for when the technician asks for info and the employee replies.
- RESOLVED → IN_PROGRESS (reopen), for when the employee rejects the resolution.
- Illegal jumps (e.g. NEW → CLOSED) must be blocked with `InvalidStateTransitionException`.

**Notifications:** at least 2 channels: in-app, plus **real email via Gmail SMTP**, wrapped in an Adapter. The email password goes in an environment variable, never in GitHub. They fire on created, assigned, priority changed, SLA approaching, SLA violated, resolved and closed. Adding a new channel later must be easy.

**Audit trail:** timestamp, user, action, incident ID, old state, new state, description.

**Search and reports:**

- At least 5 filters (ID, date, status, priority, category, service, technician, SLA status).
- Dashboards: incident overview, technician workload, incidents per service.

**Intelligent features:** at least 2. Our plan:

- **Duplicate detection:** keyword/Jaccard similarity on the description plus the same service.
- **Auto-escalation:** technician → team lead → admin if not acknowledged in time. Team lead = a `Technician` with `isTeamLead = true`, not a separate role.
- Optional third: auto-categorization by keywords.

**Persistence:** users, services, incidents, technicians, SLA policies, audit records and resolutions. H2 must run in **file mode** so data survives a restart. The default in-memory mode loses everything.

### OOP requirements (graded heavily)

- **Concepts to use visibly:**
  - Encapsulation
  - Abstraction
  - Inheritance, only for real "is-a" relationships
  - Polymorphism
  - Interfaces
  - Composition
  - Association/aggregation
  - Exception handling
  - Sensible collections
- **Design patterns:** at least 2. Planned:
  - **Strategy:** priority calculation, technician assignment, notification channel.
  - **Lifecycle:** an `IncidentStatus` enum where each state knows its allowed next states. It's simpler to save to the DB than the full State pattern. Upgrade to the full State pattern only if time allows.
  - **Observer:** written by hand (an `IncidentListener` interface, with notification and audit as listeners), not Spring events, so we can explain it.
  - **Factory:** creating incidents, notifications or users.
  - **Repository:** data access, via Spring Data.
  - **Adapter:** wraps the real email service behind our notification-channel interface.
- **Custom exceptions:** all extend one base `ItsmException`.
  - Invalid login
  - Unauthorized operation
  - Invalid incident
  - Invalid state transition
  - Technician unavailable
  - Workload exceeded
  - Service not found
  - Missing SLA config
  - Duplicate incident
  - Database failure
  - Network failure
  - Invalid input
- **Error pages:** one `@ControllerAdvice` class turns our exceptions into friendly error pages.
- **Security:** login, role-based access, hashed passwords (BCrypt), input validation, protected endpoints, plus a security write-up.

### Report contents

- Problem analysis: problem, stakeholders, existing challenges, proposed solution, assumptions
- Requirements: functional, **non-functional**, user roles, business rules
- Architecture diagram
- Use-case and class diagrams
- Tech stack
- Technology selection: answers to the 9 questions in the PDF
- DB design (ER diagram, schema, relationships, sample data)
- Priority algorithm and its rationale
- **15+ test cases**, covering normal, boundary, invalid input, exceptions, authorization and business rules

Write the report as we go, not only in the final week.

### Video workflows (must show all 3)

1. Employee reports → system prioritizes → technician assigned → notification sent.
2. Technician investigates → SLA monitored → incident resolved → user notified.
3. Several incidents reported → duplicates/related found → correlated or escalated.

**Innovation:** a real-time dashboard with charts (Chart.js from a CDN, refreshing every ~10 s). Docker + cloud deployment is rejected: free hosts wipe the H2 file on redeploy.

## Decisions made

| Topic | Decision |
|---|---|
| Language | Java (course language, strong OOP support) |
| Backend | Spring Boot 4.1 (Maven, Jar, Java 21) |
| Frontend | Thymeleaf (server-rendered HTML). No React, to keep it simple |
| Database | H2 in file mode (`jdbc:h2:file:./data/itsm`). No MySQL switch |
| IDE | IntelliJ IDEA, with the free JetBrains student licence |
| Repo | GitHub repo `itsm-project`, **private** until submission, then make it public or add the IC as collaborator |
| Base package | `com.itsm.itsm`, main class `ItsmApplication`. Sub-packages: `model`, `exception`, `priority`, `repository`, `service`, `security`, `web`, `config` |
| Spring dependencies | Spring Web, Thymeleaf, Spring Data JPA, H2, Security, Validation. **Agreed to add later:** Mail (for the email channel) |
| Java version | Build with Java 21 (the Mac also has Java 26 as its default). In IntelliJ: File → Project Structure → SDK = 21 |
| Test data | `config/DataSeeder.java` fills an empty database with categories, services and test accounts (listed in its comment). Delete the `data/` folder to start fresh. Tests use a separate in-memory DB (`src/test/resources/application.properties`) |
| Priority formula (v2) | score = impact × urgency × service criticality, each 1–3 (range 1–27). Criticality comes from the `ITService` the admin set up, not from the employee. ≥18 → P1, ≥9 → P2, ≥4 → P3, else P4. Then, if users affected ≥ 100, raise one level (never above P1). Implemented as a `PriorityStrategy` interface |
| Entities | Domain classes are JPA entities directly. `User` uses single-table inheritance (one `users` table with a `user_type` column) |
| Build style | End-to-end slices, not layers. First slice: employee logs in → reports an incident → it's saved → it appears in their list |

## Work split

**Since 29 Oct, Person A is building everything** (see "Way of working"). The split below is kept as the list of modules. Person B must still appear in the video and explain some modules, and the report must show what each member did. Agree with Person B which modules they will present.

**Person A (Udiptya), "the brain": domain model and business logic.**

- Domain classes:
  - `User` (abstract), with `Employee`, `Technician` and `Admin` extending it. `Technician` has skills, availability, max workload and `isTeamLead`
  - `Incident`, `ITService`, `Category`, `SlaPolicy`, `IncidentNote`, `Feedback`
  - Enums `Priority` and `IncidentStatus`
- Priority strategy
- Assignment strategies
- SLA engine and scheduler
- Lifecycle (status enum + transition rules)
- Intelligent features (duplicate detection, escalation)
- Unit tests and the class diagram

**Person B, "the face and memory": web, security and data.**

- DB schema and ER diagram
- Login, roles and security
- Controllers, forms and the `@ControllerAdvice` error pages
- Thymeleaf pages for all 3 roles
- Notifications (Observer, in-app + email Adapter) and the audit trail. Moved from A to balance the work; they're mostly storage + display
- Search/filter
- Dashboards and reports
- The innovation feature (charts dashboard)
- Integration tests

**Shared:** problem analysis, requirements, use-case diagram, technology selection answers, the video.

The PDF says every student must understand the whole system. Once a week, each of us explains our code to the other. It also doubles as practice for the video.

## Timeline

Re-planned on 29 Oct, because the Oct 22–28 goals slipped.

| Dates | Goal |
|---|---|
| Oct 15–21 | Setup, paper design, hello-world app (done) |
| Oct 29–Nov 2 | Catch up: model classes, entities, repositories, login with 3 roles, create + list incidents |
| Nov 3–9 | Lifecycle, priority, SLA policies, 2 assignment strategies, notifications, audit, technician + employee screens |
| Nov 10–14 | Scheduler (SLA warnings/violations, escalation), duplicate detection, 5+ filters, admin screens, dashboards |
| Nov 15–17 | Charts innovation, finish 15+ tests, bug fixes. **Feature freeze 17 Nov** |
| Nov 18–21 | Report, diagrams, video. **Submit 21 Nov** (22 Nov is the buffer) |

**If we fall behind, cut in this order:** feedback → team-lead escalation level → third intelligent feature.

## Current status (update this as we go)

- [x] Spring Boot project generated, runs on localhost:8080
- [x] `HomeController` + `templates/home.html` ("I-ITSM is running!") pushed to GitHub
- [x] Plan reviewed against the full PDF (29 Oct). Gaps and decisions are recorded above
- [x] Housekeeping: package renamed to `com.itsm.itsm` / `ItsmApplication`, `.DS_Store` and `data/` ignored, H2 in file mode
- [x] **Slice 1 (29 Oct):** domain model (`User` hierarchy, `Incident`, `Category`, `ITService`, `Priority`, `IncidentStatus` lifecycle), base + 3 custom exceptions, `PriorityStrategy` (v2 formula), login with 3 roles + BCrypt, employee register, report-incident form with validation, "My incidents" list, friendly error and 403 pages, seed data. 15 tests pass (8 priority, 6 lifecycle, 1 app start)
- [ ] Person B: install tools, clone the repo (IntelliJ → Get from VCS), run it
- [ ] Person B: agree which modules they will present in the video
- [x] **Slice 2 (29 Oct):** `SlaPolicy` (per service + priority, with defaults) and deadlines on each incident, `SlaStatus` (on track / at risk at 80% / breached / met). `AssignmentStrategy` interface + abstract `BaseAssignmentStrategy` + skill-based and load-based, auto-assign on report, admin switches strategy (stored in `AppSetting`). Technician pages: list with workload, incident page with acknowledge, start, ask for info, resume, notes (`IncidentNote`, composition), resolve. Technicians can only open their own incidents (`UnauthorizedOperationException`). 26 tests pass
- [x] **Slice 3 (30 Oct):** Observer pattern: `IncidentEvent`, `IncidentListener`, `IncidentEventPublisher`; `AuditTrailListener` saves `AuditRecord`s; `NotificationListener` sends through every `NotificationChannel` (in-app `Notification` + simulated email in the console log). Employee incident page (reply to questions, confirm fix with 1–5 feedback → VERIFIED → CLOSED, or reopen), Notifications page with unread badge for all roles, admin Audit log, history table on incident pages. Feedback is stored on `Incident` (no separate class). 33 tests pass, including 6 integration tests
- [x] **Slice 4 (3 Nov):** admin screens: services, categories and SLA policies (`AdminCatalogService`, add/edit, default SLA rules can't be deleted), users list + add/edit technicians (`AdminUserService`, `TechnicianForm`). All-incidents search with 9 filters (`IncidentSearch.matches()`), admin incident page with manual assign / retry auto-assign for unassigned incidents (`WorkloadExceededException`). Dashboard (`report/ReportService`, records `TechnicianWorkload` + `ServiceReport`): overview counts, by status/priority/SLA, technician workload, incidents per service, with CSS bars. 53 tests pass (new: `IncidentSearchTest`, `AdminFeaturesIntegrationTest`, `RoleAccessTest` with MockMvc)
- [ ] **Slice 5:** SLA scheduler (warnings, violations), auto-escalation, duplicate detection, email channel (Adapter)
- [ ] **Slice 6:** charts dashboard (innovation), more tests up to 15+ documented cases, bug fixes

## Git routine

- Pull before starting work. Commit and push when done.
- Use small commits with clear messages.
- A and B work in different files where possible, to avoid conflicts.
