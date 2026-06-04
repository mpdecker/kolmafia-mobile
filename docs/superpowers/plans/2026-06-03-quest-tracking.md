# Quest Tracking — Full Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port all 104 KoLmafia quests into the mobile enum, parse `questslog.txt` to detect quest progress from `questlog.php` HTML, sync quest state at login and after each adventure turn that signals advancement.

**Architecture:** `Quest.kt` expands from 2 → 104 enum entries. `QuestDatabase` gains `validateStep` + `setProgressByPrefKey`. `QuestLogDatabase` (new, in `data/`) parses the bundled `questslog.txt` at startup and implements HTML text matching. `QuestLogRequest` (new, in `request/`) fetches `questlog.php` in three pages and writes state via `QuestDatabase`. `AdventureManager` captures each turn's response HTML and triggers a re-sync when quest-advancement text is detected. `SessionManager` triggers a full sync after login.

**Tech Stack:** Kotlin Multiplatform, Compose Resources (`Res.readBytes`), Ktor client + MockEngine, `com.russhwolf.settings.MapSettings` for test preferences, Koin DI.

---

## File Map

| Status | File | Responsibility |
|--------|------|----------------|
| **Modify** | `quest/Quest.kt` | 2 → 104 enum entries |
| **Modify** | `quest/QuestDatabase.kt` | Add `validateStep` + `setProgressByPrefKey` |
| **Create** | `data/QuestLogDatabase.kt` | Parse `questslog.txt`; `detectStep` HTML → step name |
| **Create** | `request/QuestLogRequest.kt` | Fetch `questlog.php` pages; write to `QuestDatabase` |
| **Modify** | `adventure/AdventureManager.kt` | Capture response HTML; `checkQuestAdvancement`; `questLogRequest` param |
| **Modify** | `session/SessionManager.kt` | `questLogRequest` param; `syncAll()` on login |
| **Modify** | `data/GameDatabase.kt` | Call `QuestLogDatabase.load()` |
| **Modify** | `di/SharedModule.kt` | Register `QuestLogRequest`; inject into `AdventureManager` + `SessionManager` |
| **Modify** | `quest/QuestDatabaseTest.kt` | Tests for `validateStep` + `setProgressByPrefKey` |
| **Create** | `quest/QuestLogDatabaseTest.kt` | Tests for `load()` + `detectStep()` |
| **Create** | `request/QuestLogRequestTest.kt` | Tests for `parsePage()` with `MockEngine` |
| **Modify** | `adventure/AdventureManagerTest.kt` | Tests for `checkQuestAdvancement()` |

All source paths are under:
- `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/`
- `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/`

---

## Key Types Reference

```kotlin
// Existing — quest/QuestDatabase.kt
class QuestDatabase(private val preferences: Preferences) {
    companion object {
        const val UNSTARTED = "unstarted"
        const val STARTED   = "started"
        const val FINISHED  = "finished"
        fun stepOrdinal(step: String): Int  // -1/0/MAX/numeric
    }
    fun getProgress(quest: Quest): String
    fun setProgress(quest: Quest, step: String)
    fun isQuestLaterThan(quest: Quest, step: String): Boolean
    fun isQuestFinished(quest: Quest): Boolean
}

// Existing — questslog.txt format:
// version (int, skip)
// # comments (skip)
// prefKey\tTitle\ttext0\ttext1\t...\ttextN
// texts[0] = "started" text, texts[last] = "finished" text,
// texts[1..last-1] = "step1".."step(N-1)" texts
```

---

## Task 1: Expand Quest Enum to 104 Entries

**Files:**
- Modify: `quest/Quest.kt`
- Modify: `quest/QuestDatabaseTest.kt`

- [ ] **Step 1: Write a failing test — no duplicate pref keys**

Add to `quest/QuestDatabaseTest.kt` (at the bottom of the class, before closing `}`):

```kotlin
@Test fun allQuestPrefKeysAreUnique() {
    val keys = Quest.entries.map { it.prefKey }
    assertEquals(keys.size, keys.distinct().size,
        "Duplicate prefKeys: ${keys.groupBy { it }.filter { it.value.size > 1 }.keys}")
}
```

Also add `import kotlin.test.assertEquals` if not already present.

- [ ] **Step 2: Run test to verify it fails (or trivially passes with 2 entries)**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestDatabaseTest.allQuestPrefKeysAreUnique"
```

Expected: PASS (with 2 entries, no duplication) — but confirms test compiles.

- [ ] **Step 3: Replace Quest.kt with all 104 entries**

Replace the entire content of `quest/Quest.kt`:

```kotlin
package net.sourceforge.kolmafia.quest

enum class Quest(val prefKey: String) {
    // ── Council quests ────────────────────────────────────────────────────────
    LARVA("questL02Larva"),
    RAT("questL03Rat"),
    BAT("questL04Bat"),
    GOBLIN("questL05Goblin"),
    FRIAR("questL06Friar"),
    CYRPT("questL07Cyrptic"),
    TRAPPER("questL08Trapper"),
    TOPPING("questL09Topping"),
    GARBAGE("questL10Garbage"),
    MACGUFFIN("questL11MacGuffin"),
    BLACK("questL11Black"),
    WORSHIP("questL11Worship"),
    MANOR("questL11Manor"),
    PYRAMID("questL11Pyramid"),
    PALINDOME("questL11Palindome"),
    SHEN("questL11Shen"),
    RON("questL11Ron"),
    CURSES("questL11Curses"),
    DOCTOR("questL11Doctor"),
    BUSINESS("questL11Business"),
    SPARE("questL11Spare"),
    DESERT("questL11Desert"),
    ISLAND_WAR("questL12War"),
    HIPPY_FRAT("questL12HippyFrat"),
    FINAL("questL13Final"),
    WAREHOUSE("questL13Warehouse"),

