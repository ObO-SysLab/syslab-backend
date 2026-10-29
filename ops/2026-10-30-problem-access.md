# AUDIT-05 problem access policy

Local implementation; not deployed. The user approved this policy on 2026-10-30.
No schema migration, frontend request change or infrastructure change is included.

| Visibility | Read | Submit / practice execution |
| --- | --- | --- |
| public | Authenticated user | Authenticated user |
| private | Author only | Author only |
| group | Linked group member; author may inspect | Linked group member |
| contest | Non-banned participant after start; author may inspect before start | Non-banned participant during contest |
| Unknown / missing | Denied | Denied |

Denied problem access and missing problems both return 404. Invalid submission type,
code format or language returns 400. Existing authentication failures are unchanged.
Contest-only problems must use the contest submission route to preserve contest scoring;
ordinary submissions are rejected even for participants. An author inspection exception
does not authorize execution of a group/contest problem outside membership/time rules.
Attaching a private problem to a group or contest does not override its private visibility.

For general read/VM endpoints that identify only a problem, one authorized linked group
or contest is sufficient. Contest lists and submissions use the requested contest context;
membership in a different contest does not authorize that context. The current contract
does not bind an existing VM session to a specific contest. Revisit that contract if the
same problem needs different execution rights in several simultaneous contests.

## Implemented boundaries

- Shared ProblemAccessService checks detail, normal submission, board, VM status/create/
  reset, OBO, rankings, comments and contest problem listing/submission.
- Board/OBO/ranking controllers pass the authenticated principal, never a caller-supplied
  user ID. Foreign comment/parent IDs cannot bypass the selected problem's boundary.
- Contest submission loads by contestProblem ID **and** contest ID, checks actual problem
  type, nonempty code, platform-supported and problem-allowed language before persistence,
  counters, cooldown or queue calls. Normal submission validates before its first save too.
- Terminal issue, one-use ticket consumption and active-lease checks recheck practice
  rights from DB. Group departure, contest ban/end and visibility changes deny subsequent
  checks. The existing PTY server closes a denied lease on its next check (normally about
  5 seconds plus up to 2 seconds validation transport); no new Node deployment is required
  beyond the already required AUDIT-02 release.
- Stopping one's VM remains allowed after access is lost. Container expiry, exact stop
  target, cleanup failure and duplicate creation remain AUDIT-03/04, not solved here.

No revocation can cancel every in-flight operation atomically. Already accepted grading
jobs are not cancelled. A rejected terminal lease closes the connection; it does not by
itself delete the VM or kill every process previously launched inside that VM.

## Validation and remaining checks

Selected Java unit tests and MockMvc security/controller tests cover the visibility matrix,
contest time boundaries, cross-contest IDs, forged types/languages, denial before side
effects, principal propagation, revocation and previous grading/terminal behavior.
Final result: **70 tests passed** (37 added cases plus 33 prior cases), zero failures/errors.
Run only the selected classes through Gradle: ProblemAccess*Test, Contest*AccessTest,
TerminalAccessServiceTest, TerminalGrantStoreTest, TerminalControllerTest,
SubmissionGradeStarterServiceTest, SubmissionGradeAsyncServiceTest, CodingGradeRecoveryTest.
Repositories, DB, Redis, Docker and queues are mocked; no application listener is started.
Actual JPA query execution, production data associations, browser flows and DB/Redis/PTY
integration still require separately approved validation. This is not a global authorization
audit of every admin/feed/group endpoint or a change to comment-specific privacy rules.

## Deployment and recovery plan — not executed

1. Inventory existing private/group/contest problem links, memberships, bans and contest
   dates without exposing answers. Confirm API host time zone matches stored timestamps.
   Check that existing private problems intended for participants have the intended explicit
   visibility; do not silently rewrite visibility or membership data during rollout.
2. After approval, run isolated integration checks against a test DB and the coordinated
   AUDIT-02 terminal setup. Exercise author/member/nonmember, before/start/end times,
   normal/contest submissions, group departure and active terminal revocation.
3. Record deployed backend artifact/version and compatible prior references, then request
   approval for the exact deployment. Preserve disabled grading recovery and prior
   coordinated-release dependencies. No DB migration is needed for AUDIT-05 itself.
4. Verify allowed flows still work and denied requests cause no save, counter, queue or
   Docker action. Verify unreadable problems do not appear in the contest problem list.
5. On unexpected denial/errors, close affected problem/VM/terminal routes before restoring
   a compatible artifact. Keep routes closed while investigating; do not reopen known
   authorization bypasses. Preserve submissions/sessions and clean up only explicitly
   identified resources after approval. No automatic data deletion or rollback is included.

AUDIT-01 remains the highest-priority unresolved deployment blocker. Terraform is deferred.
