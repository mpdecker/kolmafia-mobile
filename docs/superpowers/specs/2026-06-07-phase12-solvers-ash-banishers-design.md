# Phase 12: Choice Solvers + ASH Quick Wins + Banisher Expansion

## Overview

Three parallel tracks that together make mobile KoLmafia dramatically more functional for endgame content:

- **Track A** — Implement all 6 choice adventure solvers (LightsOut, SafetyShelter, VampOut, ArcadeGame, LostKey, Gamepro), gated on a prerequisite fix to `AdventureManager.resolveChoice()`
- **Track B** — ASH breadth wins: `visit_url` POST overloads, `hermit()` function, `HermitRequest`, wire `get_moods()`/`mood_list()` to live data
- **Track D** — Expand `Banisher` enum from 20 to ~69 entries to match desktop `BanishManager.java`

---

## Architecture

### Task 0 Prerequisite: Multi-Step Choice Loop

**Problem**: `AdventureManager.resolveChoice()` discards the HTML response from `ChoiceRequest.choose()` and hardcodes `stepCount = 0`. Multi-step choice sequences (Safety Shelter, VampOut, LostKey, DungeonFist, Gamepro) submit only the first option, then silently stop. LightsOut is unaffected (each room is a separate one-shot turn).

**Fix**: Turn `resolveChoice()` into a `while(true)` loop. Each iteration:
1. Builds `ChoiceContext` with current `stepCount`
2. Dispatches via `registry.dispatch(ctx)` to pick an option
3. Submits via `choiceRequest.choose()` — now captures the returned `Result<String>`
4. Parses the HTML; if the result is `AdventureResult.Choice`, increments `stepCount`, updates `currentChoiceId`/`currentResponseText`, continues
5. On any non-Choice result (NonCombat, network error, or option ≤ 0), breaks

`ChoiceRequest.choose()` already returns `Result<String>` — only the call site changes.

**File**: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt`

```kotlin
private suspend fun resolveChoice(
    choiceId: Int,
    initialResponseText: String,
): AdventureResult.Choice {
    var currentChoiceId     = choiceId
    var currentResponseText = initialResponseText
    var stepCount           = 0
    var lastChosenOption    = 1

    while (true) {
        val ctx = ChoiceContext(
            choiceId       = currentChoiceId,
            options        = ChoiceUtilities.parseChoices(currentResponseText),
            responseText   = currentResponseText,
            characterState = character.state.value,
            inventoryState = inventory?.state?.value ?: InventoryState(),
            effectState    = effects?.state?.value ?: EffectState(),
            skillState     = skills?.state?.value ?: SkillState(),
            preferences    = preferences,
            goalManager    = goalManager,
            questDatabase  = questDatabase,
            solvers        = solvers,
            preference     = preferences.getInt("choiceAdventure$currentChoiceId", 0),
            stepCount      = stepCount,
            skillUses      = skillUses,
        )
        val option = registry.dispatch(ctx)
            ?: preferences.getString("choiceAdventure$currentChoiceId").toIntOrNull()
            ?: 1
        if (option > 0 && skillUses > 0) skillUses--
        lastChosenOption = option

        val html = choiceRequest.choose(currentChoiceId, option).getOrElse { e ->
            eventBus.emit(GameEvent.AdventureLoopStopped(StopReason.NetworkError(e)))
            return AdventureResult.Choice(currentChoiceId, "Choice Adventure", chosenOption = option)
        }
        eventBus.emit(GameEvent.ChoiceResolved(currentChoiceId, option))

        val next = AdventureParser.parseAdventureResponse(html, "choice.php")
        if (next is AdventureResult.Choice) {
            currentChoiceId     = next.choiceId
            currentResponseText = next.responseText
            stepCount++
        } else {
            break
        }
    }
    return AdventureResult.Choice(currentChoiceId, "Choice Adventure", chosenOption = lastChosenOption)
}
```

---

## Track A: Choice Solver Implementations

All 6 implementations live in `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/choice/solvers/`. Tests live in the corresponding `commonTest` path. `SharedModule.kt` replaces all `NoOp` references with the real implementations.

### A1: LightsOutSolverImpl

**File**: `adventure/choice/solvers/LightsOutSolverImpl.kt`

Port of desktop `ChoiceManager.lightsOutAutomation()`. Reads `preferences.getInt("lightsOutAutomation", 0)`:
- `0` → disabled, return `null`
- `1` → chase Elizabeth/Stephen (maximize ghost count for quest)
- `2` → power-through (always advance via the exit option)

Each room (choiceId 890–903) has distinct option text, matched against `responseText`. Each room offers 3 options: (1) explore a side room, (2) move toward exit, (3) exit the light-out sequence. Mode 1 picks the best option per room for ghost tracking; mode 2 always picks option 2 (advance).

```kotlin
class LightsOutSolverImpl(private val preferences: Preferences) : LightsOutSolver {
    override fun autoLightsOut(choiceId: Int, responseText: String): Int? {
        val mode = preferences.getInt("lightsOutAutomation", 0)
        if (mode == 0) return null
        return when (mode) {
            2 -> 2  // skip — always advance toward exit
            else -> autoByRoom(choiceId, responseText)  // mode 1: chase ghosts
        }
    }