    // ── Guild quests ──────────────────────────────────────────────────────────
    MEATCAR("questG01Meatcar"),
    CITADEL("questG02Whitecastle"),
    EGO("questG03Ego"),
    NEMESIS("questG04Nemesis"),
    DARK("questG05Dark"),
    FACTORY("questG06Delivery"),
    MYST("questG07Myst"),
    MOXIE("questG08Moxie"),
    MUSCLE("questG09Muscle"),

    // ── Miscellaneous quests ──────────────────────────────────────────────────
    UNTINKER("questM01Untinker"),
    ARTIST("questM02Artist"),
    BUGBEAR("questM03Bugbear"),
    TOOT("questM05Toot"),
    HAMMER("questM07Hammer"),
    BAKER("questM08Baker"),
    AZAZEL("questM10Azazel"),
    PIRATE("questM12Pirate"),
    ESCAPE("questM13Escape"),
    LOL("questM15Lol"),
    TEMPLE("questM16Temple"),
    SPOOKYRAVEN_BABIES("questM17Babies"),
    SWAMP("questM18Swamp"),
    HIPPY("questM19Hippy"),
    SPOOKYRAVEN_NECKLACE("questM20Necklace"),
    SPOOKYRAVEN_DANCE("questM21Dance"),
    SHIRT("questM22Shirt"),
    MEATSMITH("questM23Meatsmith"),
    DOC("questM24Doc"),
    ARMORER("questM25Armorer"),
    ORACLE("questM26Oracle"),

    // ── Future / Sea quests ───────────────────────────────────────────────────
    PRIMORDIAL("questF01Primordial"),
    FUTURE("questF03Future"),
    GENERATOR("questF04Elves"),
    CLANCY("questF05Clancy"),
    SEA_OLD_GUY("questS01OldGuy"),
    SEA_MONKEES("questS02Monkees"),

    // ── Spookyraven sub-quests ────────────────────────────────────────────────
    JIMMY_MUSHROOM("questESlMushStash"),
    JIMMY_CHEESEBURGER("questESlCheeseburger"),
    JIMMY_SALT("questESlSalt"),
    TACO_DAN_AUDIT("questESlAudit"),
    TACO_DAN_COCKTAIL("questESlCocktail"),
    TACO_DAN_FISH("questESlFish"),
    BRODEN_BACTERIA("questESlBacteria"),
    BRODEN_SPRINKLES("questESlSprinkles"),
    BRODEN_DEBT("questESlDebt"),

    // ── Special event quests ──────────────────────────────────────────────────
    EVE("questESpEVE"),
    JUNGLE_PUN("questESpJunglePun"),
    GORE("questESpGore"),
    CLIPPER("questESpClipper"),
    FAKE_MEDIUM("questESpFakeMedium"),
    SERUM("questESpSerum"),
    SMOKES("questESpSmokes"),
    OUT_OF_ORDER("questESpOutOfOrder"),

    // ── Standard event quests ─────────────────────────────────────────────────
    FISH_TRASH("questEStFishTrash"),
    GIVE_ME_FUEL("questEStGiveMeFuel"),
    NASTY_BEARS("questEStNastyBears"),
    SOCIAL_JUSTICE_I("questEStSocialJusticeI"),
    SOCIAL_JUSTICE_II("questEStSocialJusticeII"),
    SUPER_LUBER("questEStSuperLuber"),
    WORK_WITH_FOOD("questEStWorkWithFood"),
    ZIPPITY_DOO_DAH("questEStZippityDooDah"),

    // ── Community / one-off quests ────────────────────────────────────────────
    BUCKET("questECoBucket"),
    TELEGRAM("questLTTQuestByWire"),
    GHOST("questPAGhost"),
    NEW_YOU("questEUNewYou"),
    PARTY_FAIR("_questPartyFair"),
    DOCTOR_BAG("questDoctorBag"),
    GUZZLR("questGuzzlr"),
    CLUMSINESS("questClumsinessGrove"),
    MAELSTROM("questMaelstromOfLovers"),
    GLACIER("questGlacierOfJerks"),
    RUFUS("questRufus"),
    PIRATEREALM("_questPirateRealm"),
}
```

- [ ] **Step 4: Run the uniqueness test to verify it passes**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestDatabaseTest.allQuestPrefKeysAreUnique"
```

Expected: `BUILD SUCCESSFUL` — 104 unique pref keys confirmed.

- [ ] **Step 5: Run full test suite to check for regressions**

```
gradlew.bat :shared:jvmTest
```

