# Quest Tracking — Full Port Design (104 Quests)

_2026-06-03_

## Goal

Port all 104 quests from the desktop `QuestDatabase` enum into the mobile, implement server-side state sync via `questlog.php` HTML parsing, and integrate incremental quest-advancement detection into the adventure loop so quest state is always current during automation.

## Background

The mobile `Quest.kt` currently defines only 2 of the 104 desktop quests (MEATSMITH, ARMORER), because those are the only two referenced by existing choice adventure handlers (cases 1060/1061). `questscouncil.txt` and `questslog.txt` are already bundled in compose resources but neither is parsed. The desktop detects quest state by fetching `questlog.php` HTML in three pages and matching body text against `questslog.txt` patterns. KoL has no JSON API for quest state.

---

## Architecture

Four components, each with a single responsibility:

```
GameDatabase.load()
  └─ QuestLogDatabase.load()   (parses questslog.txt once at startup)

Session login
  └─ QuestLogRequest.syncAll() (fetches questlog.php pages 1-2-3, writes to QuestDatabase)

AdventureManager.doOneTurn()
  └─ checkQuestAdvancement(responseText)
       └─ QuestLogRequest.syncAll() if advancement signal detected

Choice handlers / automation
  └─ ctx.questDatabase.isQuestLaterThan(Quest.FOO, step)
```

---

## Section 1: Data Model

### `Quest.kt` — expand from 2 to 104 entries

Mirror the desktop enum exactly, grouped by prefix convention:

```kotlin
enum class Quest(val prefKey: String) {
    // ── Council quests (questL##) ────────────────────────────────────────────
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

    // ── Guild quests (questG##) ──────────────────────────────────────────────
    MEATCAR("questG01Meatcar"),
    CITADEL("questG02Whitecastle"),
    EGO("questG03Ego"),
    NEMESIS("questG04Nemesis"),
    DARK("questG05Dark"),
    FACTORY("questG06Delivery"),
    MYST("questG07Myst"),
    MOXIE("questG08Moxie"),
    MUSCLE("questG09Muscle"),

    // ── Miscellaneous quests (questM##) ─────────────────────────────────────
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
    MEATSMITH("questM23Meatsmith"),       // existing
    DOC("questM24Doc"),
    ARMORER("questM25Armorer"),            // existing
    ORACLE("questM26Oracle"),

    // ── Future/event quests ──────────────────────────────────────────────────
    PRIMORDIAL("questF01Primordial"),
    FUTURE("questF03Future"),
    GENERATOR("questF04Elves"),
    CLANCY("questF05Clancy"),

    // ── Sea quests ───────────────────────────────────────────────────────────
    SEA_OLD_GUY("questS01OldGuy"),
    SEA_MONKEES("questS02Monkees"),

    // ── Spookyraven event sub-quests ─────────────────────────────────────────
    JIMMY_MUSHROOM("questESlMushStash"),
    JIMMY_CHEESEBURGER("questESlCheeseburger"),
    JIMMY_SALT("questESlSalt"),
    TACO_DAN_AUDIT("questESlAudit"),
    TACO_DAN_COCKTAIL("questESlCocktail"),
    TACO_DAN_FISH("questESlFish"),
    BRODEN_BACTERIA("questESlBacteria"),
    BRODEN_SPRINKLES("questESlSprinkles"),
    BRODEN_DEBT("questESlDebt"),

    // ── Special event quests ─────────────────────────────────────────────────
    EVE("questESpEVE"),
    JUNGLE_PUN("questESpJunglePun"),
    GORE("questESpGore"),
    CLIPPER("questESpClipper"),
    FAKE_MEDIUM("questESpFakeMedium"),
    SERUM("questESpSerum"),
    SMOKES("questESpSmokes"),
    OUT_OF_ORDER("questESpOutOfOrder"),

    // ── Standard event quests ────────────────────────────────────────────────
    FISH_TRASH("questEStFishTrash"),
    GIVE_ME_FUEL("questEStGiveMeFuel"),
    NASTY_BEARS("questEStNastyBears"),
    SOCIAL_JUSTICE_I("questEStSocialJusticeI"),
    SOCIAL_JUSTICE_II("questEStSocialJusticeII"),
    SUPER_LUBER("questEStSuperLuber"),
    WORK_WITH_FOOD("questEStWorkWithFood"),
    ZIPPITY_DOO_DAH("questEStZippityDooDah"),

    // ── Community/one-off quests ─────────────────────────────────────────────
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

### `QuestDatabase.kt` — one addition

Add `setProgressByPrefKey(prefKey: String, step: String)` — needed by the parser, which works from raw preference key strings before resolving to `Quest` enum values:

```kotlin
fun setProgressByPrefKey(prefKey: String, step: String) =
    preferences.setString(prefKey, QuestDatabase.validateStep(step))