    private fun autoByRoom(choiceId: Int, responseText: String): Int = when (choiceId) {
        // Rooms sourced from desktop ChoiceManager.lightsOutAutomation()
        890 -> if (responseText.contains("Elizabeth")) 1 else 2
        891 -> if (responseText.contains("ghost")) 1 else 2
        892 -> 1
        893 -> if (responseText.contains("Stephen")) 1 else 2
        894 -> 1
        895 -> if (responseText.contains("ghost") || responseText.contains("Avery")) 1 else 2
        896 -> 1
        897 -> 2
        898 -> if (responseText.contains("ghost")) 1 else 2
        899 -> 1
        900 -> 2
        901 -> if (responseText.contains("ghost")) 1 else 2
        902 -> 1
        903 -> 2
        else -> 2
    }
}
```

**Note**: The exact `responseText` keywords for each room must be verified against live game HTML. The above are best-effort ports; adjust in implementation if game text differs.

**Preference key**: `"lightsOutAutomation"` (int, 0/1/2). No new constant needed — matches desktop.

---

### A2: SafetyShelterSolverImpl

**File**: `adventure/choice/solvers/SafetyShelterSolverImpl.kt`

Port of desktop `SafetyShelterManager`. Each goal (1–6) maps to a deterministic script; `stepCount` indexes into the script to pick the next option digit.

```kotlin
class SafetyShelterSolverImpl : SafetyShelterSolver {

    // Each entry is a digit-string script. preference (1-6) selects by index (preference - 1).
    // stepCount picks the char at that position; digitToInt() gives the option.
    private val ronaldScripts = arrayOf(
        "11211",   // goal 1
        "1122",    // goal 2
        "12211",   // goal 3
        "12221",   // goal 4
        "1321",    // goal 5
        "1322",    // goal 6
    )
    private val grimaceScripts = arrayOf(
        "1121",    // goal 1
        "1122",    // goal 2
        "1211",    // goal 3
        "12121",   // goal 4
        "13211",   // goal 5
        "12221",   // goal 6
    )

    override fun autoRonald(preference: Int, stepCount: Int, responseText: String): Int? {
        val idx = preference - 1
        val script = ronaldScripts.getOrNull(idx) ?: return null
        return script.getOrNull(stepCount)?.digitToInt()
    }

    override fun autoGrimace(preference: Int, stepCount: Int, responseText: String): Int? {
        val idx = preference - 1
        val script = grimaceScripts.getOrNull(idx) ?: return null
        return script.getOrNull(stepCount)?.digitToInt()
    }
}
```

**Preference**: user sets `choiceAdventure535` (Ronald) or `choiceAdventure536` (Grimace) to a value 1–6 to select the goal script. Already handled by `ctx.preference` in `SolverHandlers`.

---

### A3: LostKeySolverImpl

**File**: `adventure/choice/solvers/LostKeySolverImpl.kt`

Same pattern as SafetyShelter. Three goals (glasses / comb / pill bottle), each with a fixed step-by-step script.

```kotlin
class LostKeySolverImpl : LostKeySolver {
    private val keyScripts = arrayOf(
        "121111",   // goal 1: glasses
        "131212",   // goal 2: comb
        "131113",   // goal 3: pill bottle
    )

