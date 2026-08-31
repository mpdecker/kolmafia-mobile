# Task 2 report: Hashing Vise request

## Status

DONE

## Implementation

- Added typed `HashingViseRequest` with the required constructor and `use(schematicItemId, checksumItemId)` API.
- Validates positive schematic/checksum IDs, inventory availability, hashing-vise ownership, and schematic ownership before HTTP.
- Starts each operation with `POST inv_use.php` using hashing vise item ID `11826`, validates that choice `1551` opened, then submits option `1` and `iid` through `ChoiceRequest`.
- Rejects non-success item-use and choice HTTP responses and malformed/non-success choice HTML before applying inventory or preference effects.
- Added `parseResponse` success gating. Successful responses invoke `HashingChoiceSync` for schematic consumption and checksum extraction, and `ResultProcessor` for all resulting inventory/preference updates.
- Extended `HashingChoiceSync` to require a positive `iid` and parse checksum item IDs/counts from KoL result-table `rel` metadata.
- Updated `ChoiceRequest` to return failure for non-2xx responses, preventing typed callers from treating HTTP failures as successful choice results.
- Migrated the `vise` CLI to the typed request. Existing syntax, matching, availability limiting, and output remain; typed failures are printed directly and no fallback destructive request is sent.
- Registered `HashingViseRequest` in Koin and injected it into `GameRuntimeLibrary`.

## TDD RED/GREEN evidence

### Request RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.HashingViseRequestTest
```

Result: `BUILD FAILED in 18s` at `:shared:compileTestKotlinJvm` with the expected unresolved `HashingViseRequest` references.

### Initial request GREEN

Same command.

Result: `BUILD SUCCESSFUL in 2m 34s`; the initial four request tests passed.

### CLI RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.ash.GameRuntimeLibraryOddballCliTest
```

Result: `BUILD FAILED in 17s` with the expected missing `hashingViseRequest` runtime constructor parameter.

### CLI GREEN

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.HashingViseRequestTest --tests net.sourceforge.kolmafia.ash.GameRuntimeLibraryOddballCliTest
```

Result: `BUILD SUCCESSFUL in 39s`; all 13 request/CLI tests passed.

### Non-2xx choice RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.HashingViseRequestTest
```

Result: `BUILD FAILED in 39s`; `use_failedChoiceResponseLeavesInventoryUnchanged` failed because `ChoiceRequest` still accepted HTTP 500.

### Final focused GREEN

Same command after adding non-2xx rejection.

Result: `BUILD SUCCESSFUL in 1m 56s`; all five focused request tests passed.

## Exact final test commands/results

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.HashingViseRequestTest
```

Result: `BUILD SUCCESSFUL in 1m 56s`, exit code 0.

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.ash.GameRuntimeLibraryOddballCliTest --tests net.sourceforge.kolmafia.request.RequestRoutingResidualTest
```

Result: `BUILD SUCCESSFUL in 3s`, exit code 0.

```powershell
.\gradlew.bat :shared:jvmTest
```

Result: `BUILD SUCCESSFUL in 24s`, exit code 0.

Existing Gradle/JDK deprecation, disabled native-target, and unrelated Kotlin compiler warnings remain.

## Changed files

- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/HashingViseRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/ChoiceRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/HashingChoiceSync.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.OddballCli.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt` — Task 2 constructor/import hunk only
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt` — Task 2 import/registration/injection hunks only
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/HashingViseRequestTest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryOddballCliTest.kt`
- `.superpowers/sdd/task-2-report.md`

## Self-review

- Re-read the task brief and checked every interface and checklist item.
- Confirmed both requests are POSTs and choice form fields are exactly `whichchoice=1551`, `option=1`, and `iid=<schematic ID>`.
- Confirmed no request is sent for invalid IDs, missing hashing vise, or missing schematic.
- Confirmed item-use HTTP failure, choice HTTP failure, and malformed choice HTML leave schematic/checksum inventory and preference sentinels unchanged.
- Confirmed state mutation happens only after the exact hashing success marker and a positive URL `iid`.
- Confirmed actual KoL checksum result metadata can apply both checksum item IDs and their quantities without double-processing.
- Confirmed the CLI invokes only `HashingViseRequest`, prints its failure, and has no old `UseItemRequest`/raw `ChoiceRequest` fallback.
- Reviewed overlapping dirty files and will stage only Task 2 hunks, preserving all unrelated work.

## Concerns

None specific to Task 2. Existing build warnings are unchanged.

## Review fix: no invent-gain checksum accounting

Finding: `HashingViseRequest.parseResponse` invented a checksum gain when `checksumItemId` was provided but the HTML had no checksum `rel` metadata and `ResultProcessor.processResults` did not increase that count.

### Fix

- Removed the `ResultProcessor.processItem(checksumItemId, 1, ...)` fallback.
- Kept schematic consumption via `HashingChoiceSync`.
- Kept checksum gains that `HashingChoiceSync` confirms from KoL result-table `rel` metadata.
- Kept `ResultProcessor.processResults` as an HTML-confirmed fallback only when `HashingChoiceSync` did not already apply a checksum gain.
- Updated the success fixture to include real checksum `rel` metadata (`id=11789&...&n=1`).
- Added `use_successWithoutChecksumMetadataConsumesSchematicWithoutInventingChecksum`, which uses crush HTML without checksum metadata, consumes the schematic, and leaves checksum count at 0.

### TDD RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.HashingViseRequestTest.use_successWithoutChecksumMetadataConsumesSchematicWithoutInventingChecksum
```

Result: `BUILD FAILED in 2m 37s`, exit code 1.

```
HashingViseRequestTest[jvm] > use_successWithoutChecksumMetadataConsumesSchematicWithoutInventingChecksum[jvm] FAILED
    java.lang.AssertionError at HashingViseRequestTest.kt:58
```

The invented `processItem(checksumItemId, 1)` path made checksum count 1 instead of the expected 0.

### TDD GREEN

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.HashingViseRequestTest --tests net.sourceforge.kolmafia.ash.GameRuntimeLibraryOddballCliTest
```

Result: `BUILD SUCCESSFUL in 2m 33s`, exit code 0.

Covering tests: `HashingViseRequestTest` (6 tests, including the no-invent-gain case) and `GameRuntimeLibraryOddballCliTest`. Existing Gradle/JDK deprecation, disabled native-target, and unrelated Kotlin compiler warnings remain.
