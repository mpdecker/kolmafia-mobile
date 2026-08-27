package net.sourceforge.kolmafia.track

import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Port of desktop [net.sourceforge.kolmafia.session.TrackManager] (Phases 1091–1110).
 * Tracks monster/phylum copies for olfaction-style encounter weighting.
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

        val isAvatarReset: Boolean
            get() = this == AVATAR_RESET ||
                this == AVATAR_TURN_RESET ||
                this == AVATAR_ROLLOVER_RESET
    }

    enum class Tracker(
        val displayName: String,
        val copies: Int,
        val ignoreQueue: Boolean,
        val duration: Int,
        val resetType: TrackResetType,
        val trackType: TrackType = TrackType.MONSTER,
    ) {
        OLFACTION("Transcendent Olfaction", 3, true, -1, TrackResetType.ASCENSION_RESET),
        NOSY_NOSE("Nosy Nose", 1, false, -1, TrackResetType.ASCENSION_RESET),
        GALLAPAGOS("Gallapagosian Mating Call", 1, false, -1, TrackResetType.ROLLOVER_RESET),
        LATTE("Offer Latte to Opponent", 2, false, 30, TrackResetType.TURN_ROLLOVER_RESET),
        SUPERFICIAL("Be Superficially interested", 3, false, 80, TrackResetType.TURN_RESET),
        CREAM_JIGGLE("Staff of the Cream of the Cream", 2, false, -1, TrackResetType.AVATAR_ROLLOVER_RESET),
        MAKE_FRIENDS("Make Friends", 3, false, -1, TrackResetType.AVATAR_RESET),
        CURSE_OF_STENCH("Curse of Stench", 3, false, -1, TrackResetType.AVATAR_RESET),
        LONG_CON("Long Con", 3, true, -1, TrackResetType.AVATAR_RESET),
        PERCEIVE_SOUL("Perceive Soul", 2, false, 30, TrackResetType.AVATAR_TURN_RESET),
        MOTIF("Motif", 2, true, -1, TrackResetType.AVATAR_RESET),
        MONKEY_POINT("Monkey Point", 2, false, -1, TrackResetType.ASCENSION_RESET),
        PRANK_CARD("prank Crimbo card", 3, true, 100, TrackResetType.TURN_ROLLOVER_RESET),
        TRICK_COIN("trick coin", 3, true, 100, TrackResetType.TURN_ROLLOVER_RESET),
        HUNT("Hunt", 3, true, -1, TrackResetType.AVATAR_RESET),
        MCHUGELARGE_SLASH("McHugeLarge Slash", 3, true, -1, TrackResetType.ROLLOVER_RESET),
        MEAT_CUTE("Meat Cute", 3, true, -1, TrackResetType.ROLLOVER_RESET),
        LEFT_ZOOT_KICK("Left %n Kick", 6, true, -1, TrackResetType.ASCENSION_RESET),
        RIGHT_ZOOT_KICK("Right %n Kick", 6, true, -1, TrackResetType.ASCENSION_RESET),
        TRY_TO_REMEMBER("Try to Remember", 2, false, -1, TrackResetType.ROLLOVER_RESET),
        BASEBALL_DIAMOND("Baseball Diamond", 3, true, -1, TrackResetType.ROLLOVER_RESET),
        RED_SNAPPER("Red-Nosed Snapper", 2, false, -1, TrackResetType.ASCENSION_RESET, TrackType.PHYLUM),
        A_BEASTLY_ODOR("A Beastly Odor", 2, false, -1, TrackResetType.EFFECT_RESET, TrackType.PHYLUM),
        EW_THE_HUMANITY("Ew, The Humanity", 2, false, -1, TrackResetType.EFFECT_RESET, TrackType.PHYLUM),
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
    ) {
        fun isExpired(currentTurn: Int): Boolean {
            if (tracker.duration < 0) return false
            return when (tracker.resetType) {
                TrackResetType.TURN_RESET,
                TrackResetType.TURN_ROLLOVER_RESET,
                TrackResetType.AVATAR_TURN_RESET,
                -> currentTurn >= turnTracked + tracker.duration
                else -> false
            }
        }
    }

    /**
     * Desktop [TrackManager.trackMonster] — resolve monster vs phylum entry and track.
     */
    fun trackMonster(
        preferences: Preferences,
        monsterName: String,
        tracker: Tracker,
        currentTurn: Int,
        phylumOverride: String? = null,
    ) {
        val entry = when (tracker.trackType) {
            TrackType.PHYLUM -> {
                phylumOverride
                    ?: MonsterDatabase.getByName(monsterName)?.phylum?.takeIf { it.isNotBlank() }
                    ?: return
            }
            TrackType.MONSTER -> monsterName
        }
        track(preferences, entry, tracker, currentTurn)
    }

    /**
     * Desktop [TrackManager.track] — clears prior entries for [tracker], then appends.
     */
    fun track(
        preferences: Preferences,
        tracked: String,
        tracker: Tracker,
        currentTurn: Int = 0,
    ) {
        removeTracker(preferences, tracker)
        when (tracker) {
            Tracker.LEFT_ZOOT_KICK -> removeTracker(preferences, Tracker.RIGHT_ZOOT_KICK)
            Tracker.RIGHT_ZOOT_KICK -> removeTracker(preferences, Tracker.LEFT_ZOOT_KICK)
            else -> Unit
        }
        val prefKey = prefKeyFor(tracker)
        val entries = loadEntries(preferences, prefKey).toMutableList()
        entries.add(TrackedEntry(tracked, tracker, currentTurn))
        saveEntries(preferences, prefKey, entries)
        writeLegacyPrefs(preferences, tracked, tracker)
    }

    fun countCopies(
        preferences: Preferences,
        monsterName: String,
        currentTurn: Int = 0,
    ): Int {
        val monsterCopies = loadEntries(preferences, PREF_TRACKED_MONSTERS)
            .filter {
                it.tracked.equals(monsterName, ignoreCase = true) &&
                    !it.isExpired(currentTurn)
            }
            .sumOf { it.tracker.copies }
        val phylum = MonsterDatabase.getByName(monsterName)?.phylum?.takeIf { it.isNotBlank() }
            ?: return monsterCopies
        val phylaCopies = loadEntries(preferences, PREF_TRACKED_PHYLA)
            .filter {
                it.tracked.equals(phylum, ignoreCase = true) &&
                    !it.isExpired(currentTurn)
            }
            .sumOf { it.tracker.copies }
        return monsterCopies + phylaCopies
    }

    fun isQueueIgnored(
        preferences: Preferences,
        monsterName: String,
        currentTurn: Int = 0,
    ): Boolean =
        loadEntries(preferences, PREF_TRACKED_MONSTERS).any {
            it.tracked.equals(monsterName, ignoreCase = true) &&
                it.tracker.ignoreQueue &&
                !it.isExpired(currentTurn)
        }

    /** Desktop [TrackManager.trackedBy] — tracker display names for a monster (+ phylum). */
    fun trackedBy(
        preferences: Preferences,
        monsterName: String,
        currentTurn: Int = 0,
    ): List<String> {
        val monsterHits = loadEntries(preferences, PREF_TRACKED_MONSTERS)
            .filter {
                it.tracked.equals(monsterName, ignoreCase = true) &&
                    !it.isExpired(currentTurn)
            }
            .map { it.tracker.displayName }
        val phylum = MonsterDatabase.getByName(monsterName)?.phylum?.takeIf { it.isNotBlank() }
            ?: return monsterHits
        val phylumHits = loadEntries(preferences, PREF_TRACKED_PHYLA)
            .filter {
                it.tracked.equals(phylum, ignoreCase = true) &&
                    !it.isExpired(currentTurn)
            }
            .map { it.tracker.displayName }
        return monsterHits + phylumHits
    }

    fun resetRollover(preferences: Preferences): Int {
        val monstersBefore = countKnownEntries(preferences, PREF_TRACKED_MONSTERS)
        val phylaBefore = countKnownEntries(preferences, PREF_TRACKED_PHYLA)
        resetRolloverPref(preferences, PREF_TRACKED_MONSTERS)
        resetRolloverPref(preferences, PREF_TRACKED_PHYLA)
        val monstersAfter = countKnownEntries(preferences, PREF_TRACKED_MONSTERS)
        val phylaAfter = countKnownEntries(preferences, PREF_TRACKED_PHYLA)
        return (monstersBefore - monstersAfter) + (phylaBefore - phylaAfter)
    }

    fun resetAvatar(preferences: Preferences): Int =
        resetIf(preferences) { it.tracker.resetType.isAvatarReset }

    fun resetAscension(preferences: Preferences): Int =
        resetIf(preferences) {
            it.tracker.resetType == TrackResetType.ASCENSION_RESET ||
                it.tracker.resetType.isAvatarReset
        }

    fun resetEffect(preferences: Preferences): Int =
        resetIf(preferences) { it.tracker.resetType == TrackResetType.EFFECT_RESET }

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

    private fun prefKeyFor(tracker: Tracker): String = when (tracker.trackType) {
        TrackType.PHYLUM -> PREF_TRACKED_PHYLA
        TrackType.MONSTER -> PREF_TRACKED_MONSTERS
    }

    private fun removeTracker(preferences: Preferences, tracker: Tracker) {
        for (key in listOf(PREF_TRACKED_MONSTERS, PREF_TRACKED_PHYLA)) {
            val raw = preferences.getString(key, "")
            if (raw.isBlank()) continue
            val tokens = raw.split(':')
            val kept = mutableListOf<String>()
            var i = 0
            while (i + 2 < tokens.size) {
                val name = tokens[i]
                val trackerName = tokens[i + 1]
                val turn = tokens[i + 2]
                if (turn.toIntOrNull() == null) break
                if (Tracker.find(trackerName) != tracker) {
                    kept += name
                    kept += trackerName
                    kept += turn
                }
                i += 3
            }
            preferences.setString(key, kept.joinToString(":"))
        }
    }

    private fun resetIf(
        preferences: Preferences,
        predicate: (TrackedEntry) -> Boolean,
    ): Int {
        var cleared = 0
        for (key in listOf(PREF_TRACKED_MONSTERS, PREF_TRACKED_PHYLA)) {
            val raw = preferences.getString(key, "")
            if (raw.isBlank()) continue
            val tokens = raw.split(':').toMutableList()
            val kept = mutableListOf<String>()
            var index = 0
            while (index + 2 < tokens.size) {
                val tracked = tokens[index]
                val trackerName = tokens[index + 1]
                val turnToken = tokens[index + 2]
                val turnTracked = turnToken.toIntOrNull()
                if (turnTracked == null) break
                val tracker = Tracker.find(trackerName)
                val entry = tracker?.let { TrackedEntry(tracked, it, turnTracked) }
                val remove = entry != null && predicate(entry)
                if (remove) {
                    cleared++
                } else {
                    kept += tracked
                    kept += trackerName
                    kept += turnToken
                }
                index += 3
            }
            preferences.setString(key, kept.joinToString(":"))
        }
        return cleared
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

    private fun writeLegacyPrefs(preferences: Preferences, tracked: String, tracker: Tracker) {
        when (tracker) {
            Tracker.OLFACTION -> preferences.setString("olfactedMonster", tracked)
            Tracker.LONG_CON -> preferences.setString("longConMonster", tracked)
            Tracker.MOTIF -> preferences.setString("motifMonster", tracked)
            Tracker.MAKE_FRIENDS -> preferences.setString("makeFriendsMonster", tracked)
            Tracker.CURSE_OF_STENCH -> preferences.setString("stenchCursedMonster", tracked)
            Tracker.RED_SNAPPER -> preferences.setString("redSnapperPhylum", tracked)
            else -> Unit
        }
    }
}
