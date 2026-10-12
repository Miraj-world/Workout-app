# Workout Log testing and bug-fix report

Date: October 11, 2026

## Version checked

Fetched origin and fast-forwarded local main from 47b2c0e to 602672a (merge of PR #2, including 98d658c). The incoming changes affect MainActivity tab selection and destruction. The pre-existing untracked Workout App Debugging and Testing.docx was left untouched.

## Tests and results

Environment: Java 8, Gradle 4.4.1, Android Gradle Plugin 3.1.0, connected Android emulator emulator-5554.

| Check | Result |
| --- | --- |
| Build debug app and instrumentation APK, offline | Passed |
| Incoming version's existing instrumentation suite | 23 passed |
| Added shutdown regression test against incoming version | Failed: database closed before worker stopped |
| Full suite after fix and regression-test lifecycle synchronization | 24 passed in 8.863 seconds |
| Crash buffer after final suite | Empty |
| Launch MainActivity with am start -W | Status ok |
| git diff --check | Passed |

Suite coverage: six data tests (validation, duplicates, CRUD, persistence, ordering, foreign keys, image copying, and 1,000-entry performance); three progress-data tests (statistics queries, sample loading/deletion, transaction rollback); five screen tests (save/edit/details, validation/rotation, custom exercise return, delete/discard, picker cancellation); eight statistics tests; two tab/lifecycle tests.

## BUG-001: Database closed before background worker stops

Severity: Medium. A shutdown race can interrupt database work during activity closure or recreation. An actual crash or data loss was not observed in this run.

Reproduction: Start MainActivity, start a worker job, wait until it is running, finish the activity, and inspect whether its existing SQLite connection remains open while the job handles shutdown interruption.

Expected: Worker stops before the activity closes its database; cancelled work cannot deliver callbacks to the destroyed screen.

Actual at 602672a: The regression test fails with "Database must stay open until the worker stops". MainActivity calls super.onDestroy() first, and BaseActivity closes the database before returning.

Fix: Run worker.shutdown() before super.onDestroy(). Update the misleading shutdown comment. Keep the incoming null guard in select().

Regression coverage: TabTests.testWorkerStopsBeforeDatabaseCloses checks the connection during interruption, verifies activity destruction and final database closure, and rejects a callback delivered after shutdown. The test waits for actual destruction because finish() is asynchronous.

Verification: New regression and all 23 existing tests pass together after the fix.

## Evidence and limits

Saved evidence: bugfix-baseline-tests.log, bugfix-reproduction.log, bugfix-final-build.log, bugfix-final-tests.log, bugfix-crashes.log, and bugfix-launch.png in ../evidence/2026-10-11/. These selected files are included in Git; other temporary verification output stays local.

This is one emulator run, not a complete device/accessibility matrix or long-running stress test. The worker's existing two-second shutdown timeout was not changed; this regression checks a job that responds to interruption. The fix and regression test are committed in d542d3c. Reports and selected evidence are packaged separately for publication.
