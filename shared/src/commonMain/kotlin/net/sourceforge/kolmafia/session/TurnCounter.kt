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
        /** Mutable: the turn on which this counter last produced a warning. */
        var lastWarned: Int = -1,
    ) {
        /** Desktop `type=wander` heuristic — label contains "window" or has `type=wander` tag. */
        val isWander: Boolean
            get() = label.contains("type=wander") ||
                label.contains("window", ignoreCase = true)

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

        /** Desktop [TurnCounter.isExempt] — `loc=*` exempts every location; `loc=id` is a set. */
        fun isExempt(adventureId: String): Boolean {
            var allLocations = false
            val locs = mutableSetOf<String>()
            var text = label
            while (true) {
                val pos = text.lastIndexOf(' ')
                if (pos < 0) {
                    val word = text.trim()
                    if (word == "loc=*") allLocations = true
                    else if (word.startsWith("loc=")) locs += word.substring(4)
                    break
                }
                val word = text.substring(pos + 1).trim()
                when {
                    word == "loc=*" -> allLocations = true
                    word.startsWith("loc=") -> if (!allLocations) locs += word.substring(4)
                    word.startsWith("type=") || word.contains(".php") -> Unit
                    else -> break
                }
                text = text.substring(0, pos).trim()
            }
            return allLocations || locs.contains(adventureId)
        }
    }

    /** In-memory lastWarned by label+image — pref format has no room for it (desktop parity). */
    private val lastWarnedMemory = mutableMapOf<String, Int>()

    private fun entryKey(label: String, image: String): String = "$label\u0000$image"

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
            val warned = lastWarnedMemory[entryKey(label, image)] ?: -1
            entries.add(Entry(turn, label, image, lastWarned = warned))
            i += 3
        }
        return entries
    }

    fun save(preferences: Preferences, entries: List<Entry>) {
        val value = entries.joinToString(":") { "${it.absoluteTurn}:${it.label}:${it.image}" }
        preferences.setString(PREF_KEY, value)
        // Keep lastWarned for surviving entries; drop keys no longer present.
        val alive = entries.map { entryKey(it.label, it.image) }.toSet()
        lastWarnedMemory.keys.retainAll(alive)
        for (entry in entries) {
            if (entry.lastWarned >= 0) {
                lastWarnedMemory[entryKey(entry.label, entry.image)] = entry.lastWarned
            }
        }
    }

    /** Desktop [TurnCounter.clearCounters] — drop all relay counters on ascension. */
    fun clearCounters(preferences: Preferences) {
        lastWarnedMemory.clear()
        save(preferences, emptyList())
        preferences.setString(TEMP_PREF_KEY, "")
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

    /** Desktop [TurnCounter.addWarning] — strip ` loc=*` so the counter warns on expiry. */
    fun addWarning(preferences: Preferences, label: String) {
        val entries = load(preferences).map { entry ->
            if (entry.parsedLabel().equals(label, ignoreCase = true) && entry.label.contains(" loc=*")) {
                entry.copy(label = entry.label.replace(" loc=*", ""))
            } else {
                entry
            }
        }
        save(preferences, entries)
    }

    /** Desktop [TurnCounter.removeWarning] — append ` loc=*` so expiry is silent. */
    fun removeWarning(preferences: Preferences, label: String) {
        val entries = load(preferences).map { entry ->
            if (entry.parsedLabel().equals(label, ignoreCase = true) && !entry.label.contains(" loc=*")) {
                entry.copy(label = "${entry.label} loc=*")
            } else {
                entry
            }
        }
        save(preferences, entries)
    }

    val WANDERING_MONSTER_LABELS = listOf(
        "Romantic Monster window begin",
        "Romantic Monster window end",
        "Digitize Monster",
        "Holiday Monster window begin",
        "Holiday Monster window end",
        "Event Monster window begin",
        "Event Monster window end",
        "Taco Elf window begin",
        "Taco Elf window end",
        "Latte Monster",
    )

    /** Desktop [net.sourceforge.kolmafia.KoLmafia.resetCounters] wandering-monster window clears. */
    fun stopWanderingMonsterWindows(preferences: Preferences): Int {
        val before = load(preferences).size
        for (label in WANDERING_MONSTER_LABELS) {
            stopCounting(preferences, label)
        }
        return before - load(preferences).size
    }

    /**
     * Desktop [net.sourceforge.kolmafia.KoLmafia.resetCounters] mayonnaise window carryover:
     * re-base remaining turns for the next run.
     */
    fun resetMayonnaiseWindowsForRun(preferences: Preferences, currentRun: Int): Int {
        val entries = load(preferences)
        var adjusted = 0
        val updated = entries.map { entry ->
            if (!entry.parsedLabel().startsWith(MAYONNAISE_LABEL_PREFIX)) {
                entry
            } else {
                adjusted++
                val remaining = (entry.absoluteTurn - currentRun).coerceAtLeast(0)
                entry.copy(absoluteTurn = currentRun + remaining)
            }
        }
        if (adjusted > 0) {
            save(preferences, updated)
        }
        return adjusted
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

    fun findByLabel(preferences: Preferences, label: String): Entry? {
        val needle = label.lowercase()
        val checkExempt = needle.isEmpty()
        return load(preferences).firstOrNull { entry ->
            if (checkExempt && entry.isExempt("")) return@firstOrNull false
            if (needle.isEmpty()) {
                true
            } else {
                entry.parsedLabel().lowercase().contains(needle)
            }
        }
    }

    /** Desktop [TurnCounter.isCounting] — active counter with parsed label and turn >= currentRun. */
    fun isCounting(preferences: Preferences, label: String, currentRun: Int): Boolean =
        load(preferences).any { entry ->
            entry.parsedLabel().equals(label, ignoreCase = true) &&
                entry.absoluteTurn >= currentRun
        }

    /** Desktop [TurnCounter.isCounting] range overload — absolute turn in [currentRun+start, currentRun+stop]. */
    fun isCounting(
        preferences: Preferences,
        label: String,
        currentRun: Int,
        start: Int,
        stop: Int,
    ): Boolean {
        val begin = currentRun + start
        val end = currentRun + stop
        return load(preferences).any { entry ->
            entry.parsedLabel().equals(label, ignoreCase = true) &&
                entry.absoluteTurn in begin..end
        }
    }

    /**
     * Desktop [TurnCounter.getCounters] — parsed labels whose absolute turn falls in the offset window.
     * Returns distinct parsed labels matching [label] (case-insensitive substring when non-blank).
     */
    fun getCounterLabels(
        preferences: Preferences,
        label: String,
        currentRun: Int,
        minTurns: Int,
        maxTurns: Int,
    ): List<String> {
        val minTurn = currentRun + minTurns
        val maxTurn = currentRun + maxTurns
        val needle = label.lowercase()
        return load(preferences)
            .filter { entry ->
                entry.absoluteTurn in minTurn..maxTurn &&
                    !(needle.isBlank() && entry.isExempt("")) &&
                    (needle.isBlank() || entry.parsedLabel().lowercase().contains(needle))
            }
            .map { it.parsedLabel() }
            .distinct()
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

    // ── Temporary counters (Group B phases 5846–5855) ──────────────────────

    /**
     * Desktop [TurnCounter.startCountingTemporary] — append to `_tempRelayCounters` pref
     * as `turns:label:image|` for deferred start via [handleTemporaryCounters].
     */
    fun startCountingTemporary(preferences: Preferences, turns: Int, label: String, image: String) {
        val temp = preferences.getString(TEMP_PREF_KEY, "")
        preferences.setString(TEMP_PREF_KEY, "$temp$turns:$label:$image|")
    }

    /**
     * Desktop [TurnCounter.handleTemporaryCounters] — if the temp pref is non-empty, the last
     * location has wanderers, and (type != "Combat" OR [encounter] is not a no-wander monster),
     * start each deferred counter and clear the pref.
     */
    fun handleTemporaryCounters(
        preferences: Preferences,
        currentRun: Int,
        type: String,
        encounter: String,
        lastLocationHasWanderers: Boolean,
        isNoWanderMonster: (String) -> Boolean = { false },
    ) {
        val temp = preferences.getString(TEMP_PREF_KEY, "")
        if (temp.isBlank()) return
        if (!lastLocationHasWanderers) return
        if (type.equals("Combat", ignoreCase = true) && isNoWanderMonster(encounter)) return

        val counters = temp.split('|')
        for (counter in counters) {
            if (counter.isBlank()) continue
            val parts = counter.split(':')
            if (parts.size < 3) continue
            val turns = parts[0].toIntOrNull() ?: continue
            startCounting(preferences, currentRun, turns, parts[1], parts[2])
        }
        preferences.setString(TEMP_PREF_KEY, "")
    }

    /**
     * Desktop [TurnCounter.getExpiredCounter] — return the first counter that has expired
     * (absoluteTurn ≤ currentRun + turnsUsed - 1), respecting lastWarned and exemption.
     * Updates [Entry.lastWarned] and auto-saves.
     *
     * @param informational true for informational counters (exempt ones), false for normal
     */
    fun getExpiredCounter(
        preferences: Preferences,
        currentRun: Int,
        turnsUsed: Int,
        adventureId: String,
        informational: Boolean,
    ): Entry? {
        if (turnsUsed == 0) return null
        val currentTurns = currentRun + turnsUsed - 1
        val entries = load(preferences).toMutableList()

        for (entry in entries) {
            if (entry.absoluteTurn > currentTurns) continue
            if (entry.lastWarned == currentRun) continue
            if (entry.isExempt(adventureId) != informational) continue

            // Informational counters defer until actual expiration
            if (informational && entry.absoluteTurn > currentRun) continue

            // Remove past non-wander counters
            if (entry.absoluteTurn < currentRun) {
                if (entry.isWander) continue
                entries.remove(entry)
                save(preferences, entries)
                entry.lastWarned = currentRun
                return entry
            }

            entry.lastWarned = currentRun
            save(preferences, entries)
            return entry
        }
        return null
    }

    /**
     * Desktop [TurnCounter.getUnexpiredCounters] — newline-separated
     * `label (turnsRemaining)` for all counters at or after [currentRun].
     */
    fun getUnexpiredCounters(preferences: Preferences, currentRun: Int): String {
        val sb = StringBuilder()
        for (entry in load(preferences)) {
            if (entry.absoluteTurn < currentRun) continue
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(entry.parsedLabel())
            sb.append(" (")
            sb.append(entry.absoluteTurn - currentRun)
            sb.append(')')
        }
        return sb.toString()
    }

    const val PREF_KEY = "relayCounters"
    const val TEMP_PREF_KEY = "_tempRelayCounters"
    const val MAYONNAISE_LABEL_PREFIX = "Mmmmmmayonnaise window "

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
