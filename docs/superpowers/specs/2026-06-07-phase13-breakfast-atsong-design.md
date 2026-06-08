# Phase 13: Full BreakfastManager + AT Song Slot Management

_Design date: 2026-06-07_

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this spec task-by-task.

---

## Goal

Complete the daily breakfast automation to parity with desktop `BreakfastManager.java` (22 actions)
and add AT song slot management to `MoodManager` so Accordion Thief characters no longer silently
fail song casts when their slot is full.

---

## Background and Current State

### BreakfastManager — what's done (Phase 10 + PR #11 Task 7)

`session/BreakfastManager.kt` already implements:

1. `harvestGarden` — `CampgroundRequest.harvestGarden()`
2. `checkRumpusRoom` — `ClanRumpusRequest.visit()`
3. `checkVIPLounge` — Klaw×3, Looking Glass, Fireworks, Pool via `ClanLoungeRequest`
4. `readGuildManual` — `UseItemRequest.use(manualId, 1)` (Mus/Mys/Mox manual)
5. `makePocketWishes` — STUB (checks inventory but does nothing)

`clearBreakfastPrefs()` clears 8 done-sentinels on rollover. New actions must clear their own sentinels here.

### BreakfastManager — what's NOT done (Phase 13 target)

Desktop `BreakfastManager.java` line 98-119 lists 22 actions (including the 4 above). The
remaining 18, in desktop order, are grouped by implementation complexity:

#### Tier 1: Simple item use (UseItemRequest or HermitRequest)
| Action | Method | Item(s) | Pref sentinel |
|--------|--------|---------|---------------|
| Hermit clovers | `getHermitClovers` | `HermitRequest.trade(worthlessItem, CLOVER_ITEM_ID)` | `_cloverSought` (boolean) |
| April shower globs | `collectAprilShowerGlobs` | `GenericRequest("inventory.php?action=shower")` | `_aprilShowerGlobsCollected` |
| Book of Every Skill | `useBookOfEverySkill` | item #10917 | `_bookOfEverySkillUsed` |
| Replica snowcone | `useReplicaBooks` | item #11197 | `_replicaSnowconeTomeUsed` |
| Replica resolution | `useReplicaBooks` | item #11213 | `_replicaResolutionLibramUsed` |
| Replica smith | `useReplicaBooks` | item #11219 | `_replicaSmithsTomeUsed` |
| Handheld radio | `makeHandheldRadios` | Allied Radio Backpack #11933 → `use` → produces Handheld Allied Radio #11946 | `_handRadioUsed` |
| Anticheese | `collectAnticheese` | item #142; 5-day cooldown via `lastAnticheeseDay` pref | `_anticheeseCollected` |
| Batteries | `harvestBatteries` | "potted power plant" (item by name — use `GameDatabase.itemId("potted power plant")`); `use` | `_batteriesHarvested` |

#### Tier 2: Multi-step / choice-based
| Action | Method | Mechanism | Pref sentinel |
|--------|--------|-----------|---------------|
| Pocket wishes | `makePocketWishes` | Genie bottle #9529 or Replica Genie Bottle #11234 in inventory → `UseItemRequest.use(bottleId, 1)` → triggers choice adventure #1; option 3 = "for more wishes" | `_pocketWishesUsed` |
| Boxing daydream | `haveBoxingDaydream` | `GenericRequest("place.php?whichplace=town_wrong&action=townwrong_boxingdaycare")` → returns choice adventure #1261; option 1 = "daydream" | `_boxingDaydream` |
| Toys | `useToys` | 34 toy item IDs (see `BreakfastItemIds.kt`); for each toy present in inventory, `UseItemRequest.use(toyId, count)` where `count` = desktop's requested count (1 except school diploma=11, pump-up hightops=3); sentinel per toy: `_toyUsed_$toyId` | `_toyUsed_<id>` per toy |
| Spinning wheel | `useSpinningWheel` | `CampgroundRequest.useSpinningWheel()` — extend existing `CampgroundRequest` with `POST campground.php action=spinningwheel` | `_spinningWheelUsed` |

#### Tier 3: Requires additional infrastructure (implement; see notes)
| Action | Method | Notes |
|--------|--------|-------|
| Big Island | `visitBigIsland` | `GenericRequest("bigisland.php")` — just a page visit; no parse needed |
| Volcano Island | `visitVolcanoIsland` | `GenericRequest("place.php?whichplace=island_camp")` — page visit |
| Hardwood planks | `collectHardwood` | SpinMaster Lathe: `CoinMasterRequest.visit(latheName)` — use the existing `CoinmasterRequest` class with action `shop`; URL is `shop.php?whichshop=lathe` |
| Mr. Store 2002 credits | `collect2002MrStoreCredits` | If `_2002MrStoreCreditsCollected` false AND player has item #11257 or #11280 (catalog) → `UseItemRequest.use(catalogId, 1)`; equipping the catalog yields the credits |
| Server room | `visitServerRoom` | `GenericRequest("place.php?whichplace=airport_spooky_bunker")` — page visit |

