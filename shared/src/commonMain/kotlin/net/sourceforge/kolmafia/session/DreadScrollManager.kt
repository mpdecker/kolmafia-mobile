package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.session.DreadScrollManager] Mer-kin dreadscroll clue tracking. */
object DreadScrollManager {
    const val KNUCKLEBONE_ID = 6357
    const val MERKIN_WORKTEA_ID = 6356
    const val DREADSCROLL_ID = 6353
    const val DEEP_DARK_VISIONS_SKILL = 90
    const val HIGH_PRIEST_SUCCESS = "I guess you're the Mer-kin High Priest now"
    const val GLADIATOR_SIGIL_SUCCESS = "The sigil burned into your forehead"
    private const val WORKTEA_CLUE_PREF = "workteaClue"
    private const val DREAD_SCROLL_GUESSES_PREF = "dreadScrollGuesses"

    enum class ClueType {
        LIBRARY1,
        HEALSCROLL,
        DEEP_DARK_VISIONS,
        KNUCKLEBONE,
        KILLSCROLL,
        LIBRARY2,
        WORKTEA,
        LIBRARY3,
    }

    private val LIBRARY1_PATTERN =
        Regex("""somebody has scrawled &quot;<b>(.*?)</b>&quot;""")
    private val LIBRARY2_PATTERN =
        Regex("""a lot of references to <b>(.*?)</b> creatures\.""")
    private val LIBRARY3_PATTERN =
        Regex("""consists of the phrase <b>(.*?)</b> over and over""")
    private val HEALSCROLL_PATTERN = Regex("""a magnificent <b>(.*?)</b>""")
    private val KILLSCROLL_PATTERN =
        Regex("""recognize one of them: <b>&quot;(.*?)&quot;</b>""")
    private val KNUCKLEBONE_PATTERN =
        Regex("""it bounces straight <b>(.*?)</b>\.""")
    private val DEEP_DARK_VISIONS_PATTERN =
        Regex("""You close your eyes and let Deep visions wash over you.*?<b>(.*?)</b>.*itemimages/hp\.gif""", RegexOption.DOT_MATCHES_ALL)
    private val WORKTEA_PATTERN =
        Regex("""the leaves in the bottom look just like <b>([^<]*)</b>""")
    private val DURATION_PATTERN =
        Regex("""\((?:duration: )?(\d+) Adventures?\)""")

    private val CLUE_DATA: Array<Array<Array<String>>> = arrayOf(
        arrayOf(
            arrayOf("Mer-kin Library 1", "dreadScroll1"),
            arrayOf("LONELY", "lonely"),
            arrayOf("DOUBLED", "doubled"),
            arrayOf("THRICE-CURSED", "thrice-cursed"),
            arrayOf("FOURTH", "fourth"),
        ),
        arrayOf(
            arrayOf("Mer-kin healscroll", "dreadScroll2"),
            arrayOf("starfish"),
            arrayOf("moonfish"),
            arrayOf("sunfish"),
            arrayOf("planetfish"),
        ),
        arrayOf(
            arrayOf("Deep Dark Visions", "dreadScroll3"),
            arrayOf("The House of Cards", "Cards"),
            arrayOf("The House of Blues", "Blues"),
            arrayOf("The House of Pancakes", "Pancakes"),
            arrayOf("The House of Pain", "Pain"),
        ),
        arrayOf(
            arrayOf("Mer-kin knucklebone", "dreadScroll4"),
            arrayOf("north", "Northern"),
            arrayOf("south", "Southern"),
            arrayOf("east", "Eastern"),
            arrayOf("west", "Western"),
        ),
        arrayOf(
            arrayOf("Mer-kin killscroll", "dreadScroll5"),
            arrayOf("red", "as red as blood"),
            arrayOf("black", "as black as ink"),
            arrayOf("green", "as green as bile"),
            arrayOf("yellow", "as yellow as piss"),
        ),
        arrayOf(
            arrayOf("Mer-kin Library 2", "dreadScroll6"),
            arrayOf("blind"),
            arrayOf("giant"),
            arrayOf("finless"),
            arrayOf("two-headed"),
        ),
        arrayOf(
            arrayOf("Mer-kin worktea", "dreadScroll7"),
            arrayOf("an eel", "eel"),
            arrayOf("a turtle", "turtle"),
            arrayOf("a shark", "shark"),
            arrayOf("a whale", "whale"),
        ),
        arrayOf(
            arrayOf("Mer-kin Library 3", "dreadScroll8"),
            arrayOf("one thousand squirming young"),
            arrayOf("two and twenty stillborn spawn"),
            arrayOf("conjoined triplets"),
            arrayOf("a brand new dance craze"),
        ),
    )

