# KoLmafia Mobile — Phase 3: Skills, Effects & Zone Browser

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add skill management (view and cast skills, track daily limits), active effects display, full Muscle/Mysticality/Moxie character stats, and a zone browser to replace manual zone-ID entry — making the app practical for daily play without memorising zone IDs or skill numbers.

**Architecture:** Two new domain managers (`SkillManager`, `EffectManager`) following the Phase 2 event-bus pattern. `CharacterState` extended with three combat stats, subpoint XP, sign, path, and hardcore flag. `LocationDatabase` is a pure in-memory lookup over a bundled zone list (no API call required). `EffectsScreen` is accessed from `CharacterScreen` via a `ModalBottomSheet`. `App.kt` gains a fifth bottom-nav tab (Skills).

**Tech Stack:** Existing stack — Ktor 3.0.3, Koin 4.0.0, Compose Multiplatform 1.7.3, kotlinx.serialization 1.7.3, kotlin.test + MockEngine. No new dependencies.

---

## File Map

```
shared/src/commonMain/kotlin/net/sourceforge/kolmafia/
  character/
    CharacterApiResponse.kt     ← modify: add mus/musexp/mys/mysexp/mox/moxexp/sign/path/roninleft/hardcore
    CharacterState.kt           ← modify: add baseMusc/muscSubpoints/baseMyst/mystSubpoints/baseMoxie/moxieSubpoints/zodiacSign/challengePath/roninLeft/isHardcore
    KoLCharacter.kt             ← modify: update updateFromApiResponse for new fields
  skill/
    SkillType.kt                ← new: enum PASSIVE, NONCOMBAT, COMBAT, BUFF, SUMMON, OTHER
    SkillData.kt                ← new: data class — id, name, type, mpCost, dailyLimit, timesCast
    SkillState.kt               ← new: data class — skills list + isStale
    SkillCastRequest.kt         ← new: HTTP POST skills.php
    SkillManager.kt             ← new: fetch api.php?what=skills, cast, track daily counts, subscribe to events
  effect/
    EffectData.kt               ← new: data class — id, name, duration
    EffectState.kt              ← new: data class — effects list + isStale
    EffectManager.kt            ← new: fetch api.php?what=effects, subscribe to TurnConsumed + SkillCast
  location/
    LocationData.kt             ← new: data class — snarfblat, name, zone, recommendedLevel
    LocationDatabase.kt         ← new: bundled list of ~35 common zones + case-insensitive search
  event/
    GameEvent.kt                ← modify: add SkillCast, EffectsRefreshed subtypes
  di/
    SharedModule.kt             ← modify: add SkillCastRequest, SkillManager, EffectManager singletons
  session/
    SessionManager.kt           ← modify: add skillManager/effectManager params + initialize calls
  ui/
    character/CharacterScreen.kt     ← modify: add Mus/Mys/Mox rows, sign/path, effects button
    effects/EffectsScreen.kt         ← new: scrollable effects list (used as BottomSheet content)
    skills/SkillsScreen.kt           ← new: filterable skill list + cast button + daily limit badge
    adventure/ZoneBrowserSheet.kt    ← new: ModalBottomSheet zone search + selection
    adventure/AdventureScreen.kt     ← modify: add "Browse" button → ZoneBrowserSheet
    App.kt                           ← modify: 5-tab nav (add Skills tab)

shared/src/commonTest/kotlin/net/sourceforge/kolmafia/
  character/KoLCharacterTest.kt      ← modify: extend with stat field assertions
  skill/SkillManagerTest.kt          ← new
  effect/EffectManagerTest.kt        ← new
  location/LocationDatabaseTest.kt   ← new
```

---

## Task 1: Character Stats Enhancement

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/CharacterApiResponse.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/CharacterState.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/KoLCharacter.kt`
- Modify: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/character/KoLCharacterTest.kt`

- [ ] **Step 1: Add a failing test for the new stat fields**

```kotlin
// Add to KoLCharacterTest — new test at the bottom of the class
@Test
fun updateFromApiResponse_populatesStatFields() {
    val character = KoLCharacter()
    character.updateFromApiResponse(
        CharacterApiResponse(
            name = "StatPlayer",
            mus = "80",
            musexp = "1234",
            mys = "120",
            mysexp = "5678",
            mox = "100",
            moxexp = "9012",
            sign = "Mongoose",
            path = "Standard",
            roninleft = "0",
            hardcore = "1"
        )
    )
    val state = character.state.value
    assertEquals(80, state.baseMusc)
    assertEquals(1234, state.muscSubpoints)
    assertEquals(120, state.baseMyst)
    assertEquals(5678, state.mystSubpoints)
    assertEquals(100, state.baseMoxie)
    assertEquals(9012, state.moxieSubpoints)
    assertEquals("Mongoose", state.zodiacSign)
    assertEquals("Standard", state.challengePath)
    assertEquals(0, state.roninLeft)
    assertTrue(state.isHardcore)
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
cd C:\Development\kolmafia-mobile
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.character.KoLCharacterTest.updateFromApiResponse_populatesStatFields"
```

Expected: FAIL — `CharacterApiResponse` does not have `mus` field yet.

