package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks Dreadsylvania kiss counts per sub-zone. Mirrors desktop [FightRequest.dreadKisses].
 */
class DreadKissesTracker(private val preferences: Preferences) {

    private var woodsKisses = 0
    private var villageKisses = 0
    private var castleKisses = 0
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        woodsKisses = preferences.getInt(PREF_WOODS, 0)
        villageKisses = preferences.getInt(PREF_VILLAGE, 0)
        castleKisses = preferences.getInt(PREF_CASTLE, 0)
    }

    fun kissesForLocation(locationName: String): Long {
        ensureLoaded()
        val count = when {
            locationName.endsWith("Woods") -> woodsKisses
            locationName.endsWith("Village") -> villageKisses
            locationName.endsWith("Castle") -> castleKisses
            else -> return 0L
        }
        return maxOf(count, 1).toLong()
    }

    fun updateFromFight(locationName: String, fightHtml: String) {
        ensureLoaded()
        val zone = AdventureDatabase.getByName(locationName) ?: return
        if (!zone.zoneName.equals("Dreadsylvania", ignoreCase = true)) return

        val title = KISS_TITLE.find(fightHtml)?.groupValues?.getOrNull(1) ?: return
        if (!title.contains("kiss", ignoreCase = true)) return

        val matcher = KISS_PATTERN.find(title) ?: return
        var kisses = matcher.groupValues[1].toIntOrNull() ?: return
        if (kisses > 1) {
            kisses = 0
        } else {
            matcher.groupValues.getOrNull(2)?.toIntOrNull()?.let { bonus ->
                kisses += bonus
            }
        }

        when {
            locationName.endsWith("Woods") -> woodsKisses = kisses
            locationName.endsWith("Village") -> villageKisses = kisses
            locationName.endsWith("Castle") -> castleKisses = kisses
            else -> return
        }
        save()
    }

    internal fun setKissesForTest(locationName: String, kisses: Int) {
        ensureLoaded()
        when {
            locationName.endsWith("Woods") -> woodsKisses = kisses
            locationName.endsWith("Village") -> villageKisses = kisses
            locationName.endsWith("Castle") -> castleKisses = kisses
            else -> return
        }
        save()
    }

    private fun ensureLoaded() {
        if (!loaded) load()
    }

    private fun save() {
        preferences.setInt(PREF_WOODS, woodsKisses)
        preferences.setInt(PREF_VILLAGE, villageKisses)
        preferences.setInt(PREF_CASTLE, castleKisses)
    }

    companion object {
        private const val PREF_WOODS = "_dreadWoodsKisses"
        private const val PREF_VILLAGE = "_dreadVillageKisses"
        private const val PREF_CASTLE = "_dreadCastleKisses"

        private val KISS_TITLE = Regex("""title="([^"]*kiss[^"]*)"""", RegexOption.IGNORE_CASE)
        private val KISS_PATTERN = Regex(
            """(\d+) kiss(?:es)? for winning(?: \+(\d+) for difficulty)?""",
            RegexOption.IGNORE_CASE,
        )
    }
}