    fun applyFromResponse(
        url: String?,
        html: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
        eventBus: GameEventBus?,
        itemIdHint: Int? = null,
        skillIdHint: Int? = null,
    ) {
        if (url != null) {
            when {
                url.contains("whichchoice=704", ignoreCase = true) ->
                    handleLibrary(html, preferences, sessionLogger)
                url.contains("whichchoice=703", ignoreCase = true) -> {
                    if (html.contains(HIGH_PRIEST_SUCCESS, ignoreCase = true)) {
                        handleHighPriestSuccess(html, preferences, eventBus, sessionLogger)
                    } else {
                        recordFailure(url, html, preferences)
                    }
                }
                url.contains("whichchoice=709", ignoreCase = true) ||
                    url.contains("whichchoice=713", ignoreCase = true) ||
                    url.contains("whichchoice=717", ignoreCase = true) ->
                    MerkinQuestSync.applyFromUrl(url, preferences, sessionLogger)
                url.contains("fight.php", ignoreCase = true) || html.contains("You're fighting") -> {
                    handleKillscroll(html, preferences, sessionLogger)
                    handleHealscroll(html, preferences, sessionLogger)
                }
                url.contains("inv_use.php", ignoreCase = true) &&
                    (itemIdHint == DREADSCROLL_ID || url.contains("whichitem=$DREADSCROLL_ID")) ->
                    parseDreadscrollUse(html, preferences, eventBus, sessionLogger)
                url.contains("inv_use.php", ignoreCase = true) &&
                    (itemIdHint == KNUCKLEBONE_ID || url.contains("whichitem=$KNUCKLEBONE_ID")) ->
                    handleKnucklebone(html, preferences, sessionLogger)
                url.contains("skills.php", ignoreCase = true) &&
                    (skillIdHint == DEEP_DARK_VISIONS_SKILL || url.contains("whichskill=$DEEP_DARK_VISIONS_SKILL")) ->
                    handleDeepDarkVisions(html, preferences, sessionLogger)
            }
        } else if (html.contains("You're fighting")) {
            handleKillscroll(html, preferences, sessionLogger)
            handleHealscroll(html, preferences, sessionLogger)
        }

        itemIdHint?.takeIf { it == DREADSCROLL_ID }?.let {
            parseDreadscrollUse(html, preferences, eventBus, sessionLogger)
        }
        itemIdHint?.takeIf { it == KNUCKLEBONE_ID }?.let {
            handleKnucklebone(html, preferences, sessionLogger)
        }
        skillIdHint?.takeIf { it == DEEP_DARK_VISIONS_SKILL }?.let {
            handleDeepDarkVisions(html, preferences, sessionLogger)
        }
    }

    fun handleLibrary(
        responseText: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        LIBRARY1_PATTERN.find(responseText)?.groupValues?.get(1)?.let {
            setClue(ClueType.LIBRARY1, it, preferences, sessionLogger)
            return
        }
        LIBRARY2_PATTERN.find(responseText)?.groupValues?.get(1)?.let {
            setClue(ClueType.LIBRARY2, it, preferences, sessionLogger)
            return
        }
        LIBRARY3_PATTERN.find(responseText)?.groupValues?.get(1)?.let {
            setClue(ClueType.LIBRARY3, it, preferences, sessionLogger)
        }
    }

    fun handleHealscroll(
        responseText: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        HEALSCROLL_PATTERN.find(responseText)?.groupValues?.get(1)?.let {
            setClue(ClueType.HEALSCROLL, it, preferences, sessionLogger)
        }
    }

    fun handleKillscroll(
        responseText: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        KILLSCROLL_PATTERN.find(responseText)?.groupValues?.get(1)?.let {
            setClue(ClueType.KILLSCROLL, it, preferences, sessionLogger)
        }
    }

    fun handleKnucklebone(
        responseText: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        KNUCKLEBONE_PATTERN.find(responseText)?.groupValues?.get(1)?.let {
            setClue(ClueType.KNUCKLEBONE, it, preferences, sessionLogger)
        }
    }

    fun handleDeepDarkVisions(
        responseText: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        DEEP_DARK_VISIONS_PATTERN.find(responseText)?.groupValues?.get(1)?.let {
            setClue(ClueType.DEEP_DARK_VISIONS, it, preferences, sessionLogger)
        }
    }

    fun handleWorktea(
        responseText: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
        eventBus: GameEventBus?,
    ) {
        val clue = WORKTEA_PATTERN.find(responseText)?.groupValues?.get(1) ?: return
        setClue(ClueType.WORKTEA, clue, preferences, sessionLogger)
        preferences?.setString(WORKTEA_CLUE_PREF, clue)
        eventBus?.tryEmit(GameEvent.ItemConsumed(MERKIN_WORKTEA_ID, 1))
    }

    fun getClues(preferences: Preferences): String {
        val buffer = StringBuilder()
        for (clue in ClueType.entries) {
            clueStatus(buffer, clue, preferences)
        }
        return buffer.toString().trimEnd()
    }

