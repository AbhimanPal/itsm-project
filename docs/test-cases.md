# I-ITSM test cases

All cases below are automated JUnit 5 tests. Run them all in IntelliJ by right-clicking
`src/test/java` → **Run 'All Tests'**, or in a terminal with `./mvnw test`.
The project has **78 automated tests**; the 31 below are the documented cases for the report.
Every one passes (last run: 3 Nov 2026).

Unit tests check one class on its own. Integration tests (`@SpringBootTest`) start the
whole app with an in-memory database filled by `DataSeeder`. Authorization tests use
MockMvc, which sends fake browser requests as a pretend logged-in user (`@WithMockUser`).

| ID | Type | What is tested | Input / steps | Expected result | Test method |
|---|---|---|---|---|---|
| TC01 | Normal | Lowest priority score | impact 1, urgency 1, criticality 1, 1 user | P4 | `ScoreBasedPriorityStrategyTest.lowestScoreIsP4` |
| TC02 | Boundary | P4/P3 border | score 3 (3×1×1), then score 4 (2×2×1) | P4, then P3 | `ScoreBasedPriorityStrategyTest.score3IsP4AndScore4IsP3` |
| TC03 | Boundary | P2/P1 border | score 12 (3×2×2), then score 18 (3×3×2) | P2, then P1 | `ScoreBasedPriorityStrategyTest.score12IsP2AndScore18IsP1` |
| TC04 | Boundary | Users-affected bump | score 1 with 100 users, then with 99 users | P3 (raised one level), then P4 | `ScoreBasedPriorityStrategyTest.manyUsersRaisesPriorityOneLevel` |
| TC05 | Boundary | Bump never goes above P1 | score 27 with 500 users | P1 | `ScoreBasedPriorityStrategyTest.manyUsersNeverGoesAboveP1` |
| TC06 | Invalid input | Impact outside 1–3 | impact 0, then impact 4 | `InvalidInputException` | `ScoreBasedPriorityStrategyTest.impactOutOfRangeThrows` |
| TC07 | Invalid input | Zero users affected | users affected 0 | `InvalidInputException` | `ScoreBasedPriorityStrategyTest.zeroUsersAffectedThrows` |
| TC08 | Normal | Full lifecycle | NEW → CLASSIFIED → … → VERIFIED → CLOSED | Every step allowed, ends CLOSED | `IncidentLifecycleTest.fullHappyPathReachesClosed` |
| TC09 | Exception | Illegal status jump | NEW → CLOSED | `InvalidStateTransitionException` | `IncidentLifecycleTest.newToClosedIsBlocked` |
| TC10 | Business rule | Reopen after rejection | RESOLVED → IN_PROGRESS | Allowed | `IncidentLifecycleTest.resolvedCanBeReopened` |
| TC11 | Exception | Resolve before starting work | ASSIGNED incident, technician clicks resolve | `InvalidStateTransitionException` | `IncidentWorkflowIntegrationTest.cannotResolveBeforeStartingWork` |
| TC12 | Normal | Report → assign → audit → notify | Asha reports a Wi-Fi incident | Assigned to Ravi (only Network skill); audit CREATED + ASSIGNED; 2 notifications for Asha, 1 for Ravi | `IncidentWorkflowIntegrationTest.reportingAssignsTechnicianAndRecordsAuditAndNotifications` |
| TC13 | Normal | Whole workflow with feedback | Acknowledge, start, ask for info, employee replies, resolve, employee confirms with rating 5 | CLOSED, rating stored, 2 notes, SLA met | `IncidentWorkflowIntegrationTest.fullWorkflowEndsClosedWithFeedback` |
| TC14 | Business rule | Skill-based assignment | Network incident; Ravi (Network, 3 active), Meera (Network, 1 active), Karan (Hardware only, 0 active) | Meera: the least busy with the skill (Karan is idler but lacks it) | `AssignmentStrategyTest.skillBasedPicksLeastBusyTechnicianWithTheSkill` |
| TC15 | Business rule | Workload limit respected | Technician already at max active incidents | Skipped by the strategy | `AssignmentStrategyTest.technicianAtMaxWorkloadIsSkipped` |
| TC16 | Exception | Admin goes over workload limit | Admin manually assigns to a full technician | `WorkloadExceededException` | `AdminFeaturesIntegrationTest.adminCannotGoOverTechnicianWorkloadLimit` |
| TC17 | Business rule | SLA status after response deadline | P1 not acknowledged after 5 min | Status BREACHED | `SlaStatusTest.notAcknowledgedAfterResponseDeadlineIsBreached` |
| TC18 | Normal | 80% SLA warning sent once | P1 checked at 25 of 30 min, then again 30 s later | One `SLA_WARNING`, not two | `SlaMonitorIntegrationTest.warnsOnceWhen80PercentOfTimeIsUsed` |
| TC19 | Business rule | Escalation chain | P1 not acknowledged: check at 6, 10 and 12 min | Team lead at 6 min, still team lead at 10, admin at 12 | `SlaMonitorIntegrationTest.notAcknowledgedGoesToTeamLeadThenAdmin` |
| TC20 | Boundary | Escalation with no technician | All technicians unavailable, P1 reported, check at 6 min | Escalated straight to admin | `SlaMonitorIntegrationTest.unassignedIncidentGoesToAdmin` |
| TC21 | Business rule | Duplicate from another employee | Vikram reports the same Wi-Fi problem as Asha | Linked to the original, same technician | `DuplicateDetectionIntegrationTest.similarReportFromSomeoneElseIsLinkedAndGoesToSameTechnician` |
| TC22 | Exception | Same employee reports twice | Asha reports her own open problem again | `DuplicateIncidentException` | `DuplicateDetectionIntegrationTest.samePersonReportingAgainIsBlocked` |
| TC23 | Invalid input | Bad SLA rule | Response time longer than resolution time | `InvalidInputException` | `AdminFeaturesIntegrationTest.responseLongerThanResolutionIsRejected` |
| TC24 | Authorization | Not logged in | Open `/admin/incidents` | Redirect to `/login` | `RoleAccessTest.notLoggedInIsSentToLoginPage` |
| TC25 | Authorization | Employee opens admin page | Employee opens `/admin/users` and `/admin/api/stats` | 403 Forbidden | `RoleAccessTest.employeeCannotOpenAdminPages`, `employeeCannotReadChartData` |
| TC26 | Authorization | Technician acts on someone else's incident | Wi-Fi incident is assigned to Ravi; Meera tries to acknowledge it | `UnauthorizedOperationException` | `IncidentWorkflowIntegrationTest.otherTechnicianCannotActOnIncident` |
| TC27 | Normal | Factory builds a complete incident | `IncidentFactory.create` with impact 3, urgency 3, criticality 2 | Title trimmed, P1, CLASSIFIED, deadlines from the SLA rule | `IncidentFactoryTest.createsAClassifiedIncidentWithPriorityAndDeadlines` |
| TC28 | Invalid input | Blank incident title or description | Title "   ", then description null | `InvalidIncidentException` | `IncidentFactoryTest.blankTitleOrDescriptionIsRejected` |
| TC29 | Business rule | Admin raises priority | Wi-Fi incident P3 → P1 with a reason | Deadlines become created + 5 / 30 min, `PRIORITY_CHANGED` in audit, reporter notified | `PriorityChangeIntegrationTest.raisingPriorityShortensDeadlinesAndTellsEveryone` |
| TC30 | Exception | Mail server unreachable | "Send test email" with a mail sender that fails | `NetworkFailureException` | `EmailNotificationChannelTest.gmailUnreachableGivesNetworkFailure` |
| TC31 | Authorization | Logged in as a deleted user | Session for `ghost@itsm.local`, open `/employee/incidents` | Friendly error page (`InvalidLoginException`) with Log out | `RoleAccessTest.unknownLoggedInUserGetsFriendlyErrorPage` |

## Coverage by type

| Type | Cases |
|---|---|
| Normal | TC01, TC08, TC12, TC13, TC18, TC27 |
| Boundary | TC02, TC03, TC04, TC05, TC20 |
| Invalid input | TC06, TC07, TC23, TC28 |
| Exception | TC09, TC11, TC16, TC22, TC30 |
| Authorization | TC24, TC25, TC26, TC31 |
| Business rule | TC10, TC14, TC15, TC17, TC19, TC21, TC29 |