#### Deferred (not Phase 13)
| Action | Method | Why deferred |
|--------|--------|-------------|
| Sea jelly | `collectSeaJelly` | Requires jellyfish familiar swap + adventure + parse jelly amount from fight result |
| Jackass Plumber | `checkJackass` | Full arcade game integration via `ArcadeRequest` — complex flash-game emulation logic |

### AT Song Slot Management — current state

`MoodManager.executeMood()` casts triggers unconditionally. When an Accordion Thief character
has 3 songs active (the default limit), KoL silently rejects a 4th song cast — the HTTP
request returns success but KoL doesn't apply the effect. Mobile never evicts the lowest-priority
active song before casting the new one.

---

## Architecture

### New Files

**`session/BreakfastItemIds.kt`** — compile-time constants for all breakfast item IDs and toy
counts. Pure data, no dependencies.

```kotlin
package net.sourceforge.kolmafia.session

object BreakfastItemIds {
    const val CLOVER_ITEM_ID              = 24   // 11-leaf clover
    const val WORTHLESS_TRINKET_ID        = 7
    const val WORTHLESS_KNICK_KNACK_ID    = 8
    const val WORTHLESS_GEWGAW_ID         = 9
    const val BOOK_OF_EVERY_SKILL_ID      = 10917
    const val REPLICA_SNOWCONE_ID         = 11197
    const val REPLICA_RESOLUTION_ID       = 11213
    const val REPLICA_SMITH_ID            = 11219
    const val ALLIED_RADIO_BACKPACK_ID    = 11933
    const val ANTICHEESE_ID               = 142
    const val GENIE_BOTTLE_ID             = 9529
    const val REPLICA_GENIE_BOTTLE_ID     = 11234
    const val MR_STORE_2002_CATALOG_ID    = 11257
    const val REPLICA_MR_STORE_CATALOG_ID = 11280
    const val SPINNING_WHEEL_ITEM_ID      = 7140  // For inventory presence check

    // 34 toys: item ID → daily count (count = how many times to use per day)
    val TOYS: Map<Int, Int> = mapOf(
        3092 to 1,   // hobby horse
        3093 to 1,   // ball-in-a-cup
        3094 to 1,   // set of jacks
        3261 to 1,   // bag of candy
        3010 to 1,   // Emblem of Akgyxoth
        3009 to 1,   // Idol of Akgyxoth
        3629 to 1,   // Burrowgrub hive
        3731 to 1,   // gnoll eye
        4641 to 1,   // KoL Con six-pack
        5502 to 1,   // Trivial Avocations game
        5062 to 1,   // creepy voodoo doll
        5664 to 1,   // cursed keg
        5663 to 1,   // cursed microwave
        6051 to 1,   // taco flier
        7059 to 1,   // WarBear soda machine
        7056 to 1,   // WarBear breakfast machine
        7060 to 1,   // WarBear bank
        7729 to 1,   // Chroner trigger
        7723 to 1,   // Chroner cross
        7936 to 1,   // picky tweezers
        8283 to 1,   // cocktail shaker
        9025 to 1,   // bacon machine
        637  to 1,   // toaster
        9123 to 11,  // school of hard knocks diploma (11 uses)
        5739 to 1,   // CSA fire-starting kit
        9961 to 3,   // pump-up high tops (3 uses)
        10265 to 1,  // etched hourglass
        10207 to 1,  // glitch item
        10652 to 1,  // subscription cocoa dispenser
        10670 to 1,  // overflowing gift basket
        10878 to 1,  // meatball machine
        10879 to 1,  // refurbished air fryer
        11451 to 1,  // punching mirror
        11485 to 1,  // li'l snowball factory
    )
}
```

**Note on PORK_ELF_TOILETRIES_KIT (12192):** This toy was added late in the desktop
source but the item ID is tentative. The implementer should verify against `ItemPool.java` and
add it to the TOYS map if confirmed. The desktop toy list also includes items that change over
game updates; the map above reflects the `BreakfastManager.java` toy array as of 2026-06-07.

### Modified Files

**`session/BreakfastManager.kt`** — add 16 new `private suspend fun` methods; call each from
`runBreakfast()`; add new constants to companion object; inject `hermitRequest: HermitRequest`
and `genericRequest: suspend (String) -> Result<String>` (or a thin HTTP client reference).

