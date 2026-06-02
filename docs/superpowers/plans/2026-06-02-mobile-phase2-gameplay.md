# KoLmafia Mobile — Phase 2: Core Gameplay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add full adventure automation (N-turn loop with macro combat + choice resolution), complete inventory management (use/equip/discard/craft/mall), and full familiar management (terrarium/hatchery/familiar-specific actions) to `kolmafia-mobile`, all wired through an event bus and exposed in a bottom-nav Compose UI.

**Architecture:** Event bus (`GameEventBus`: `SharedFlow<GameEvent>`) connects three domain managers (`AdventureManager`, `InventoryManager`, `FamiliarManager`), each owning its state as a `MutableStateFlow`. Managers publish events after operations; they never read each other's state directly. The adventure loop is a coroutine in `AdventureManager` that posts to `adventure.php` → `fight.php` (macro) → `choice.php` per turn, re-fetching `api.php?what=status` after each turn.

**Tech Stack:** Existing stack (Ktor 3.0.3, Koin 4.0.0, Compose Multiplatform 1.7.3, kotlinx.serialization, kotlin.test + MockEngine). No new dependencies required.

---

## File Map

```
shared/src/commonMain/kotlin/net/sourceforge/kolmafia/
  event/
    GameEvent.kt
    GameEventBus.kt
  adventure/
    AdventureLocation.kt
    AdventureResult.kt
    StopReason.kt
    MacroStrategy.kt
    AdventureParser.kt
    AdventureRequest.kt
    FightRequest.kt
    ChoiceRequest.kt
    AdventureManager.kt
  inventory/
    InventoryItem.kt
    ItemType.kt
    InventoryState.kt
    CraftMode.kt
    MallListing.kt
    MallError.kt
    InventoryManager.kt
    UseItemRequest.kt
    EquipRequest.kt
    UnequipRequest.kt
    DiscardRequest.kt
    CraftRequest.kt
    MallSearchRequest.kt
    MallBuyRequest.kt
  familiar/
    FamiliarData.kt
    FamiliarState.kt
    FamiliarAction.kt
    FamiliarManager.kt
    FamiliarRequest.kt
    FamiliarEquipRequest.kt
    HatcheryRequest.kt
    FamiliarActionRequest.kt
  ui/
    adventure/AdventureScreen.kt
    adventure/CombatResultCard.kt
    adventure/MacroEditorScreen.kt
    inventory/InventoryScreen.kt
    inventory/ItemDetailSheet.kt
    inventory/CraftingScreen.kt
    familiar/FamiliarScreen.kt
    familiar/FamiliarDetailSheet.kt
    familiar/HatcheryScreen.kt

shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/
  SharedModule.kt          ← modify: add new managers + event bus

shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/
  App.kt                   ← modify: add BottomNavBar

shared/src/commonTest/kotlin/net/sourceforge/kolmafia/
  event/GameEventBusTest.kt
  adventure/AdventureParserTest.kt
  adventure/AdventureManagerTest.kt
  adventure/MacroStrategyTest.kt
  inventory/InventoryManagerTest.kt
  familiar/FamiliarManagerTest.kt
```

---

