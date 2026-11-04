# I-ITSM security write-up

This page lists how I-ITSM protects its users and data, where each protection lives in
the code, and the limits we know about. Most of it is provided by **Spring Security**;
our code configures it in `security/SecurityConfig.java`.

## 1. Login (authentication)

- Every page except `/login`, `/register`, `/error` and the CSS needs a login.
  Anyone who isn't logged in is redirected to `/login` (test: `RoleAccessTest.notLoggedInIsSentToLoginPage`).
- Users log in with email + password. `security/ItsmUserDetailsService` loads the user from the
  database, and Spring Security compares the password.
- A wrong email or password shows "Wrong email or password." The message is the same in
  both cases, so an attacker can't find out which emails have accounts.
- If a browser is still logged in as a user who no longer exists (e.g. the database was reset),
  every page shows a friendly `InvalidLoginException` page with a **Log out** button
  (test: `RoleAccessTest.unknownLoggedInUserGetsFriendlyErrorPage`).

- The admin can **deactivate** an employee (Users → Edit). Spring Security then refuses that
  login (`ItsmUserDetailsService` marks the account disabled) and the login page says the account
  is deactivated. Accounts are never deleted, so the audit trail stays complete
  (test: `EmployeeManagementTest.deactivatedEmployeeCannotLogInUntilReactivated`).

## 2. Hashed passwords

- Passwords are **never stored as plain text**. `BCryptPasswordEncoder` (in `SecurityConfig`)
  turns each password into a BCrypt hash before saving: at registration (`web/RegisterForm` →
  `RegisterController`), for new technicians (`AdminUserService.createTechnician`) and for seed data.
- BCrypt adds a random **salt** to each password, so two users with the same password get
  different hashes, and it is deliberately slow, so guessing passwords takes a very long time.
- Test: `AdminFeaturesIntegrationTest.newTechnicianCanLogInWithHashedPassword` checks that the
  stored value is not the plain password.

## 3. Role-based access (authorization)

Two layers, so a mistake in one is caught by the other:

1. **URL rules** in `SecurityConfig`: `/employee/**` needs role EMPLOYEE, `/technician/**` needs
   TECHNICIAN, `/admin/**` (including the JSON at `/admin/api/stats`) and `/h2-console/**` need
   ADMIN. Anyone else gets the 403 "Access denied" page.
2. **Ownership checks** in `IncidentService`: an employee can only open incidents they reported
   (`getForReporter`), and a technician can only act on incidents assigned to them
   (`getForTechnician`). Otherwise `UnauthorizedOperationException` is thrown. This stops someone
   from just typing another incident's number into the URL.

Tests: `RoleAccessTest` (employee and technician can't open admin pages, employee can't read the
chart JSON or the database console) and `IncidentWorkflowIntegrationTest.otherTechnicianCannotActOnIncident`
/ `otherEmployeeCannotSeeIncident`.

## 4. Input validation

- **In the browser form:** `required`, `maxlength`, number ranges (quick feedback, but easy to bypass).
- **On the server, always:** form objects (`IncidentForm`, `RegisterForm`, `TechnicianForm`) use
  Bean Validation annotations such as `@NotBlank`, `@Size`, `@Email`, `@Min`/`@Max`, checked by
  `@Valid` in the controllers. Errors are shown next to the field.
- **In the business logic, again:** `IncidentFactory` rejects blank titles (`InvalidIncidentException`),
  `ScoreBasedPriorityStrategy` rejects impact/urgency outside 1–3 (`InvalidInputException`),
  `AdminCatalogService` rejects bad SLA rules and duplicate names. So even code that doesn't come
  from a form can't create bad data.

## 5. Protection against common web attacks

| Attack | Protection |
|---|---|
| **CSRF** (another site makes your browser submit our forms) | Spring Security requires a secret CSRF token on every POST. Thymeleaf's `th:action` adds it to each form automatically. |
| **XSS** (a user types `<script>` into a description) | Thymeleaf's `th:text` escapes HTML, so the text is shown, never run. We never use `th:utext`. |
| **SQL injection** | We never build SQL by hand. Spring Data JPA uses parameterised queries. |
| **Clickjacking** | Pages may only be shown in frames on our own site (`frameOptions().sameOrigin()`). |
| **Leaking data through JSON** | `/admin/api/stats` returns the `DashboardStats` record, which only has labels and numbers, never entities, so no password hashes or emails can end up in it. |

## 6. Secrets

- The Gmail app password is read from the environment variable `MAIL_PASSWORD`
  (`application.properties`: `spring.mail.password=${MAIL_PASSWORD:}`). It is set in IntelliJ's
  run configuration, which lives in `.idea/`, and `.idea` is in `.gitignore`. **No password is in GitHub.**
- We use a Gmail **app password**, not the account's real password, so it can be revoked on its own.
- The database file `data/` is also in `.gitignore`.

## 7. Known limits (honest list for the report)

- **Development database console:** `/h2-console` is enabled for the demo, now restricted to admins.
  The H2 database user `sa` has an empty password. In production we would switch the console off
  and use a real database password.
- **HTTP, not HTTPS:** the app runs locally on `http://localhost:8080`. A real deployment would need
  HTTPS so passwords aren't sent in plain text over the network.
- **No login rate limiting / account lock-out:** someone could try many passwords. BCrypt being
  slow helps, but a real system would lock an account after several failed attempts.
- **Demo accounts** share one simple password (in `DataSeeder`), which is fine for testing only.
- **Email** is sent from a shared project Gmail account; anyone with the app password could send mail as it.