```

`validateStep` accepts `"unstarted"`, `"started"`, `"finished"`, `"step\d+"` and rejects anything else (returns `"unstarted"` as safe default). All existing methods (`getProgress`, `setProgress`, `isQuestLaterThan`, `isQuestFinished`, `stepOrdinal`) are unchanged.

---

## Section 2: QuestLogDatabase — Data Layer

**File:** `data/QuestLogDatabase.kt`

Parses `questslog.txt` once at startup (called from `GameDatabase.load()`). The file format is tab-separated: `prefKey\tQuest Title\tstep1text\tstep2text\t…\tfinishedtext`. Step names are inferred from position: index 0 = `"started"`, last = `"finished"`, intermediate = `"step1"`, `"step2"`, etc.

```kotlin
data class QuestLogEntry(
    val prefKey: String,
    val title: String,
    val steps: List<Pair<String, String>>,  // stepName → normalized body text
)

object QuestLogDatabase {
    private val byTitle = mutableMapOf<String, QuestLogEntry>()  // normalized title → entry

    suspend fun load()                                      // called by GameDatabase.load()
    fun findByTitle(title: String): QuestLogEntry?         // case-insensitive lookup
    fun detectStep(entry: QuestLogEntry, bodyHtml: String): String  // returns step name
}
```

**`detectStep` algorithm:**
1. Strip HTML tags from `bodyHtml` using a simple `<[^>]+>` regex replacement
2. Collapse whitespace to single spaces, lowercase
3. Walk `entry.steps` from last to first; return the step name of the last entry whose text is a substring of the normalized body
4. If no match: return `"started"` (quest is in log but step unrecognised — safe default)

This mirrors the desktop's `findQuestProgress` fallback strategy.

---

## Section 3: QuestLogRequest — HTTP Sync

**File:** `request/QuestLogRequest.kt`

```kotlin
class QuestLogRequest(
    private val client: HttpClient,
    private val questDatabase: QuestDatabase,
) {
    suspend fun syncAll() {
        syncPage(1)  // Council quests
        syncPage(2)  // Guild + misc quests
        syncPage(3)  // Accomplishments
    }

    suspend fun syncPage(which: Int)

    private fun parsePage(html: String)
}
```

**`parsePage` logic:**
1. Apply `HEADER_PATTERN` (`<b>([^<]+)</b>`) to find each quest title in the page
2. Extract the following body block (text between this `<b>` and the next or end of section)
3. Call `QuestLogDatabase.findByTitle(title)` — if found, call `detectStep(entry, bodyHtml)` and write via `questDatabase.setProgressByPrefKey(entry.prefKey, step)`
4. Quests in the log but not in `questslog.txt` are set to `"started"` (present but unrecognised step)
5. Quests NOT in the log remain at their current preference value (not reset to `"unstarted"`)

**Important:** `syncPage` must not reset quests that aren't present on a given page — a quest missing from page 1 might be on page 2.

---

## Section 4: AdventureManager Integration

**Incremental sync trigger**

`AdventureManager` gains an optional `questLogRequest: QuestLogRequest? = null` parameter (matching the existing nullable pattern).

After each `doOneTurn`, before the stop-condition checks:

```kotlin
private val QUEST_ADVANCE_SIGNALS = listOf(
    "Quest Completed", "Quest Updated", "added to your Quest Log",
    "Your quest log has been updated",
)