    override fun autoKey(preference: Int, stepCount: Int, responseText: String): Int? {
        val idx = preference - 1
        val script = keyScripts.getOrNull(idx) ?: return null
        return script.getOrNull(stepCount)?.digitToInt()
    }
}
```

---

### A4: ArcadeGameSolverImpl

**File**: `adventure/choice/solvers/ArcadeGameSolverImpl.kt`

120-character Dungeon Fist script from desktop `ArcadeRequest.java`. Shortcut: if `responseText` contains "Finish from Memory", find its option number in the response HTML and return it immediately (skips the script).

```kotlin
class ArcadeGameSolverImpl : ArcadeGameSolver {

    // Exactly 120 characters, sourced from desktop ArcadeRequest.FIST_SCRIPT
    private val fistScript =
        "311111111111111111111111111111211211111111111111111111111111" +
        "211111111111111111211122211111121111111111111111122211133111113"

    override fun autoDungeonFist(stepCount: Int, responseText: String): Int? {
        // Shortcut: skip remaining steps if memory option is available
        if (responseText.contains("Finish from Memory", ignoreCase = true)) {
            // Option 4 is the "Finish from Memory" shortcut in DungeonFist
            return 4
        }
        return fistScript.getOrNull(stepCount)?.digitToInt()
    }
}
```

**Note**: The "Finish from Memory" option index (4) should be verified against live game HTML. The desktop checks for it by scanning available option text.

---

### A5: GameproSolverImpl

**File**: `adventure/choice/solvers/GameproSolverImpl.kt`

Reads `choiceAdventure665` as a comma-separated list of option digits configured by the user. Returns `choices[stepCount]`.

```kotlin
class GameproSolverImpl(private val preferences: Preferences) : GameproSolver {
    override fun autoSolve(stepCount: Int): Int? {
        val raw = preferences.getString("choiceAdventure665").trim()
        if (raw.isBlank()) return null
        val choices = raw.split(",").map { it.trim() }
        return choices.getOrNull(stepCount)?.toIntOrNull()
    }
}
```

---

### A6: VampOutSolverImpl

**File**: `adventure/choice/solvers/VampOutSolverImpl.kt`

The most complex solver. 13 goals, each with a script string containing digits and location codes.

**Script conventions**:
- Digit `1`–`4`: choose that option number directly
- Letter `m`: choose the option whose text contains "Malkovich"
- Letter `b`: choose the option whose text contains "Brouhaha"  
- Letter `t`: choose the option whose text contains "Torremolinos"
- Letter `v`: choose the option whose text contains "Ventrilo"
- Position 0 in every script: pick the correct starting location (parsed from responseText option labels — look for "Vlad's", "Isabella's", "Masquerade")

**Starting location by goal** (from desktop VampOutManager.java):
- Goals 1–3 (Mistified, Bat Attitude, There Wolf): start at option matching "Isabella's"
- Goals 4–7 (Muscle, Mysticality, Moxie, Meat): start at option matching "Vlad's"
- Goals 8–13 (Prince/Interview variants): start at option matching "Vlad's"

```kotlin
class VampOutSolverImpl : VampOutSolver {

    // Index 0 = goal 1. '0' at position 0 = starting location (handled separately).
    private val scripts = arrayOf(
        "022121221",    // 1: Mistified
        "02212111",     // 2: Bat Attitude
        "02231111",     // 3: There Wolf
        "011",          // 4: Muscle
        "0131",         // 5: Mysticality
        "01221",        // 6: Moxie
        "01232",        // 7: Meat
        "031241mtbv11", // 8: Kill Prince (Sword)
        "042112mvtb11", // 9: Kill Prince (Sceptre)
        "014423vmbt11", // 10: Kill Prince (Medallion)
        "023334tvbm11", // 11: Kill Prince (Chalice)
        "031241vmtb11", // 12: Interview
        "031241vbtm11", // 13: Black heart
    )

