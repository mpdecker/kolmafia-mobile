package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [TavernRequest] cellar map parse + explore-square layout writers.
 */
object TavernCellarSync {

    const val EMPTY_LAYOUT = "0000000000000000000000000"

    private val MAP_PATTERN =
        Regex("""alt="([^"]*) \((\d*),(\d*)\)"""")

    private val SPOT_PATTERN = Regex("""whichspot=([\d,]+)""", RegexOption.IGNORE_CASE)

    fun validateFaucetQuest(preferences: Preferences, ascensionNumber: Int) {
        val lastAscension = preferences.getInt("lastTavernAscension", 0)
        if (lastAscension < ascensionNumber) {
            preferences.setInt("lastTavernSquare", 0)
            preferences.setInt("lastTavernAscension", ascensionNumber)
            preferences.setString("tavernLayout", EMPTY_LAYOUT)
        }
    }

    fun tavernLayout(preferences: Preferences, ascensionNumber: Int): String {
        validateFaucetQuest(preferences, ascensionNumber)
        var layout = preferences.getString("tavernLayout", "")
        if (layout.length != 25) {
            layout = EMPTY_LAYOUT
            preferences.setString("tavernLayout", layout)
        }
        return layout
    }

    fun addTavernLocation(
        preferences: Preferences,
        square: Int,
        value: Char,
        ascensionNumber: Int,
    ) {
        if (square < 1 || square > 25) return
        val layout = StringBuilder(tavernLayout(preferences, ascensionNumber))
        layout.setCharAt(square - 1, value)
        preferences.setString("tavernLayout", layout.toString())
    }

    fun parseCellarMap(
        html: String,
        preferences: Preferences?,
        ascensionNumber: Int,
    ): Boolean {
        if (preferences == null) return false
        val oldLayout = tavernLayout(preferences, ascensionNumber)
        val layout = StringBuilder(oldLayout)
        for (match in MAP_PATTERN.findAll(html)) {
            val type = match.groupValues[1]
            val col = match.groupValues[2].toIntOrNull() ?: continue
            val row = match.groupValues[3].toIntOrNull() ?: continue
            val square = (row - 1) * 5 + (col - 1)
            if (square < 0 || square >= 25) continue
            var code = layout[square]
            when {
                type.startsWith("Darkness") -> code = '0'
                type.startsWith("Explored") -> {
                    if (code == '1' || code == '2' || code == '5') continue
                    code = '1'
                }
                type.startsWith("A Rat Faucet") -> code = '3'
                type.startsWith("A Tiny Mansion") ->
                    code = if (html.contains("mansion2.gif")) '6' else '4'
                type.startsWith("Stairs Up") -> code = '1'
                else -> continue
            }
            layout.setCharAt(square, code)
        }
        val newLayout = layout.toString()
        if (oldLayout == newLayout) return false
        preferences.setString("tavernLayout", newLayout)
        return true
    }

    fun getSquare(url: String?): Int {
        val urlString = url.orEmpty()
        if (!urlString.contains("cellar.php", ignoreCase = true) ||
            !urlString.contains("action=explore", ignoreCase = true)
        ) {
            return 0
        }
        val raw = SPOT_PATTERN.find(urlString)?.groupValues?.getOrNull(1) ?: return 0
        return raw.replace(",", "").toIntOrNull() ?: 0
    }

    fun preVisit(
        url: String?,
        preferences: Preferences?,
        ascensionNumber: Int,
    ): Boolean {
        if (preferences == null) return false
        validateFaucetQuest(preferences, ascensionNumber)
        val square = getSquare(url)
        if (square == 0) return false
        preferences.setInt("lastTavernSquare", square)
        return true
    }

    fun postVisit(
        url: String?,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
        ascensionNumber: Int,
        shouldSkipExplore: () -> Boolean = { false },
    ): Boolean {
        if (preferences == null) return false
        val urlString = url.orEmpty()
        if (isBareCellarMap(urlString)) {
            return parseCellarMap(html, preferences, ascensionNumber)
        }
        if (shouldSkipExplore()) return false
        if (urlString.contains("fight.php", ignoreCase = true) ||
            urlString.contains("fambattle.php", ignoreCase = true)
        ) {
            val square = preferences.getInt("lastTavernSquare", 0)
            if (square == 0) return false
            val replacement = if (html.contains("Baron")) '4' else '1'
            addTavernLocation(preferences, square, replacement, ascensionNumber)
            return true
        }
        val square = if (urlString.contains("choice.php", ignoreCase = true)) {
            preferences.getInt("lastTavernSquare", 0)
        } else {
            getSquare(urlString)
        }
        if (square == 0) return false
        var replacement = '1'
        when {
            html.contains("Those Who Came Before You") -> replacement = '2'
            html.contains("Of Course!") ||
                html.contains("Hot and Cold Running Rats") ||
                html.contains("Everything in Moderation") ||
                html.contains("Hot and Cold Dripping Rats") -> {
                replacement = '3'
                questDatabase?.setQuestIfBetter(Quest.RAT, "step2")
            }
            html.contains("is it Still a Mansion") -> replacement = '4'
            html.contains("little mansion is silent and empty") -> replacement = '6'
            html.contains("whichchoice") -> replacement = '5'
        }
        addTavernLocation(preferences, square, replacement, ascensionNumber)
        preferences.setInt("lastTavernSquare", square)
        return true
    }

    fun applyFromVisit(
        url: String?,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
        ascensionNumber: Int,
        shouldSkipExplore: () -> Boolean = { false },
    ): Boolean {
        if (preferences == null) return false
        val urlString = url.orEmpty()
        if (!urlString.contains("cellar.php", ignoreCase = true) &&
            !urlString.contains("fight.php", ignoreCase = true) &&
            !urlString.contains("fambattle.php", ignoreCase = true) &&
            !urlString.contains("choice.php", ignoreCase = true)
        ) {
            return false
        }
        var changed = false
        if (getSquare(urlString) != 0) {
            changed = preVisit(urlString, preferences, ascensionNumber) || changed
        }
        changed = postVisit(
            url = urlString,
            html = html,
            preferences = preferences,
            questDatabase = questDatabase,
            ascensionNumber = ascensionNumber,
            shouldSkipExplore = shouldSkipExplore,
        ) || changed
        return changed
    }

    private fun isBareCellarMap(url: String): Boolean =
        url.contains("cellar.php", ignoreCase = true) &&
            !url.contains("action=explore", ignoreCase = true)
}