- [ ] **Step 3: Replace `CharacterApiResponse.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/CharacterApiResponse.kt
package net.sourceforge.kolmafia.character

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Models the JSON returned by https://www.kingdomofloathing.com/api.php?what=status
// All numeric fields come back as strings in the KoL API.
@Serializable
data class CharacterApiResponse(
    val name: String = "",
    val playerid: String = "0",
    val level: String = "1",
    @SerialName("class") val classId: String = "0",
    val hp: String = "0",
    val hpmax: String = "0",
    val mp: String = "0",
    val mpmax: String = "0",
    val meat: String = "0",
    val adventures: String = "0",
    val fullness: String = "0",
    val drunk: String = "0",
    val spleen: String = "0",
    // Combat stats (base adjusted values, all returned as strings)
    val mus: String = "0",
    val musexp: String = "0",
    val mys: String = "0",
    val mysexp: String = "0",
    val mox: String = "0",
    val moxexp: String = "0",
    // Character identity
    val sign: String = "",
    val path: String = "",
    val roninleft: String = "0",
    val hardcore: String = "0"
)
```

- [ ] **Step 4: Replace `CharacterState.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/CharacterState.kt
package net.sourceforge.kolmafia.character

data class CharacterState(
    val name: String = "",
    val playerId: Int = 0,
    val level: Int = 1,
    val characterClass: Int = 0,
    val currentHp: Int = 0,
    val maxHp: Int = 0,
    val currentMp: Int = 0,
    val maxMp: Int = 0,
    val meat: Int = 0,
    val adventuresLeft: Int = 0,
    val fullness: Int = 0,
    val inebriety: Int = 0,
    val spleenUsed: Int = 0,
    // Combat stats
    val baseMusc: Int = 0,
    val muscSubpoints: Int = 0,
    val baseMyst: Int = 0,
    val mystSubpoints: Int = 0,
    val baseMoxie: Int = 0,
    val moxieSubpoints: Int = 0,
    // Identity
    val zodiacSign: String = "",
    val challengePath: String = "",
    val roninLeft: Int = 0,
    val isHardcore: Boolean = false,
    val isLoggedIn: Boolean = false
)
```

- [ ] **Step 5: Replace `KoLCharacter.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/character/KoLCharacter.kt
package net.sourceforge.kolmafia.character

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KoLCharacter {
    private val _state = MutableStateFlow(CharacterState())
    val state: StateFlow<CharacterState> = _state.asStateFlow()

    fun updateFromApiResponse(response: CharacterApiResponse) {
        _state.value = CharacterState(
            name = response.name,
            playerId = response.playerid.toIntOrNull() ?: 0,
            level = response.level.toIntOrNull() ?: 1,
            characterClass = response.classId.toIntOrNull() ?: 0,
            currentHp = response.hp.toIntOrNull() ?: 0,
            maxHp = response.hpmax.toIntOrNull() ?: 0,
            currentMp = response.mp.toIntOrNull() ?: 0,
            maxMp = response.mpmax.toIntOrNull() ?: 0,
            meat = response.meat.toIntOrNull() ?: 0,
            adventuresLeft = response.adventures.toIntOrNull() ?: 0,
            fullness = response.fullness.toIntOrNull() ?: 0,
            inebriety = response.drunk.toIntOrNull() ?: 0,
            spleenUsed = response.spleen.toIntOrNull() ?: 0,
            baseMusc = response.mus.toIntOrNull() ?: 0,
            muscSubpoints = response.musexp.toIntOrNull() ?: 0,
            baseMyst = response.mys.toIntOrNull() ?: 0,
            mystSubpoints = response.mysexp.toIntOrNull() ?: 0,
            baseMoxie = response.mox.toIntOrNull() ?: 0,
            moxieSubpoints = response.moxexp.toIntOrNull() ?: 0,
            zodiacSign = response.sign,
            challengePath = response.path,
            roninLeft = response.roninleft.toIntOrNull() ?: 0,
            isHardcore = response.hardcore == "1",
            isLoggedIn = true
        )
    }

    fun reset() {
        _state.value = CharacterState()
    }
}
```

- [ ] **Step 6: Run the full KoLCharacterTest — verify all 4 tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.character.KoLCharacterTest"
```

Expected: PASS (4 tests — 3 existing + 1 new)

- [ ] **Step 7: Commit**

```bash
git add shared/src/
git commit -m "feat: extend CharacterState with Mus/Mys/Mox stats, sign, path, ronin, hardcore"
```

---

## Task 2: Skill + Effect Data Models + GameEvent Extensions

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillType.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillData.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillState.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/effect/EffectData.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/effect/EffectState.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/event/GameEvent.kt`

- [ ] **Step 1: Write `SkillType.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillType.kt
package net.sourceforge.kolmafia.skill

enum class SkillType {
    PASSIVE, NONCOMBAT, COMBAT, BUFF, SUMMON, OTHER;

    companion object {
        fun fromApiInt(type: Int): SkillType = when (type) {
            0, 11 -> PASSIVE
            1, 2, 13 -> NONCOMBAT
            3 -> COMBAT
            5, 6 -> BUFF
            4 -> SUMMON
            else -> OTHER
        }
    }
}
```

- [ ] **Step 2: Write `SkillData.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillData.kt
package net.sourceforge.kolmafia.skill

data class SkillData(
    val id: Int,
    val name: String,
    val type: SkillType,
    val mpCost: Int,
    val dailyLimit: Int,    // 0 = unlimited
    val timesCast: Int
) {
    val isActive: Boolean get() = type != SkillType.PASSIVE
    val canCastMore: Boolean get() = dailyLimit == 0 || timesCast < dailyLimit
}
```

