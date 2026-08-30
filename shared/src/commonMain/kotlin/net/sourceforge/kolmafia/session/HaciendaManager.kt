package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * Desktop [HaciendaManager] — haciendaLayout solver, clue/reward catalog,
 * fight/key markers, recording cast prefs (Phases 3351–3365).
 */
object HaciendaManager {

    const val LAYOUT_PREF = "haciendaLayout"
    const val CHOICE_HALLWAY = 410
    const val CHOICE_HALLWAY_LEFT = 411
    const val CHOICE_HALLWAY_RIGHT = 412
    val ROOM_CHOICE_IDS = 413..418
    const val RECORDING_CHOICE = 440

    private val CLUES = arrayOf(
        "a potato peeler" to 0,
        "an empty sardine can" to 1,
        "an apple core" to 2,
        "a silver pepper-mill" to 3,
        "the lid from a can of sterno" to 4,
        "an empty teacup" to 5,
        "a small crowbar" to 6,
        "a pair of needle-nose pliers" to 7,
        "an empty rifle cartridge" to 8,
        "a long nightcap with a pom-pom on the end" to 9,
        "a dirty sock" to 10,
        "a toothbrush" to 11,
    )

    private val REWARDS = arrayOf(
        "silver cheese-slicer",
        "taco shells",
        "fettucini Inconnu",
        "silver salt-shaker",
        "can of sterno",
        "silver pat&eacute; knife",
        "fancy beef jerky",
        "pipe wrench",
        "gun cleaning kit",
        "sleep mask",
        "sock garters",
        "mariachi toothpaste",
        "heavy leather-bound tome",
        "large handful of meat",
        "leather bookmark",
        "ivory cue ball",
        "decanter of fine Scotch",
        "expensive cigar",
    )

    private data class Recording(val effectId: Int, val setting: String)

    private val RECORDINGS = arrayOf(
        Recording(530, "_thingfinderCasts"),
        Recording(531, "_benettonsCasts"),
        Recording(532, "_elronsCasts"),
        Recording(533, "_companionshipCasts"),
        Recording(534, "_precisionCasts"),
        Recording(614, "_donhosCasts"),
        Recording(716, "_inigosCasts"),
    )

    private val OPTION_PATTERN =
        Regex("""<option value="?(\d+)"? *>(.*?) \((\d+)/(\d+)\)</option>""", RegexOption.IGNORE_CASE)
    private val WHICHEFFECT_PATTERN = Regex("""whicheffect=(\d+)""", RegexOption.IGNORE_CASE)
    private val TIMES_PATTERN = Regex("""times=(\d+)""", RegexOption.IGNORE_CASE)

    fun parseRoom(
        lastChoice: Int,
        lastDecision: Int,
        text: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
        sessionLog: (String) -> Unit = {},
    ) {
        if (preferences == null) return
        var layout = preferences.getString(LAYOUT_PREF, "000000000000000000")
        if (layout.length != 18) layout = "000000000000000000"
        val newLayout = StringBuilder(layout)

        val currentSearch = lastChoice * 3 + lastDecision - 1240
        if (currentSearch !in 0 until 18) return

        val clueTo = returnClue(text)
        when {
            text.contains("Fight!") -> newLayout.setCharAt(currentSearch, 'F')
            text.contains("hacienda key", ignoreCase = true) -> newLayout.setCharAt(currentSearch, 'K')
            clueTo != -1 -> {
                newLayout.setCharAt(currentSearch, 'C')
                if (layout[currentSearch] != 'K') {
                    newLayout.setCharAt(clueTo, 'k')
                }
                val clue = "You have found a clue: ${getClue(text)}"
                sessionLog(clue)
            }
            text.contains("large handful of meat", ignoreCase = true) || verifyReward(text) ->
                newLayout.setCharAt(currentSearch, 'R')
            layout[currentSearch] == '0' -> newLayout.setCharAt(currentSearch, 'X')
        }

        layout = newLayout.toString()
        preferences.setString(LAYOUT_PREF, layout)

        if (questDatabase?.isQuestFinished(Quest.NEMESIS) == true &&
            countString(layout.lowercase(), "f") > 0
        ) {
            questCompleted(preferences)
            layout = preferences.getString(LAYOUT_PREF, layout)
            newLayout.clear()
            newLayout.append(layout)
        }

        val questComplete = questDatabase?.isQuestFinished(Quest.NEMESIS) == true
        for (i in 0 until 6) {
            val room = layout.substring(i * 3, i * 3 + 3)
            if (questComplete) {
                for (j in 0 until 3) {
                    val currentCheck = i * 3 + j
                    if (layout[currentCheck] == '0') {
                        if (room.contains('K') || room.contains('C')) {
                            newLayout.setCharAt(currentCheck, 'r')
                        } else if (countString(room.lowercase(), "r") == 2) {
                            newLayout.setCharAt(currentCheck, 'C')
                        }
                    }
                }
            } else {
                for (j in 0 until 3) {
                    val currentCheck = i * 3 + j
                    if (layout[currentCheck] == '0') {
                        val keyOrClue = room.lowercase().contains('k') ||
                            room.lowercase().contains('c') ||
                            room.lowercase().contains('u')
                        when {
                            keyOrClue && room.contains('F') -> newLayout.setCharAt(currentCheck, 'r')
                            keyOrClue && room.contains('R') -> newLayout.setCharAt(currentCheck, 'f')
                            room.contains('F') && room.contains('R') -> {
                                when {
                                    countString(layout.lowercase(), "k") == 4 ->
                                        newLayout.setCharAt(currentCheck, 'c')
                                    countString(layout.lowercase(), "c") == 2 ->
                                        newLayout.setCharAt(currentCheck, 'k')
                                    else -> newLayout.setCharAt(currentCheck, 'u')
                                }
                            }
                        }
                    }
                }
            }
        }
        preferences.setString(LAYOUT_PREF, newLayout.toString())
    }