    // Goals whose path starts at Isabella's (index 0 = goal 1)
    private val isabellaGoals = setOf(1, 2, 3)

    override fun autoVampOut(preference: Int, stepCount: Int, responseText: String): Int? {
        val idx = preference - 1
        val script = scripts.getOrNull(idx) ?: return null
        val scriptChar = script.getOrNull(stepCount) ?: return null

        return if (stepCount == 0) {
            // Pick starting location
            pickStartingLocation(preference, responseText)
        } else {
            resolveScriptChar(scriptChar, responseText)
        }
    }

    private fun pickStartingLocation(goal: Int, responseText: String): Int? {
        val targetKeyword = if (goal in isabellaGoals) "Isabella" else "Vlad"
        // Options are presented as hyperlinks; scan option lines for the keyword
        val optionRegex = Regex("""option=(\d+)">[^<]*$targetKeyword""", RegexOption.IGNORE_CASE)
        return optionRegex.find(responseText)?.groupValues?.get(1)?.toIntOrNull()
            ?: 1  // fallback: option 1
    }

    private fun resolveScriptChar(ch: Char, responseText: String): Int? {
        if (ch.isDigit()) return ch.digitToInt()
        val keyword = when (ch) {
            'm' -> "Malkovich"
            'b' -> "Brouhaha"
            't' -> "Torremolinos"
            'v' -> "Ventrilo"
            else -> return null
        }
        val optionRegex = Regex("""option=(\d+)">[^<]*$keyword""", RegexOption.IGNORE_CASE)
        return optionRegex.find(responseText)?.groupValues?.get(1)?.toIntOrNull()
    }
}
```

**Preference**: user sets `choiceAdventure546` to 1–13 for the goal. Value 0 = automation disabled (no solver fires).

---

### A7: SharedModule wiring

**File**: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/di/SharedModule.kt`

Replace the `ChoiceSolvers(...)` block's `NoOp` references:

```kotlin
single {
    ChoiceSolvers(
        safetyShelter = SafetyShelterSolverImpl(),
        vampOut       = VampOutSolverImpl(),
        arcadeGame    = ArcadeGameSolverImpl(),
        lostKey       = LostKeySolverImpl(),
        gamepro       = GameproSolverImpl(get()),
        lightsOut     = LightsOutSolverImpl(get()),
    )
}
```

Also add imports for all 6 new `Impl` classes.

---

## Track B: ASH Quick Wins

### B1: visit_url POST overloads

**File**: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.WebRequest.kt`

Add a `doPost()` helper alongside the existing `doVisit()`, then register two new overloads:

```kotlin
fun doPost(url: String, postData: String, encoded: Boolean): String {
    val client = httpClient ?: return ""
    val fullUrl = if (encoded) url else "$KOL_BASE_URL/${url.trimStart('/')}"
    return runBlocking {
        try {
            val response = client.submitForm(
                url = fullUrl,
                formParameters = ParametersBuilder().apply {
                    postData.split("&").forEach { pair ->
                        val (k, v) = pair.split("=", limit = 2).let {
                            if (it.size == 2) it[0] to it[1] else it[0] to ""
                        }
                        append(k, v)
                    }
                }.build(),
            )
            response.body<String>()
        } catch (e: Exception) {
            ""
        }
    }
}

// visit_url(url, postData) → string
regFn(scope, "visit_url", AshType.STRING,
    listOf("url" to AshType.STRING, "post_data" to AshType.STRING)) { _, args ->
    AshValue.of(doPost(args[0].toString(), args[1].toString(), encoded = false))
}

// visit_url(url, postData, encoded) → string
regFn(scope, "visit_url", AshType.STRING,
    listOf("url" to AshType.STRING, "post_data" to AshType.STRING, "encoded" to AshType.BOOLEAN)) { _, args ->
    AshValue.of(doPost(args[0].toString(), args[1].toString(), args[2].toBoolean()))
}
```

Required imports: `io.ktor.client.request.forms.submitForm`, `io.ktor.http.ParametersBuilder`.

---

### B2: HermitRequest + hermit() ASH function

**New file**: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/request/HermitRequest.kt`