- [ ] **Step 3: Write `SkillState.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillState.kt
package net.sourceforge.kolmafia.skill

data class SkillState(
    val skills: List<SkillData> = emptyList(),
    val isStale: Boolean = false
)
```

- [ ] **Step 4: Write `EffectData.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/effect/EffectData.kt
package net.sourceforge.kolmafia.effect

data class EffectData(
    val id: Int,
    val name: String,
    val duration: Int
)
```

- [ ] **Step 5: Write `EffectState.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/effect/EffectState.kt
package net.sourceforge.kolmafia.effect

data class EffectState(
    val effects: List<EffectData> = emptyList(),
    val isStale: Boolean = false
)
```

- [ ] **Step 6: Replace `GameEvent.kt` with new subtypes**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/event/GameEvent.kt
package net.sourceforge.kolmafia.event

import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureResult
import net.sourceforge.kolmafia.adventure.StopReason
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.inventory.InventoryItem

sealed class GameEvent {
    // Adventure
    data class TurnConsumed(val location: AdventureLocation, val result: AdventureResult) : GameEvent()
    data class CombatFinished(val won: Boolean, val monster: String) : GameEvent()
    data class ChoiceResolved(val choiceId: Int, val option: Int) : GameEvent()
    data class AdventureLoopStopped(val reason: StopReason) : GameEvent()
    // Inventory
    data class ItemObtained(val item: InventoryItem) : GameEvent()
    data class ItemConsumed(val itemId: Int, val quantity: Int) : GameEvent()
    data class ItemEquipped(val item: InventoryItem, val slot: String) : GameEvent()
    data class ItemDiscarded(val itemId: Int, val quantity: Int) : GameEvent()
    data class ItemCrafted(val resultItem: InventoryItem) : GameEvent()
    data class MallPurchase(val item: InventoryItem, val meatSpent: Int) : GameEvent()
    // Familiar
    data class FamiliarSwitched(val familiar: FamiliarData) : GameEvent()
    data class FamiliarEquipped(val familiar: FamiliarData, val item: InventoryItem) : GameEvent()
    data class FamiliarHatched(val familiar: FamiliarData) : GameEvent()
    // Skills + Effects (Phase 3)
    data class SkillCast(val skillId: Int, val skillName: String, val quantity: Int) : GameEvent()
    object EffectsRefreshed : GameEvent()
}
```

- [ ] **Step 7: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add shared/src/
git commit -m "feat: skill + effect data models, extend GameEvent with SkillCast/EffectsRefreshed"
```

---

