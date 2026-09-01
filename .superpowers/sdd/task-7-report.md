# Task 7 report: Ascension History

## Status

DONE

## Implementation

- Added typed read-only `AscensionHistoryRequest` with `fetch(playerId: Int? = null)` and `parse(html)`.
- GET only: `ascensionhistory.php?back=self`, plus optional `who` when a player id is supplied. No POST.
- Tolerant table-row parser: splits on `</tr>`, reads cells by structure, keeps unknown class/path text, and uses nullable `number`/`turns`/`points` when values are absent. Hardcore images yield 2 points; normal/casual/spacer yield 1; unknown type yields null.
- `AscensionHistoryManager` caches parsed rows and formats headless status lines. Failed HTTP leaves the cache unchanged.
- CLI `ascensionhistory [player]` prints formatted records on success. On HTTP failure it prints cached records, or `Ascension history HTTP unavailable.` when the cache is empty. Player names resolve through `ProfileRequest.fromPlayerName` / `PlayerIdRegistry` only.
- Residual dispatcher routes `ascensionhistory.php` visits through instance `parseResponse`, which caches rows and never writes character prefs, class/path, or Valhalla state.
- Koin registers `AscensionHistoryManager` and `AscensionHistoryRequest`.

## TDD RED/GREEN evidence

### RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.AscensionHistoryRequestTest
```

Result: `BUILD FAILED` at `:shared:compileTestKotlinJvm` in 19s.

Expected failure: unresolved `AscensionHistoryRequest`, `AscensionHistoryManager`, `AscensionRecord`, `fetch`, and no `ascensionHistoryRequest` constructor parameter on `GameRuntimeLibrary`.

### GREEN

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.AscensionHistoryRequestTest
```

First implementation run: `BUILD FAILED`; 12 tests, 4 failures. The row regex required a leading `</tr>` and therefore dropped every row after the first in the current-style fixture (desktop backs up 5 characters to reuse that closer). Parser now splits on `</tr>` and skips header rows (`#` / `Class`).

Re-run: `BUILD SUCCESSFUL in 2m 3s`; exit code 0; 12 tests, 0 failures.

### Full shared JVM verification

Command:

```powershell
.\gradlew.bat :shared:jvmTest
```

Result: `BUILD SUCCESSFUL in 2m 21s`; exit code 0.

## Exact final focused test command/result

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.AscensionHistoryRequestTest
```

Result: `BUILD SUCCESSFUL`, exit code 0; 12 tests, 0 failures (`tests="12" failures="0"` in `TEST-net.sourceforge.kolmafia.request.AscensionHistoryRequestTest.xml`).

## Files changed

Created:

- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/AscensionHistoryRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/AscensionHistoryManager.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/AscensionHistoryRequestTest.kt`

Modified:

- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

Commit: `45862fe0` — feat: add read-only Ascension History request and CLI.

Unrelated uncommitted `GameRuntimeLibrary.kt` / `SharedModule.kt` work was excluded from the commit and restored afterward.

## Self-review

- Fetch is GET with `back=self` and optional `who`. HTTP errors do not parse or replace the cache.
- Parser preserves unknown class/path strings and nulls missing numerics. Header rows are skipped.
- Parse/fetch/visit hooks do not write prefs (`borisPoints`, `awolPointsCowpuncher`), character state, or `CharpaneValhallaSync.inValhalla`.
- Optional player lookup uses `ProfileRequest` / `PlayerIdRegistry` only.
- Visit routing goes through Task 1 `processVisitResponseHooksForPath`.
- `IMPLEMENTED_CLI_COMMANDS` / `RequestLogger` were left for Task 8.

## Concerns

- Desktop `AscensionHistoryRequest.parseResponse` writes challenge path/class prefs when the page is the current player. This task is specified read-only, so those writes are intentionally omitted.
- `character` and `preferences` are injected and unused so DI can pass live session objects without this request writing them.
- Isolating `GameRuntimeLibrary.kt` / `SharedModule.kt` to HEAD+Task 7 cannot compile the mixed working tree’s later constructor params by itself; focused and full JVM verification ran on the mixed working tree that includes those files plus Task 7.

## Reviewer fix: unknown player name

Finding: unresolved player names made `resolveAscensionHistoryPlayerId` return null (`PlayerIdRegistry.getPlayerId` returns the name, then `toIntOrNull()` fails), so `cliAscensionHistory` called `fetch(null)` and silently loaded self history.

Fix: if CLI rest is non-empty and does not resolve to a positive player id (numeric or registry name), print `Unknown player: <name>.` and return without calling `fetch()`. Non-positive numeric ids are treated the same way.

Optional empty-parse-over-cache skip was not applied: a successful empty history (zero ascensions) should replace the cache; distinguishing that from a parse miss would be scope creep.

### RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.AscensionHistoryRequestTest.cli_unknownPlayerNameDoesNotFetchSelfHistory
```

Result: `BUILD FAILED in 39s`; 1 test completed, 1 failed. `cli_unknownPlayerNameDoesNotFetchSelfHistory` `AssertionError` at `AscensionHistoryRequestTest.kt:219` (`out.contains("Unknown player")`) because the CLI fetched self history and printed Seal Clubber records.

### GREEN

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.AscensionHistoryRequestTest
```

Result: `BUILD SUCCESSFUL in 2m`; exit code 0; 13 tests, 0 failures (`tests="13" failures="0"` in `TEST-net.sourceforge.kolmafia.request.AscensionHistoryRequestTest.xml`), including `cli_unknownPlayerNameDoesNotFetchSelfHistory`.
