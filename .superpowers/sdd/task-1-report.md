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
