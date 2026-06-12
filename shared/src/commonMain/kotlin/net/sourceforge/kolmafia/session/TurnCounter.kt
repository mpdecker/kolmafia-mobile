package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Minimal relay turn-counter storage matching desktop `relayCounters` pref format.
 * Stores absolute turn values: `turns:label:image:turns:label:image`.
 */
object TurnCounter {

    data class Entry(
        val absoluteTurn: Int,
        val label: String,
        val image: String,
    ) {
        fun parsedLabel(): String {
            var text = label
            while (true) {
                val pos = text.lastIndexOf(' ')
                if (pos < 0) break
                val word = text.substring(pos + 1)
                if (word.startsWith("loc=") || word.startsWith("type=") || word.contains(".php")) {
                    text = text.substring(0, pos).trim()
                } else break
            }
            return text.ifBlank { "Manual" }
        }
    }

    fun load(preferences: Preferences): List<Entry> {
        val raw = preferences.getString(PREF_KEY, "")
        if (raw.isBlank()) return emptyList()
        val tokens = raw.split(':')
        val entries = mutableListOf<Entry>()
        var i = 0
        while (i + 2 < tokens.size) {
            val turn = tokens[i].toIntOrNull() ?: break
            val label = tokens[i + 1]
            val image = tokens[i + 2]
            entries.add(Entry(turn, label, image))
            i += 3
        }
        return entries
    }

    fun save(preferences: Preferences, entries: List<Entry>) {
        val value = entries.joinToString(":") { "${it.absoluteTurn}:${it.label}:${it.image}" }
        preferences.setString(PREF_KEY, value)
    }

    fun startCounting(preferences: Preferences, currentRun: Int, turns: Int, label: String, image: String) {
        if (turns < 0) return
        val entries = load(preferences).toMutableList()
        val entry = Entry(currentRun + turns, label, image)
        if (entries.none { it.parsedLabel() == entry.parsedLabel() && it.image == entry.image }) {
            entries.add(entry)
        }
        save(preferences, entries)
    }

    fun stopCounting(preferences: Preferences, label: String) {
        val entries = load(preferences).filterNot { it.parsedLabel().equals(label, ignoreCase = true) }
        save(preferences, entries)
    }

    /** Remove counters whose absolute turn has been reached or passed. */
    fun removeExpired(preferences: Preferences, currentRun: Int) {
        val entries = load(preferences).filter { it.absoluteTurn > currentRun }
        if (entries.size != load(preferences).size) {
            save(preferences, entries)
        }
    }

    fun turnsRemaining(entry: Entry?, currentRun: Int): Int =
        if (entry == null) -1 else (entry.absoluteTurn - currentRun).coerceAtLeast(0)

    fun findByLabel(preferences: Preferences, label: String): Entry? =
        load(preferences).firstOrNull {
            it.parsedLabel().equals(label, ignoreCase = true) ||
                it.label.contains(label, ignoreCase = true)
        }

    fun formatRelayCounters(preferences: Preferences, currentRun: Int): String {
        val entries = load(preferences)
        if (entries.isEmpty()) return ""
        return entries.joinToString("\n") { entry ->
            val turns = turnsRemaining(entry, currentRun)
            "${entry.parsedLabel()}: $turns turns (${entry.image})"
        }
    }

    fun resetNemesisAssassinWindow(preferences: Preferences, currentRun: Int) {
        stopCounting(preferences, "Nemesis Assassin window begin")
        stopCounting(preferences, "Nemesis Assassin window end")
        startCounting(
            preferences, currentRun, 35,
            "Nemesis Assassin window begin loc=*", "lparen.gif",
        )
        startCounting(
            preferences, currentRun, 50,
            "Nemesis Assassin window end loc=* type=wander", "rparen.gif",
        )
    }

    fun startNemesisAssassinUnlock(preferences: Preferences, currentRun: Int) {
        stopCounting(preferences, "Nemesis Assassin window begin")
        stopCounting(preferences, "Nemesis Assassin window end")
        startCounting(
            preferences, currentRun, 5,
            "Nemesis Assassin window begin loc=*", "lparen.gif",
        )
        startCounting(
            preferences, currentRun, 15,
            "Nemesis Assassin window end loc=* type=wander", "rparen.gif",
        )
    }

    const val PREF_KEY = "relayCounters"

    val NEMESIS_ASSASSIN_MONSTERS = setOf(
        "menacing thug",
        "Mob Penguin hitman",
        "hunting seal",
        "turtle trapper",
        "evil spaghetti cult assassin",
        "béarnaise zombie",
        "flock of seagulls",
        "mariachi bandolero",
        "Argarggagarg the Dire Hellseal",
        "Safari Jack, Small-Game Hunter",
        "Yakisoba the Executioner",
        "Heimandatz, Nacho Golem",
        "Jocko Homo",
        "The Mariachi With No Name",
    )
}