Expected: `BUILD SUCCESSFUL` — existing `MEATSMITH` and `ARMORER` references still compile.

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/Quest.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/QuestDatabaseTest.kt
git commit -m "feat: expand Quest enum from 2 to 104 entries"
```

---

## Task 2: QuestDatabase — validateStep and setProgressByPrefKey

**Files:**
- Modify: `quest/QuestDatabase.kt`
- Modify: `quest/QuestDatabaseTest.kt`

- [ ] **Step 1: Write failing tests**

Append to `QuestDatabaseTest.kt` (inside the class, before closing `}`):

```kotlin
    // ── validateStep ────────────────────────────────────────────────────────

    @Test fun validateStep_acceptsUnstarted() =
        assertEquals(QuestDatabase.UNSTARTED, QuestDatabase.validateStep("unstarted"))

    @Test fun validateStep_acceptsStarted() =
        assertEquals(QuestDatabase.STARTED, QuestDatabase.validateStep("started"))

    @Test fun validateStep_acceptsFinished() =
        assertEquals(QuestDatabase.FINISHED, QuestDatabase.validateStep("finished"))

    @Test fun validateStep_acceptsStep1() =
        assertEquals("step1", QuestDatabase.validateStep("step1"))

    @Test fun validateStep_acceptsStep42() =
        assertEquals("step42", QuestDatabase.validateStep("step42"))

    @Test fun validateStep_rejectsGarbage_returnsUnstarted() =
        assertEquals(QuestDatabase.UNSTARTED, QuestDatabase.validateStep("bogus"))

    @Test fun validateStep_rejectsStepWithNoNumber_returnsUnstarted() =
        assertEquals(QuestDatabase.UNSTARTED, QuestDatabase.validateStep("step"))

    // ── setProgressByPrefKey ────────────────────────────────────────────────

    @Test fun setProgressByPrefKey_validStep_writesPreference() {
        val db = db()
        db.setProgressByPrefKey("questL02Larva", "step1")
        // Read back via raw preferences — QuestDatabase stores via quest.prefKey
        // Verify by setting LARVA to started first and then checking step1 is later
        val db2 = QuestDatabase(db.preferencesForTest())
        assertTrue(db2.isQuestLaterThan(Quest.LARVA, QuestDatabase.STARTED))
    }

    @Test fun setProgressByPrefKey_invalidStep_writesUnstarted() {
        val db = db()
        db.setProgressByPrefKey("questL02Larva", "garbage")
        val db2 = QuestDatabase(db.preferencesForTest())
        assertFalse(db2.isQuestLaterThan(Quest.LARVA, QuestDatabase.UNSTARTED))
    }
```

These tests need `db().preferencesForTest()` — a way to expose the preferences for cross-instance verification. Add a helper to `QuestDatabaseTest.kt`:

```kotlin
    // Override db() to return a TestableQuestDatabase instead
    // Simpler: use a shared Preferences instance per test
    private val sharedSettings = MapSettings()
    private fun sharedDb() = QuestDatabase(Preferences(sharedSettings))