private suspend fun checkQuestAdvancement(responseText: String) {
    if (QUEST_ADVANCE_SIGNALS.none { responseText.contains(it, ignoreCase = true) }) return
    questLogRequest?.syncAll()
}
```

`syncAll()` on a signal costs 3 HTTP calls — acceptable since quest advancement is rare (a few times per session at most).

**Session login**

`SessionManager` (or wherever login success is handled) calls `questLogRequest.syncAll()` once after successful authentication.

---

## Section 5: Choice Handlers

**Current state:** Only two choice IDs reference `QuestDatabase` in the desktop (1060 MEATSMITH, 1061 ARMORER) — both already implemented in `QuestHandlers.kt`. No new handlers needed.

**Future readiness:** With all 104 quests in the enum, any future choice handler that needs quest gates (e.g. council quest branches, guild quest gating) can reference `Quest.LARVA`, `Quest.CITADEL`, etc. directly — no further enum changes needed.

---

## Testing Strategy

| Component | Test file | Approach |
|-----------|-----------|----------|
| `Quest` enum | — | Compile-time only; assert no duplicate pref keys via a unit test |
| `QuestDatabase.setProgressByPrefKey` | `QuestDatabaseTest.kt` | Extend existing tests |
| `QuestLogDatabase.load + detectStep` | `QuestLogDatabaseTest.kt` | Fixture: a few `questslog.txt` rows; assert step detection for known texts |
| `QuestLogRequest.parsePage` | `QuestLogRequestTest.kt` | Ktor `MockEngine` returning fixture HTML from `questlog.php`; assert preferences updated correctly |
| `AdventureManager.checkQuestAdvancement` | `AdventureManagerTest.kt` | Assert `syncAll` called when response contains signal text; not called otherwise |

No live HTTP calls in any test. Fixtures are short HTML snippets (10–20 lines each) embedded in test files.

---

## File Map

| Status | File | Notes |
|--------|------|-------|
| **Modify** | `quest/Quest.kt` | 2 → 104 entries |
| **Modify** | `quest/QuestDatabase.kt` | Add `setProgressByPrefKey` + `validateStep` |
| **Create** | `data/QuestLogDatabase.kt` | Singleton; parses `questslog.txt` |
| **Create** | `request/QuestLogRequest.kt` | HTTP fetch + parsePage |
| **Modify** | `adventure/AdventureManager.kt` | Add `questLogRequest` param + `checkQuestAdvancement` |
| **Modify** | `session/SessionManager.kt` | Call `questLogRequest.syncAll()` on login |
| **Modify** | `di/SharedModule.kt` | Register `QuestLogRequest`; inject into `AdventureManager` and `SessionManager` |
| **Modify** | `data/GameDatabase.kt` | Call `QuestLogDatabase.load()` |
| **Modify** | `quest/QuestDatabaseTest.kt` | Add `setProgressByPrefKey` tests |
| **Create** | `quest/QuestLogDatabaseTest.kt` | detectStep + load tests |
| **Create** | `request/QuestLogRequestTest.kt` | parsePage tests with MockEngine |
| **Modify** | `adventure/AdventureManagerTest.kt` | checkQuestAdvancement tests |

---

## Known Deferrals

- **Quest-specific step detection** — a handful of quests (Telegram, Party Fair, Doctor Bag, PirateRealm) have special-cased step detection in the desktop using custom regex rather than `questslog.txt` text matching. These will fall back to `"started"` until their patterns are ported.
- **Council text** — `questscouncil.txt` is bundled but not parsed; it drives UI display of council dialogue, not automation state. Deferred to a future UI phase.
- **Quest step validation strictness** — `validateStep` defaults unrecognised strings to `"unstarted"` conservatively. If future automation needs stricter validation, this can be tightened.