```kotlin
package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL

class HermitRequest(private val client: HttpClient) {
    /**
     * Trades [quantity] of item [itemId] with the hermit.
     * POST hermit.php?action=trade&whichitem=ID&quantity=N
     * Returns the response HTML on success.
     */
    suspend fun trade(itemId: Int, quantity: Int): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/hermit.php",
            formParameters = parameters {
                append("action", "trade")
                append("whichitem", itemId.toString())
                append("quantity", quantity.toString())
            }
        )
        Result.success(response.body())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**New file**: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Hermit.kt`

```kotlin
package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking

internal fun GameRuntimeLibrary.registerHermit(scope: AshScope) {

    // hermit(item, count) → int
    // Returns count on success, 0 on failure.
    regFn(scope, "hermit", AshType.INT,
        listOf("it" to AshType.ITEM, "n" to AshType.INT)) { _, args ->
        val itemName = args[0].toString()
        val count    = args[1].toLong().toInt()
        val itemId   = gameDatabase?.item(itemName)?.id ?: 0
        if (itemId == 0 || count <= 0) return@regFn AshValue.of(0L)
        val success = runBlocking {
            hermitRequest?.trade(itemId, count)?.isSuccess == true
        }
        AshValue.of(if (success) count.toLong() else 0L)
    }
}
```

**GameRuntimeLibrary.kt** — add `hermitRequest` constructor parameter:

```kotlin
class GameRuntimeLibrary(
    // ... existing params ...
    internal val hermitRequest: net.sourceforge.kolmafia.request.HermitRequest? = null,
) : RuntimeLibrary() {
```

Call `registerHermit(scope)` from `GameRuntimeLibrary.registerAll()` alongside the other `register*` calls.

**SharedModule.kt** — add `singleOf(::HermitRequest)` and pass `hermitRequest = get()` to `GameRuntimeLibrary`:

```kotlin
singleOf(::HermitRequest)

single {
    GameRuntimeLibrary(
        // ... existing ...
        hermitRequest = get(),
    )
}
```

---

### B3: Wire get_moods() / mood_list() to MoodManager

**File**: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.Mood.kt`

Replace the two stub implementations with live data from `moodManager.moodLibrary`:

```kotlin
internal fun GameRuntimeLibrary.registerMoodQueries(scope: AshScope) {
    val stringIntType = AggregateType(AshType.INT, AshType.STRING)

    fun buildMoodList(): AggregateValue {
        val agg = AggregateValue(stringIntType)
        moodManager?.moodLibrary?.keys?.sorted()?.forEachIndexed { i, name ->
            agg[AshValue.of(i.toLong())] = AshValue.of(name)
        }
        return agg
    }

    // get_moods() → string[int]
    regFn(scope, "get_moods", stringIntType, emptyList()) { _, _ ->
        buildMoodList()
    }

    // mood_list() → string[int]  (alias)
    regFn(scope, "mood_list", stringIntType, emptyList()) { _, _ ->
        buildMoodList()
    }
}
```

`MoodManager.moodLibrary` is already a `Map<String, Mood>` (populated by `loadLibrary()` on login). No DI changes needed — `moodManager` is already an existing constructor param of `GameRuntimeLibrary`.

---

## Track D: Banisher Expansion

### D1: Banisher.kt — add ~49 new entries

**File**: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/banish/Banisher.kt`

Add all entries before `UNKNOWN`. Desktop `Reset` types map to mobile `ResetType` as follows:
- `TURN_RESET` → `TURNS`
- `ROLLOVER_RESET` → `ROLLOVER`
- `TURN_ROLLOVER_RESET` → `TURN_ROLLOVER`
- `AVATAR_RESET` → `AVATAR`
- `COSMIC_BOWLING_BALL_RESET` → `ROLLOVER`
- `EFFECT_RESET` → `ROLLOVER`

New entries (canonical names must match desktop `BanishManager` exactly for pref round-tripping):

