package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [IslandRequest] concert effect tables + resolution helpers. */
object IslandWarConcert {

    val HIPPY_CONCERTS: List<Pair<String, String>> = listOf(
        "Moon'd" to "+5 Stat(s) Per Fight",
        "Dilated Pupils" to "Item Drop +20%",
        "Optimist Primal" to "Familiar Weight +5",
    )

    val FRATBOY_CONCERTS: List<Pair<String, String>> = listOf(
        "Elvish" to "All Attributes +10%",
        "Winklered" to "Meat Drop +40%",
        "White-boy Angst" to "Initiative +50%",
    )

    fun effectToConcertNumber(
        completer: String,
        effect: String,
        preferences: Preferences,
    ): Int {
        if (effect.isEmpty()) return 0

        val loser = preferences.getString("sideDefeated", "neither")
        if (loser == completer || loser == "both") return 0

        val array = when (completer) {
            "hippies" -> HIPPY_CONCERTS
            "fratboys" -> FRATBOY_CONCERTS
            else -> return 0
        }

        val compare = effect.lowercase()
        for ((index, entry) in array.withIndex()) {
            if (entry.first.lowercase().startsWith(compare)) {
                return index + 1
            }
        }
        return 0
    }

    fun concertError(arg: String, preferences: Preferences): String {
        if (preferences.getString("warProgress", "unstarted") == "unstarted") {
            return "You have not started the island war yet."
        }

        val completer = IslandWarPaths.questCompleter("sidequestArenaCompleted", preferences)
        if (completer == "none") {
            return "The arena is not open."
        }

        val loser = preferences.getString("sideDefeated", "neither")
        if (loser == completer || loser == "both") {
            return "The arena's fans were defeated in the war."
        }

        if (arg.isNotEmpty() && arg[0].isDigit()) {
            val option = arg.toIntOrNull() ?: 0
            if (option < 0 || option > 3) {
                return "Invalid concert number."
            }
        } else {
            val option = effectToConcertNumber(completer, arg, preferences)
            if (option == 0) {
                return "The \"$arg\" effect is not available to $completer."
            }
        }

        return ""
    }

    /**
     * Resolve a concert CLI argument to an option 0–3, or null when unavailable.
     * Mirrors desktop [IslandRequest.getConcertRequest] null cases.
     */
    fun resolveConcertOption(arg: String, preferences: Preferences): Int? {
        if (IslandWarPaths.currentIsland(preferences) == "bogus.php") return null

        return if (arg.isNotEmpty() && arg[0].isDigit()) {
            val option = arg.toIntOrNull() ?: return null
            if (option < 0 || option > 3) null else option
        } else {
            val completer = IslandWarPaths.questCompleter("sidequestArenaCompleted", preferences)
            val option = effectToConcertNumber(completer, arg, preferences)
            if (option == 0) null else option
        }
    }

    fun concertUrl(option: Int, preferences: Preferences): String? {
        val island = IslandWarPaths.currentIsland(preferences)
        if (island == "bogus.php") return null
        if (option < 0 || option > 3) return null
        return "$island?action=concert&option=$option"
    }

    fun nunneryUrl(preferences: Preferences): String? {
        val island = IslandWarPaths.currentIsland(preferences)
        if (island == "bogus.php") return null
        return "$island?place=nunnery&action=nuns"
    }
}