    fun getScrollText(preferences: Preferences): String =
        "When the " +
            cluePhrase(ClueType.LIBRARY1, preferences) +
            " " +
            cluePhrase(ClueType.HEALSCROLL, preferences) +
            " is in the House of " +
            cluePhrase(ClueType.DEEP_DARK_VISIONS, preferences) +
            "," +
            "\n" +
            "and the " +
            cluePhrase(ClueType.KNUCKLEBONE, preferences) +
            " Current runs " +
            cluePhrase(ClueType.KILLSCROLL, preferences) +
            "," +
            "\n" +
            "when a " +
            cluePhrase(ClueType.LIBRARY2, preferences) +
            " " +
            cluePhrase(ClueType.WORKTEA, preferences) +
            " births " +
            cluePhrase(ClueType.LIBRARY3, preferences) +
            "," +
            "\n" +
            "the Elder shall awaken. "

    fun parseDreadscrollUse(
        responseText: String,
        preferences: Preferences?,
        eventBus: GameEventBus?,
        sessionLogger: SessionLogger?,
    ) {
        if (responseText.contains(HIGH_PRIEST_SUCCESS, ignoreCase = true)) {
            handleHighPriestSuccess(responseText, preferences, eventBus, sessionLogger)
            return
        }
        handleGladiatorChampionSuccess(responseText, preferences, eventBus, sessionLogger)
    }

    fun handleHighPriestSuccess(
        responseText: String,
        preferences: Preferences?,
        eventBus: GameEventBus?,
        sessionLogger: SessionLogger?,
    ) {
        if (!responseText.contains(HIGH_PRIEST_SUCCESS, ignoreCase = true)) return
        val prefs = preferences ?: return
        prefs.setBoolean("isMerkinHighPriest", true)
        prefs.setString("merkinQuestPath", "scholar")
        eventBus?.tryEmit(GameEvent.ItemConsumed(DREADSCROLL_ID, 1))
        sessionLogger?.appendRawLine("Mer-kin High Priest achieved")
    }

    fun handleGladiatorChampionSuccess(
        responseText: String,
        preferences: Preferences?,
        eventBus: GameEventBus?,
        sessionLogger: SessionLogger?,
    ) {
        if (!responseText.contains(GLADIATOR_SIGIL_SUCCESS, ignoreCase = true)) return
        val prefs = preferences ?: return
        if (prefs.getString("merkinQuestPath", "") == "done") return
        prefs.setBoolean("isMerkinGladiatorChampion", true)
        prefs.setString("merkinQuestPath", "gladiator")
        prefs.setInt("lastColosseumRoundWon", 15)
        eventBus?.tryEmit(GameEvent.ItemConsumed(DREADSCROLL_ID, 1))
        sessionLogger?.appendRawLine("Mer-kin Gladiator Champion achieved")
    }

    fun recordFailure(url: String, text: String, preferences: Preferences?) {
        val prefs = preferences ?: return
        val duration = DURATION_PATTERN.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: return
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return
        val params = url.substring(queryStart + 1).split('&')
        val pros = Array<String?>(8) { null }
        for (param in params) {
            if (!param.startsWith("pro")) continue
            val pair = param.split('=', limit = 2)
            if (pair.size < 2) return
            val index = pair[0].substring(3).toIntOrNull()?.minus(1) ?: return
            if (index !in pros.indices) return
            pros[index] = pair[1]
        }
        if (pros.any { it == null }) return
        val attempt = pros.joinToString("")
        val newGuess = "$attempt:${duration / 3}"
        val knownTries = prefs.getString(DREAD_SCROLL_GUESSES_PREF, "")
        prefs.setString(
            DREAD_SCROLL_GUESSES_PREF,
            if (knownTries.isEmpty()) newGuess else "$knownTries,$newGuess",
        )
    }

    private fun setClue(
        clue: ClueType,
        value: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        val prefs = preferences ?: return
        val data = CLUE_DATA[clue.ordinal]
        val setting = data[0][1]
        for (i in 1..4) {
            if (value == data[i][0]) {
                prefs.setInt(setting, i)
                break
            }
        }
        val message = "${data[0][0]} clue: $value"
        sessionLogger?.appendRawLine(message)
    }

    private fun cluePhrase(clue: ClueType, preferences: Preferences): String {
        val data = CLUE_DATA[clue.ordinal]
        val setting = data[0][1]
        val value = preferences.getInt(setting, 0)
        if (value !in 1..4) return "???"
        val option = data[value]
        return if (option.size > 1) option[1] else option[0]
    }

    private fun clueStatus(buffer: StringBuilder, clue: ClueType, preferences: Preferences) {
        val data = CLUE_DATA[clue.ordinal]
        val setting = data[0][1]
        val value = preferences.getInt(setting, 0)
        buffer.append(setting)
        buffer.append(" (")
        buffer.append(data[0][0])
        buffer.append("): ")
        buffer.append(value)
        buffer.append(" (")
        buffer.append(if (value == 0) "unknown" else data[value][0])
        buffer.append(")")
        buffer.append('\n')
    }
}
