# Task 5 report: Palm Frond/MUSE and Pizza Cube

## Status

DONE_WITH_CONCERNS

## Implementation

- Preserved `MuseCreateRequest.create(concoction, quantity, state, preferences)` and the existing `CreateItemIngredients` retrieve/queue path. No second crafting engine.
- Added optional `inventoryManager` / `preferences` / `sessionLogger` constructor params. After a successful `You acquire` response (and not `You don't have that many`), consume all recipe ingredients once, `processResults` for HTML gains only, and append `Use N name + M other`. Qty 1 posts `inv_use.php`; qty > 1 posts `multiuse.php`. Retrieval failure issues no HTTP. Malformed HTML does not consume, gain, or log. Instance `parseResponse` is once-per-signature.
- DI uses a dedicated `UseItemRequest` without `inventoryManager` so `UseItemConsumptionSync` does not double-consume the first ingredient.
- Added typed `PizzaCubeRequest(client, inventoryManager, preferences, sessionLogger)` with `makePizza(ingredients: List<Int>): Result<String>`.
- Validates exactly four positive owned IDs before HTTP (duplicates allowed when owned). POST `campground.php` `action=pizza` + `pizza=id,id,id,id`. Parser accepts `action=pizza` and `action=makepizza`. Success requires `You acquire` and not `You don't have that many`. Companion parse consumes ingredients and writes `lastDiabolicPizza`; it does not invent a pizza and does not `processResults` (visit HTML already contains `You acquire`). `makePizza` and `CampgroundRequest.visitAction` call `processResults` after a successful parse. Session log `pizza name1, name2, name3, name4` only after success. Instance and Task 1 dispatcher signatures are once-per-response.
- `CampgroundRequest.visitAction` accepts `extraFields`. `CampgroundSync.parseResponse` returns after `parseCampground` for `pizza`/`makepizza` so generic campground `processResults` does not triple-gain. Residual dispatcher routes pizza URLs through `PizzaCubeRequest.parseResponse`.
- `ResultProcessor.consumeItems` batches negative `processItem` deltas. ItemPool constants: `PALM_FROND=2605`, `PALM_FROND_FAN=2606`, `DIABOLIC_PIZZA_CUBE=10335`, `DIABOLIC_PIZZA=10336`.
- Koin registers `PizzaCubeRequest` and injects `pizzaCubeRequest` on `GameRuntimeLibrary`.

## TDD RED/GREEN evidence

### RED

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.PizzaCubeRequestTest --tests net.sourceforge.kolmafia.request.MuseCreateRequestTest
```

Result: `BUILD FAILED` at `:shared:compileTestKotlinJvm` in 17s.

Expected failure: unresolved `PizzaCubeRequest` and missing `MuseCreateRequest` accounting API, including unresolved `PizzaCubeRequest` and no `parseResponse` / `inventoryManager` / `preferences` / `sessionLogger` on `MuseCreateRequest`.

### GREEN

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.PizzaCubeRequestTest --tests net.sourceforge.kolmafia.request.MuseCreateRequestTest
```

Result after implementation plus pizza-gain split: `BUILD SUCCESSFUL in 2m 38s`; 18 tests.

Re-run on the mixed working tree immediately before commit: `BUILD SUCCESSFUL in 2m`; exit code 0. `PizzaCubeRequestTest` 8 tests, `MuseCreateRequestTest` 10 tests (18 total).

### Full shared JVM verification

Command:

```powershell
.\gradlew.bat :shared:jvmTest
```

Result: `BUILD SUCCESSFUL in 24s`; exit code 0.

## Exact final focused test command/result

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.PizzaCubeRequestTest --tests net.sourceforge.kolmafia.request.MuseCreateRequestTest
```

Result: `BUILD SUCCESSFUL`, exit code 0; 18 tests (8 pizza + 10 MUSE).

## Files changed

Created:

- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/PizzaCubeRequest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/PizzaCubeRequestTest.kt`

Modified:

- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/MuseCreateRequest.kt`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/MuseCreateRequestTest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/CampgroundRequest.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/campground/CampgroundSync.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/ResultProcessor.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/choice/ItemPool.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

`ConcoctionCreateRequest.kt` was listed in the brief but was not modified; it already routes `MUSE` to `museCreateRequest`.

Commit: `51668a9f` — feat: add MUSE response accounting and typed Pizza Cube request.

Unrelated uncommitted `GameRuntimeLibrary.kt` / `SharedModule.kt` work was excluded from the commit and restored afterward.

## Self-review

- MUSE palm-frond fan posts `multiuse.php` quantity 2; single-ingredient qty-1 posts `inv_use.php`; retrieval failure issues no HTTP; malformed HTML preserves inventory and skips the session log; success accounting is once-per-signature.
- Pizza Cube rejects unowned/invalid/wrong-count IDs with no HTTP; duplicate IDs are allowed when owned; `action=pizza` POST form matches the brief; `action=makepizza` consume path does not invent a pizza; malformed/HTTP failure preserve inventory and prefs; visit-hook consume/`lastDiabolicPizza` is once per signature; malformed visit HTML does not consume.
- No automatic retries. Gains come from HTML via `processResults`, not invented item IDs.

## Concerns

- `ConcoctionCreateRequest.kt` listed in the brief is unchanged (already routes MUSE to `MuseCreateRequest`).
- `lastDiabolicPizza` is not a desktop preference; desktop `PizzaCubeRequest` only removes ingredients and session-logs `pizza names`.
- Visit-hook residual parse consumes ingredients and writes the pref but does not apply result gain; `makePizza` and `CampgroundRequest.visitAction` do call `processResults`.
- Dedicated `UseItemRequest` without inventory for MUSE avoids double-consume of the first ingredient via `UseItemConsumptionSync`.
- Pizza cube workshed is not a pre-HTTP gate (desktop does not check it either).
- Isolating `GameRuntimeLibrary.kt` / `SharedModule.kt` to HEAD+Task 5 cannot compile while uncommitted CLI files still reference later constructor params; focused and full JVM verification were run on the mixed working tree that includes those files plus Task 5.

## Reviewer fix (Critical + Important)

Status after reviewer findings: DONE_WITH_CONCERNS.

### Fixes

- `PizzaCubeRequest.makePizza` and each `MuseCreateRequest.create` loop iteration clear `handledSignatures` so a later identical craft is not treated as a duplicate parse of the same body. Same-body idempotency remains for visit hook vs typed request.
- Pizza `ResultProcessor.processResults` moved into the instance `parseResponse` first-handle path; a duplicate `true` cannot gain again. `makePizza` no longer processResults after parse.
- `processVisitResponseHooksForPath` prefers instance `parseResponse` when `pizzaCubeRequest` is injected; otherwise companion parse then processResults once. `ResponseTextParser.externalUpdate` skips pizza URLs so campground generic processResults cannot triple-gain with the dispatcher (same reason `CampgroundSync` already returns early for `pizza`/`makepizza`).
- `makePizza` aborts before HTTP via `RequestAbortGate.abortIfInFightOrChoice()`, matching `CampgroundRequest.visitAction`.

### Tests added

- `makePizza_twoIdenticalCallsEachConsumeAndGain`
- `makePizza_abortsWithoutHttpWhenInFightOrChoice`
- `makePizza_thenVisitHook_doesNotDoubleGain`
- `visitHook_routesCampgroundPizzaIdempotently` now asserts pizza count 1
- `create_palmFrondFan_quantityTwoAccountsEachCraft`

### Focused verification

Command:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.PizzaCubeRequestTest --tests net.sourceforge.kolmafia.request.MuseCreateRequestTest
```

Result: `BUILD SUCCESSFUL in 2m 38s`; exit code 0. `PizzaCubeRequestTest` 11 tests, `MuseCreateRequestTest` 11 tests (22 total).

### Remaining concerns

- Visit-hook pizza gain is owned by the residual dispatcher / typed first-handle path. Generic campground `ResponseTextParser` processResults is skipped for pizza URLs so a second `processVisitResponseHooks` delivery cannot add another pizza. Non-pizza campground actions are unchanged.
- `GameRuntimeLibrary.kt` still carries unrelated uncommitted constructor params; the pizza dispatcher hunk is the only Task 5 change isolated for commit. Focused tests ran on the mixed working tree.