```

Rewrite the two `setProgressByPrefKey` tests to use `sharedDb()`:

```kotlin
    @Test fun setProgressByPrefKey_validStep_writesPreference() {
        val db = sharedDb()
        db.setProgressByPrefKey("questL02Larva", "step1")
        assertTrue(sharedDb().isQuestLaterThan(Quest.LARVA, QuestDatabase.STARTED))
    }

    @Test fun setProgressByPrefKey_invalidStep_writesUnstarted() {
        val db = sharedDb()
        db.setProgressByPrefKey("questL02Larva", "garbage")
        assertFalse(sharedDb().isQuestLaterThan(Quest.LARVA, QuestDatabase.UNSTARTED))
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestDatabaseTest"
```

Expected: compile failure — `validateStep` and `setProgressByPrefKey` not defined.

- [ ] **Step 3: Implement validateStep and setProgressByPrefKey in QuestDatabase.kt**

Add `validateStep` to the `companion object` and `setProgressByPrefKey` as an instance method:

```kotlin
package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

class QuestDatabase(private val preferences: Preferences) {

    companion object {
        const val UNSTARTED = "unstarted"
        const val STARTED   = "started"
        const val FINISHED  = "finished"

        fun stepOrdinal(step: String): Int = when (step) {
            UNSTARTED -> -1
            STARTED   ->  0
            FINISHED  ->  Int.MAX_VALUE
            else      -> step.removePrefix("step").toIntOrNull() ?: -1
        }

        fun validateStep(step: String): String = when {
            step == UNSTARTED || step == STARTED || step == FINISHED -> step
            step.matches(Regex("step\\d+")) -> step
            else -> UNSTARTED
        }
    }

    fun getProgress(quest: Quest): String =
        preferences.getString(quest.prefKey, UNSTARTED)

    fun setProgress(quest: Quest, step: String) =
        preferences.setString(quest.prefKey, step)

    fun setProgressByPrefKey(prefKey: String, step: String) =
        preferences.setString(prefKey, validateStep(step))

    fun isQuestLaterThan(quest: Quest, step: String): Boolean {
        val current = getProgress(quest)
        return stepOrdinal(current) > stepOrdinal(step)
    }

    fun isQuestFinished(quest: Quest): Boolean =
        getProgress(quest) == FINISHED
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestDatabaseTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/QuestDatabase.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/QuestDatabaseTest.kt
git commit -m "feat: QuestDatabase validateStep and setProgressByPrefKey"
```

---

## Task 3: QuestLogDatabase — Parse questslog.txt

**Files:**
- Create: `data/QuestLogDatabase.kt`
- Create: `quest/QuestLogDatabaseTest.kt`

- [ ] **Step 1: Write failing tests for load() and findByTitle()**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/QuestLogDatabaseTest.kt`:

```kotlin
package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.data.QuestLogEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestLogDatabaseTest {

    // Parse a minimal questslog.txt fixture (no file I/O needed)
    private fun loadFixture(text: String): Map<String, QuestLogEntry> =
        QuestLogDatabase.parseForTest(text)

    // ── parseForTest / findByTitle ───────────────────────────────────────────

    @Test fun parse_skipsVersionAndCommentLines() {
        val entries = loadFixture("1\n# comment\n")
        assertTrue(entries.isEmpty())
    }

    @Test fun parse_singleEntry_twoTexts_startedAndFinished() {
        val entries = loadFixture(
            "questL02Larva\tFind a Larva\tGo find the larva.\tYou found the larva!\n"
        )
        val entry = entries["find a larva"]
        assertNotNull(entry)
        assertEquals("questL02Larva", entry.prefKey)
        assertEquals(2, entry.steps.size)
        assertEquals("started", entry.steps[0].first)
        assertEquals("go find the larva.", entry.steps[0].second)
        assertEquals("finished", entry.steps[1].first)
        assertEquals("you found the larva!", entry.steps[1].second)
    }

    @Test fun parse_threeTexts_hasStep1() {
        val entries = loadFixture(
            "questL03Rat\tSmell a Rat\tTalk to Bart.\tExplore the cellar.\tDone!\n"
        )
        val entry = entries["smell a rat"]
        assertNotNull(entry)
        assertEquals(3, entry.steps.size)
        assertEquals("started", entry.steps[0].first)
        assertEquals("step1",   entry.steps[1].first)
        assertEquals("finished", entry.steps[2].first)
    }

    @Test fun parse_singleText_finishedOnly() {
        val entries = loadFixture("questM05Toot\tToot Tutorial\tYou are done.\n")
        val entry = entries["toot tutorial"]
        assertNotNull(entry)
        assertEquals(1, entry.steps.size)
        assertEquals("finished", entry.steps[0].first)
    }

    @Test fun parse_titleLookupCaseInsensitive() {
        val entries = loadFixture("questL02Larva\tFind A Larva\tGo.\tDone!\n")
        assertNotNull(entries["find a larva"])
        assertNotNull(entries["FIND A LARVA"])
    }

    @Test fun parse_missingColumns_skipped() {
        val entries = loadFixture("questL02Larva\tTitle Only\n")
        assertNull(entries["title only"])
    }

    @Test fun parse_stripsHtmlTagsFromStepText() {
        val entries = loadFixture(
            "questL02Larva\tFind a Larva\t<b>Go</b> find <i>the</i> larva.\tDone!\n"
        )
        val entry = entries["find a larva"]!!
        assertEquals("go find the larva.", entry.steps[0].second)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestLogDatabaseTest"
```

Expected: compile failure — `QuestLogDatabase` does not exist.

- [ ] **Step 3: Create QuestLogDatabase.kt**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/QuestLogDatabase.kt`:

```kotlin
package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

data class QuestLogEntry(
    val prefKey: String,
    val title: String,
    val steps: List<Pair<String, String>>,  // stepName → normalized body text
)

@OptIn(ExperimentalResourceApi::class)
object QuestLogDatabase {

    private val byTitle = mutableMapOf<String, QuestLogEntry>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/questslog.txt").decodeToString()
        val parsed = parse(text)
        byTitle.putAll(parsed)
        loaded = true
    }

    fun findByTitle(title: String): QuestLogEntry? = byTitle[title.lowercase().trim()]

    fun detectStep(entry: QuestLogEntry, bodyHtml: String): String {
        val normalized = bodyHtml
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
        for ((stepName, stepText) in entry.steps.asReversed()) {
            if (stepText.isNotEmpty() && normalized.contains(stepText)) return stepName
        }
        return "started"
    }

    // Exposed for tests — parses text and returns the byTitle map without touching the singleton.
    internal fun parseForTest(text: String): Map<String, QuestLogEntry> = parse(text)

    private fun parse(text: String): Map<String, QuestLogEntry> {
        val result = mutableMapOf<String, QuestLogEntry>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue  // version line
            val parts = line.split('\t')
            if (parts.size < 3) continue  // need prefKey, title, at least one text
            val prefKey = parts[0]
            val title   = parts[1]
            val texts   = parts.drop(2)
            val steps   = buildSteps(texts)
            result[title.lowercase().trim()] = QuestLogEntry(prefKey, title, steps)
        }
        return result
    }

    private fun buildSteps(texts: List<String>): List<Pair<String, String>> {
        if (texts.isEmpty()) return emptyList()
        if (texts.size == 1) return listOf("finished" to texts[0].normalizeText())
        val result = mutableListOf<Pair<String, String>>()
        result.add("started" to texts[0].normalizeText())
        for (i in 1 until texts.size - 1) {
            result.add("step$i" to texts[i].normalizeText())
        }
        result.add("finished" to texts.last().normalizeText())
        return result
    }

    private fun String.normalizeText(): String =
        replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
}
```

- [ ] **Step 4: Run tests to verify they pass**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestLogDatabaseTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/QuestLogDatabase.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/QuestLogDatabaseTest.kt
git commit -m "feat: QuestLogDatabase — parse questslog.txt and detectStep"
```

