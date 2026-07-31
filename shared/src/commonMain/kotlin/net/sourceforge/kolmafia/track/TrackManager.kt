package net.sourceforge.kolmafia.track

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Partial port of desktop [net.sourceforge.kolmafia.session.TrackManager] for rollover reset.
 * Combat track APIs (`trackMonster`, `countCopies`, etc.) remain deferred.
 */
object TrackManager {

    const val PREF_TRACKED_MONSTERS = "trackedMonsters"
    const val PREF_TRACKED_PHYLA = "trackedPhyla"

    enum class TrackType {
        MONSTER,
        PHYLUM,
    }

    enum class TrackResetType {
        TURN_RESET,
        TURN_ROLLOVER_RESET,
        ROLLOVER_RESET,
        EFFECT_RESET,
        AVATAR_RESET,
        AVATAR_TURN_RESET,
        AVATAR_ROLLOVER_RESET,
        ASCENSION_RESET,
        ;

        val isRolloverReset: Boolean
            get() = this == TURN_ROLLOVER_RESET ||
                this == ROLLOVER_RESET ||
                this == AVATAR_ROLLOVER_RESET
    }

    enum class Tracker(
        val displayName: String,
        val resetType: TrackResetType,
        val trackType: TrackType = TrackType.MONSTER,
    ) {
        OLFACTION("Transcendent Olfaction", TrackResetType.ASCENSION_RESET),
        NOSY_NOSE("Nosy Nose", TrackResetType.ASCENSION_RESET),
        GALLAPAGOS("Gallapagosian Mating Call", TrackResetType.ROLLOVER_RESET),
        LATTE("Offer Latte to Opponent", TrackResetType.TURN_ROLLOVER_RESET),
        SUPERFICIAL("Be Superficially interested", TrackResetType.TURN_RESET),
        CREAM_JIGGLE("Staff of the Cream of the Cream", TrackResetType.AVATAR_ROLLOVER_RESET),
        MAKE_FRIENDS("Make Friends", TrackResetType.AVATAR_RESET),
        CURSE_OF_STENCH("Curse of Stench", TrackResetType.AVATAR_RESET),
        LONG_CON("Long Con", TrackResetType.AVATAR_RESET),
        PERCEIVE_SOUL("Perceive Soul", TrackResetType.AVATAR_TURN_RESET),
        MOTIF("Motif", TrackResetType.AVATAR_RESET),
        MONKEY_POINT("Monkey Point", TrackResetType.ASCENSION_RESET),
        PRANK_CARD("prank Crimbo card", TrackResetType.TURN_ROLLOVER_RESET),
        TRICK_COIN("trick coin", TrackResetType.TURN_ROLLOVER_RESET),
        HUNT("Hunt", TrackResetType.AVATAR_RESET),
        MCHUGELARGE_SLASH("McHugeLarge Slash", TrackResetType.ROLLOVER_RESET),
        MEAT_CUTE("Meat Cute", TrackResetType.ROLLOVER_RESET),
        LEFT_ZOOT_KICK("Left %n Kick", TrackResetType.ASCENSION_RESET),
        RIGHT_ZOOT_KICK("Right %n Kick", TrackResetType.ASCENSION_RESET),
        TRY_TO_REMEMBER("Try to Remember", TrackResetType.ROLLOVER_RESET),
        BASEBALL_DIAMOND("Baseball Diamond", TrackResetType.ROLLOVER_RESET),
        RED_SNAPPER("Red-Nosed Snapper", TrackResetType.ASCENSION_RESET, TrackType.PHYLUM),
        A_BEASTLY_ODOR("A Beastly Odor", TrackResetType.EFFECT_RESET, TrackType.PHYLUM),
        EW_THE_HUMANITY("Ew, The Humanity", TrackResetType.EFFECT_RESET, TrackType.PHYLUM),
        ;

        companion object {
            fun find(name: String): Tracker? =
                entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
        }
    }

    data class TrackedEntry(
        val tracked: String,
        val tracker: Tracker,
        val turnTracked: Int,
    )

    /**
     * Desktop [net.sourceforge.kolmafia.session.TrackManager.resetRollover] — remove spent
     * rollover tracks from monster and phylum prefs. Unknown tracker names are preserved.
     */
    fun resetRollover(preferences: Preferences): Int {
        val monstersBefore = countKnownEntries(preferences, PREF_TRACKED_MONSTERS)
        val phylaBefore = countKnownEntries(preferences, PREF_TRACKED_PHYLA)
        resetRolloverPref(preferences, PREF_TRACKED_MONSTERS)
        resetRolloverPref(preferences, PREF_TRACKED_PHYLA)
        val monstersAfter = countKnownEntries(preferences, PREF_TRACKED_MONSTERS)
        val phylaAfter = countKnownEntries(preferences, PREF_TRACKED_PHYLA)
        return (monstersBefore - monstersAfter) + (phylaBefore - phylaAfter)
    }

    internal fun loadEntries(preferences: Preferences, prefKey: String): List<TrackedEntry> {
        val raw = preferences.getString(prefKey, "")
        if (raw.isBlank()) return emptyList()
        val tokens = raw.split(':')
        val entries = mutableListOf<TrackedEntry>()
        var index = 0
        while (index + 2 < tokens.size) {
            val tracked = tokens[index]
            val trackerName = tokens[index + 1]
            val turnTracked = tokens[index + 2].toIntOrNull()
            if (turnTracked == null) break
            val tracker = Tracker.find(trackerName)
            if (tracker != null) {
                entries.add(TrackedEntry(tracked, tracker, turnTracked))
            }
            index += 3
        }
        return entries
    }

    internal fun saveEntries(preferences: Preferences, prefKey: String, entries: List<TrackedEntry>) {
        val value = entries.joinToString(":") { entry ->
            "${entry.tracked}:${entry.tracker.displayName}:${entry.turnTracked}"
        }
        preferences.setString(prefKey, value)
    }

    private fun resetRolloverPref(preferences: Preferences, prefKey: String) {
        val raw = preferences.getString(prefKey, "")
        if (raw.isBlank()) return

        val tokens = raw.split(':').toMutableList()
        val kept = mutableListOf<String>()
        var index = 0
        while (index + 2 < tokens.size) {
            val tracked = tokens[index]
            val trackerName = tokens[index + 1]
            val turnToken = tokens[index + 2]
            val turnTracked = turnToken.toIntOrNull()
            if (turnTracked == null) {
                break
            }
            val tracker = Tracker.find(trackerName)
            val remove = tracker?.resetType?.isRolloverReset == true
            if (!remove) {
                kept += tracked
                kept += trackerName
                kept += turnToken
            }
            index += 3
        }
        preferences.setString(prefKey, kept.joinToString(":"))
    }

    private fun countKnownEntries(preferences: Preferences, prefKey: String): Int =
        loadEntries(preferences, prefKey).size
}
