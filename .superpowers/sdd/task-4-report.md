# Task 4 report: Umbrella and KGB actions

## Status

DONE_WITH_CONCERNS

## Implementation

- Kept existing `ModeableRequest.setMode(modeable: Modeable, mode: String): Result<Unit>`.
- Added optional `inventoryManager` / `equipmentManager` constructor params. Umbrella ownership is rejected before HTTP when those (or `character`) can be checked and the umbrella is neither in inventory nor equipped.
- Choice 1466 is posted with `whichchoice=1466` and the mode option. `umbrellaState` is written only after the success phrase is present in the choice HTML (`ModeableChoiceSync.applyUmbrellaMode`), once.
- Added typed `KgbRequest` with `visit()`, `button(action)`, and `dispenser(itemId)`.
- `visit` / `button` / `dispenser` use `GET place.php?whichplace=kgb` with `action` and optional `whichitem`.
- Parser covers desktop KGB actions (`kgb_button*`, `kgb_dispenser`, drawers, daily, handle), click counting, dispenser uses, and button enchantment swaps via `ModifierDatabase.overrideModifier`. Failed/malformed responses do not mutate prefs or modifiers. No automatic retries.
- Routed `place.php?whichplace=kgb` through `processVisitResponseHooksForPath` with the Task 1 signature guard. Successful button enchantment swaps trigger `checkDynamicModifiers()`.
- Registered `KgbRequest` in Koin and `GameRuntimeLibrary`. Extended the umbrella CLI adapter (typed failure messages) and added a `kgb` CLI (`kgb` status is local; `kgb button` / `kgb dispenser` / `kgb visit` use the typed request).

## TDD RED/GREEN evidence

### RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.UmbrellaKgbRequestTest
```

Result: `BUILD FAILED in 18s` at `:shared:compileTestKotlinJvm`.

Expected failure: unresolved `KgbRequest` and missing constructor parameters, including:

```
e: .../UmbrellaKgbRequestTest.kt:51:13 No parameter with name 'inventoryManager' found.
e: .../UmbrellaKgbRequestTest.kt:153:23 Unresolved reference 'KgbRequest'.
e: .../UmbrellaKgbRequestTest.kt:289:13 No parameter with name 'kgbRequest' found.
```

### GREEN

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.UmbrellaKgbRequestTest --tests net.sourceforge.kolmafia.request.ModeableRequestTest
```

Result: `BUILD SUCCESSFUL in 2m 36s`; exit code 0. 13 tests in `UmbrellaKgbRequestTest` plus existing `ModeableRequestTest`.

### Full shared JVM verification

Command:

```powershell
.\gradlew.bat :shared:jvmTest
```

First post-GREEN run: `GameRuntimeLibraryCliTest.cliExecute_umbrella_setsMode` failed because it still used `"ok"` HTML. Updated that fixture to the bucket-style success phrase.

Second run: `GameRuntimeLibraryAshP503Test.repeat_replaysPreviousCliExecute` failed once (`hello` missing after `Repetition 1 of 1...`). Isolated re-run of that class passed. Third full run: `BUILD SUCCESSFUL in 24s`; exit code 0. Treated as a pre-existing `MaximizerContinuation` race, not a Task 4 regression.

## Exact final focused test command/result

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.UmbrellaKgbRequestTest
```

Result: `BUILD SUCCESSFUL`, exit code 0 (13 tests). Combined with `ModeableRequestTest` in the GREEN run above.

## Files changed

Created:

- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/KgbRequest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/UmbrellaKgbRequestTest.kt`

Modified:

- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ModeableRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/ModeableChoiceSync.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ModeableRequestTest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryCliTest.kt`

Commit: `d875bcce` — Add typed KGB request and umbrella ownership validation.

Unrelated uncommitted `GameRuntimeLibrary.kt` / `SharedModule.kt` work was excluded from the commit and restored afterward.

## Self-review

- Umbrella posts choice 1466 with the expected option, rejects missing ownership/equipment before HTTP, allows equipped-only umbrellas, and does not write `umbrellaState` on malformed choice HTML.
- Pref write is once (`applyUmbrellaMode` only; choice URL parse is not used for the umbrella POST path).
- KGB visit/button/dispenser forms, modifier refresh, failed/malformed preservation, and visit-hook idempotency are covered by tests that failed before the type existed.
- CLI status/already-used paths issue no HTTP; live paths print typed-request failure messages with no destructive fallback.

## Concerns

- Kept `setMode(modeable, mode): Result<Unit>` rather than adding `setMode(itemId, mode): Result<String>`; the brief allowed either.
- `KgbRequest` has an optional fourth constructor parameter `refreshModifiers` (default null) so instance parses can refresh without going through visit hooks. The brief listed a 3-arg constructor; tests and DI use the 3-arg form.
- `kgb` is not in the LongTailCli help verb list (Task 8 help reconcile).
- Ownership validation is skipped when `inventoryManager`, `equipmentManager`, and `character` are all null, so legacy `ModeableRequest` call sites without those deps still issue HTTP.
- One full-suite flake in `repeat_replaysPreviousCliExecute` on the second JVM run; isolated and subsequent full runs passed.

## Reviewer Important findings (follow-up)

Fixed three Important findings without changing umbrella/KGB happy paths.

1. **CLI tokenization.** `cliKgb` splits on whitespace as whole words. Bare `kgb button` prints `Usage: kgb button <action>` and issues no HTTP (no default to `kgb_button1`). `kgb button 1` and `kgb button1` both resolve through `resolveButtonAction` to `kgb_button1`.
2. **Malformed button HTML.** `parseResponse` returns false for `kgb_button*` unless clicks were counted or enchantments updated. `button()` returns `Result.failure` in that case. Session log is written only after a handled parse.
3. **Dispenser click counting.** `countClicks` runs only on the `kgb_button*` branch. Failed/malformed dispenser HTML that contains Click chrome does not increment `_kgbClicksUsed`.

### RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.UmbrellaKgbRequestTest
```

Result: `BUILD FAILED`; 18 tests, 4 failed:

- `cliKgb_bareButtonPrintsUsageWithoutHttp` — printed `Unknown KGB button: button` / issued no usage, or defaulted
- `cliKgb_button1_postsKgbButton1` — `button1` did not map to `action=kgb_button1`
- `kgb_malformedButtonHtmlDoesNotSucceedOrSessionLog` — `button()` returned success
- `kgb_failedDispenserWithClickChromeDoesNotCountClicks` — `_kgbClicksUsed` incremented

`cliKgb_buttonSpace1_postsKgbButton1` already passed on the pre-fix parser (`button 1` → `1`).

### GREEN

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.UmbrellaKgbRequestTest
```

Result: `BUILD SUCCESSFUL in 2m 1s`; exit code 0. 18 tests.

Covering tests: `net.sourceforge.kolmafia.request.UmbrellaKgbRequestTest` (CLI cases live in this class via `cli_execute`; no extra `GameRuntimeLibrary` CLI class required).
