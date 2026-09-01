# Residual HTTP Request Parity Mega Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement live, typed HTTP parity for the remaining request families in Phases 3951–4010.

**Architecture:** Typed request classes own endpoint construction, validation, response parsing, success-only local accounting, and session logging. Existing managers and synchronizers remain the state boundary; `GameRuntimeLibrary.*` adapters only resolve dependencies, parse CLI arguments, invoke requests, and print results. All responses flow through the existing visit, quest, inventory, meat, preference, modifier, and session-log hooks with idempotent guards.

**Tech Stack:** Kotlin Multiplatform, Ktor `HttpClient`, Kotlin coroutines, Koin, `kotlin.test`, existing `ResultProcessor`, `QuestChoiceRules`, `RequestLogger`, and `SessionLogger`.

## Global Constraints

- Live mutations are required for Hashing Vise, Tea Tree, Foresee, Umbrella, KGB, Pizza Cube, and Flea Market actions.
- Ascension History is read-only.
- GUI/Relay behavior, JavaScript, full TCRS class/sign dump generation, and desktop scripting remain explicit non-goals.
- Failed or malformed responses must not consume items, mark daily uses, or apply local gains.
- Apply local inventory, meat, preference, quest, and modifier changes only after a successful response and only once per response signature.
- Never retry a destructive mutation automatically.
- Update `GameRuntimeLibrary.REVISION` to `phase4010`.
- Verify with `.\gradlew.bat :shared:jvmTest` and `.\gradlew.bat :androidApp:assembleDebug`.

---

## Task 1: Request routing substrate

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/RequestLogger.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/RequestRoutingResidualTest.kt`

**Interfaces:**
- Preserve the existing `GameRuntimeLibrary.processVisitResponseHooks(html: String, url: String? = null)` signature while adding one normalized URL path before dispatch.
- Route the currently existing Tea Tree and Hashing choice parsers centrally; later request tasks register their own KGB, Flea Market, and Ascension History parsers through this same normalized path.
- Ensure `RequestLogger.registerRequest(url: String)` maps Hashing Vise choice 1551 to Hashing Vise and Tea Tree choices 1104/1105 to Tea Tree.

- [ ] **Step 1: Write routing regression tests**

Add tests that pass normalized relative and absolute URLs through the hook entry point twice and assert existing Tea Tree and Hashing synchronizer state changes once. Add a request-log test asserting 1551 is not labeled as TakerSpace and 1104/1105 are labeled as Tea Tree.

- [ ] **Step 2: Run the focused routing tests**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.RequestRoutingResidualTest
```

Expected: the new tests fail until the route and logging entries exist.

- [ ] **Step 3: Implement route normalization and deduplication**

Use a single normalized path/query value before dispatch:

```kotlin
val normalizedUrl = url.orEmpty()
    .removePrefix(KOL_BASE_URL)
    .removePrefix("/")
processVisitResponseHooksForPath(normalizedUrl, html, choiceId)
```

Route each currently available choice endpoint to its existing parser, and retain the response signature guard so repeated hook calls do not repeat inventory or preference effects. Leave the dispatcher extension point used by Tasks 2–7.

- [ ] **Step 4: Run the focused routing tests again**

Expected: PASS, including malformed and repeated-response cases.

## Task 2: Hashing Vise request

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/HashingViseRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/HashingChoiceSync.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.OddballCli.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/HashingViseRequestTest.kt`

**Interfaces:**
- `open class HashingViseRequest(client: HttpClient, choiceRequest: ChoiceRequest, inventoryManager: InventoryManager?, preferences: Preferences?, sessionLogger: SessionLogger?)`
- `suspend fun use(schematicItemId: Int, checksumItemId: Int? = null): Result<String>`
- `fun parseResponse(url: String, html: String, ...): Boolean`

- [ ] **Step 1: Write failing form and failure tests**

Assert that use starts with `POST inv_use.php` and submits choice 1551 with `iid`. Assert non-success and malformed choice responses leave the schematic inventory and checksum preferences unchanged.

- [ ] **Step 2: Implement the typed request**

Validate positive IDs, required item ownership, and the hashing-vise availability gate. Submit the item-use form with Ktor `submitForm`, submit the selected choice through `ChoiceRequest`, then invoke `HashingChoiceSync` and `ResultProcessor` only after successful responses.

- [ ] **Step 3: Migrate `vise` CLI and register DI**

Resolve `HashingViseRequest` from the runtime/DI graph. Keep existing command syntax and print the typed request failure message without issuing a fallback destructive request.

- [ ] **Step 4: Run request and CLI tests**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.HashingViseRequestTest
```

Expected: PASS.