    fun questCompleted(preferences: Preferences) {
        var layout = preferences.getString(LAYOUT_PREF, "000000000000000000")
        if (layout.length != 18) layout = "000000000000000000"
        val newLayout = StringBuilder(layout)

        for (i in 0 until 6) {
            val room = layout.substring(i * 3, i * 3 + 3)
            for (j in 0 until 3) {
                val currentCheck = i * 3 + j
                when (layout[currentCheck]) {
                    'u' -> newLayout.setCharAt(currentCheck, 'C')
                    'F', 'f' -> newLayout.setCharAt(currentCheck, 'r')
                }
            }
            layout = newLayout.toString()
            preferences.setString(LAYOUT_PREF, layout)
            for (j in 0 until 3) {
                val currentCheck = i * 3 + j
                if (layout[currentCheck] == '0') {
                    if (room.contains('K') || room.contains('C')) {
                        newLayout.setCharAt(currentCheck, 'r')
                    } else if (countString(room.lowercase(), "r") == 2) {
                        newLayout.setCharAt(currentCheck, 'C')
                    }
                }
            }
        }
        preferences.setString(LAYOUT_PREF, newLayout.toString())
    }

    fun getSpoiler(spoiler: Int, preferences: Preferences?, questDatabase: QuestDatabase?): String {
        var layout = preferences?.getString(LAYOUT_PREF, "000000000000000000").orEmpty()
        if (layout.length != 18) layout = "000000000000000000"
        val questComplete = questDatabase?.isQuestFinished(Quest.NEMESIS) == true
        if (spoiler !in 0 until 18) return "unknown result"
        val roomNumber = spoiler / 3
        val room = layout.substring(roomNumber * 3, roomNumber * 3 + 3)
        val result = when (layout[spoiler]) {
            'K' -> "empty"
            'u' -> "gain hacienda key or clue"
            'k' -> "gain hacienda key"
            'c' -> "gain clue"
            'F', 'f' -> "fight mariachi"
            'R' -> "empty"
            'r' -> "gain ${returnReward(spoiler)}"
            'C', 'X' -> "empty"
            '0' -> when {
                room.contains('F') -> "key, clue or reward"
                room.lowercase().contains('k') ||
                    room.lowercase().contains('c') ||
                    room.lowercase().contains('u') -> "fight or reward"
                room.contains('R') || room.contains('r') ->
                    if (questComplete) "reward or clue" else "key, clue or fight"
                else -> if (questComplete) "reward or clue" else "unknown"
            }
            else -> "unknown result"
        }
        return if (questComplete && spoiler == 17) "$result make recordings" else result
    }

    fun getWingSpoilers(spoiler: Int, preferences: Preferences?, questDatabase: QuestDatabase?): String {
        var layout = preferences?.getString(LAYOUT_PREF, "000000000000000000").orEmpty()
        if (layout.length != 18) layout = "000000000000000000"
        val questComplete = questDatabase?.isQuestFinished(Quest.NEMESIS) == true
        val wingNumber = spoiler / 9
        val wing = layout.substring(wingNumber * 9, wingNumber * 9 + 9)
        val keysFound = countString(wing, "K")
        val keysLocated = countString(wing, "k")
        val cluesLocated = countString(wing.lowercase(), "c")
        val rewardsFound = countString(wing, "R")
        val rewardsLocated = countString(wing, "r")
        return buildString {
            if (questComplete) {
                append(6 - rewardsFound)
                append(" rewards left, ")
                append(rewardsLocated)
                append(" located")
                if (spoiler == 9) append(", make recordings")
                append('.')
            } else {
                append(3 - keysFound - cluesLocated)
                append(" keys or clues left, ")
                append(keysLocated)
                append(" keys located.")
            }
        }
    }

    fun preRecording(text: String, preferences: Preferences?) {
        preferences ?: return
        for (match in OPTION_PATTERN.findAll(text)) {
            val effectId = match.groupValues[1].toIntOrNull() ?: continue
            val used = match.groupValues[3].toIntOrNull() ?: continue
            effectIdToSetting(effectId)?.let { preferences.setInt(it, used) }
        }
    }

    fun parseRecording(urlString: String, text: String, preferences: Preferences?) {
        preferences ?: return
        if (!text.contains("You acquire", ignoreCase = true)) return
        val effectId = WHICHEFFECT_PATTERN.find(urlString)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val times = TIMES_PATTERN.find(urlString)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        effectIdToSetting(effectId)?.let { setting ->
            preferences.setInt(setting, preferences.getInt(setting, 0) + times)
        }
        preRecording(text, preferences)
    }

    private fun returnClue(text: String): Int =
        CLUES.firstOrNull { text.contains(it.first, ignoreCase = true) }?.second ?: -1

    private fun getClue(text: String): String? =
        CLUES.firstOrNull { text.contains(it.first, ignoreCase = true) }?.first

    private fun verifyReward(text: String): Boolean {
        val items = ResultProcessor.parseItems(text)
        if (items.isEmpty()) return false
        val itemName = items.first().first
        return REWARDS.any { it.equals(itemName, ignoreCase = true) }
    }

    private fun returnReward(location: Int): String =
        REWARDS.getOrElse(location) { "unknown reward" }

    private fun effectIdToSetting(effectId: Int): String? =
        RECORDINGS.firstOrNull { it.effectId == effectId }?.setting

    private fun countString(inString: String, lookFor: String): Int {
        var result = 0
        var index = inString.indexOf(lookFor)
        while (index != -1) {
            result++
            index = inString.indexOf(lookFor, index + 1)
        }
        return result
    }
}
