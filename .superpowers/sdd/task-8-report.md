# Task 8 report: Final CLI, regression, and parity closure

## Status

DONE_WITH_CONCERNS

## Implementation

- Set `GameRuntimeLibrary.REVISION` to `"phase4010"` and bulk-updated revision assertion tests from `phase3950`.
- Added help verbs `ascensionhistory`, `fleamarket`, `foresee`, and `kgb` to `IMPLEMENTED_CLI_COMMANDS`. Existing `vise`, `teatree`, `umbrella`, and `flea` were already listed. Did **not** add `pizza` (no pizza CLI).
- Bare `help` and non-goal topics (`relay`, `javascript`/`js`, `tcrs`, `script`, `gui`) print: `GUI/Relay, JavaScript, full TCRS dumps, and desktop scripting are not available in KoLmafia Mobile.`
- `RequestLogger` labels: choice 1466 `Umbrella`, choice 1558 `Foresee`, `place.php?whichplace=kgb` `kgb $action` / `Visiting KGB`, campground `pizza`/`makepizza` `pizza`, `ascensionhistory.php` `ascension history`.
- Typed residual requests remain Koin-injected on `GameRuntimeLibrary`. Command spellings and aliases were not changed.
- Recounted and updated `docs/parity-audit.md` + `AGENTS.md` for Phases 3951–4010. Struck the named HTTP Request class gap as **Live (3951–4010)**. Did not edit `docs/superpowers/plans/2026-08-31-http-request-residual.md`.

## TDD RED/GREEN evidence

### RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.ash.GameRuntimeLibraryHttpResidualCliTest
```

Result: 9 tests, 6 failures (revision, help verbs/non-goals, help filters, help javascript, RequestLogger labels, Koin DI / `createKoLHttpClient` `ExceptionInInitializerError`). 3 already-green: unavailable HTTP messages, injected fakes, idempotent tea-tree/KGB hooks.

### GREEN

Same command after implementation: first 8/9 (Koin `get<HashingViseRequest>()` / full `GameRuntimeLibrary` graph `StackOverflowError`). DI test then resolves cycle-free Koin types and constructs the remaining request objects from those deps.

Re-run: `BUILD SUCCESSFUL in 38s`; exit code 0; 9 tests, 0 failures (`tests="9" failures="0"` in `TEST-net.sourceforge.kolmafia.ash.GameRuntimeLibraryHttpResidualCliTest.xml`).

## Exact final focused test command/result

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.ash.GameRuntimeLibraryHttpResidualCliTest
```

Result: `BUILD SUCCESSFUL`, exit code 0; 9 tests, 0 failures.

## Full verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
git diff --check
```

- `:shared:jvmTest`: `BUILD SUCCESSFUL`; 8,352 tests, 0 failures, 0 errors, 0 skipped (1,300 suites).
- `:androidApp:assembleDebug`: `BUILD SUCCESSFUL in 2m 41s`.
- `git diff --check`: exit code 0 (CRLF warnings only; no whitespace errors).

## Files changed

Created:

- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryHttpResidualCliTest.kt`

Modified (Task 8 core):

- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.LongTailCli.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/RequestLogger.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt` (already wired; no new bindings required)
- `docs/parity-audit.md`
- `AGENTS.md`
- revision assertion tests under `shared/src/commonTest` (`phase3950` → `phase4010`)

Accumulated uncommitted parity also staged with this closure (CLI Tier-4 / GuildUnlock + Beach): `GameRuntimeLibrary.CliTier4.kt`, `GameRuntimeLibrary.GuildBeach.kt`, `GuildRequest.kt`, `PandamoniumRequest.kt`, `SpadeRequest.kt`, Beach/clan/dad/guild/slime/TCRS session managers, and their tests.

## Metrics

- commonMain: **1,516** `.kt` files / **171,295** LOC
- commonTest: **1,278** files / **8,352** `@Test`
- ASH `regFn`: **1,032**
- `*Request.kt`: **165**
- `*Manager.kt`: **97**
- Help verbs listed: **273**

## Self-review

- No GUI/Relay, JavaScript, TCRS dump, or desktop scripting files were added.
- Attached roadmap plan was not modified.
- `pizza` is visit/log-only; help does not list it.
- Command spellings/aliases unchanged.

## Concerns

- Full `sharedModule` cannot `get<HashingViseRequest>()` or `get<GameRuntimeLibrary>()` in tests: pre-existing Koin cycle (`StackOverflowError` through the InventoryManager / GameRuntimeLibrary subgraph). The DI test therefore resolves cycle-free types (`KgbRequest`, `AscensionHistoryRequest`, `AscensionHistoryManager`, `ChoiceRequest`, `HttpClient`) from Koin and constructs the other residual requests with those deps. Production Koin wiring on `GameRuntimeLibrary` is unchanged.
- `help tcrs` also prints the non-goal footer because the leftover contains `tcrs` (full TCRS dumps remain a non-goal; local `tcrs` CLI still lists).
- Remaining `*Request` class gap vs desktop is unnamed Sync/CLI stand-ins, not the named holes closed by this mega.