## Task 3: Tea Tree and Foresee requests

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/PottedTeaTreeRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ForeseeRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/TeaTreeChoiceSync.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/PerilChoiceSync.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.IotmCli.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/PottedTeaTreeRequestTest.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/ForeseeRequestTest.kt`

**Interfaces:**
- `suspend fun PottedTeaTreeRequest.shake(): Result<String>`
- `suspend fun PottedTeaTreeRequest.select(teaItemId: Int): Result<String>`
- `suspend fun ForeseeRequest.foresee(perilId: Int? = null): Result<String>`
- Each request exposes a parser that accepts the final URL and response HTML and writes daily-use state only once.

- [ ] **Step 1: Write failing endpoint and daily-limit tests**

Cover `campground.php?action=teatree`, choices 1104/1105 with `itemid`, `inventory.php?action=foresee`, choice 1558, the three-peril cap, unavailable equipment, and failed-response preservation.

- [ ] **Step 2: Implement Tea Tree**

Delegate campground and choice HTTP through injected `CampgroundRequest` and `ChoiceRequest` instances. Reject a second daily use before HTTP, use the selected tea item only on a successful response, and route both choice IDs through `TeaTreeChoiceSync`.

- [ ] **Step 3: Implement Foresee**

Validate the Peridot/Peril selection and remaining uses before submitting. Route the final response through `PerilChoiceSync`; do not increment the count on HTTP errors or malformed choice pages.

- [ ] **Step 4: Migrate CLI adapters and register DI**

Replace manual request construction in `cliTeatree` and the Foresee path with injected typed requests while preserving output and no-HTTP behavior.

- [ ] **Step 5: Run focused tests**

Run:

```powershell
.\gradlew.bat :shared:jvmTest --tests net.sourceforge.kolmafia.request.PottedTeaTreeRequestTest --tests net.sourceforge.kolmafia.request.ForeseeRequestTest
```

Expected: PASS.

## Task 4: Umbrella and KGB actions

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/KgbRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ModeableRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/ModeableChoiceSync.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/UmbrellaKgbRequestTest.kt`

**Interfaces:**
- Preserve `ModeableRequest.setMode(itemId: Int, mode: String): Result<String>` and add ownership/equipment validation before choice 1466.
- `open class KgbRequest(client: HttpClient, preferences: Preferences?, sessionLogger: SessionLogger?)`
- `suspend fun visit(): Result<String>`
- `suspend fun button(action: String): Result<String>`
- `suspend fun dispenser(itemId: Int): Result<String>`

- [ ] **Step 1: Write failing mode/action tests**

Assert umbrella mode posts choice 1466 with the expected fields and rejects an unequipped/non-owned umbrella. Assert KGB place actions parse state, refresh modifier overrides, and do not mutate on failure.

- [ ] **Step 2: Complete Umbrella validation**

Use existing `Modeable` definitions and `ModeableChoiceSync`; apply the mode only after successful choice response and invalidate current modifiers once.

- [ ] **Step 3: Implement KGB request and parser**

Support `place.php?whichplace=kgb`, `kgb_button`, and `kgb_dispenser` forms. Parse button/dispenser state into preferences and trigger the existing dynamic modifier refresh path.

- [ ] **Step 4: Wire CLI, hooks, and DI**

Add the typed request to `SharedModule`, route KGB pages through visit hooks, and migrate the existing command adapter without adding GUI behavior.

- [ ] **Step 5: Run focused tests**

Expected: all `UmbrellaKgbRequestTest` tests pass.

## Task 5: Palm Frond/MUSE and Pizza Cube

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/PizzaCubeRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/MuseCreateRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/ConcoctionCreateRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/CampgroundRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/CampgroundSync.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/ResultProcessor.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/PizzaCubeRequestTest.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/MuseCreateRequestTest.kt`

**Interfaces:**
- Preserve existing `MuseCreateRequest` creation interfaces and add response/session accounting only where missing.
- `open class PizzaCubeRequest(client: HttpClient, inventoryManager: InventoryManager?, preferences: Preferences?, sessionLogger: SessionLogger?)`
- `suspend fun makePizza(ingredients: List<Int>): Result<String>`

- [ ] **Step 1: Write failing MUSE and Pizza tests**

Cover single versus multi-use Palm Frond paths, ingredient retrieval failure, `campground.php?action=pizza`, `action=makepizza`, success-only ingredient consumption/result gain, and malformed response preservation.

- [ ] **Step 2: Extend MUSE accounting**

Reuse `MuseCreateRequest` and `CreateItemIngredients`; ensure single-use and `multiuse.php` operations use the existing queue/retrieve behavior and apply local deltas once after success.

- [ ] **Step 3: Implement Pizza Cube**

Validate ingredient IDs and ownership, submit the campground form, parse the final result, call `ResultProcessor`, update pizza preferences, and append a session-log line only after success.

- [ ] **Step 4: Wire response routing and DI**

Register Pizza Cube with Koin and route campground/pizza responses through existing hooks.

- [ ] **Step 5: Run focused tests**

Expected: MUSE and Pizza tests pass.

## Task 6: Flea Market buy and sell

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/FleaMarketRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/FleaMarketSellRequest.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/ResultProcessor.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/RequestLogger.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/FleaMarketRequestTest.kt`