---

## Task 4: QuestLogDatabase — detectStep Tests

**Files:**
- Modify: `quest/QuestLogDatabaseTest.kt`

- [ ] **Step 1: Add detectStep tests**

Append to `QuestLogDatabaseTest.kt` (inside the class, before closing `}`):

```kotlin
    // ── detectStep ───────────────────────────────────────────────────────────

    private fun entry(vararg steps: Pair<String, String>) = QuestLogEntry(
        prefKey = "questTest",
        title   = "Test Quest",
        steps   = steps.toList(),
    )

    @Test fun detectStep_bodyMatchesStarted_returnsStarted() {
        val e = entry("started" to "go find the larva", "finished" to "you found it")
        assertEquals("started", QuestLogDatabase.detectStep(e, "Go find the larva."))
    }

    @Test fun detectStep_bodyMatchesFinished_returnsFinished() {
        val e = entry("started" to "go find the larva", "finished" to "you found it")
        assertEquals("finished", QuestLogDatabase.detectStep(e, "You found it!"))
    }

    @Test fun detectStep_bodyMatchesStep1_returnsStep1() {
        val e = entry(
            "started" to "go talk to bart",
            "step1"   to "explore the cellar",
            "finished" to "done!"
        )
        assertEquals("step1", QuestLogDatabase.detectStep(e, "Explore the cellar now."))
    }

    @Test fun detectStep_noMatch_returnsStarted() {
        val e = entry("started" to "go find", "finished" to "you found")
        assertEquals("started", QuestLogDatabase.detectStep(e, "Something completely different."))
    }

    @Test fun detectStep_stripsHtmlBeforeMatching() {
        val e = entry("started" to "go find the larva", "finished" to "you found it")
        assertEquals("started", QuestLogDatabase.detectStep(e, "<b>Go</b> find <i>the larva</i>."))
    }

    @Test fun detectStep_caseInsensitive() {
        val e = entry("started" to "go find the larva", "finished" to "you found it")
        assertEquals("started", QuestLogDatabase.detectStep(e, "GO FIND THE LARVA NOW"))
    }

    @Test fun detectStep_prefersLaterStepWhenBodyContainsBoth() {
        // When the body shows step1 text AND finished text (progressive disclosure)
        val e = entry(
            "started"  to "go talk to bart",
            "step1"    to "explore the cellar",
            "finished" to "rats gone"
        )
        // Body contains both step1 text and finished text (quest log accumulates)
        val body = "Explore the cellar. Rats gone. Quest complete!"
        assertEquals("finished", QuestLogDatabase.detectStep(e, body))
    }
```

- [ ] **Step 2: Run tests to verify they pass**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestLogDatabaseTest"
```

Expected: `BUILD SUCCESSFUL` — all detectStep tests pass.

- [ ] **Step 3: Commit**

```
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/QuestLogDatabaseTest.kt
git commit -m "test: QuestLogDatabase detectStep edge cases"
```

---

## Task 5: GameDatabase — Load QuestLogDatabase at Startup

**Files:**
- Modify: `data/GameDatabase.kt`

- [ ] **Step 1: Add QuestLogDatabase.load() to GameDatabase.load()**

In `GameDatabase.kt`, find the `load()` method. The method loads various databases in order. Find the section that loads quest data (or add after the `NpcStoreDatabase.load()` / `DailyLimitDatabase.load()` lines) and add:

```kotlin
        // Quest log text patterns
        QuestLogDatabase.load()
```

The full load() will look like this at the relevant section:

```kotlin
        // Item relationships and shop data
        ZapGroupDatabase.load()
        FoldGroupDatabase.load()
        PackageDatabase.load()
        NpcStoreDatabase.load()
        DailyLimitDatabase.load()

        // Quest log text patterns (for quest state detection)
        QuestLogDatabase.load()

        isLoaded = true