```kotlin
BALEFUL_HOWL("baleful howl", -1, ResetType.ROLLOVER, false),
BASEBALL_DIAMOND("Baseball Diamond", -1, ResetType.ROLLOVER, true),
BATTER_UP("batter up!", -1, ResetType.ROLLOVER, false),
BE_A_MIND_MASTER("Be a Mind Master", 80, ResetType.TURNS, true),
BLART_SPRAY_WIDE("B. L. A. R. T. Spray (wide)", -1, ResetType.ROLLOVER, true),
BUNDLE_OF_FRAGRANT_HERBS("bundle of \"fragrant\" herbs", -1, ResetType.ROLLOVER, true),
CLASSY_MONKEY("classy monkey", 20, ResetType.TURNS, false),
COCKTAIL_NAPKIN("cocktail napkin", 20, ResetType.TURNS, true),
CRIMBUCCANEER_RIGGING_LASSO("Crimbuccaneer rigging lasso", 100, ResetType.TURN_ROLLOVER, false),
CRYSTAL_SKULL("crystal skull", 20, ResetType.TURNS, false),
CURSE_OF_VACATION("curse of vacation", -1, ResetType.ROLLOVER, false),
DEATHCHUCKS("deathchucks", -1, ResetType.ROLLOVER, true),
DIRTY_STINKBOMB("dirty stinkbomb", -1, ResetType.ROLLOVER, true),
GINGERBREAD_RESTRAINING_ORDER("gingerbread restraining order", -1, ResetType.ROLLOVER, false),
GLITCHED_MALWARE("Deploy Glitched Malware", -1, ResetType.ROLLOVER, false),
HAROLDS_BELL("harold's bell", 20, ResetType.TURNS, false),
HEARTSTONE_BANISH("Heartstone banish", 50, ResetType.TURNS, false),
HOWL_OF_THE_ALPHA("howl of the alpha", -1, ResetType.AVATAR, false),
HUMAN_MUSK("human musk", -1, ResetType.ROLLOVER, true),
ICE_HOTEL_BELL("ice hotel bell", -1, ResetType.ROLLOVER, true),
LEFT_ZOOT_KICK("Left Zoot Kick", 100, ResetType.TURNS, true),
LICORICE_ROPE("licorice rope", -1, ResetType.ROLLOVER, false),
MARK_YOUR_TERRITORY("Mark Your Territory", -1, ResetType.ROLLOVER, false),
MONKEY_SLAP("Monkey Slap", -1, ResetType.ROLLOVER, false),
NANORHINO("nanorhino", -1, ResetType.ROLLOVER, false),
PEEL_OUT("peel out", -1, ResetType.AVATAR, true),
PEPPERMINT_BOMB("peppermint bomb", 100, ResetType.TURN_ROLLOVER, false),
PULLED_INDIGO_TAFFY("pulled indigo taffy", 40, ResetType.TURNS, true),
PUNCH_OUT_YOUR_FOE("Punch Out your Foe", 20, ResetType.TURNS, true),
PUNT_AOSOL("[28021]Punt", -1, ResetType.ROLLOVER, false),
PUNT_WEREPROF("[7510]Punt", 40, ResetType.TURNS, false),
RIGHT_ZOOT_KICK("Right Zoot Kick", 100, ResetType.TURNS, true),
ROAR_LIKE_A_LION("Roar like a Lion", -1, ResetType.ROLLOVER, false),
SEADENT_LIGHTNING("Sea *dent Lightning", -1, ResetType.ROLLOVER, false),
SHOW_YOUR_BORING_FAMILIAR_PICTURES("Show your boring familiar pictures", 100, ResetType.TURNS, true),
SMOKE_GRENADE("smoke grenade", 20, ResetType.TURNS, false),
SPOOKY_MUSIC_BOX_MECHANISM("spooky music box mechanism", -1, ResetType.ROLLOVER, false),
SPLIT_PEA_SOUP("handful of split pea soup", 30, ResetType.TURN_ROLLOVER, true),
SPRING_KICK("Spring Kick", -1, ResetType.ROLLOVER, true),
STAFF_OF_THE_STANDALONE_CHEESE("staff of the standalone cheese", -1, ResetType.AVATAR, false),
STINKY_CHEESE_EYE("stinky cheese eye", 10, ResetType.TURNS, true),
TENNIS_BALL("tennis ball", 30, ResetType.TURN_ROLLOVER, true),
THROWIN_EMBER("throwin' ember", 30, ResetType.TURN_ROLLOVER, false),
THUNDER_CLAP("thunder clap", 40, ResetType.TURNS, false),
TRYPTOPHAN_DART("tryptophan dart", -1, ResetType.ROLLOVER, false),
ULTRA_HAMMER("Ultra Hammer", -1, ResetType.ROLLOVER, false),
V_FOR_VIVALA_MASK("v for vivala mask", 10, ResetType.TURNS, true),
WALK_AWAY_FROM_EXPLOSION("walk away from explosion", 30, ResetType.TURNS, false),
PATRIOTIC_SCREECH("Patriotic Screech", 100, ResetType.TURNS, false),
```