**Interfaces:**
- `suspend fun buy(itemId: Int, quantity: Int): Result<String>`
- `suspend fun sell(itemId: Int, quantity: Int, price: Int): Result<String>`
- `fun parseResponse(url: String, html: String, ...): Boolean`

- [ ] **Step 1: Write failing buy/sell tests**

Assert exact form fields for `town_fleamarket.php` and `town_sellflea.php`, successful inventory/meat effects, server rejection handling, malformed HTML handling, and session-log output.

- [ ] **Step 2: Implement buy**

Validate positive quantity and item resolution before submission. Submit `buying=Yep.`, `which`, `whichitem`, and `howmuch`; parse the response's confirmed purchase quantity and price, then apply item/meat state only when the response confirms acquisition.

- [ ] **Step 3: Implement sell**

Validate inventory ownership, quantity, and positive sale price. Submit the sell form and apply inventory loss/meat gain only after a confirmed successful response.

- [ ] **Step 4: Wire logging, hooks, and DI**

Register both request types and ensure request logging identifies buy versus sell without treating either as a general mall operation.

- [ ] **Step 5: Run focused tests**

Expected: all Flea Market fixture tests pass.

## Task 7: Ascension History

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/AscensionHistoryRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/AscensionHistoryManager.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/AscensionHistoryRequestTest.kt`

**Interfaces:**
- `data class AscensionRecord(val number: Int?, val className: String, val pathName: String, val turns: Int?, val points: Int?)`
- `suspend fun fetch(playerId: Int? = null): Result<List<AscensionRecord>>`
- `fun parse(html: String): List<AscensionRecord>`

- [ ] **Step 1: Write parser and endpoint tests**

Cover `ascensionhistory.php?back=self`, optional `who`, historical rows with missing values, changed whitespace/HTML structure, unknown class/path text, and HTTP errors.

- [ ] **Step 2: Implement tolerant parser**

Parse table rows by cell structure, preserve unknown names as strings, use nullable numeric fields for absent values, and never overwrite current character state.

- [ ] **Step 3: Implement read-only request and status adapter**

Use a GET request only, return parsed records, and expose a headless CLI/status path that reports cached records or a clear HTTP-unavailable message.

- [ ] **Step 4: Register DI and integrate optional player lookup**

Use existing `ProfileRequest`/`PlayerIdRegistry` only for resolving a requested player; do not merge historical records into Valhalla mutation state.

- [ ] **Step 5: Run focused tests**

Expected: all Ascension History tests pass.

## Task 8: Final CLI, regression, and parity closure

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.LongTailCli.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/RequestLogger.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Modify: `docs/parity-audit.md`
- Modify: `AGENTS.md`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryHttpResidualCliTest.kt`

**Interfaces:**
- Preserve existing command spellings and aliases for `vise`, `teatree`, `umbrella`, `foresee`, `kgb`, `pizza`, `fleamarket`, and `ascensionhistory`.
- Set `GameRuntimeLibrary.REVISION` to `"phase4010"`.
- Keep explicit non-goal help text for GUI/Relay, JavaScript, full TCRS dumps, and desktop scripting.

- [ ] **Step 1: Add CLI/help and DI regression coverage**

Exercise supported commands with injected fakes, assert unavailable-HTTP messages, verify all typed requests resolve from Koin, and assert repeated response hooks are idempotent.

- [ ] **Step 2: Recount and update documentation**

Count `commonMain` files/LOC, common tests/`@Test`, and `regFn` sites. Add the 3951–4010 row and history entry to `docs/parity-audit.md`; add the corresponding mega and metrics entry to `AGENTS.md`.

- [ ] **Step 3: Run the complete verification suite**

Run:

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
git diff --check
```

Expected: all JVM tests pass, the Android debug APK assembles, and `git diff --check` reports no whitespace errors.

- [ ] **Step 4: Review scope**

Confirm no GUI/Relay, JavaScript, TCRS dump, or desktop scripting files were added, and confirm the attached roadmap plan was not modified.