**`preferences/Preferences.kt`** — add new pref key constants for all new done-sentinels:

```kotlin
// BreakfastManager new sentinels (Phase 13)
const val CLOVER_SOUGHT               = "_cloverSought"
const val APRIL_SHOWER_GLOBS          = "_aprilShowerGlobsCollected"
const val BOOK_OF_EVERY_SKILL_USED    = "_bookOfEverySkillUsed"
const val REPLICA_SNOWCONE_USED       = "_replicaSnowconeTomeUsed"
const val REPLICA_RESOLUTION_USED     = "_replicaResolutionLibramUsed"
const val REPLICA_SMITH_USED          = "_replicaSmithsTomeUsed"
const val HAND_RADIO_USED             = "_handRadioUsed"
const val ANTICHEESE_COLLECTED        = "_anticheeseCollected"
const val LAST_ANTICHEESE_DAY         = "lastAnticheeseDay"   // int; daycount when anticheese last collected
const val BATTERIES_HARVESTED         = "_batteriesHarvested"
const val POCKET_WISHES_USED          = "_pocketWishesUsed"
const val BOXING_DAYDREAM             = "_boxingDaydream"
const val SPINNING_WHEEL_USED         = "_spinningWheelUsed"
const val BIG_ISLAND_VISITED          = "_bigIslandVisited"
const val VOLCANO_ISLAND_VISITED      = "_volcanoIslandVisited"
const val HARDWOOD_COLLECTED          = "_hardwoodCollected"
const val MR_STORE_CREDITS_COLLECTED  = "_2002MrStoreCreditsCollected"
const val SERVER_ROOM_VISITED         = "_serverRoomVisited"
// Per-toy sentinels are dynamic: "_toyUsed_$toyId" (string key built at runtime)
```

**`clearBreakfastPrefs()`** — add `preferences.setBoolean(...)` calls for each new boolean sentinel
and `preferences.setInt(LAST_ANTICHEESE_DAY, -1)` reset. The per-toy sentinels must be cleared
by iterating `BreakfastItemIds.TOYS.keys` and calling `preferences.setBoolean("_toyUsed_$id", false)`.

**`request/CampgroundRequest.kt`** — add `useSpinningWheel()`:
```kotlin
suspend fun useSpinningWheel(): Result<String> =
    post("campground.php", mapOf("action" to "spinningwheel"))
```

**`di/SharedModule.kt`** — inject `HermitRequest` into `BreakfastManager`.

**`mood/MoodManager.kt`** — add AT song slot check before casting song triggers.

**`effect/EffectManager.kt`** — add `isAtSong()` and `activeAtSongs()`.

**`character/KoLCharacter.kt`** / **`CharacterState.kt`** — add `atSongLimit` computed from
class + skills.

---

## BreakfastManager: Action Implementations

### Tier 1 — Simple item use

All Tier 1 actions follow this pattern:
```kotlin
private suspend fun <actionName>(inventoryState: InventoryState) {
    if (preferences.getBoolean(Preferences.<SENTINEL>, false)) return
    if (!inventoryState.items.containsKey(BreakfastItemIds.<ITEM_ID>)) return
    useItemRequest.use(BreakfastItemIds.<ITEM_ID>, 1).onSuccess {
        preferences.setBoolean(Preferences.<SENTINEL>, true)
    }
}
```

**Hermit clovers** differs: uses `HermitRequest` not `UseItemRequest`.
```kotlin
private suspend fun getHermitClovers() {
    if (preferences.getBoolean(Preferences.CLOVER_SOUGHT, false)) return
    // Worthless items (IDs 7, 8, 9) are the currency; clover ID = 24
    // Attempt to trade 1 worthless item for 1 clover (if any worthless items in inventory)
    val worthlessId = listOf(
        BreakfastItemIds.WORTHLESS_TRINKET_ID,
        BreakfastItemIds.WORTHLESS_KNICK_KNACK_ID,
        BreakfastItemIds.WORTHLESS_GEWGAW_ID,
    ).firstOrNull { inventoryState.items.containsKey(it) } ?: return
    hermitRequest.trade(worthlessId, BreakfastItemIds.CLOVER_ITEM_ID, 1).onSuccess {
        preferences.setBoolean(Preferences.CLOVER_SOUGHT, true)
    }
}
```