**Note on canonical names**: `PUNT_AOSOL`/`PUNT_WEREPROF` use bracket notation matching desktop. `HEARTSTONE_BANISH` canonical name should be verified against desktop `BanishManager.java` — the desktop uses "Heartstone %banish" with a format placeholder; adjust if needed.

---

### D2: AdventureParser.BANISHER_PATTERNS — add detection entries

**File**: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureParser.kt`

Append to `BANISHER_PATTERNS` list. Patterns sourced from desktop `FightRequest.java`; mark uncertain ones with a `// TODO: verify` comment so they can be spot-checked against live game HTML before shipping.

```kotlin
// New entries — verify against live game HTML before release
"stinky cheese"                                           to Banisher.STINKY_CHEESE_EYE,
"smoke grenade"                                           to Banisher.SMOKE_GRENADE,
"walk away from the explosion"                            to Banisher.WALK_AWAY_FROM_EXPLOSION,
"split pea soup"                                          to Banisher.SPLIT_PEA_SOUP,
"tennis ball"                                             to Banisher.TENNIS_BALL,
"cocktail napkin"                                         to Banisher.COCKTAIL_NAPKIN,
"vivala"                                                  to Banisher.V_FOR_VIVALA_MASK,
"indigo taffy"                                            to Banisher.PULLED_INDIGO_TAFFY,
"thunder clap"                                            to Banisher.THUNDER_CLAP,
"harold's bell"                                           to Banisher.HAROLDS_BELL, // TODO: verify
"dirty stinkbomb"                                         to Banisher.DIRTY_STINKBOMB,
"peppermint bomb"                                         to Banisher.PEPPERMINT_BOMB,
"ember"                                                   to Banisher.THROWIN_EMBER, // TODO: verify
"crystal skull"                                           to Banisher.CRYSTAL_SKULL, // TODO: verify
"classy monkey"                                           to Banisher.CLASSY_MONKEY, // TODO: verify
"punch out"                                               to Banisher.PUNCH_OUT_YOUR_FOE, // TODO: verify
"mind master"                                             to Banisher.BE_A_MIND_MASTER, // TODO: verify
```

Banishers **without patterns** (skill-triggered or no distinctive HTML text yet): `BALEFUL_HOWL`, `BASEBALL_DIAMOND`, `BATTER_UP`, `BLART_SPRAY_WIDE`, `BUNDLE_OF_FRAGRANT_HERBS`, `CRIMBUCCANEER_RIGGING_LASSO`, `CURSE_OF_VACATION`, `DEATHCHUCKS`, `GINGERBREAD_RESTRAINING_ORDER`, `GLITCHED_MALWARE`, `HEARTSTONE_BANISH`, `HOWL_OF_THE_ALPHA`, `HUMAN_MUSK`, `ICE_HOTEL_BELL`, `LEFT_ZOOT_KICK`, `LICORICE_ROPE`, `MARK_YOUR_TERRITORY`, `MONKEY_SLAP`, `NANORHINO`, `PEEL_OUT`, `PUNT_AOSOL`, `PUNT_WEREPROF`, `RIGHT_ZOOT_KICK`, `ROAR_LIKE_A_LION`, `SEADENT_LIGHTNING`, `SHOW_YOUR_BORING_FAMILIAR_PICTURES`, `SPOOKY_MUSIC_BOX_MECHANISM`, `SPRING_KICK`, `STAFF_OF_THE_STANDALONE_CHEESE`, `TENNIS_BALL` (deferred), `TRYPTOPHAN_DART`, `ULTRA_HAMMER`, `PATRIOTIC_SCREECH`. These will map to `UNKNOWN` on detection but correctly round-trip from `banishedMonsters` preference strings.