```

- [ ] **Step 2: Compile check**

```
gradlew.bat :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt
git commit -m "feat: load QuestLogDatabase as part of GameDatabase startup"
```

---

## Task 6: QuestLogRequest — HTTP Fetch and Parse

**Files:**
- Create: `request/QuestLogRequest.kt`
- Create: `request/QuestLogRequestTest.kt`

- [ ] **Step 1: Write failing tests**

Create `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/QuestLogRequestTest.kt`:

```kotlin
package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.data.QuestLogEntry
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class QuestLogRequestTest {

    // Pre-populate QuestLogDatabase with a fixture entry (bypass file I/O)
    private fun setupDatabase(vararg entries: QuestLogEntry) {
        QuestLogDatabase.injectForTest(entries.toList())
    }

    private fun db(settings: MapSettings = MapSettings()) =
        QuestDatabase(Preferences(settings))

    // fixture HTML simulating a questlog.php page
    private val fixtureHtmlPage1 = """
        <html><body>
        <b>Looking for a Larva in All the Wrong Places</b><br>
        Return to the Council of Loathing with the mosquito larva.
        <p>
        <b>Ooh, I Think I Smell a Rat</b><br>
        Go talk to the owner of the Typical Tavern in the Distant Woods.
        </body></html>
    """.trimIndent()

    private fun mockClient(page1: String, page2: String = "", page3: String = ""): HttpClient {
        var callCount = 0
        return HttpClient(MockEngine { _ ->
            callCount++
            val body = when (callCount) {
                1 -> page1
                2 -> page2
                else -> page3
            }
            respond(body, HttpStatusCode.OK)
        })
    }

    @Test fun parsePage_knownQuest_setsProgress() = runBlocking {
        val settings = MapSettings()
        val questDb = db(settings)
        setupDatabase(
            QuestLogEntry(
                prefKey = "questL02Larva",
                title   = "Looking for a Larva in All the Wrong Places",
                steps   = listOf(
                    "started"  to "go find the larva",
                    "step1"    to "return to the council of loathing with the mosquito larva",
                    "finished" to "you delivered a mosquito larva"
                )
            )
        )
        val request = QuestLogRequest(mockClient(fixtureHtmlPage1), questDb)
        request.syncPage(1)
        // Body contains "return to the council" → matches step1
        assertEquals("step1", questDb.getProgress(Quest.LARVA))
    }

    @Test fun parsePage_unknownQuest_setsStarted() = runBlocking {
        val settings = MapSettings()
        val questDb = db(settings)
        // QuestLogDatabase has NO entry for "Ooh, I Think I Smell a Rat"
        setupDatabase() // empty
        val request = QuestLogRequest(mockClient(fixtureHtmlPage1), questDb)
        request.syncPage(1)
        // Unknown quest title → skipped (preference unchanged = unstarted)
        assertEquals(QuestDatabase.UNSTARTED, questDb.getProgress(Quest.LARVA))
    }

    @Test fun parsePage_httpError_doesNotCrash() = runBlocking {
        val questDb = db()
        val client = HttpClient(MockEngine { _ ->
            respond("", HttpStatusCode.InternalServerError)
        })
        val request = QuestLogRequest(client, questDb)
        request.syncPage(1)  // must not throw
    }

    @Test fun syncAll_fetchesThreePages() = runBlocking {
        var callCount = 0
        val client = HttpClient(MockEngine { _ ->
            callCount++
            respond("<html></html>", HttpStatusCode.OK)
        })
        val request = QuestLogRequest(client, db())
        request.syncAll()
        assertEquals(3, callCount)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.request.QuestLogRequestTest"
```

Expected: compile failure — `QuestLogRequest` and `QuestLogDatabase.injectForTest` do not exist.

- [ ] **Step 3: Add injectForTest to QuestLogDatabase**

In `data/QuestLogDatabase.kt`, add inside the `object` body (after `parseForTest`):

```kotlin
    // Test-only: inject fixture entries without loading from compose resources
    internal fun injectForTest(entries: List<QuestLogEntry>) {
        byTitle.clear()
        entries.forEach { byTitle[it.title.lowercase().trim()] = it }
    }
```

- [ ] **Step 4: Create QuestLogRequest.kt**

Create `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/QuestLogRequest.kt`:

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.data.QuestLogDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.quest.QuestDatabase

class QuestLogRequest(
    private val client: HttpClient,
    private val questDatabase: QuestDatabase,
) {
    suspend fun syncAll() {
        syncPage(1)
        syncPage(2)
        syncPage(3)
    }

    suspend fun syncPage(which: Int) {
        try {
            val response = client.get("$KOL_BASE_URL/questlog.php") {
                url.parameters.append("which", which.toString())
            }
            if (!response.status.isSuccess()) return
            parsePage(response.bodyAsText())
        } catch (_: Exception) {
            // Network errors are non-fatal; quest state stays as-is
        }
    }

    internal fun parsePage(html: String) {
        // Split on opening <b> tags; each chunk = "Title</b>body"
        val sections = html.split(Regex("<b>", RegexOption.IGNORE_CASE))
        for (section in sections.drop(1)) {
            val closeIdx = section.indexOf("</b>", ignoreCase = true)
            if (closeIdx < 0) continue
            val title   = section.substring(0, closeIdx).trim()
            val bodyHtml = section.substring(closeIdx + 4)
            val entry = QuestLogDatabase.findByTitle(title) ?: continue
            val step  = QuestLogDatabase.detectStep(entry, bodyHtml)
            questDatabase.setProgressByPrefKey(entry.prefKey, step)
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.request.QuestLogRequestTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/QuestLogDatabase.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/QuestLogRequest.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/request/QuestLogRequestTest.kt
git commit -m "feat: QuestLogRequest fetches questlog.php and updates quest state"
```

---

## Task 7: AdventureManager — Quest Advancement Detection

**Files:**
- Modify: `adventure/AdventureManager.kt`
- Modify: `adventure/AdventureManagerTest.kt` (or create if it doesn't exist)

- [ ] **Step 1: Write failing tests for checkQuestAdvancement**

Check if `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt` exists. If not, create it. Append (or add) these tests:

```kotlin
package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.QuestLogRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class AdventureManagerQuestTest {

    private var syncCallCount = 0

    private fun countingSyncRequest(): QuestLogRequest {
        val client = HttpClient(MockEngine { _ ->
            respond("<html></html>", HttpStatusCode.OK)
        })
        val db = QuestDatabase(Preferences(MapSettings()))
        return object : QuestLogRequest(client, db) {
            override suspend fun syncAll() { syncCallCount++ }
        }
    }

    @Test fun checkQuestAdvancement_signalPresent_callsSyncAll() = runBlocking {
        syncCallCount = 0
        val manager = minimalAdventureManager(questLogRequest = countingSyncRequest())
        manager.testCheckQuestAdvancement("Your quest log has been updated.")
        assertEquals(1, syncCallCount)
    }

    @Test fun checkQuestAdvancement_noSignal_doesNotCallSyncAll() = runBlocking {
        syncCallCount = 0
        val manager = minimalAdventureManager(questLogRequest = countingSyncRequest())
        manager.testCheckQuestAdvancement("You fought a monster and won.")
        assertEquals(0, syncCallCount)
    }

    @Test fun checkQuestAdvancement_caseInsensitive_callsSyncAll() = runBlocking {
        syncCallCount = 0
        val manager = minimalAdventureManager(questLogRequest = countingSyncRequest())
        manager.testCheckQuestAdvancement("QUEST COMPLETED! Well done.")
        assertEquals(1, syncCallCount)
    }

    @Test fun checkQuestAdvancement_nullQuestLogRequest_doesNotCrash() = runBlocking {
        val manager = minimalAdventureManager(questLogRequest = null)
        manager.testCheckQuestAdvancement("Quest Completed")  // must not throw
    }
}
```

`minimalAdventureManager` and `testCheckQuestAdvancement` are helpers you'll add after the implementation. Add a note for now — they'll be defined in steps 3-4.

- [ ] **Step 2: Add questLogRequest param and checkQuestAdvancement to AdventureManager**

In `adventure/AdventureManager.kt`:

**2a. Add import:**
```kotlin
import net.sourceforge.kolmafia.request.QuestLogRequest
```

**2b. Add constructor parameter** after `private val moodManager: MoodManager? = null,`:
```kotlin
    private val questLogRequest: QuestLogRequest? = null,
```

**2c. Add member variable and constants** (after `private var skillUses: Int = 0`):
```kotlin
    private var lastTurnResponseText: String = ""

    companion object {
        private val QUEST_ADVANCE_SIGNALS = listOf(
            "Quest Completed", "Quest Updated",
            "added to your Quest Log", "Your quest log has been updated",
        )
    }
```

**2d. Add `checkQuestAdvancement` method** (after the `stop()` function):
```kotlin
    internal suspend fun checkQuestAdvancement(responseText: String) {
        if (QUEST_ADVANCE_SIGNALS.none { responseText.contains(it, ignoreCase = true) }) return
        questLogRequest?.syncAll()
    }

    // Test-only accessor
    internal suspend fun testCheckQuestAdvancement(text: String) = checkQuestAdvancement(text)
```

**2e. Capture response in `doOneTurn`** — add `lastTurnResponseText = html` immediately after the `val (html, url)` destructure:

```kotlin
    private suspend fun doOneTurn(location: AdventureLocation): AdventureResult? {
        val (html, url) = adventureRequest.adventure(location).getOrElse {
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(it)))
            return null
        }
        lastTurnResponseText = html  // capture for quest advancement check
        return when (val parsed = AdventureParser.parseAdventureResponse(html, url)) {
            is AdventureResult.Combat -> resolveCombat(location)
            is AdventureResult.Choice -> resolveChoice(parsed.choiceId, parsed.responseText)
            is AdventureResult.NonCombat -> parsed.also { emitItemEvents(it.itemsGained) }
        }
    }
```

**2f. Call `checkQuestAdvancement` in `runAdventures`** — add after the `if (healed == true)` block, before `eventBus.emit(GameEvent.TurnConsumed(...))`:

```kotlin
                    checkQuestAdvancement(lastTurnResponseText)
```

- [ ] **Step 3: Add minimalAdventureManager helper to the test file**

In `AdventureManagerQuestTest.kt`, add this helper function (inside the class or as a top-level function in the test file):

```kotlin
    private fun minimalAdventureManager(questLogRequest: QuestLogRequest?): AdventureManager {
        val client = HttpClient(MockEngine { _ -> respond("", HttpStatusCode.OK) })
        val prefs = Preferences(MapSettings())
        val eventBus = net.sourceforge.kolmafia.event.GameEventBus()
        val character = net.sourceforge.kolmafia.character.KoLCharacter()
        val charRequest = net.sourceforge.kolmafia.request.CharacterRequest(client)
        val advRequest = AdventureRequest(client)
        val fightRequest = FightRequest(client, eventBus)
        val choiceRequest = ChoiceRequest(client)
        return AdventureManager(
            adventureRequest = advRequest,
            fightRequest     = fightRequest,
            choiceRequest    = choiceRequest,
            characterRequest = charRequest,
            character        = character,
            preferences      = prefs,
            eventBus         = eventBus,
            questLogRequest  = questLogRequest,
        )
    }
```

Add the needed imports at the top of the test file.

- [ ] **Step 4: Make QuestLogRequest.syncAll open**

In `request/QuestLogRequest.kt`, mark the class `open` and `syncAll` as `open`:

```kotlin
open class QuestLogRequest(
    private val client: HttpClient,
    private val questDatabase: QuestDatabase,
) {
    open suspend fun syncAll() { ... }
    ...
}
```

- [ ] **Step 5: Run tests**

```
gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.adventure.AdventureManagerQuestTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Run full test suite for regressions**

```
gradlew.bat :shared:jvmTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/QuestLogRequest.kt
git add shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerQuestTest.kt
git commit -m "feat: AdventureManager checks quest advancement after each turn"
```

---

## Task 8: SessionManager — Sync Quest State on Login

**Files:**
- Modify: `session/SessionManager.kt`

- [ ] **Step 1: Add questLogRequest parameter and syncAll call**

In `session/SessionManager.kt`:

**1a. Add import:**
```kotlin
import net.sourceforge.kolmafia.request.QuestLogRequest
```

**1b. Add constructor parameter** at the end of the constructor (after `private val dailyResourceTracker: DailyResourceTracker`):
```kotlin
    private val questLogRequest: QuestLogRequest? = null,
```

**1c. Add syncAll call** inside `login()`, in the `onSuccess` block, after `effectManager.initialize(appScope)`:

```kotlin
                        effectManager.initialize(appScope)
                        scriptManager.initialize()
                        questLogRequest?.syncAll()   // sync quest state from server
                        SessionState.LoggedIn
```

The complete `onSuccess` block will look like:

```kotlin
                    onSuccess = { apiResponse ->
                        character.updateFromApiResponse(apiResponse)
                        dailyResourceTracker.syncDay(character.state.value.dayCount)
                        inventoryManager.initialize(appScope)
                        familiarManager.initialize(appScope)
                        skillManager.initialize(appScope)
                        effectManager.initialize(appScope)
                        scriptManager.initialize()
                        questLogRequest?.syncAll()
                        SessionState.LoggedIn
                    },
```

- [ ] **Step 2: Compile check**

```
gradlew.bat :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run full test suite**

```
gradlew.bat :shared:jvmTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SessionManager.kt
git commit -m "feat: SessionManager syncs quest state from questlog.php after login"
```

---

## Task 9: DI Wiring — SharedModule

**Files:**
- Modify: `di/SharedModule.kt`

- [ ] **Step 1: Add QuestLogRequest import and singleton registration**

In `SharedModule.kt`:

**1a. Add import:**
```kotlin
import net.sourceforge.kolmafia.request.QuestLogRequest
```

**1b. Register QuestLogRequest** after `single { QuestDatabase(get()) }`:
```kotlin
    singleOf(::QuestLogRequest)
```

**1c. Add to AdventureManager block** — after `moodManager = get(),` add:
```kotlin
            questLogRequest  = get(),
```

**1d. Update SessionManager registration** — `SessionManager` currently uses `singleOf(::SessionManager)` but now needs `questLogRequest` which requires named injection. Change to an explicit `single` block:

```kotlin
    single {
        SessionManager(
            loginRequest         = get(),
            characterRequest     = get(),
            character            = get(),
            preferences          = get(),
            inventoryManager     = get(),
            familiarManager      = get(),
            skillManager         = get(),
            effectManager        = get(),
            scriptManager        = get(),
            gameDatabase         = get(),
            dailyResourceTracker = get(),
            questLogRequest      = get(),
        )
    }
```

Remove the `singleOf(::SessionManager)` line that was there before.

- [ ] **Step 2: Compile check**

```
gradlew.bat :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run full test suite**

```
gradlew.bat :shared:jvmTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt
git commit -m "feat: register QuestLogRequest in Koin; inject into AdventureManager and SessionManager"
```

---

## Self-Review

**Spec coverage:**

| Requirement | Task | ✓ |
|---|---|---|
| 104 quests in Quest enum | Task 1 | ✓ |
| No duplicate pref keys | Task 1 | ✓ |
| `validateStep` + `setProgressByPrefKey` | Task 2 | ✓ |
| `QuestLogDatabase.load()` parses questslog.txt | Task 3 | ✓ |
| `detectStep` strips HTML, case-insensitive, last-match-wins | Tasks 3+4 | ✓ |
| `GameDatabase.load()` calls `QuestLogDatabase.load()` | Task 5 | ✓ |
| `QuestLogRequest.syncAll()` fetches 3 pages | Task 6 | ✓ |
| `parsePage` extracts title+body, writes via `setProgressByPrefKey` | Task 6 | ✓ |
| Unknown quests skipped (not reset to unstarted) | Task 6 (spec note 5) | ✓ |
| HTTP errors non-fatal | Task 6 | ✓ |
| `AdventureManager` captures response HTML | Task 7 | ✓ |
| `checkQuestAdvancement` triggers syncAll on signals | Task 7 | ✓ |
| `SessionManager` calls syncAll after login | Task 8 | ✓ |
| Koin DI wired for all new singletons | Task 9 | ✓ |

**Known deferrals (not covered by this plan):**
- Telegram, Party Fair, Doctor Bag, PirateRealm special-case regex step detection — these fall back to `"started"` via `detectStep` no-match path
- `questscouncil.txt` parsing — UI-only, deferred to a future phase