**April shower globs** — no item ID check; check for `APRIL_SHOWER_THOUGHTS_SHIELD` (#11884)
in inventory (the shield enables the shower action):
```kotlin
private suspend fun collectAprilShowerGlobs(inventoryState: InventoryState) {
    if (preferences.getBoolean(Preferences.APRIL_SHOWER_GLOBS, false)) return
    if (!inventoryState.items.containsKey(11884)) return  // APRIL_SHOWER_THOUGHTS_SHIELD
    genericGet("inventory.php?action=shower").onSuccess {
        preferences.setBoolean(Preferences.APRIL_SHOWER_GLOBS, true)
    }
}
```

**Anticheese** — 5-day cooldown:
```kotlin
private suspend fun collectAnticheese(inventoryState: InventoryState) {
    if (preferences.getBoolean(Preferences.ANTICHEESE_COLLECTED, false)) return
    val lastAnticheeseDay = preferences.getInt(Preferences.LAST_ANTICHEESE_DAY, -1)
    val currentDays = preferences.getInt(Preferences.LAST_DAYCOUNT, -1)
    // 5-day cooldown check; LAST_DAYCOUNT is the daycount from the last rollover sentinel
    if (lastAnticheeseDay >= 0 && currentDays >= 0 && currentDays < lastAnticheeseDay + 5) return
    if (!inventoryState.items.containsKey(BreakfastItemIds.ANTICHEESE_ID)) return
    useItemRequest.use(BreakfastItemIds.ANTICHEESE_ID, 1).onSuccess {
        preferences.setBoolean(Preferences.ANTICHEESE_COLLECTED, true)
        preferences.setInt(Preferences.LAST_ANTICHEESE_DAY, currentDays)
    }
}
```

**Batteries** — item by name:
```kotlin
private suspend fun harvestBatteries(inventoryState: InventoryState) {
    if (preferences.getBoolean(Preferences.BATTERIES_HARVESTED, false)) return
    val plantId = gameDatabase.itemId("potted power plant")
    if (plantId < 0 || !inventoryState.items.containsKey(plantId)) return
    useItemRequest.use(plantId, 1).onSuccess {
        preferences.setBoolean(Preferences.BATTERIES_HARVESTED, true)
    }
}
```

### Tier 2 — Multi-step

**Toy uses**:
```kotlin
private suspend fun useToys(inventoryState: InventoryState) {
    for ((toyId, count) in BreakfastItemIds.TOYS) {
        val sentinelKey = "_toyUsed_$toyId"
        if (preferences.getBoolean(sentinelKey, false)) continue
        if (!inventoryState.items.containsKey(toyId)) continue
        try {
            useItemRequest.use(toyId, count)
            preferences.setBoolean(sentinelKey, true)
        } catch (e: Exception) {
            // best-effort; continue to next toy
        }
    }
}
```

**Pocket wishes** — use genie bottle (triggers choice adventure; mobile's resolveChoice() handles
the response automatically since `makePocketWishes` calls `use` which triggers the choice):
```kotlin
private suspend fun makePocketWishes(inventoryState: InventoryState) {
    if (preferences.getBoolean(Preferences.POCKET_WISHES_USED, false)) return
    val bottleId = when {
        inventoryState.items.containsKey(BreakfastItemIds.GENIE_BOTTLE_ID) ->
            BreakfastItemIds.GENIE_BOTTLE_ID
        inventoryState.items.containsKey(BreakfastItemIds.REPLICA_GENIE_BOTTLE_ID) ->
            BreakfastItemIds.REPLICA_GENIE_BOTTLE_ID
        else -> return
    }
    // Using the bottle triggers a choice adventure (select "for more wishes" = option 3)
    // Register a one-shot choice handler for choice #1 that picks option 3
    // Then call useItemRequest.use(bottleId, 1) — the adventure loop handles the choice
    useItemRequest.use(bottleId, 1).onSuccess {
        preferences.setBoolean(Preferences.POCKET_WISHES_USED, true)
    }
}
```

**Pocket wishes implementation note:** `UseItemRequest.use()` submits `inv_use.php?which=f&whichitem=<id>&ajax=1`.
The genie bottle redirects to a choice adventure page. Rather than routing through `AdventureManager.resolveChoice()`,
the simplest approach is to detect the choice response directly in `makePocketWishes()` and POST `choice.php`
with the wish option:
```kotlin
val response = useItemRequest.use(bottleId, 1).getOrNull() ?: return
if (response.contains("whichchoice")) {
    // option 3 = "for more wishes" per desktop GenieRequest
    genericPost("choice.php", mapOf("whichchoice" to "1", "option" to "3"))
}
preferences.setBoolean(Preferences.POCKET_WISHES_USED, true)
```
The implementer should verify choice ID `1` by checking the HTML returned from the genie bottle use.

**Boxing daydream** — direct URL visit:
```kotlin
private suspend fun haveBoxingDaydream() {
    if (preferences.getBoolean(Preferences.BOXING_DAYDREAM, false)) return
    genericGet("place.php?whichplace=town_wrong&action=townwrong_boxingdaycare").onSuccess { html ->
        // Response may be a choice adventure — if so, resolve it (pick option 1 = daydream)
        if (html.contains("whichchoice")) {
            // Submit choice: option=1
            genericPost("choice.php", mapOf("whichchoice" to "1261", "option" to "1"))
        }
        preferences.setBoolean(Preferences.BOXING_DAYDREAM, true)
    }
}
```

**Note:** The boxing daydream choice ID is 1261 based on desktop; the implementer should verify
by checking `ChoiceManager.java` or testing against a live session.

**Spinning wheel** — extend `CampgroundRequest`:
```kotlin
// In CampgroundRequest.kt:
suspend fun useSpinningWheel(): Result<String> =
    post("campground.php", mapOf("action" to "spinningwheel"))

// In BreakfastManager:
private suspend fun useSpinningWheel(inventoryState: InventoryState) {
    if (preferences.getBoolean(Preferences.SPINNING_WHEEL_USED, false)) return
    // Only if the spinning wheel item is present (as a campground item)
    // Desktop checks CampgroundRequest.haveWorkshed() — mobile can check
    // if any known workshed item ID is in inventory as a proxy
    campgroundRequest.useSpinningWheel().onSuccess {
        preferences.setBoolean(Preferences.SPINNING_WHEEL_USED, true)
    }
}
```

### Tier 3 — Additional infrastructure

**Big Island / Volcano Island / Server Room** — simple URL visits:
```kotlin
private suspend fun visitBigIsland() {
    if (preferences.getBoolean(Preferences.BIG_ISLAND_VISITED, false)) return
    genericGet("bigisland.php").onSuccess {
        preferences.setBoolean(Preferences.BIG_ISLAND_VISITED, true)
    }
}

private suspend fun visitVolcanoIsland() {
    if (preferences.getBoolean(Preferences.VOLCANO_ISLAND_VISITED, false)) return
    genericGet("place.php?whichplace=island_camp").onSuccess {
        preferences.setBoolean(Preferences.VOLCANO_ISLAND_VISITED, true)
    }
}

private suspend fun visitServerRoom() {
    if (preferences.getBoolean(Preferences.SERVER_ROOM_VISITED, false)) return
    genericGet("place.php?whichplace=airport_spooky_bunker").onSuccess {
        preferences.setBoolean(Preferences.SERVER_ROOM_VISITED, true)
    }
}
```

**Hardwood planks** — SpinMaster Lathe:
```kotlin
private suspend fun collectHardwood() {
    if (preferences.getBoolean(Preferences.HARDWOOD_COLLECTED, false)) return
    // SpinMaster Lathe URL: shop.php?whichshop=lathe
    genericGet("shop.php?whichshop=lathe").onSuccess {
        preferences.setBoolean(Preferences.HARDWOOD_COLLECTED, true)
    }
}
```

**Mr. Store 2002 credits**:
```kotlin
private suspend fun collect2002MrStoreCredits(inventoryState: InventoryState) {
    if (preferences.getBoolean(Preferences.MR_STORE_CREDITS_COLLECTED, false)) return
    val catalogId = when {
        inventoryState.items.containsKey(BreakfastItemIds.MR_STORE_2002_CATALOG_ID) ->
            BreakfastItemIds.MR_STORE_2002_CATALOG_ID
        inventoryState.items.containsKey(BreakfastItemIds.REPLICA_MR_STORE_CATALOG_ID) ->
            BreakfastItemIds.REPLICA_MR_STORE_CATALOG_ID
        else -> return
    }
    useItemRequest.use(catalogId, 1).onSuccess {
        preferences.setBoolean(Preferences.MR_STORE_CREDITS_COLLECTED, true)
    }
}
```

### runBreakfast() call order

Update `runBreakfast()` to call all methods in desktop order:
```kotlin
open suspend fun runBreakfast(charState: CharacterState, inventoryState: InventoryState) {
    if (preferences.getBoolean(Preferences.BREAKFAST_COMPLETED, false)) return
    val suffix = if (charState.isHardcore) "Hardcore" else "Softcore"
    harvestGarden(suffix)
    checkRumpusRoom(suffix)
    checkVIPLounge(suffix, inventoryState)
    readGuildManual(suffix, charState, inventoryState)
    getHermitClovers(inventoryState)
    collectHardwood()
    collect2002MrStoreCredits(inventoryState)
    collectAprilShowerGlobs(inventoryState)
    useSpinningWheel()
    visitBigIsland()
    visitVolcanoIsland()
    makePocketWishes(inventoryState)
    haveBoxingDaydream()
    useToys(inventoryState)
    collectAnticheese(inventoryState)
    visitServerRoom()
    // collectSeaJelly() — stub; familiar swap not yet implemented; log and skip
    harvestBatteries(inventoryState)
    useBookOfEverySkill(inventoryState)
    useReplicaBooks(inventoryState)
    makeHandheldRadios(inventoryState)
    preferences.setBoolean(Preferences.BREAKFAST_COMPLETED, true)
}
```

`collectSeaJelly()` remains a stub (log "sea jelly requires familiar swap — skipped").

### genericGet / genericPost injection

`BreakfastManager` needs a way to make generic HTTP GET and POST requests. The cleanest approach:
inject `HttpClient` directly (already in DI graph) and add two private helper methods:

```kotlin
class BreakfastManager(
    ...,
    private val hermitRequest: HermitRequest,
    private val httpClient: HttpClient,
    private val gameDatabase: GameDatabase,
) {
    private suspend fun genericGet(path: String): Result<String> = runCatching {
        httpClient.get("https://www.kingdomofloathing.com/$path").bodyAsText()
    }

    private suspend fun genericPost(path: String, params: Map<String, String>): Result<String> =
        runCatching {
            httpClient.submitForm(
                "https://www.kingdomofloathing.com/$path",
                formParameters = Parameters.build { params.forEach { (k, v) -> append(k, v) } }
            ).bodyAsText()
        }
}
```

---

## AT Song Slot Management

### EffectManager changes

Add AT song detection. Source of truth: `skills.txt` has a "type" column. Skills of type
`"SONG"` grant AT songs. The mobile `GameDatabase` already parses `skills.txt`; add:

```kotlin
// In GameDatabase.kt, during skills.txt parse:
if (skillType == "SONG") atSongEffectNames.add(effectGrantedBySkill)
```

`EffectManager` receives the `atSongEffectNames: Set<String>` at construction.

```kotlin
class EffectManager(
    private val atSongEffectNames: Set<String> = emptySet(),
) {
    // existing activeEffects: StateFlow<Map<String, Int>> (name → turns remaining)

    fun isAtSong(effectName: String): Boolean = effectName in atSongEffectNames

    fun activeAtSongs(): Set<String> =
        activeEffects.value.keys.filter { isAtSong(it) }.toSet()
}
```

**Fallback if skills.txt type column isn't available:** Hardcode the 40 classic AT songs:
```kotlin
// In BreakfastItemIds companion or a new AtSongDatabase.kt:
val KNOWN_AT_SONGS = setOf(
    "Polka of Plenty",
    "Fat Leon's Phat Loot Lyric",
    "The Moxious Madrigal",
    "Aloysius' Antiphon of Aptitude",
    "Carlweather's Cantata of Confrontation",
    "Ur-Kel's Aria of Annihilation",
    "The Psalm of Pointiness",
    "Jackasses' Symphony of Destruction",
    "The Magical Mojomuscular Melody",
    "Stevedave's Shanty of Superiority",
    "Ode to Booze",
    "The Ballad of Richardo",
    "Chorale of Companionship",
    "Dirge of Dreadfulness",
    "Elron's Explosive Etude",
    "Inigo's Incantation of Inspiration",
    "Phat Beach Boys",
    "Donho's Bubbly Ballad",
    "Suspicious Stench",
    "Antiphon of Aptitude",
    // ... implementer should check skills.txt for complete list
)
```

The implementer should prefer parsing `skills.txt` type column if feasible, with the hardcoded
set as fallback.

### KoLCharacter / CharacterState changes

`KoLCharacter.atSongLimit` is a computed property, **not** a `CharacterState` field (it depends
on class + skills, both of which are already accessible from `KoLCharacter`). No `CharacterState`
change needed.
```kotlin
val atSongLimit: Int get() {
    val state = _state.value
    if (!state.characterClassEnum.isAccordionThief) return 0
    var limit = 3  // base for AT
    if (skillManager.hasSkill("The Missing Accordion")) limit += 1
    if (skillManager.hasSkill("Accordion Appreciation")) limit += 1
    return limit
}
```

**Note:** The skill names `"The Missing Accordion"` and `"Accordion Appreciation"` should be
verified against the desktop `SkillDatabase`. The implementer should grep `skills.txt` for AT
song limit modifiers.

### MoodManager changes

In `executeMood()`, before casting any skill that grants an AT song:

```kotlin
// In MoodManager.executeMood() or the trigger evaluation loop:
for (trigger in triggers) {
    if (trigger.type == TriggerType.CAST_SKILL) {
        val grantedEffect = skillManager.grantedEffect(trigger.skillId)
        if (grantedEffect != null && effectManager.isAtSong(grantedEffect)) {
            val activeSongs = effectManager.activeAtSongs()
            val limit = character.atSongLimit
            if (limit > 0 && activeSongs.size >= limit) {
                // Evict the lowest-priority active AT song
                // "Lowest priority" = last trigger in the current mood's trigger list
                //  that is also an active song
                val toEvict = lowestPriorityActiveSong(activeSongs, triggers)
                if (toEvict != null) {
                    uneffectRequest.uneffect(toEvict)  // best-effort; log on failure
                }
            }
        }
    }
    // ... existing cast logic
}

private fun lowestPriorityActiveSong(
    activeSongs: Set<String>,
    triggers: List<MoodTrigger>,
): String? {
    // Return the active song that appears LAST in the trigger list
    // (lowest priority = bottom of the queue)
    // If no active song appears in triggers (externally added songs), return activeSongs.last()
    val triggerEffects = triggers
        .mapNotNull { skillManager.grantedEffect(it.skillId) }
        .filter { it in activeSongs }
    return if (triggerEffects.isNotEmpty()) triggerEffects.last()
    else activeSongs.lastOrNull()
}
```

**Note:** `skillManager.grantedEffect(skillId)` maps a skill ID to the name of the effect it
grants. This mapping may need to be added to `SkillManager` if not present. Check `skills.txt`
column layout for the granted-effect field.

---

## Error Handling

1. **Each breakfast action** is wrapped in `try/catch(Exception)`. Log a warning and continue.
   The outer `runBreakfast()` must never throw — it's called from session startup.

2. **Toy sentinel timing**: set `_toyUsed_$toyId = true` AFTER a successful response.
   If the item isn't in inventory, skip silently (not an error).

3. **AT song eviction failure**: if `uneffectRequest.uneffect()` returns failure, log the failure
   and skip the cast for this trigger. Do not retry in an eviction loop.

4. **clearBreakfastPrefs()**: per-toy sentinel clearing must iterate `BreakfastItemIds.TOYS.keys`.
   This is called at rollover from `SessionManager`. New boolean sentinels get `setBoolean(key, false)`.

---

## Testing

### BreakfastManager tests

Each new action gets one test that:
1. Mocks the relevant request class
2. Sets up inventory with the item present
3. Calls `runBreakfast()`
4. Asserts the request was called with the correct parameters
5. Asserts the sentinel pref is set

Example:
```kotlin
@Test fun hermitClovers_tradesWorthlessTrinket() = runTest {
    val mockHermit = mockk<HermitRequest>(relaxed = true)
    coEvery { mockHermit.trade(7, 24, 1) } returns Result.success("ok")
    val mgr = BreakfastManager(
        ..., hermitRequest = mockHermit, prefs = testPrefs, ...
    )
    val inventory = InventoryState(items = mapOf(7 to 1))  // worthless trinket
    mgr.runBreakfast(defaultCharState, inventory)
    coVerify { mockHermit.trade(7, 24, 1) }
    assertEquals(true, testPrefs.getBoolean("_cloverSought", false))
}
```

**Sentinel tests:**
```kotlin
@Test fun hermitClovers_skipsWhenAlreadyDone() = runTest {
    testPrefs.setBoolean("_cloverSought", true)
    val mockHermit = mockk<HermitRequest>(relaxed = true)
    mgr.runBreakfast(defaultCharState, inventory)
    coVerify(exactly = 0) { mockHermit.trade(any(), any(), any()) }
}
```

**Toys test:**
```kotlin
@Test fun useToys_callsUseForEachPresentToy() = runTest {
    val inventoryWithToys = InventoryState(items = mapOf(
        3092 to 1,  // hobby horse
        3093 to 1,  // ball-in-a-cup
    ))
    mgr.runBreakfast(defaultCharState, inventoryWithToys)
    coVerify { mockUseItem.use(3092, 1) }
    coVerify { mockUseItem.use(3093, 1) }
}

@Test fun useToys_skipsAlreadyUsedToy() = runTest {
    testPrefs.setBoolean("_toyUsed_3092", true)
    mgr.runBreakfast(defaultCharState, inventoryWithHobbyHorse)
    coVerify(exactly = 0) { mockUseItem.use(3092, any()) }
}
```

**Anticheese 5-day cooldown test:**
```kotlin
@Test fun anticheese_skipsWithin5Days() = runTest {
    testPrefs.setInt("lastAnticheeseDay", 10)
    val charState = defaultCharState.copy(currentDays = 13)  // only 3 days later
    mgr.runBreakfast(charState, inventoryWithAnticheese)
    coVerify(exactly = 0) { mockUseItem.use(142, any()) }
}

@Test fun anticheese_runsAfter5Days() = runTest {
    testPrefs.setInt("lastAnticheeseDay", 10)
    val charState = defaultCharState.copy(currentDays = 15)  // 5 days later
    mgr.runBreakfast(charState, inventoryWithAnticheese)
    coVerify { mockUseItem.use(142, 1) }
}
```

### AT Song tests

```kotlin
@Test fun isAtSong_detectsKnownSong() {
    val effectManager = EffectManager(atSongEffectNames = setOf("Polka of Plenty"))
    assertTrue(effectManager.isAtSong("Polka of Plenty"))
    assertFalse(effectManager.isAtSong("Mariachi Mood"))
}

@Test fun activeAtSongs_returnsOnlySongs() {
    val effectManager = EffectManager(atSongEffectNames = setOf("Polka of Plenty"))
    effectManager.updateEffects(mapOf("Polka of Plenty" to 10, "Strength of Ten Ettins" to 5))
    assertEquals(setOf("Polka of Plenty"), effectManager.activeAtSongs())
}

@Test fun atSongLimit_isThreeForBaseAT() {
    val character = KoLCharacter(...)
    // Set character class to Accordion Thief, no extra skills
    assertEquals(3, character.atSongLimit)
}

@Test fun atSongLimit_isZeroForNonAT() {
    val character = KoLCharacter(...)
    // Set character class to Seal Clubber
    assertEquals(0, character.atSongLimit)
}

@Test fun moodManager_evictsLowestPrioritySongBeforeCasting() = runTest {
    // Setup: 3 active songs at limit=3; trigger for a 4th song
    val mockUneffect = mockk<UneffectRequest>(relaxed = true)
    coEvery { mockUneffect.uneffect(any()) } returns Result.success("ok")
    // ... configure moodManager with songs at limit
    moodManager.executeMood(triggersWithFourthSong)
    coVerify { mockUneffect.uneffect("lowest priority song name") }
}
```

---

## Dependency Injection Changes (SharedModule.kt)

`BreakfastManager` now takes two additional params: `hermitRequest: HermitRequest` and
`httpClient: HttpClient` (and `gameDatabase: GameDatabase`). Update the Koin single:

```kotlin
single {
    BreakfastManager(
        campgroundRequest  = get(),
        clanRumpusRequest  = get(),
        clanLoungeRequest  = get(),
        preferences        = get(),
        useItemRequest     = get(),
        hermitRequest      = get(),    // NEW
        httpClient         = get(),    // NEW
        gameDatabase       = get(),    // NEW
    )
}
```

`EffectManager` now takes `atSongEffectNames: Set<String>` — inject from `GameDatabase.atSongEffects`
(the set populated during `skills.txt` parsing).

---

## Implementation Tasks

| Task | Description | Complexity |
|------|-------------|------------|
| T1 | Add pref key constants (19 new keys) | Trivial |
| T2 | `BreakfastItemIds.kt` — toy map + item ID constants | Trivial |
| T3 | Tier 1 breakfast actions (9 actions) | Easy |
| T4 | Tier 2 breakfast actions — toys + spinning wheel + anticheese | Moderate |
| T5 | Tier 2 breakfast actions — pocket wishes + boxing daydream | Moderate |
| T6 | Tier 3 breakfast actions (6 URL visits + Mr. Store + hardwood) | Moderate |
| T7 | AT song detection — `GameDatabase` skills.txt parsing + `EffectManager.isAtSong/activeAtSongs` | Moderate |
| T8 | AT song limit — `CharacterState.atSongLimit` + `KoLCharacter` computation | Easy |
| T9 | AT song eviction — `MoodManager.executeMood()` pre-cast check + `lowestPriorityActiveSong` | Moderate |
| T10 | DI wiring — `SharedModule.kt` + `clearBreakfastPrefs()` per-toy sentinel clearing | Easy |

Test command: `./gradlew :shared:jvmTest`

---

## Explicit Non-Goals (Phase 13)

- `collectSeaJelly` — familiar swap required; leave as stub
- `checkJackass` (Jackass Plumber arcade game) — leave as stub  
- Phase 14 will cover: 4 collection endpoints live (`get_closet`, `get_storage`, `get_stash`, `get_display`) + `buy()` / `retrieve_item()` infrastructure