---

## Testing Strategy

### Task 0 (resolveChoice loop)
Test `AdventureManager` with mock `ChoiceRequest` that returns a sequence of choice HTML pages, then a non-choice page. Verify: (a) correct number of `choose()` calls, (b) correct stepCount passed to each `ChoiceContext`, (c) returns after non-choice response.

### Solver tests (A1–A6)
Each solver gets its own test class in `commonTest`. Tests cover:
- Disabled/preference-0 → returns null (LightsOut, VampOut)
- Each goal index at stepCount 0, 1, 2, … → returns expected option digit
- Out-of-range goal/stepCount → returns null
- VampOut: keyword matching for m/b/t/v chars; location selection from mock HTML
- ArcadeGame: "Finish from Memory" shortcut

### B1 (visit_url POST)
Mock `HttpClient` to capture the POST body, verify ktor `submitForm` call with correct parameters.

### B2 (hermit)
Mock `HermitRequest.trade()` returning success/failure; verify ASH `hermit()` returns count vs 0.

### B3 (mood list)
Populate `moodManager.moodLibrary` with a map; call `get_moods()` and `mood_list()` via ASH; verify keys appear in sorted order.

### D (banishers)
Extend existing `BanisherTest` / `AdventureParserTest`:
- `Banisher.fromName()` round-trips all 69 canonical names (including bracket names)
- `AdventureParser.parseFightResult()` with HTML containing each new pattern → correct `Banisher` returned

---

## File Summary

| Status | File |
|--------|------|
| **Modify** | `adventure/AdventureManager.kt` |
| **Create** | `adventure/choice/solvers/LightsOutSolverImpl.kt` |
| **Create** | `adventure/choice/solvers/SafetyShelterSolverImpl.kt` |
| **Create** | `adventure/choice/solvers/LostKeySolverImpl.kt` |
| **Create** | `adventure/choice/solvers/ArcadeGameSolverImpl.kt` |
| **Create** | `adventure/choice/solvers/GameproSolverImpl.kt` |
| **Create** | `adventure/choice/solvers/VampOutSolverImpl.kt` |
| **Modify** | `di/SharedModule.kt` |
| **Modify** | `ash/GameRuntimeLibrary.WebRequest.kt` |
| **Create** | `request/HermitRequest.kt` |
| **Create** | `ash/GameRuntimeLibrary.Hermit.kt` |
| **Modify** | `ash/GameRuntimeLibrary.kt` (add `hermitRequest` param + call `registerHermit`) |
| **Modify** | `ash/GameRuntimeLibrary.Mood.kt` |
| **Modify** | `banish/Banisher.kt` |
| **Modify** | `adventure/AdventureParser.kt` |

All files are in `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/` (prefix omitted for brevity).

---

## Open Questions

1. **LightsOut room keywords** — the per-room `responseText` keywords for rooms 890–903 need verification against live game HTML. The spec provides best-effort keywords from desktop ChoiceManager; adjust during implementation if text differs.

2. **HEARTSTONE_BANISH canonical name** — desktop uses `"Heartstone %banish"` (format placeholder). Verify exact string written to `banishedMonsters` pref by a running desktop build before finalising the canonical name.

3. **VampOut Isabella goals** — goals 1–3 are documented as starting at Isabella's, goals 4–13 at Vlad's. Confirm this against desktop VampOutManager before shipping.

4. **ArcadeGame "Finish from Memory" option index** — option 4 is assumed. Verify from live game HTML.
