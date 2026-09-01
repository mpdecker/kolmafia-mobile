# Task 1 report: Request routing substrate

## Status

DONE

## Implementation

- Preserved the existing `processVisitResponseHooks(html: String, url: String? = null)` API.
- Normalized absolute and relative KoL URLs once at the hook entry point using the brief's `KOL_BASE_URL`/leading-slash removal sequence.
- Added `processVisitResponseHooksForPath`, the central extension point for residual request response parsers.
- Routed Tea Tree choices 1104/1105 through `TeaTreeChoiceSync`.
- Routed Hashing Vise choice 1551 through `HashingChoiceSync`, including local inventory consumption.
- Added a normalized URL plus response-body signature guard so duplicate hook delivery cannot repeat routed state effects.
- Corrected request logging so choices 1104/1105 are labeled `Tea Tree` and choice 1551 is labeled `Hashing Vise`, not `TakerSpace`.
- No `SharedModule` change was necessary in the narrowed task: the dispatcher uses dependencies already held by `GameRuntimeLibrary`, and later tasks can add their parser dependencies at the existing construction site.

## Tests and exact results

### Focused RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.RequestRoutingResidualTest
```

Result: `BUILD FAILED` in 3m 49s; 5 tests completed, 4 failed.

Expected failures:

- `absoluteTeaTreeResponseIsRoutedIdempotently`
- `relativeSpecificTeaResponseIsRoutedIdempotently`
- `equivalentAbsoluteAndRelativeHashingResponsesConsumeOnce`
- `requestLoggerNamesResidualChoicesCorrectly`

The malformed-URL no-op test passed before implementation.

### Focused GREEN

Same command.

Result: exit code 0 in 2m 12s; all 5 focused tests passed.

### Full shared JVM suite

Command:

```powershell
.\gradlew.bat :shared:jvmTest
```

Result: `BUILD SUCCESSFUL` in 27s; exit code 0.

Existing Gradle/JDK deprecation and Kotlin compiler warnings remain; no task-specific failure occurred.

## TDD RED/GREEN evidence

The new test file was written before production changes. The first focused run produced the four expected behavior failures listed above. After the minimal dispatcher and logging implementation, the same focused command passed all five tests, followed by a successful full shared JVM run.

## Changed files

- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
  - normalized dispatcher, existing Tea Tree/Hashing routes, duplicate-response guard
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/RequestLogger.kt`
  - corrected choice labels
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/RequestRoutingResidualTest.kt`
  - focused relative/absolute, duplicate, malformed, and logging regressions
- `.superpowers/sdd/task-1-report.md`
  - implementation and verification report

## Self-review

- Re-read the corrected brief and checked each interface and checklist item.
- Confirmed normalization makes equivalent absolute and relative Hashing URLs share a signature.
- Confirmed the signature is retained only after a parser actually handles a response, so malformed or unrelated responses do not suppress later valid routing.
- Confirmed Tea Tree decision and item fields are read from the normalized URL.
- Confirmed Hashing consumption delegates to the existing inventory synchronizer path.
- Confirmed request-log assertions cover both Tea Tree IDs and explicitly reject the old 1551 mapping by requiring `Hashing Vise`.
- Reviewed the scoped diff against the substantial pre-existing working-tree edits and staged only task-owned hunks/files.
- Preserved all unrelated uncommitted work.

## Concerns

- Duplicate suppression intentionally covers consecutive equivalent normalized URL/body responses handled by this residual dispatcher. A later task adding a parser whose identical response is legitimately repeatable should define a request-specific signature or reset boundary.
- `SharedModule.kt` remains untouched because Task 1 introduces no new injectable dependency.

## Commits

One scoped Task 1 commit; the resulting hash is recorded in the final status response.

## Reviewer-finding fixes

### Implementation

- Tea Tree choices 1104 and 1105 now require response HTML containing the validated success marker `You acquire an item` before `TeaTreeChoiceSync.apply` can mark `_pottedTeaTreeUsed`.
- Failed and malformed Tea Tree responses remain unhandled and do not write the daily-use preference.
- Replaced immediate-previous signature storage with a handled-signature set, so duplicate delivery remains suppressed across an `A -> B -> A` sequence.
- Added `resetVisitResponseHookSignatures()` as the explicit request-boundary mechanism. Clearing the boundary permits a later legitimate request with an identical normalized URL and response body to be processed again.

### Focused regression coverage

- Added write-counting `Settings` coverage proving duplicate Tea Tree responses write `_pottedTeaTreeUsed` exactly once.
- Added `A -> B -> A` coverage proving both unique handled responses write once while the repeated `A` remains suppressed.
- Added request-boundary reset coverage proving a later identical handled response can write again.
- Added failed and malformed Tea Tree HTML coverage proving zero preference writes.
- Retained absolute/relative normalization, Hashing inventory idempotence, malformed choice URL, and request-label coverage.

### Reviewer-fix TDD evidence

RED command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.RequestRoutingResidualTest
```

RED result: exit code 1 in 20s at `:shared:compileTestKotlinJvm`; the new request-boundary test failed to compile with the expected `Unresolved reference 'resetVisitResponseHookSignatures'` before the mechanism was implemented.

GREEN command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.RequestRoutingResidualTest
```

GREEN result: `BUILD SUCCESSFUL in 2m 45s`, exit code 0; all 8 focused tests passed. Gradle reported `14 actionable tasks: 4 executed, 10 up-to-date`. Existing JDK-target, deprecation, and unrelated compiler warnings remain.

### Reviewer-fix self-review

- Success validation occurs before the existing Tea Tree parser, keeping the synchronizer unchanged and preventing failure HTML from entering its decision-only logic.
- Only successfully handled signatures enter the set; malformed or failed responses cannot poison deduplication.
- Signature normalization still collapses absolute and relative KoL URLs before tracking.
- Reset is explicit and scoped to `GameRuntimeLibrary`, allowing typed request wrappers added by later tasks to establish request boundaries without parallel state.
- No unrelated working-tree changes were altered.