## Task 3: LocationDatabase

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/location/LocationData.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/location/LocationDatabase.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/location/LocationDatabaseTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/location/LocationDatabaseTest.kt
package net.sourceforge.kolmafia.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocationDatabaseTest {

    @Test
    fun search_byNameSubstring_returnMatchingZones() {
        val results = LocationDatabase.search("knob")
        assertTrue(results.isNotEmpty(), "Expected zones matching 'knob'")
        assertTrue(results.all { it.name.contains("knob", ignoreCase = true) ||
            it.zone.contains("knob", ignoreCase = true) })
    }

    @Test
    fun search_caseInsensitive() {
        val lower = LocationDatabase.search("forest")
        val upper = LocationDatabase.search("FOREST")
        assertEquals(lower.map { it.snarfblat }, upper.map { it.snarfblat })
    }

    @Test
    fun search_emptyQuery_returnsAll() {
        val all = LocationDatabase.search("")
        assertEquals(LocationDatabase.ALL_LOCATIONS.size, all.size)
    }

    @Test
    fun search_noMatch_returnsEmpty() {
        val results = LocationDatabase.search("zzznomatch")
        assertTrue(results.isEmpty())
    }

    @Test
    fun findBySnarfblat_returnsCorrectZone() {
        val zone = LocationDatabase.findBySnarfblat("20")
        assertNotNull(zone)
        assertTrue(zone.name.contains("Spooky Forest", ignoreCase = true))
    }

    @Test
    fun allLocations_haveUniqueSnarfblats() {
        val ids = LocationDatabase.ALL_LOCATIONS.map { it.snarfblat }
        assertEquals(ids.size, ids.toSet().size)
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.location.LocationDatabaseTest"
```

Expected: FAIL — `LocationDatabase` does not exist yet.

- [ ] **Step 3: Write `LocationData.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/location/LocationData.kt
package net.sourceforge.kolmafia.location

data class LocationData(
    val snarfblat: String,
    val name: String,
    val zone: String,
    val recommendedLevel: Int
)
```

- [ ] **Step 4: Write `LocationDatabase.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/location/LocationDatabase.kt
package net.sourceforge.kolmafia.location

object LocationDatabase {

    val ALL_LOCATIONS: List<LocationData> = listOf(
        // Level 1–4: Starter zones
        LocationData("18",  "The Haunted Pantry",                    "The Nearby Plains",           1),
        LocationData("20",  "The Spooky Forest",                     "The Nearby Plains",           1),
        LocationData("23",  "The Degrassi Knoll Garage",             "The Nearby Plains",           2),
        LocationData("24",  "The Degrassi Knoll Gym",                "The Nearby Plains",           2),
        LocationData("28",  "Cobb's Knob Kitchen",                   "Cobb's Knob",                 3),
        LocationData("29",  "Cobb's Knob Harem",                     "Cobb's Knob",                 3),
        LocationData("26",  "Outskirts of Cobb's Knob",              "Cobb's Knob",                 3),
        LocationData("27",  "Cobb's Knob Barracks",                  "Cobb's Knob",                 4),
        LocationData("30",  "Cobb's Knob Treasury",                  "Cobb's Knob",                 4),
        LocationData("219", "Cobb's Knob Laboratory",                "Cobb's Knob",                 4),
        // Level 5–8: Mid-game
        LocationData("93",  "The Haunted Conservatory",              "Spookyraven Manor",           5),
        LocationData("101", "The Haunted Ballroom",                  "Spookyraven Manor",           5),
        LocationData("106", "The Haunted Library",                   "Spookyraven Manor",           6),
        LocationData("125", "The Haunted Wine Cellar",               "Spookyraven Manor",           7),
        LocationData("140", "A-Boo Peak",                            "The Big Mountains",           7),
        LocationData("141", "Twin Peak",                             "The Big Mountains",           7),
        LocationData("142", "The Lair of the Ninja Snowmen",         "The Big Mountains",           8),
        LocationData("128", "Whitey's Grove",                        "The Nearby Plains",           8),
        // Level 9–12: Late mid-game
        LocationData("131", "The Battlefield (Frat Uniform)",        "The Mysterious Island",       9),
        LocationData("132", "The Battlefield (Hippy Outfit)",        "The Mysterious Island",       9),
        LocationData("150", "The Valley of Rof L'm Fao",             "The Penultimate Fantasy",    10),
        LocationData("167", "The Dungeons of Doom",                  "The Nearby Plains",          11),
        LocationData("176", "The Slime Tube",                        "The Nearby Plains",          11),
        LocationData("180", "The Penultimate Fantasy Airship",       "The Penultimate Fantasy",    11),
        LocationData("184", "The Castle in the Clouds (Basement)",   "The Penultimate Fantasy",    12),
        LocationData("185", "The Castle in the Clouds (Ground)",     "The Penultimate Fantasy",    12),
        LocationData("186", "The Castle in the Clouds (Top)",        "The Penultimate Fantasy",    12),
        // Seasonal / special
        LocationData("191", "Fernswarthy's Tower (Second Floor)",    "The Nearby Plains",          10),
        LocationData("406", "Dreadsylvania",                         "Dreadsylvania",              10),
        LocationData("385", "The Briniest Deepests",                 "The Sea",                    12),
        // Clan
        LocationData("371", "The Haunted Storage Room",              "Dreadsylvania",              10),
        // Farm zones
        LocationData("100", "The Degrassi Knoll Restroom",           "The Nearby Plains",           2),
        LocationData("337", "Uncle Gator's Country Fun-Time Liquid Waste Sluice", "The Nearby Plains", 1),
        // Goblin King
        LocationData("31",  "The Throne Room",                       "Cobb's Knob",                 5)
    )

    fun search(query: String): List<LocationData> {
        if (query.isBlank()) return ALL_LOCATIONS
        val q = query.trim().lowercase()
        return ALL_LOCATIONS.filter {
            it.name.lowercase().contains(q) || it.zone.lowercase().contains(q)
        }
    }

    fun findBySnarfblat(snarfblat: String): LocationData? =
        ALL_LOCATIONS.find { it.snarfblat == snarfblat }
}
```

- [ ] **Step 5: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.location.LocationDatabaseTest"
```

Expected: PASS (6 tests)

- [ ] **Step 6: Commit**

```bash
git add shared/src/
git commit -m "feat: LocationData + LocationDatabase with bundled zones and search"
```

---

## Task 4: SkillCastRequest + SkillManager

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillCastRequest.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillManager.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/skill/SkillManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/skill/SkillManagerTest.kt
package net.sourceforge.kolmafia.skill

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SkillManagerTest {

    // api.php?what=skills returns a JSON object keyed by skill ID string.
    private val skillsJson = """
        {
          "3": {"name": "Thrust-Smack", "type": 3, "dailylimit": 0, "timescast": 5, "mpcost": 0},
          "6": {"name": "Moxie of the Mariachi", "type": 5, "dailylimit": 3, "timescast": 1, "mpcost": 10}
        }
    """.trimIndent()

    private fun makeManager(
        skillsResponse: String = skillsJson,
        castResponseBody: String = "<html>You cast the skill!</html>"
    ): Pair<SkillManager, GameEventBus> {
        val engine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "skills" ->
                    respond(skillsResponse, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("skills.php") ->
                    respond(castResponseBody, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val bus = GameEventBus()
        return SkillManager(client, SkillCastRequest(client), bus) to bus
    }

    @Test
    fun fetchSkills_populatesSkillList() = runTest {
        val (manager, _) = makeManager()
        manager.fetchSkills()
        val state = manager.state.value
        assertEquals(2, state.skills.size)
        assertFalse(state.isStale)
    }

    @Test
    fun fetchSkills_mapsTypeCorrectly() = runTest {
        val (manager, _) = makeManager()
        manager.fetchSkills()
        val skills = manager.state.value.skills
        val combat = skills.find { it.id == 3 }
        val buff = skills.find { it.id == 6 }
        assertNotNull(combat)
        assertNotNull(buff)
        assertEquals(SkillType.COMBAT, combat.type)
        assertEquals(SkillType.BUFF, buff.type)
    }

    @Test
    fun fetchSkills_tracksDailyLimits() = runTest {
        val (manager, _) = makeManager()
        manager.fetchSkills()
        val buff = manager.state.value.skills.find { it.id == 6 }
        assertNotNull(buff)
        assertEquals(3, buff.dailyLimit)
        assertEquals(1, buff.timesCast)
        assertFalse(buff.canCastMore.not()) // canCastMore should be true (1 < 3)
    }

    @Test
    fun castSkill_incrementsTimesCast_andEmitsEvent() = runTest {
        val (manager, bus) = makeManager()
        manager.fetchSkills()
        val received = mutableListOf<GameEvent>()
        val job = launch { bus.events.collect { received.add(it) } }

        val thrustSmack = manager.state.value.skills.first { it.id == 3 }
        manager.cast(thrustSmack, 1)

        job.cancel()
        val castEvents = received.filterIsInstance<GameEvent.SkillCast>()
        assertEquals(1, castEvents.size)
        assertEquals(3, castEvents.first().skillId)
        assertEquals("Thrust-Smack", castEvents.first().skillName)
    }

    @Test
    fun castSkill_returnsFailure_onMpError() = runTest {
        val (manager, _) = makeManager(
            castResponseBody = "<html>You don't have enough MP to cast that skill.</html>"
        )
        manager.fetchSkills()
        val skill = manager.state.value.skills.first { it.id == 3 }
        val result = manager.cast(skill, 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun initialState_isEmpty() {
        val (manager, _) = makeManager()
        assertTrue(manager.state.value.skills.isEmpty())
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.skill.SkillManagerTest"
```

Expected: FAIL — `SkillManager` does not exist yet.

- [ ] **Step 3: Write `SkillCastRequest.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillCastRequest.kt
package net.sourceforge.kolmafia.skill

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class SkillCastRequest(private val client: HttpClient) {

    suspend fun cast(skillId: Int, quantity: Int = 1): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/skills.php",
            formParameters = parameters {
                append("action", "useskill")
                append("whichskill", skillId.toString())
                append("quantity", quantity.toString())
            }
        )
        val body = response.bodyAsText()
        when {
            body.contains("don't have enough", ignoreCase = true) ||
            body.contains("not enough mp", ignoreCase = true) ->
                Result.failure(Exception("Not enough MP"))
            body.contains("daily limit", ignoreCase = true) ->
                Result.failure(Exception("Daily limit reached"))
            else -> Result.success(body)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

- [ ] **Step 4: Write `SkillManager.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillManager.kt
package net.sourceforge.kolmafia.skill

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

// Verify field names against live api.php?what=skills response before shipping.
@Serializable
private data class SkillApiEntry(
    val name: String = "",
    val type: Int = 0,
    val dailylimit: Int = 0,
    val timescast: Int = 0,
    val mpcost: Int = 0
)

class SkillManager(
    private val client: HttpClient,
    private val castRequest: SkillCastRequest,
    private val eventBus: GameEventBus
) {
    private val _state = MutableStateFlow(SkillState())
    val state: StateFlow<SkillState> = _state.asStateFlow()

    fun initialize(scope: CoroutineScope) {
        scope.launch { fetchSkills() }
    }

    suspend fun fetchSkills() {
        try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "skills")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) {
                _state.value = _state.value.copy(isStale = true)
                return
            }
            val raw: Map<String, SkillApiEntry> = response.body()
            val skills = raw.entries.mapNotNull { (idStr, entry) ->
                val id = idStr.toIntOrNull() ?: return@mapNotNull null
                SkillData(
                    id = id,
                    name = entry.name,
                    type = SkillType.fromApiInt(entry.type),
                    mpCost = entry.mpcost,
                    dailyLimit = entry.dailylimit,
                    timesCast = entry.timescast
                )
            }.sortedBy { it.name }
            _state.value = SkillState(skills = skills, isStale = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isStale = true)
        }
    }

    suspend fun cast(skill: SkillData, quantity: Int = 1): Result<Unit> {
        val result = castRequest.cast(skill.id, quantity)
        result.onSuccess {
            val updatedSkills = _state.value.skills.map { s ->
                if (s.id == skill.id) s.copy(timesCast = s.timesCast + quantity) else s
            }
            _state.value = _state.value.copy(skills = updatedSkills)
            eventBus.emit(GameEvent.SkillCast(skill.id, skill.name, quantity))
        }
        return result.map { Unit }
    }
}
```

- [ ] **Step 5: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.skill.SkillManagerTest"
```

Expected: PASS (6 tests)

- [ ] **Step 6: Commit**

```bash
git add shared/src/
git commit -m "feat: SkillCastRequest + SkillManager — fetch skills, cast, track daily counts"
```

---

## Task 5: EffectManager

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/effect/EffectManager.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/effect/EffectManagerTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/effect/EffectManagerTest.kt
package net.sourceforge.kolmafia.effect

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

class EffectManagerTest {

    // api.php?what=effects returns a JSON object keyed by effect ID string.
    private val effectsJson = """
        {
          "1":   {"name": "On the Trail",      "duration": 10},
          "189": {"name": "Musk of the Moose", "duration": 5}
        }
    """.trimIndent()

    private fun makeManager(effectsResponse: String = effectsJson): EffectManager {
        val engine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "effects" ->
                    respond(effectsResponse, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return EffectManager(client, GameEventBus())
    }

    @Test
    fun fetchEffects_populatesEffectList() = runTest {
        val manager = makeManager()
        manager.fetchEffects()
        val state = manager.state.value
        assertEquals(2, state.effects.size)
        assertFalse(state.isStale)
    }

    @Test
    fun fetchEffects_parsesNamesAndDurations() = runTest {
        val manager = makeManager()
        manager.fetchEffects()
        val effects = manager.state.value.effects
        val trail = effects.find { it.id == 1 }
        val moose = effects.find { it.id == 189 }
        assertEquals("On the Trail", trail?.name)
        assertEquals(10, trail?.duration)
        assertEquals("Musk of the Moose", moose?.name)
        assertEquals(5, moose?.duration)
    }

    @Test
    fun fetchEffects_emptyResponse_clearsEffects() = runTest {
        val manager = makeManager(effectsResponse = "{}")
        manager.fetchEffects()
        assertTrue(manager.state.value.effects.isEmpty())
        assertFalse(manager.state.value.isStale)
    }

    @Test
    fun initialState_isEmpty() {
        val manager = makeManager()
        assertTrue(manager.state.value.effects.isEmpty())
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.effect.EffectManagerTest"
```

Expected: FAIL — `EffectManager` does not exist yet.

- [ ] **Step 3: Write `EffectManager.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/effect/EffectManager.kt
package net.sourceforge.kolmafia.effect

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.http.KOL_BASE_URL

// Verify field names against live api.php?what=effects response before shipping.
@Serializable
private data class EffectApiEntry(
    val name: String = "",
    val duration: Int = 0
)

class EffectManager(
    private val client: HttpClient,
    private val eventBus: GameEventBus
) {
    private val _state = MutableStateFlow(EffectState())
    val state: StateFlow<EffectState> = _state.asStateFlow()

    fun initialize(scope: CoroutineScope) {
        scope.launch {
            fetchEffects()
            eventBus.events.collect { event ->
                when (event) {
                    is GameEvent.TurnConsumed, is GameEvent.SkillCast -> fetchEffects()
                    else -> {}
                }
            }
        }
    }

    suspend fun fetchEffects() {
        try {
            val response = client.get("$KOL_BASE_URL/api.php") {
                parameter("what", "effects")
                parameter("for", "KoLmafia-Mobile")
            }
            if (!response.status.isSuccess()) {
                _state.value = _state.value.copy(isStale = true)
                return
            }
            val raw: Map<String, EffectApiEntry> = response.body()
            val effects = raw.entries.mapNotNull { (idStr, entry) ->
                val id = idStr.toIntOrNull() ?: return@mapNotNull null
                EffectData(id = id, name = entry.name, duration = entry.duration)
            }.sortedBy { it.name }
            _state.value = EffectState(effects = effects, isStale = false)
            eventBus.tryEmit(GameEvent.EffectsRefreshed)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isStale = true)
        }
    }
}
```

- [ ] **Step 4: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.effect.EffectManagerTest"
```

Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: EffectManager — fetch active effects, subscribe to TurnConsumed + SkillCast"
```

---

## Task 6: Wire into SharedModule + SessionManager

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt`

- [ ] **Step 1: Replace `SharedModule.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
package net.sourceforge.kolmafia.di

import io.ktor.client.*
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.FightRequest
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.http.createKoLHttpClient
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
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
    singleOf(::SkillCastRequest)
    singleOf(::SkillManager)
    singleOf(::EffectManager)
    singleOf(::SessionManager)
}
```

- [ ] **Step 2: Replace `SessionManager.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt
package net.sourceforge.kolmafia.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.request.LoginResult
import net.sourceforge.kolmafia.skill.SkillManager

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
    private val familiarManager: FamiliarManager,
    private val skillManager: SkillManager,
    private val effectManager: EffectManager
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
                        skillManager.initialize(appScope)
                        effectManager.initialize(appScope)
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

- [ ] **Step 4: Run full test suite — all existing tests still green**

```bash
./gradlew :shared:jvmTest
```

Expected: All previously passing tests PASS. No regressions.

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: wire SkillManager + EffectManager into SharedModule and SessionManager"
```

---

## Task 7: Update CharacterScreen — Full Stats + Effects Button

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/character/CharacterScreen.kt`

- [ ] **Step 1: Replace `CharacterScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/character/CharacterScreen.kt
package net.sourceforge.kolmafia.ui.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.ui.effects.EffectsScreen
import org.koin.compose.koinInject

@Composable
fun CharacterScreen(character: KoLCharacter) {
    val state by character.state.collectAsState()
    val effectManager: EffectManager = koinInject()
    val effectState by effectManager.state.collectAsState()
    var showEffects by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.name, style = MaterialTheme.typography.headlineMedium)
            if (state.isHardcore) {
                Text("HC", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error)
            }
        }
        Text("Level ${state.level}", style = MaterialTheme.typography.titleMedium)
        if (state.challengePath.isNotBlank() && state.challengePath != "None") {
            Text(state.challengePath, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.zodiacSign.isNotBlank()) {
            Text("Sign: ${state.zodiacSign}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.roninLeft > 0) {
            Text("Ronin: ${state.roninLeft} turns remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary)
        }

        Spacer(Modifier.height(16.dp))
        // HP/MP bars
        StatBar(label = "HP", current = state.currentHp, max = state.maxHp)
        Spacer(Modifier.height(8.dp))
        StatBar(label = "MP", current = state.currentMp, max = state.maxMp)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Combat stats
        Text("Stats", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        StatRow("Muscle",       state.baseMusc, state.muscSubpoints)
        StatRow("Mysticality",  state.baseMyst, state.mystSubpoints)
        StatRow("Moxie",        state.baseMoxie, state.moxieSubpoints)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Resources
        Text("Resources", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        InfoRow("Meat",        state.meat.toString())
        InfoRow("Adventures",  state.adventuresLeft.toString())
        InfoRow("Fullness",    "${state.fullness} / 15")
        InfoRow("Inebriety",   "${state.inebriety} / 14")
        InfoRow("Spleen",      "${state.spleenUsed} / 15")

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Effects (tap to open full list)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEffects = true }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Active Effects", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${effectState.effects.size} →",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        if (effectState.effects.isNotEmpty()) {
            effectState.effects.take(3).forEach { effect ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(effect.name, style = MaterialTheme.typography.bodySmall)
                    Text("${effect.duration} turns", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (effectState.effects.size > 3) {
                Text("… and ${effectState.effects.size - 3} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showEffects) {
        EffectsScreen(effectManager = effectManager, onDismiss = { showEffects = false })
    }
}

@Composable
private fun StatBar(label: String, current: Int, max: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$current / $max", style = MaterialTheme.typography.labelLarge)
        }
        if (max > 0) {
            LinearProgressIndicator(
                progress = { current.toFloat() / max.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatRow(label: String, base: Int, subpoints: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("$base ($subpoints xp)", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: FAIL — `EffectsScreen` does not exist yet. This is expected — proceed to Task 8.

---

## Task 8: EffectsScreen

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/effects/EffectsScreen.kt`

- [ ] **Step 1: Write `EffectsScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/effects/EffectsScreen.kt
package net.sourceforge.kolmafia.ui.effects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.effect.EffectManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectsScreen(effectManager: EffectManager, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartialExpansion = true)
    val scope = rememberCoroutineScope()
    val effectState by effectManager.state.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Active Effects", style = MaterialTheme.typography.titleLarge)
                Text("${effectState.effects.size} effects",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))

            if (effectState.effects.isEmpty()) {
                Text("No active effects.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp))
            } else {
                LazyColumn {
                    items(effectState.effects, key = { it.id }) { effect ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(effect.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium)
                            val color = when {
                                effect.duration <= 1 -> MaterialTheme.colorScheme.error
                                effect.duration <= 5 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text("${effect.duration}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = color)
                        }
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add shared/src/
git commit -m "feat: CharacterScreen full stats + EffectsScreen bottom sheet"
```

---

## Task 9: SkillsScreen

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/skills/SkillsScreen.kt`

- [ ] **Step 1: Write `SkillsScreen.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/skills/SkillsScreen.kt
package net.sourceforge.kolmafia.ui.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import org.koin.compose.koinInject

private val TABS = listOf("All", "Combat", "Noncombat", "Buff", "Summon", "Passive")

@Composable
fun SkillsScreen() {
    val skillManager: SkillManager = koinInject()
    val state by skillManager.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var castingSkillId by remember { mutableStateOf<Int?>(null) }
    var castError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val filteredSkills = remember(selectedTab, state.skills) {
        state.skills.filter { skill ->
            when (selectedTab) {
                0 -> true
                1 -> skill.type == SkillType.COMBAT
                2 -> skill.type == SkillType.NONCOMBAT
                3 -> skill.type == SkillType.BUFF
                4 -> skill.type == SkillType.SUMMON
                5 -> skill.type == SkillType.PASSIVE
                else -> true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Skills", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp))

        ScrollableTabRow(selectedTabIndex = selectedTab) {
            TABS.forEachIndexed { i, label ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i; castError = null }) {
                    Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp))
                }
            }
        }

        castError?.let { error ->
            Text(error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredSkills, key = { it.id }) { skill ->
                SkillRow(
                    skill = skill,
                    isCasting = castingSkillId == skill.id,
                    onCast = {
                        castError = null
                        castingSkillId = skill.id
                        scope.launch {
                            skillManager.cast(skill, 1).fold(
                                onSuccess = { castingSkillId = null },
                                onFailure = { e ->
                                    castingSkillId = null
                                    castError = "${skill.name}: ${e.message}"
                                }
                            )
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SkillRow(skill: SkillData, isCasting: Boolean, onCast: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(skill.name, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(skill.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (skill.mpCost > 0) {
                    Text("${skill.mpCost} MP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (skill.isActive) {
            Spacer(Modifier.size(8.dp))
            BadgedBox(
                badge = {
                    if (skill.dailyLimit > 0) {
                        Badge {
                            Text("${skill.timesCast}/${skill.dailyLimit}",
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            ) {
                Button(
                    onClick = onCast,
                    enabled = !isCasting && skill.canCastMore
                ) {
                    if (isCasting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Cast")
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add shared/src/
git commit -m "feat: SkillsScreen — filterable skill list with cast button and daily limit badge"
```

---

## Task 10: ZoneBrowserSheet + Wire into AdventureScreen

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/adventure/ZoneBrowserSheet.kt`
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/adventure/AdventureScreen.kt`

- [ ] **Step 1: Write `ZoneBrowserSheet.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/adventure/ZoneBrowserSheet.kt
package net.sourceforge.kolmafia.ui.adventure

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.location.LocationData
import net.sourceforge.kolmafia.location.LocationDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneBrowserSheet(
    onLocationSelected: (LocationData) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartialExpansion = true)
    var query by remember { mutableStateOf("") }
    val results = remember(query) { LocationDatabase.search(query) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Browse Zones", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search by name or zone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Text("${results.size} zones", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            LazyColumn {
                items(results, key = { it.snarfblat }) { location ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLocationSelected(location)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(location.name, style = MaterialTheme.typography.bodyMedium)
                            Text("${location.zone} · Lv ${location.recommendedLevel}+",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("#${location.snarfblat}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 2: Replace `AdventureScreen.kt` to add Browse button**

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    var showZoneBrowser by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
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

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = zoneId,
                onValueChange = { zoneId = it },
                label = { Text("Zone ID (snarfblat)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = { showZoneBrowser = true },
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
            ) { Text("Browse") }
        }
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

    if (showZoneBrowser) {
        ZoneBrowserSheet(
            onLocationSelected = { location ->
                zoneId = location.snarfblat
                zoneName = location.name
            },
            onDismiss = { showZoneBrowser = false }
        )
    }
}
```

- [ ] **Step 3: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add shared/src/
git commit -m "feat: ZoneBrowserSheet + Browse button in AdventureScreen"
```

---

## Task 11: Update App.kt — 5-Tab Navigation

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/App.kt`

- [ ] **Step 1: Replace `App.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ui/App.kt
package net.sourceforge.kolmafia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Favorite
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
import net.sourceforge.kolmafia.ui.skills.SkillsScreen
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
                        icon = { Icon(Icons.AutoMirrored.Filled.List, "Inventory") },
                        label = { Text("Inventory") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.AutoFixHigh, "Skills") },
                        label = { Text("Skills") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
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
                3 -> SkillsScreen()
                4 -> FamiliarScreen()
            }
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run full test suite**

```bash
./gradlew :shared:jvmTest
```

Expected: All tests PASS. Check count includes all new tests from Tasks 1–5.

- [ ] **Step 4: Build Android APK**

```bash
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. APK at `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

- [ ] **Step 5: Commit**

```bash
git add shared/ androidApp/
git commit -m "feat: 5-tab navigation — add Skills tab (Character/Adventure/Inventory/Skills/Familiars)"
```

---

## Self-Review

**Spec coverage (Phase 2 out-of-scope items addressed):**

| Phase 2 out-of-scope | Phase 3 task |
|---|---|
| N/A (Phase 2 had no explicit Phase 3 roadmap; Phase 3 scope defined here) | — |

**Phase 3 deliverables:**

| Feature | Task |
|---|---|
| Muscle/Mysticality/Moxie stats + subpoints in CharacterState | Task 1 |
| Sign, path, hardcore flag, ronin countdown | Task 1 |
| SkillData: id, name, type, mpCost, dailyLimit, timesCast | Task 2 |
| EffectData: id, name, duration | Task 2 |
| GameEvent.SkillCast + EffectsRefreshed | Task 2 |
| LocationData + bundled zone list with search | Task 3 |
| SkillCastRequest: POST skills.php, detect MP/daily-limit errors | Task 4 |
| SkillManager: fetch api.php?what=skills, cast, increment timesCast | Task 4 |
| EffectManager: fetch api.php?what=effects, subscribe to TurnConsumed + SkillCast | Task 5 |
| SharedModule + SessionManager wired with new managers | Task 6 |
| CharacterScreen: Mus/Mys/Mox rows, sign/path/hardcore/ronin, effects count + tap | Task 7 |
| EffectsScreen: ModalBottomSheet, effects list with duration colour coding | Task 8 |
| SkillsScreen: filterable tabs, Cast button, daily-limit badge, loading/error state | Task 9 |
| ZoneBrowserSheet: search field, zone list with level hints | Task 10 |
| AdventureScreen: Browse button → ZoneBrowserSheet, zone auto-fill on selection | Task 10 |
| App.kt: 5-tab nav (Character/Adventure/Inventory/Skills/Familiars) | Task 11 |

**Placeholder scan:** No TBD, TODO, "implement later", or "similar to Task N" patterns. Every step shows complete code.

**Type consistency check:**
- `SkillData` defined in Task 2, used identically in Tasks 4 and 9 — ✓
- `EffectData` defined in Task 2, used identically in Tasks 5 and 8 — ✓
- `LocationData` defined in Task 3, used identically in Tasks 10 — ✓
- `CharacterState.baseMusc / muscSubpoints / baseMyst / mystSubpoints / baseMoxie / moxieSubpoints / zodiacSign / challengePath / roninLeft / isHardcore` defined in Task 1, read in Task 7 — ✓
- `GameEvent.SkillCast` defined in Task 2, emitted in Task 4, subscribed in Task 5 — ✓
- `GameEvent.EffectsRefreshed` defined in Task 2, emitted in Task 5 — ✓
- `EffectManager` added to `SharedModule` + `SessionManager` in Task 6, injected via `koinInject()` in Task 7 — ✓
- `SkillManager` + `SkillCastRequest` added to `SharedModule` in Task 6, injected via `koinInject()` in Task 9 — ✓

**Note for developers:** The exact JSON field names for `api.php?what=skills` and `api.php?what=effects` should be verified against the live KoL API before shipping. Both managers use `ignoreUnknownKeys = true` and default all fields to safe values, so unrecognised or missing fields will degrade gracefully.