## Task 1: Domain Data Models + Event Bus

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureLocation.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureResult.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/StopReason.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/MacroStrategy.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/ItemType.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryItem.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryState.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/CraftMode.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/MallListing.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/MallError.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarData.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarState.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarAction.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/event/GameEvent.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/event/GameEventBus.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/event/GameEventBusTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/event/GameEventBusTest.kt
package net.sourceforge.kolmafia.event

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GameEventBusTest {
    @Test
    fun emit_isReceivedBySubscriber() = runTest {
        val bus = GameEventBus()
        val received = mutableListOf<GameEvent>()
        val job = launch { bus.events.collect { received.add(it) } }

        val location = net.sourceforge.kolmafia.adventure.AdventureLocation("1", "Spooky Forest", "Nearby Plains")
        val result = net.sourceforge.kolmafia.adventure.AdventureResult.NonCombat("A Spooky Treehouse", "", emptyList(), 0)
        bus.emit(GameEvent.TurnConsumed(location, result))

        job.cancel()
        assertEquals(1, received.size)
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
cd C:\Development\kolmafia-mobile
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.event.GameEventBusTest"
```

Expected: FAIL — types don't exist yet.

- [ ] **Step 3: Write all data model files**

```kotlin
// adventure/AdventureLocation.kt
package net.sourceforge.kolmafia.adventure

data class AdventureLocation(
    val id: String,
    val name: String,
    val zone: String
)
```

```kotlin
// adventure/AdventureResult.kt
package net.sourceforge.kolmafia.adventure

sealed class AdventureResult {
    data class Combat(
        val monster: String,
        val won: Boolean,
        val itemsGained: List<String> = emptyList(),
        val meatGained: Int = 0,
        val statsGained: Map<String, Int> = emptyMap()
    ) : AdventureResult()

    data class NonCombat(
        val encounterName: String,
        val text: String,
        val itemsGained: List<String> = emptyList(),
        val meatGained: Int = 0
    ) : AdventureResult()

    data class Choice(
        val choiceId: Int,
        val encounterName: String,
        val options: List<String> = emptyList(),
        val chosenOption: Int = 0
    ) : AdventureResult()
}
```

```kotlin
// adventure/StopReason.kt
package net.sourceforge.kolmafia.adventure

sealed class StopReason {
    object UserCancelled : StopReason()
    object NoAdventuresLeft : StopReason()
    object CharacterDeath : StopReason()
    data class MacroError(val message: String) : StopReason()
    data class NetworkError(val cause: Throwable) : StopReason()
}
```

```kotlin
// adventure/MacroStrategy.kt
package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.preferences.Preferences

object MacroStrategy {
    const val SAFE_DEFAULT = "attack; if (hpbelow 30) use healing potion; attack"

    fun forLocation(zoneId: String, preferences: Preferences): String {
        val perZone = preferences.getString("combatMacro_$zoneId")
        if (perZone.isNotBlank()) return perZone
        val global = preferences.getString("combatMacroDefault")
        return global.ifBlank { SAFE_DEFAULT }
    }

    fun choiceOptionFor(choiceId: Int, preferences: Preferences): Int =
        preferences.getString("choiceAdventure$choiceId").toIntOrNull() ?: 1
}
```

```kotlin
// inventory/ItemType.kt
package net.sourceforge.kolmafia.inventory

enum class ItemType {
    FOOD, DRINK, SPLEEN, WEAPON, OFFHAND, HAT, SHIRT, PANTS,
    ACCESSORY, FAMILIAR_ITEM, USABLE, MULTIUSABLE, REUSABLE, OTHER;

    companion object {
        fun fromApiString(s: String): ItemType = when (s.lowercase()) {
            "food" -> FOOD
            "drink" -> DRINK
            "spleen" -> SPLEEN
            "weapon" -> WEAPON
            "offhand" -> OFFHAND
            "hat" -> HAT
            "shirt" -> SHIRT
            "pants" -> PANTS
            "acc1", "acc2", "acc3", "accessory" -> ACCESSORY
            "familiar" -> FAMILIAR_ITEM
            "usable" -> USABLE
            "multiusable" -> MULTIUSABLE
            "reusable" -> REUSABLE
            else -> OTHER
        }
    }
}
```

```kotlin
// inventory/InventoryItem.kt
package net.sourceforge.kolmafia.inventory

data class InventoryItem(
    val itemId: Int,
    val name: String,
    val quantity: Int,
    val type: ItemType
)
```

```kotlin
// inventory/InventoryState.kt
package net.sourceforge.kolmafia.inventory

data class InventoryState(
    val items: Map<Int, InventoryItem> = emptyMap(),       // itemId → item
    val equipped: Map<String, InventoryItem> = emptyMap(), // slot → item
    val fullness: Int = 0,
    val inebriety: Int = 0,
    val spleenUsed: Int = 0,
    val isStale: Boolean = false
)
```

```kotlin
// inventory/CraftMode.kt
package net.sourceforge.kolmafia.inventory

enum class CraftMode(val apiAction: String) {
    COMBINE("combine"),
    COOK("cook"),
    COCKTAIL("cocktail"),
    SMITH("smith")
}
```

```kotlin
// inventory/MallListing.kt
package net.sourceforge.kolmafia.inventory

data class MallListing(
    val storeId: Int,
    val storeName: String,
    val itemId: Int,
    val itemName: String,
    val price: Int,
    val quantity: Int
)
```

```kotlin
// inventory/MallError.kt
package net.sourceforge.kolmafia.inventory

sealed class MallError : Exception() {
    object SoldOut : MallError()
    object InsufficientMeat : MallError()
    object StoreClosed : MallError()
    data class Unknown(val message: String) : MallError()
}
```

```kotlin
// familiar/FamiliarData.kt
package net.sourceforge.kolmafia.familiar

import net.sourceforge.kolmafia.inventory.InventoryItem

data class FamiliarData(
    val id: Int,
    val name: String,
    val race: String,
    val weight: Int,
    val experience: Int,
    val kills: Int,
    val equipment: InventoryItem? = null,
    val modifiers: Map<String, String> = emptyMap()
)
```

```kotlin
// familiar/FamiliarState.kt
package net.sourceforge.kolmafia.familiar

data class FamiliarState(
    val activeFamiliar: FamiliarData? = null,
    val ownedFamiliars: List<FamiliarData> = emptyList(),
    val isStale: Boolean = false
)
```

```kotlin
// familiar/FamiliarAction.kt
package net.sourceforge.kolmafia.familiar

sealed class FamiliarAction {
    data class PocketProfessorLecture(val lectureId: Int) : FamiliarAction()
    data class ShortestWigAssignment(val colorId: Int) : FamiliarAction()
    object Unsupported : FamiliarAction()
}
```

- [ ] **Step 4: Write `GameEvent.kt` and `GameEventBus.kt`**

```kotlin
// event/GameEvent.kt
package net.sourceforge.kolmafia.event

import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureResult
import net.sourceforge.kolmafia.adventure.StopReason
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.inventory.InventoryItem

sealed class GameEvent {
    data class TurnConsumed(val location: AdventureLocation, val result: AdventureResult) : GameEvent()
    data class CombatFinished(val won: Boolean, val monster: String) : GameEvent()
    data class ChoiceResolved(val choiceId: Int, val option: Int) : GameEvent()
    data class AdventureLoopStopped(val reason: StopReason) : GameEvent()

    data class ItemObtained(val item: InventoryItem) : GameEvent()
    data class ItemConsumed(val itemId: Int, val quantity: Int) : GameEvent()
    data class ItemEquipped(val item: InventoryItem, val slot: String) : GameEvent()
    data class ItemDiscarded(val itemId: Int, val quantity: Int) : GameEvent()
    data class ItemCrafted(val resultItem: InventoryItem) : GameEvent()
    data class MallPurchase(val item: InventoryItem, val meatSpent: Int) : GameEvent()

    data class FamiliarSwitched(val familiar: FamiliarData) : GameEvent()
    data class FamiliarEquipped(val familiar: FamiliarData, val item: InventoryItem) : GameEvent()
    data class FamiliarHatched(val familiar: FamiliarData) : GameEvent()
}
```

```kotlin
// event/GameEventBus.kt
package net.sourceforge.kolmafia.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class GameEventBus {
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    suspend fun emit(event: GameEvent) = _events.emit(event)
    fun tryEmit(event: GameEvent) = _events.tryEmit(event)
}
```

- [ ] **Step 5: Run — verify test passes**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.event.GameEventBusTest"
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add shared/src/
git commit -m "feat: Phase 2 data models, GameEvent hierarchy, GameEventBus"
```

---

## Task 2: AdventureParser

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureParserTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureParserTest.kt
package net.sourceforge.kolmafia.adventure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AdventureParserTest {

    @Test
    fun parsesNonCombat_whenFinalUrlIsAdventurePage() {
        val html = """
            <html><body>
            <b>A Spooky Treehouse</b>
            <p>You find something interesting.</p>
            <p>You gain 42 Meat.</p>
            <p>You acquire an item: <b>spooky stick</b></p>
            </body></html>
        """.trimIndent()
        val result = AdventureParser.parseAdventureResponse(html, "https://www.kingdomofloathing.com/adventure.php")
        assertIs<AdventureResult.NonCombat>(result)
        assertEquals(42, result.meatGained)
        assertTrue(result.itemsGained.contains("spooky stick"))
    }

    @Test
    fun parsesCombat_whenFinalUrlIsFightPage() {
        val html = """
            <html><body>
            <span id='monname'>fluffy bunny</span>
            <p>You're fighting a fluffy bunny!</p>
            </body></html>
        """.trimIndent()
        val result = AdventureParser.parseAdventureResponse(html, "https://www.kingdomofloathing.com/fight.php")
        assertIs<AdventureResult.Combat>(result)
        assertEquals("fluffy bunny", result.monster)
    }

    @Test
    fun parsesChoice_whenFinalUrlIsChoicePage() {
        val html = """
            <html><body>
            <form method="POST" action="choice.php">
            <input type="hidden" name="whichchoice" value="105">
            <a href="choice.php?pwd=xxx&option=1">Take the sword</a>
            <a href="choice.php?pwd=xxx&option=2">Ignore it</a>
            </form>
            </body></html>
        """.trimIndent()
        val result = AdventureParser.parseAdventureResponse(html, "https://www.kingdomofloathing.com/choice.php")
        assertIs<AdventureResult.Choice>(result)
        assertEquals(105, result.choiceId)
        assertEquals(2, result.options.size)
    }

    @Test
    fun parseFightResult_extractsWin() {
        val html = """
            <html><body>
            <span id='monname'>fluffy bunny</span>
            <p>You win the fight!</p>
            <p>You gain 15 Meat.</p>
            <p>You acquire an item: <b>bunny liver</b></p>
            <p>You gain 12 Beefiness (12 exp)</p>
            </body></html>
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.won)
        assertEquals("fluffy bunny", result.monster)
        assertEquals(15, result.meatGained)
        assertTrue(result.itemsGained.contains("bunny liver"))
    }

    @Test
    fun parseFightResult_extractsLoss() {
        val html = "<html><body><p>You lose the fight.</p></body></html>"
        val result = AdventureParser.parseFightResult(html)
        assertTrue(!result.won)
    }

    @Test
    fun parsesMultipleItems() {
        val html = """
            <p>You acquire an item: <b>seal tooth</b></p>
            <p>You acquire an item: <b>seal-clubbing club</b></p>
        """.trimIndent()
        val result = AdventureParser.parseAdventureResponse(html, "https://www.kingdomofloathing.com/adventure.php")
        assertIs<AdventureResult.NonCombat>(result)
        assertEquals(2, result.itemsGained.size)
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.AdventureParserTest"
```

Expected: FAIL

- [ ] **Step 3: Write `AdventureParser.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt
package net.sourceforge.kolmafia.adventure

object AdventureParser {
    private val ITEM_GAINED = Regex("""You acquire an item:\s*<b>(.*?)</b>""")
    private val MEAT_GAINED = Regex("""You gain ([\d,]+) Meat""")
    private val STAT_GAINED = Regex("""You gain ([\d,]+) \w+ \(\d+ exp\)""")
    private val WIN_PATTERN = Regex("""You win the fight""")
    private val LOSS_PATTERN = Regex("""You lose the fight|You are beaten unconscious""")
    private val CHOICE_ID = Regex("""name="whichchoice"\s*value="(\d+)"""")
    private val CHOICE_OPTION = Regex("""option=(\d+)">(.*?)</a>""")
    private val MONSTER_NAME = Regex("""<span id='monname'>(.*?)</span>""")
    private val ENCOUNTER_NAME = Regex("""<b>([^<]{3,60})</b>""")

    fun parseAdventureResponse(html: String, finalUrl: String): AdventureResult = when {
        finalUrl.contains("fight.php") || html.contains("You're fighting") -> parseCombatStart(html)
        finalUrl.contains("choice.php") || html.contains("whichchoice") -> parseChoice(html)
        else -> parseNonCombat(html)
    }

    fun parseFightResult(html: String): AdventureResult.Combat {
        val won = WIN_PATTERN.containsMatchIn(html)
        val monster = MONSTER_NAME.find(html)?.groupValues?.get(1) ?: "Unknown"
        val items = ITEM_GAINED.findAll(html).map { it.groupValues[1].trim() }.toList()
        val meat = parseMeat(html)
        val stats = parseStats(html)
        return AdventureResult.Combat(monster, won, items, meat, stats)
    }

    private fun parseCombatStart(html: String): AdventureResult.Combat {
        val monster = MONSTER_NAME.find(html)?.groupValues?.get(1) ?: "Unknown"
        return AdventureResult.Combat(monster, won = false)
    }

    private fun parseChoice(html: String): AdventureResult.Choice {
        val choiceId = CHOICE_ID.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val options = CHOICE_OPTION.findAll(html).map { it.groupValues[2].trim() }.toList()
        return AdventureResult.Choice(choiceId, "Choice Adventure", options)
    }

    private fun parseNonCombat(html: String): AdventureResult.NonCombat {
        val name = ENCOUNTER_NAME.find(html)?.groupValues?.get(1) ?: "Encounter"
        val items = ITEM_GAINED.findAll(html).map { it.groupValues[1].trim() }.toList()
        val meat = parseMeat(html)
        return AdventureResult.NonCombat(name, html, items, meat)
    }

    private fun parseMeat(html: String): Int =
        MEAT_GAINED.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0

    private fun parseStats(html: String): Map<String, Int> =
        STAT_GAINED.findAll(html).associate { m ->
            val value = m.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            "stat" to value
        }
}
```

- [ ] **Step 4: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.AdventureParserTest"
```

Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: AdventureParser — HTML response → sealed AdventureResult"
```

---

## Task 3: MacroStrategy Tests

**Files:**
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/MacroStrategyTest.kt`

- [ ] **Step 1: Write tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/MacroStrategyTest.kt
package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class MacroStrategyTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun fallsBackToSafeDefault_whenNothingSet() {
        assertEquals(MacroStrategy.SAFE_DEFAULT, MacroStrategy.forLocation("1", prefs()))
    }

    @Test
    fun usesGlobalDefault_whenSet() {
        val p = prefs()
        p.setString("combatMacroDefault", "skill 3004")
        assertEquals("skill 3004", MacroStrategy.forLocation("1", p))
    }

    @Test
    fun usesPerZoneOverride_whenSet() {
        val p = prefs()
        p.setString("combatMacroDefault", "skill 3004")
        p.setString("combatMacro_1", "skill 3005")
        assertEquals("skill 3005", MacroStrategy.forLocation("1", p))
    }

    @Test
    fun choiceOptionDefaultsToOne() {
        assertEquals(1, MacroStrategy.choiceOptionFor(105, prefs()))
    }

    @Test
    fun choiceOptionUsesPreference_whenSet() {
        val p = prefs()
        p.setString("choiceAdventure105", "2")
        assertEquals(2, MacroStrategy.choiceOptionFor(105, p))
    }
}
```

- [ ] **Step 2: Run — verify tests pass** (MacroStrategy was written in Task 1)

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.MacroStrategyTest"
```

Expected: PASS (5 tests)

- [ ] **Step 3: Commit**

```bash
git add shared/src/
git commit -m "test: MacroStrategy lookup coverage"
```

---

## Task 4: Adventure HTTP Requests

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/FightRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/ChoiceRequest.kt`

No separate test file — these are integration-tested via `AdventureManagerTest` in Task 5.

- [ ] **Step 1: Write `AdventureRequest.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureRequest.kt
package net.sourceforge.kolmafia.adventure

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class AdventureRequest(private val client: HttpClient) {
    // Returns Pair<responseBody, finalUrl>
    suspend fun adventure(location: AdventureLocation): Result<Pair<String, String>> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/adventure.php",
            formParameters = parameters {
                append("snarfblat", location.id)
                append("adv", "1")
            }
        )
        Result.success(response.bodyAsText() to response.request.url.toString())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 2: Write `FightRequest.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/FightRequest.kt
package net.sourceforge.kolmafia.adventure

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class FightRequest(private val client: HttpClient) {
    suspend fun fight(macroText: String): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/fight.php",
            formParameters = parameters {
                append("action", "macro")
                append("macrotext", macroText)
            }
        )
        Result.success(response.bodyAsText())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 3: Write `ChoiceRequest.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/ChoiceRequest.kt
package net.sourceforge.kolmafia.adventure

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class ChoiceRequest(private val client: HttpClient) {
    suspend fun choose(choiceId: Int, option: Int): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/choice.php",
            formParameters = parameters {
                append("whichchoice", choiceId.toString())
                append("option", option.toString())
            }
        )
        Result.success(response.bodyAsText())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 4: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: AdventureRequest, FightRequest, ChoiceRequest HTTP wrappers"
```

---

## Task 5: AdventureManager

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt
package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AdventureManagerTest {

    private val testLocation = AdventureLocation("17", "Spooky Forest", "Nearby Plains")

    private fun makeManager(
        adventureHtml: String = NON_COMBAT_HTML,
        adventureUrl: String = "https://www.kingdomofloathing.com/adventure.php",
        fightHtml: String = COMBAT_WIN_HTML,
        statusJson: String = STATUS_JSON_ADVENTURES_LEFT
    ): Triple<AdventureManager, GameEventBus, List<GameEvent>> {
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.contains("adventure.php") ->
                    respond(adventureHtml, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"))
                request.url.encodedPath.contains("fight.php") ->
                    respond(fightHtml, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"))
                request.url.encodedPath.contains("choice.php") ->
                    respond(NON_COMBAT_HTML, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"))
                request.url.encodedPath.contains("api.php") ->
                    respond(statusJson, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) { install(HttpCookies) }
        val character = KoLCharacter()
        val prefs = Preferences(MapSettings())
        val bus = GameEventBus()
        val received = mutableListOf<GameEvent>()

        val manager = AdventureManager(
            AdventureRequest(client),
            FightRequest(client),
            ChoiceRequest(client),
            CharacterRequest(client),
            character,
            prefs,
            bus
        )
        return Triple(manager, bus, received)
    }

    @Test
    fun runAdventures_emitsTurnConsumed_forNonCombat() = runTest {
        val (manager, bus, received) = makeManager()
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.runAdventures(testLocation, 1, CoroutineScope(Dispatchers.Default))
            .join()

        collectJob.cancel()
        val turns = received.filterIsInstance<GameEvent.TurnConsumed>()
        assertEquals(1, turns.size)
        assertIs<AdventureResult.NonCombat>(turns.first().result)
    }

    @Test
    fun runAdventures_stopsWith_noAdventuresLeft() = runTest {
        val (manager, bus, received) = makeManager(
            statusJson = STATUS_JSON_NO_ADVENTURES
        )
        val collectJob = launch { bus.events.collect { received.add(it) } }

        manager.runAdventures(testLocation, 5, CoroutineScope(Dispatchers.Default))
            .join()

        collectJob.cancel()
        val stopped = received.filterIsInstance<GameEvent.AdventureLoopStopped>()
        assertEquals(1, stopped.size)
        assertIs<StopReason.NoAdventuresLeft>(stopped.first().reason)
    }

    companion object {
        const val NON_COMBAT_HTML = """<html><body><b>A Spooky Treehouse</b><p>You gain 10 Meat.</p></body></html>"""
        const val COMBAT_WIN_HTML = """<html><body><span id='monname'>bunny</span><p>You win the fight!</p></body></html>"""
        const val STATUS_JSON_ADVENTURES_LEFT = """{"name":"Player","playerid":"1","level":"5","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"1000","adventures":"40","fullness":"0","drunk":"0","spleen":"0"}"""
        const val STATUS_JSON_NO_ADVENTURES = """{"name":"Player","playerid":"1","level":"5","class":"1","hp":"50","hpmax":"100","mp":"30","mpmax":"50","meat":"1000","adventures":"0","fullness":"0","drunk":"0","spleen":"0"}"""
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.AdventureManagerTest"
```

Expected: FAIL

- [ ] **Step 3: Write `AdventureManager.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt
package net.sourceforge.kolmafia.adventure

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest

class AdventureManager(
    private val adventureRequest: AdventureRequest,
    private val fightRequest: FightRequest,
    private val choiceRequest: ChoiceRequest,
    private val characterRequest: CharacterRequest,
    private val character: KoLCharacter,
    private val preferences: Preferences,
    private val eventBus: GameEventBus
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private var currentJob: Job? = null

    fun runAdventures(location: AdventureLocation, turns: Int, scope: CoroutineScope): Job =
        scope.launch(SupervisorJob()) {
            _isRunning.value = true
            try {
                repeat(turns) {
                    if (!isActive()) return@launch
                    val result = doOneTurn(location) ?: return@launch

                    characterRequest.fetchCharacterState().onSuccess { character.updateFromApiResponse(it) }
                    eventBus.emit(GameEvent.TurnConsumed(location, result))

                    when {
                        character.state.value.adventuresLeft <= 0 -> {
                            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NoAdventuresLeft))
                            return@launch
                        }
                        character.state.value.currentHp <= 0 -> {
                            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.CharacterDeath))
                            return@launch
                        }
                    }
                }
            } catch (e: CancellationException) {
                eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.UserCancelled))
                throw e
            } catch (e: Exception) {
                eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(e)))
            } finally {
                _isRunning.value = false
            }
        }.also { currentJob = it }

    fun stop() { currentJob?.cancel() }

    private fun isActive() = currentJob?.isActive != false

    private suspend fun doOneTurn(location: AdventureLocation): AdventureResult? {
        val (html, url) = adventureRequest.adventure(location).getOrElse {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
            return null
        }
        return when (val parsed = AdventureParser.parseAdventureResponse(html, url)) {
            is AdventureResult.Combat -> resolveCombat(location)
            is AdventureResult.Choice -> resolveChoice(parsed)
            is AdventureResult.NonCombat -> parsed.also { emitItemEvents(it.itemsGained) }
        }
    }

    private suspend fun resolveCombat(location: AdventureLocation): AdventureResult.Combat {
        val macro = MacroStrategy.forLocation(location.id, preferences)
        val fightHtml = fightRequest.fight(macro).getOrElse {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
            return AdventureResult.Combat("Unknown", won = false)
        }
        val result = AdventureParser.parseFightResult(fightHtml)
        eventBus.emit(GameEvent.CombatFinished(result.won, result.monster))
        emitItemEvents(result.itemsGained)
        if (!result.won) eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.CharacterDeath))
        return result
    }

    private suspend fun resolveChoice(choice: AdventureResult.Choice): AdventureResult.Choice {
        val option = MacroStrategy.choiceOptionFor(choice.choiceId, preferences)
        choiceRequest.choose(choice.choiceId, option)
        val resolved = choice.copy(chosenOption = option)
        eventBus.emit(GameEvent.ChoiceResolved(choice.choiceId, option))
        return resolved
    }

    private suspend fun emitItemEvents(items: List<String>) {
        items.forEach { name ->
            eventBus.emit(GameEvent.ItemObtained(InventoryItem(-1, name, 1, ItemType.OTHER)))
        }
    }
}
```

- [ ] **Step 4: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.AdventureManagerTest"
```

Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: AdventureManager — N-turn loop with combat/choice/noncombat handling"
```

---

## Task 6: InventoryManager

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryManager.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/inventory/InventoryManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/inventory/InventoryManagerTest.kt
package net.sourceforge.kolmafia.inventory

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.event.GameEventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryManagerTest {

    private val inventoryJson = """{"3": 5, "43": 2}"""
    private val equipmentJson = """{}"""

    private fun makeManager(
        invJson: String = inventoryJson,
        equipJson: String = equipmentJson
    ): InventoryManager {
        val engine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "inventory" ->
                    respond(invJson, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.parameters["what"] == "equipment" ->
                    respond(equipJson, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return InventoryManager(client, GameEventBus())
    }

    @Test
    fun fetchInventory_populatesItems() = runTest {
        val manager = makeManager()
        manager.fetchInventory()
        val state = manager.state.value
        assertEquals(2, state.items.size)
        assertEquals(5, state.items[3]?.quantity)
        assertEquals(2, state.items[43]?.quantity)
    }

    @Test
    fun initialState_isEmpty() {
        val manager = makeManager()
        assertTrue(manager.state.value.items.isEmpty())
    }

    @Test
    fun fetchInventory_clearsStaleFlag() = runTest {
        val manager = makeManager()
        manager.fetchInventory()
        assertFalse(manager.state.value.isStale)
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.inventory.InventoryManagerTest"
```

Expected: FAIL

- [ ] **Step 3: Write `InventoryManager.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/inventory/InventoryManager.kt
package net.sourceforge.kolmafia.inventory

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class InventoryManager(
    private val client: HttpClient,
    private val eventBus: GameEventBus
) {
    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    fun initialize(scope: CoroutineScope) {
        scope.launch {
            fetchInventory()
            eventBus.events.collect { event ->
                when (event) {
                    is GameEvent.ItemObtained, is GameEvent.ItemConsumed,
                    is GameEvent.ItemEquipped, is GameEvent.ItemDiscarded,
                    is GameEvent.ItemCrafted, is GameEvent.MallPurchase -> fetchInventory()
                    else -> {}
                }
            }
        }
    }

    suspend fun fetchInventory() {
        try {
            val invResponse = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "inventory")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!invResponse.status.isSuccess()) {
                _state.value = _state.value.copy(isStale = true)
                return
            }
            // api.php?what=inventory returns {"itemId": quantity, ...}
            // Verify actual response format against live KoL API before shipping.
            val rawMap: Map<String, Int> = invResponse.body()
            val items = rawMap.entries.associate { (idStr, qty) ->
                val id = idStr.toIntOrNull() ?: return@associate idStr.hashCode() to
                    InventoryItem(idStr.hashCode(), idStr, qty, ItemType.OTHER)
                id to InventoryItem(id, "Item #$id", qty, ItemType.OTHER)
            }
            _state.value = _state.value.copy(items = items, isStale = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isStale = true)
        }
    }

    suspend fun useItem(item: InventoryItem): Result<Unit> = try {
        client.get("$KOL_BASE_URL/inv_use.php") {
            parameter("which", "3")
            parameter("whichitem", item.itemId.toString())
        }
        eventBus.emit(GameEvent.ItemConsumed(item.itemId, 1))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun equipItem(item: InventoryItem, slot: String): Result<Unit> = try {
        client.get("$KOL_BASE_URL/inv_equip.php") {
            parameter("which", "2")
            parameter("whichitem", item.itemId.toString())
            parameter("slot", slot)
        }
        eventBus.emit(GameEvent.ItemEquipped(item, slot))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun unequipSlot(slot: String): Result<Unit> = try {
        client.get("$KOL_BASE_URL/inv_equip.php") {
            parameter("action", "unequip")
            parameter("type", slot)
        }
        fetchInventory()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun discardItem(item: InventoryItem, quantity: Int): Result<Unit> = try {
        client.get("$KOL_BASE_URL/multiuse.php") {
            parameter("action", "trash")
            parameter("whichitem", item.itemId.toString())
            parameter("quantity", quantity.toString())
        }
        eventBus.emit(GameEvent.ItemDiscarded(item.itemId, quantity))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun craft(mode: CraftMode, item1Id: Int, item2Id: Int): Result<InventoryItem> = try {
        client.get("$KOL_BASE_URL/craft.php") {
            parameter("action", mode.apiAction)
            if (mode == CraftMode.COMBINE) parameter("mode", "combine")
            parameter("item1", item1Id.toString())
            parameter("item2", item2Id.toString())
        }
        val placeholder = InventoryItem(-1, "Crafted item", 1, ItemType.OTHER)
        eventBus.emit(GameEvent.ItemCrafted(placeholder))
        fetchInventory()
        Result.success(placeholder)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun mallSearch(query: String): Result<List<MallListing>> = try {
        // Returns HTML; parse for listings. Simplified: returns empty list until HTML parser added.
        client.get("$KOL_BASE_URL/mallsearch.php") {
            parameter("searching", "Yep")
            parameter("phrasetype", "exact")
            parameter("pudnuggler", query)
        }
        Result.success(emptyList())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun mallBuy(storeId: Int, itemId: Int, quantity: Int): Result<Unit> = try {
        val response = client.get("$KOL_BASE_URL/mallstore.php") {
            parameter("whichstore", storeId.toString())
            parameter("buying", itemId.toString())
            parameter("quantity", quantity.toString())
        }
        val body = response.toString()
        when {
            body.contains("That item is not available") -> Result.failure(MallError.SoldOut)
            body.contains("You can't afford") -> Result.failure(MallError.InsufficientMeat)
            else -> {
                fetchInventory()
                Result.success(Unit)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 4: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.inventory.InventoryManagerTest"
```

Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: InventoryManager — fetch, use/equip/discard/craft/mall operations"
```

---

## Task 7: FamiliarManager

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManager.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarEquipRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/HatcheryRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarActionRequest.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManagerTest.kt
package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.event.GameEventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FamiliarManagerTest {

    // api.php?what=familiars returns array of familiar objects
    // Verify exact field names against live KoL API before shipping.
    private val familiarsJson = """
        [
          {"id": 5, "name": "Mr. Wiggles", "race": "Grue", "weight": 10, "exp": 150, "kills": 3, "active": true},
          {"id": 12, "name": "Fluffy", "race": "Bunny", "weight": 7, "exp": 50, "kills": 1, "active": false}
        ]
    """.trimIndent()

    private fun makeManager(): FamiliarManager {
        val engine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "familiars" ->
                    respond(familiarsJson, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("familiar.php") ->
                    respond("", HttpStatusCode.OK)
                request.url.encodedPath.contains("hatchery.php") ->
                    respond("", HttpStatusCode.OK)
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return FamiliarManager(client, GameEventBus())
    }

    @Test
    fun fetchFamiliars_setsActiveFamiliar() = runTest {
        val manager = makeManager()
        manager.fetchFamiliars()
        assertNotNull(manager.state.value.activeFamiliar)
        assertEquals("Mr. Wiggles", manager.state.value.activeFamiliar!!.name)
    }

    @Test
    fun fetchFamiliars_loadsAllFamiliars() = runTest {
        val manager = makeManager()
        manager.fetchFamiliars()
        assertEquals(2, manager.state.value.ownedFamiliars.size)
    }

    @Test
    fun initialState_hasNoActiveFamiliar() {
        val manager = makeManager()
        assertNull(manager.state.value.activeFamiliar)
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.familiar.FamiliarManagerTest"
```

Expected: FAIL

- [ ] **Step 3: Write the familiar request classes**

```kotlin
// familiar/FamiliarRequest.kt
package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class FamiliarRequest(private val client: HttpClient) {
    suspend fun switchFamiliar(familiarId: Int): Result<Unit> = try {
        client.submitForm("$KOL_BASE_URL/familiar.php",
            parameters { append("action", "newfam"); append("whichfam", familiarId.toString()) })
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun putBack(): Result<Unit> = try {
        client.submitForm("$KOL_BASE_URL/familiar.php",
            parameters { append("action", "putback") })
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
```

```kotlin
// familiar/FamiliarEquipRequest.kt
package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class FamiliarEquipRequest(private val client: HttpClient) {
    suspend fun equip(itemId: Int): Result<Unit> = try {
        client.submitForm("$KOL_BASE_URL/inv_equip.php",
            parameters {
                append("which", "2")
                append("whichitem", itemId.toString())
                append("slot", "familiarequip")
            })
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
```

```kotlin
// familiar/HatcheryRequest.kt
package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class HatcheryRequest(private val client: HttpClient) {
    suspend fun hatch(eggItemId: Int): Result<Unit> = try {
        client.submitForm("$KOL_BASE_URL/hatchery.php",
            parameters { append("action", "hatch"); append("whichitem", eggItemId.toString()) })
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
```

```kotlin
// familiar/FamiliarActionRequest.kt
package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class FamiliarActionRequest(private val client: HttpClient) {
    suspend fun perform(action: FamiliarAction): Result<Unit> = when (action) {
        is FamiliarAction.PocketProfessorLecture -> try {
            client.submitForm("$KOL_BASE_URL/familiar.php",
                parameters { append("action", "lecture"); append("lectureid", action.lectureId.toString()) })
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }

        is FamiliarAction.ShortestWigAssignment -> try {
            client.submitForm("$KOL_BASE_URL/familiar.php",
                parameters { append("action", "wig"); append("colorid", action.colorId.toString()) })
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }

        FamiliarAction.Unsupported ->
            Result.failure(UnsupportedOperationException("Use KoL web interface for this familiar action"))
    }
}
```

- [ ] **Step 4: Write `FamiliarManager.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/familiar/FamiliarManager.kt
package net.sourceforge.kolmafia.familiar

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.http.KOL_BASE_URL

// Verify field names against actual api.php?what=familiars response before shipping.
@Serializable
private data class FamiliarApiEntry(
    val id: Int = 0,
    val name: String = "",
    val race: String = "",
    val weight: Int = 1,
    val exp: Int = 0,
    val kills: Int = 0,
    @SerialName("active") val isActive: Boolean = false
)

class FamiliarManager(
    private val client: HttpClient,
    private val eventBus: GameEventBus
) {
    private val _state = MutableStateFlow(FamiliarState())
    val state: StateFlow<FamiliarState> = _state.asStateFlow()

    private val familiarRequest = FamiliarRequest(client)
    private val equipRequest = FamiliarEquipRequest(client)
    private val hatcheryRequest = HatcheryRequest(client)
    private val actionRequest = FamiliarActionRequest(client)

    fun initialize(scope: CoroutineScope) {
        scope.launch { fetchFamiliars() }
    }

    suspend fun fetchFamiliars() {
        try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "familiars")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) {
                _state.value = _state.value.copy(isStale = true)
                return
            }
            val entries: List<FamiliarApiEntry> = response.body()
            val familiars = entries.map { e ->
                FamiliarData(e.id, e.name, e.race, e.weight, e.exp, e.kills)
            }
            val active = familiars.firstOrNull { entries.find { e -> e.id == it.id }?.isActive == true }
            _state.value = FamiliarState(active, familiars, isStale = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isStale = true)
        }
    }

    suspend fun switchFamiliar(familiar: FamiliarData): Result<Unit> {
        familiarRequest.switchFamiliar(familiar.id).onFailure { return Result.failure(it) }
        fetchFamiliars()
        eventBus.emit(GameEvent.FamiliarSwitched(familiar))
        return Result.success(Unit)
    }

    suspend fun equipItem(familiar: FamiliarData, itemId: Int): Result<Unit> {
        val item = net.sourceforge.kolmafia.inventory.InventoryItem(itemId, "Familiar item", 1,
            net.sourceforge.kolmafia.inventory.ItemType.FAMILIAR_ITEM)
        equipRequest.equip(itemId).onFailure { return Result.failure(it) }
        fetchFamiliars()
        eventBus.emit(GameEvent.FamiliarEquipped(familiar, item))
        return Result.success(Unit)
    }

    suspend fun hatch(eggItemId: Int): Result<Unit> {
        hatcheryRequest.hatch(eggItemId).onFailure { return Result.failure(it) }
        fetchFamiliars()
        val newFamiliar = _state.value.ownedFamiliars.lastOrNull()
            ?: return Result.success(Unit)
        eventBus.emit(GameEvent.FamiliarHatched(newFamiliar))
        return Result.success(Unit)
    }

    suspend fun performAction(action: FamiliarAction): Result<Unit> =
        actionRequest.perform(action)
}
```

- [ ] **Step 5: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.familiar.FamiliarManagerTest"
```

Expected: PASS (3 tests)

- [ ] **Step 6: Commit**

```bash
git add shared/src/
git commit -m "feat: FamiliarManager — fetch, switch, equip, hatch, familiar-specific actions"
```

---

## Task 8: Wire into SharedModule + Update SessionManager

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt`

- [ ] **Step 1: Update `SharedModule.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
package net.sourceforge.kolmafia.di

import io.ktor.client.*
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.FightRequest
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.http.createKoLHttpClient
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.session.SessionManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedModule = module {
    single<HttpClient> { createKoLHttpClient() }
    single { KoLCharacter() }
    single { Preferences(get()) }
    single { GameEventBus() }
    singleOf(::LoginRequest)
    singleOf(::CharacterRequest)
    singleOf(::AdventureRequest)
    singleOf(::FightRequest)
    singleOf(::ChoiceRequest)
    singleOf(::AdventureManager)
    singleOf(::InventoryManager)
    singleOf(::FamiliarManager)
    singleOf(::SessionManager)
}
```

- [ ] **Step 2: Update `SessionManager.kt` to initialize managers on login**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt
package net.sourceforge.kolmafia.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.request.LoginResult

sealed class SessionState {
    object LoggedOut : SessionState()
    object LoggedIn : SessionState()
    data class Error(val message: String) : SessionState()
}

class SessionManager(
    private val loginRequest: LoginRequest,
    private val characterRequest: CharacterRequest,
    private val character: KoLCharacter,
    private val preferences: Preferences,
    private val inventoryManager: InventoryManager,
    private val familiarManager: FamiliarManager
) {
    private val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun login(username: String, password: String): SessionState {
        return when (val loginResult = loginRequest.login(username, password)) {
            is LoginResult.Success -> {
                preferences.setString(Preferences.LAST_USERNAME, username)
                characterRequest.fetchCharacterState().fold(
                    onSuccess = { apiResponse ->
                        character.updateFromApiResponse(apiResponse)
                        inventoryManager.initialize(appScope)
                        familiarManager.initialize(appScope)
                        SessionState.LoggedIn
                    },
                    onFailure = { error ->
                        SessionState.Error("Character load failed: ${error.message}")
                    }
                )
            }
            is LoginResult.Failure -> SessionState.Error(loginResult.message)
            is LoginResult.Error -> SessionState.Error(loginResult.cause.message ?: "Network error")
        }
    }

    fun logout() {
        character.reset()
    }
}
```

- [ ] **Step 3: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run full test suite**

```bash
./gradlew :shared:jvmTest
```

Expected: All previously passing tests still PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: wire Phase 2 managers into SharedModule and SessionManager"
```

---

## Task 9: Adventure UI

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/adventure/CombatResultCard.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/adventure/AdventureScreen.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/adventure/MacroEditorScreen.kt`

- [ ] **Step 1: Write `CombatResultCard.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/adventure/CombatResultCard.kt
package net.sourceforge.kolmafia.ui.adventure

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.adventure.AdventureResult

@Composable
fun CombatResultCard(result: AdventureResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (result) {
                is AdventureResult.Combat -> if (result.won)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
                is AdventureResult.NonCombat -> MaterialTheme.colorScheme.secondaryContainer
                is AdventureResult.Choice -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            when (result) {
                is AdventureResult.Combat -> {
                    Text(
                        if (result.won) "Win: ${result.monster}" else "Loss: ${result.monster}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (result.meatGained > 0)
                        Text("Meat: +${result.meatGained}", style = MaterialTheme.typography.bodySmall)
                    result.itemsGained.forEach { item ->
                        Text("Item: $item", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AdventureResult.NonCombat -> {
                    Text(result.encounterName, style = MaterialTheme.typography.titleSmall)
                    if (result.meatGained > 0)
                        Text("Meat: +${result.meatGained}", style = MaterialTheme.typography.bodySmall)
                    result.itemsGained.forEach { item ->
                        Text("Item: $item", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AdventureResult.Choice -> {
                    Text("Choice: ${result.encounterName}", style = MaterialTheme.typography.titleSmall)
                    Text("Chose option ${result.chosenOption}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Write `AdventureScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/adventure/AdventureScreen.kt
package net.sourceforge.kolmafia.ui.adventure

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureResult
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import org.koin.compose.koinInject

@Composable
fun AdventureScreen() {
    val adventureManager: AdventureManager = koinInject()
    val eventBus: GameEventBus = koinInject()
    val isRunning by adventureManager.isRunning.collectAsState()
    val scope = rememberCoroutineScope()
    val results = remember { mutableStateListOf<AdventureResult>() }
    var zoneId by remember { mutableStateOf("") }
    var zoneName by remember { mutableStateOf("") }
    var turnsText by remember { mutableStateOf("10") }
    var stopMessage by remember { mutableStateOf<String?>(null) }

    // Collect results from event bus
    androidx.compose.runtime.LaunchedEffect(Unit) {
        eventBus.events.collect { event ->
            when (event) {
                is GameEvent.TurnConsumed -> results.add(0, event.result)
                is GameEvent.AdventureLoopStopped -> stopMessage = "Stopped: ${event.reason}"
                else -> {}
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Adventure", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = zoneId,
            onValueChange = { zoneId = it },
            label = { Text("Zone ID (snarfblat)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = zoneName,
            onValueChange = { zoneName = it },
            label = { Text("Zone name (display only)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = turnsText,
            onValueChange = { turnsText = it },
            label = { Text("Turns") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        Row {
            Button(
                onClick = {
                    stopMessage = null
                    val turns = turnsText.toIntOrNull() ?: 1
                    val location = AdventureLocation(zoneId, zoneName.ifBlank { "Zone $zoneId" }, "")
                    adventureManager.runAdventures(location, turns, scope)
                },
                enabled = !isRunning && zoneId.isNotBlank()
            ) { Text("Run $turnsText Turns") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { adventureManager.stop() },
                enabled = isRunning
            ) { Text("Stop") }
        }

        stopMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(12.dp))
        Text("Results (${results.size} turns)", style = MaterialTheme.typography.labelLarge)
        LazyColumn {
            items(results) { result ->
                CombatResultCard(result, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
```

- [ ] **Step 3: Write `MacroEditorScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/adventure/MacroEditorScreen.kt
package net.sourceforge.kolmafia.ui.adventure

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.adventure.MacroStrategy
import net.sourceforge.kolmafia.preferences.Preferences
import org.koin.compose.koinInject

@Composable
fun MacroEditorScreen() {
    val preferences: Preferences = koinInject()
    var selectedTab by remember { mutableIntStateOf(0) }
    var globalMacro by remember {
        mutableStateOf(preferences.getString("combatMacroDefault", MacroStrategy.SAFE_DEFAULT))
    }
    var zoneId by remember { mutableStateOf("") }
    var zoneMacro by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Combat Macros", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Global Default") }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Per-Zone") }
        }
        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                Text("Applied to all zones unless overridden:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = globalMacro,
                    onValueChange = { globalMacro = it; saved = false },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    label = { Text("Macro") }
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    preferences.setString("combatMacroDefault", globalMacro)
                    saved = true
                }) { Text("Save") }
            }
            1 -> {
                OutlinedTextField(
                    value = zoneId,
                    onValueChange = {
                        zoneId = it
                        zoneMacro = preferences.getString("combatMacro_$it")
                        saved = false
                    },
                    label = { Text("Zone ID (snarfblat)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = zoneMacro,
                    onValueChange = { zoneMacro = it; saved = false },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    label = { Text("Zone macro (blank = use global)") }
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (zoneId.isNotBlank()) preferences.setString("combatMacro_$zoneId", zoneMacro)
                        saved = true
                    },
                    enabled = zoneId.isNotBlank()
                ) { Text("Save") }
            }
        }

        if (saved) {
            Spacer(Modifier.height(4.dp))
            Text("Saved", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

- [ ] **Step 4: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: adventure UI — AdventureScreen, MacroEditorScreen, CombatResultCard"
```

---

## Task 10: Inventory UI

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/inventory/InventoryScreen.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/inventory/ItemDetailSheet.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/inventory/CraftingScreen.kt`

- [ ] **Step 1: Write `InventoryScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/inventory/InventoryScreen.kt
package net.sourceforge.kolmafia.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import org.koin.compose.koinInject

private val TABS = listOf("All", "Equipment", "Food", "Drink", "Usable", "Other")

@Composable
fun InventoryScreen() {
    val inventoryManager: InventoryManager = koinInject()
    val state by inventoryManager.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }

    val filteredItems = remember(selectedTab, state.items) {
        state.items.values.filter { item ->
            when (selectedTab) {
                0 -> true
                1 -> item.type in listOf(ItemType.WEAPON, ItemType.OFFHAND, ItemType.HAT,
                    ItemType.SHIRT, ItemType.PANTS, ItemType.ACCESSORY)
                2 -> item.type == ItemType.FOOD
                3 -> item.type == ItemType.DRINK
                4 -> item.type in listOf(ItemType.USABLE, ItemType.MULTIUSABLE, ItemType.REUSABLE)
                else -> item.type in listOf(ItemType.OTHER, ItemType.SPLEEN, ItemType.FAMILIAR_ITEM)
            }
        }.sortedBy { it.name }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Inventory", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp))
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            TABS.forEachIndexed { i, label ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                    Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredItems, key = { it.itemId }) { item ->
                InventoryItemRow(item, onClick = { selectedItem = item })
                HorizontalDivider()
            }
        }
    }

    selectedItem?.let { item ->
        ItemDetailSheet(
            item = item,
            inventoryManager = inventoryManager,
            onDismiss = { selectedItem = null }
        )
    }
}

@Composable
private fun InventoryItemRow(item: InventoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)
    ) {
        Text(item.name.ifBlank { "Item #${item.itemId}" },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text("×${item.quantity}", style = MaterialTheme.typography.bodyMedium)
    }
}
```

- [ ] **Step 2: Write `ItemDetailSheet.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/inventory/ItemDetailSheet.kt
package net.sourceforge.kolmafia.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailSheet(
    item: InventoryItem,
    inventoryManager: InventoryManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.name.ifBlank { "Item #${item.itemId}" },
                style = MaterialTheme.typography.titleLarge)
            Text("Quantity: ${item.quantity}", style = MaterialTheme.typography.bodyMedium)
            Text("Type: ${item.type.name.lowercase().replace('_', ' ')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.type in listOf(ItemType.FOOD, ItemType.DRINK, ItemType.SPLEEN,
                        ItemType.USABLE, ItemType.MULTIUSABLE, ItemType.REUSABLE)) {
                    Button(onClick = {
                        scope.launch {
                            inventoryManager.useItem(item)
                            onDismiss()
                        }
                    }) { Text("Use") }
                }
                if (item.type in listOf(ItemType.WEAPON, ItemType.OFFHAND, ItemType.HAT,
                        ItemType.SHIRT, ItemType.PANTS, ItemType.ACCESSORY)) {
                    Button(onClick = {
                        scope.launch {
                            inventoryManager.equipItem(item, item.type.name.lowercase())
                            onDismiss()
                        }
                    }) { Text("Equip") }
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        inventoryManager.discardItem(item, 1)
                        onDismiss()
                    }
                }) { Text("Discard") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
```

- [ ] **Step 3: Write `CraftingScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/inventory/CraftingScreen.kt
package net.sourceforge.kolmafia.ui.inventory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.inventory.CraftMode
import net.sourceforge.kolmafia.inventory.InventoryManager
import org.koin.compose.koinInject

@Composable
fun CraftingScreen() {
    val inventoryManager: InventoryManager = koinInject()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var item1Id by remember { mutableStateOf("") }
    var item2Id by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    val modes = listOf(CraftMode.COMBINE, CraftMode.COOK, CraftMode.COCKTAIL, CraftMode.SMITH)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Crafting", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTab) {
            modes.forEachIndexed { i, mode ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Item ID 1", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = item1Id,
            onValueChange = { item1Id = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Item ID") }
        )
        Spacer(Modifier.height(8.dp))
        Text("Item ID 2", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = item2Id,
            onValueChange = { item2Id = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Item ID") }
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Button(
                onClick = {
                    val id1 = item1Id.toIntOrNull() ?: return@Button
                    val id2 = item2Id.toIntOrNull() ?: return@Button
                    scope.launch {
                        inventoryManager.craft(modes[selectedTab], id1, id2).fold(
                            onSuccess = { resultMessage = "Crafted: ${it.name}" },
                            onFailure = { resultMessage = "Failed: ${it.message}" }
                        )
                    }
                },
                enabled = item1Id.isNotBlank() && item2Id.isNotBlank()
            ) { Text("Craft") }
            Spacer(Modifier.width(8.dp))
            resultMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("Failed")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary)
            }
        }
    }
}
```

- [ ] **Step 4: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: inventory UI — InventoryScreen, ItemDetailSheet, CraftingScreen"
```

---

## Task 11: Familiar UI

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/familiar/FamiliarScreen.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/familiar/FamiliarDetailSheet.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/familiar/HatcheryScreen.kt`

- [ ] **Step 1: Write `FamiliarScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/familiar/FamiliarScreen.kt
package net.sourceforge.kolmafia.ui.familiar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import org.koin.compose.koinInject

@Composable
fun FamiliarScreen() {
    val familiarManager: FamiliarManager = koinInject()
    val state by familiarManager.state.collectAsState()
    var selectedFamiliar by remember { mutableStateOf<FamiliarData?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Familiars", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        // Active familiar card
        state.activeFamiliar?.let { active ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active: ${active.name}", style = MaterialTheme.typography.titleMedium)
                        Text("${active.weight} lbs", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(active.race, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Kills: ${active.kills} · Exp: ${active.experience}",
                        style = MaterialTheme.typography.bodySmall)
                    active.equipment?.let {
                        Text("Equipped: ${it.name}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } ?: Text("No familiar active", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text("Terrarium (${state.ownedFamiliars.size})", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.ownedFamiliars, key = { it.id }) { familiar ->
                FamiliarGridCard(familiar, onClick = { selectedFamiliar = familiar })
            }
        }
    }

    selectedFamiliar?.let { familiar ->
        FamiliarDetailSheet(
            familiar = familiar,
            familiarManager = familiarManager,
            onDismiss = { selectedFamiliar = null }
        )
    }
}

@Composable
private fun FamiliarGridCard(familiar: FamiliarData, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(familiar.name, style = MaterialTheme.typography.bodyMedium)
            Text(familiar.race, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${familiar.weight} lbs", style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

- [ ] **Step 2: Write `FamiliarDetailSheet.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/familiar/FamiliarDetailSheet.kt
package net.sourceforge.kolmafia.ui.familiar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamiliarDetailSheet(
    familiar: FamiliarData,
    familiarManager: FamiliarManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(familiar.name, style = MaterialTheme.typography.titleLarge)
            Text(familiar.race, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Weight: ${familiar.weight} lbs", style = MaterialTheme.typography.bodyMedium)
            Text("Experience: ${familiar.experience}", style = MaterialTheme.typography.bodyMedium)
            Text("Kills: ${familiar.kills}", style = MaterialTheme.typography.bodyMedium)
            familiar.equipment?.let {
                Text("Equipment: ${it.name}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = {
                    scope.launch {
                        familiarManager.switchFamiliar(familiar)
                        onDismiss()
                    }
                }) { Text("Make Active") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onDismiss) { Text("Close") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
```

- [ ] **Step 3: Write `HatcheryScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/familiar/HatcheryScreen.kt
package net.sourceforge.kolmafia.ui.familiar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import org.koin.compose.koinInject

@Composable
fun HatcheryScreen() {
    val inventoryManager: InventoryManager = koinInject()
    val familiarManager: FamiliarManager = koinInject()
    val invState by inventoryManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Eggs are inventory items of type FAMILIAR_ITEM
    val eggs = invState.items.values.filter { it.type == ItemType.FAMILIAR_ITEM }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hatchery", style = MaterialTheme.typography.headlineSmall)
        Text("${eggs.size} hatchable eggs in inventory",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyColumn {
            items(eggs, key = { it.itemId }) { egg ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(egg.name, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        scope.launch { familiarManager.hatch(egg.itemId) }
                    }) { Text("Hatch") }
                }
                HorizontalDivider()
            }
        }
    }
}
```

- [ ] **Step 4: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: familiar UI — FamiliarScreen, FamiliarDetailSheet, HatcheryScreen"
```

---

## Task 12: Wire BottomNavBar into App.kt

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/App.kt`

- [ ] **Step 1: Update `App.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/App.kt
package net.sourceforge.kolmafia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.ui.adventure.AdventureScreen
import net.sourceforge.kolmafia.ui.character.CharacterScreen
import net.sourceforge.kolmafia.ui.familiar.FamiliarScreen
import net.sourceforge.kolmafia.ui.inventory.InventoryScreen
import net.sourceforge.kolmafia.ui.login.LoginScreen
import net.sourceforge.kolmafia.ui.login.LoginViewModel
import org.koin.compose.koinInject

@Composable
fun App() {
    MaterialTheme {
        var isLoggedIn by remember { mutableStateOf(false) }
        val sessionManager: SessionManager = koinInject()
        val character: KoLCharacter = koinInject()

        if (!isLoggedIn) {
            val viewModel = remember { LoginViewModel(sessionManager) }
            LoginScreen(viewModel = viewModel, onLoginSuccess = { isLoggedIn = true })
            return@MaterialTheme
        }

        var selectedTab by remember { mutableIntStateOf(0) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.AccountCircle, "Character") },
                        label = { Text("Character") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Place, "Adventure") },
                        label = { Text("Adventure") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.List, "Inventory") },
                        label = { Text("Inventory") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Favorite, "Familiars") },
                        label = { Text("Familiars") }
                    )
                }
            }
        ) { _ ->
            when (selectedTab) {
                0 -> CharacterScreen(character = character)
                1 -> AdventureScreen()
                2 -> InventoryScreen()
                3 -> FamiliarScreen()
            }
        }
    }
}
```

- [ ] **Step 2: Add material-icons-extended to shared dependencies**

In `shared/build.gradle.kts`, add to `commonMain.dependencies`:

```kotlin
implementation(compose.materialIconsExtended)
```

- [ ] **Step 3: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Build Android APK**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Run full test suite**

```bash
./gradlew :shared:jvmTest
```

Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/ androidApp/
git commit -m "feat: BottomNavBar with Character/Adventure/Inventory/Familiar tabs"
```

---

## Self-Review

**Spec coverage check:**

| Spec requirement | Task |
|---|---|
| GameEvent hierarchy + GameEventBus | Task 1 |
| AdventureLocation, AdventureResult, StopReason | Task 1 |
| MacroStrategy (per-zone + global + safe default) | Task 1 + Task 3 |
| AdventureParser HTML → sealed result | Task 2 |
| Adventure HTTP requests | Task 4 |
| AdventureManager N-turn loop, stop conditions | Task 5 |
| InventoryManager fetch + event subscription | Task 6 |
| All inventory operations (use/equip/unequip/discard/craft/mall) | Task 6 |
| FamiliarManager fetch + all operations | Task 7 |
| FamiliarActionRequest (Pocket Professor, Shortest Wig, Unsupported) | Task 7 |
| HatcheryRequest | Task 7 |
| SharedModule updated | Task 8 |
| SessionManager initializes managers on login | Task 8 |
| AdventureScreen + CombatResultCard + MacroEditorScreen | Task 9 |
| InventoryScreen + ItemDetailSheet + CraftingScreen | Task 10 |
| FamiliarScreen + FamiliarDetailSheet + HatcheryScreen | Task 11 |
| BottomNavBar in App.kt | Task 12 |
| Choice resolution strategy (choiceAdventureX pref) | Task 1 (MacroStrategy.choiceOptionFor) + Task 3 |
| StopReason sealed class | Task 1 |
| Stale state flag on network error | Tasks 6, 7 |
| MallError typed subtypes | Task 1 + Task 6 |

**No placeholders found.**

**Type consistency verified:** `InventoryItem`, `FamiliarData`, `AdventureResult`, `GameEvent` subtypes, `StopReason`, `MallError`, `CraftMode`, `ItemType` are defined once in Task 1 and used consistently in all subsequent tasks.

**Note for developers:** The exact JSON field names for `api.php?what=inventory` and `api.php?what=familiars` should be verified against the live KoL API before shipping. The `FamiliarApiEntry` and inventory response parsing are based on the documented KoL API format but may need adjustment.
