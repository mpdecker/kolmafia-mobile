package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.CultShortsDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks Yeg (demon 13) name syllables from cargo-cult scrap pockets.
 * Mirrors desktop [CargoCultistShortsRequest.checkScrapPocket] and
 * [SummoningChamberRequest.updateYegName].
 */
class YegDemonNameSync(private val preferences: Preferences) {

    fun demonName(): String =
        preferences.getString(Preferences.DEMON_NAME_13, "")

    fun knownScrapPockets(): Map<Int, String> {
        val value = preferences.getString(Preferences.CARGO_POCKET_SCRAPS, "")
        if (value.isEmpty()) return emptyMap()

        val map = linkedMapOf<Int, String>()
        for (item in value.split('|')) {
            if (item.isBlank()) continue
            val parts = item.split(Regex(": *"), limit = 3)
            val key = parts[0].toIntOrNull() ?: continue
            val syllable = when {
                parts.size >= 3 -> parts[2]
                parts.size == 2 -> parts[1]
                else -> continue
            }
            map[key] = syllable.trim()
        }
        return map
    }

    fun saveScrapPockets(map: Map<Int, String>) {
        val ordered = CultShortsDatabase.scrapPocketsInOrder
        val pockets = if (ordered.isNotEmpty()) ordered else map.keys.sorted()
        val value = buildString {
            for (pocket in pockets) {
                val syllable = map[pocket] ?: continue
                if (isNotEmpty()) append('|')
                append(pocket)
                append(':')
                append(syllable)
            }
        }
        preferences.setString(Preferences.CARGO_POCKET_SCRAPS, value)
    }

    fun updateYegName(syllables: Map<Int, String>) {
        if (syllables.size != SCRAP_COUNT) return

        val ordered = CultShortsDatabase.scrapPocketsInOrder
        if (ordered.size != SCRAP_COUNT) return

        val name = buildString {
            for (pocket in ordered) {
                val syllable = syllables[pocket] ?: return
                append(syllable)
            }
        }
        preferences.setString(
            Preferences.DEMON_NAME_13,
            name.replace('_', ' '),
        )
    }

    fun checkScrapPocket(pocket: Int, responseText: String) {
        val match = SCRAP_PATTERN.find(responseText) ?: return
        if (match.groupValues[1].isNotEmpty()) {
            return
        }

        var syllable = match.groupValues[2]
        val colon = syllable.indexOf(':')
        if (colon == -1) return
        syllable = syllable.substring(colon + 1).trim()

        val map = knownScrapPockets().toMutableMap()
        map[pocket] = syllable
        saveScrapPockets(map)
        updateYegName(map)
    }

    companion object {
        private const val SCRAP_COUNT = 7

        private val SCRAP_PATTERN = Regex(
            """This pocket contains a (waterlogged )?scrap of paper that reads: <[Bb]>([^<]+)</[Bb]>""",
        )
    }
}
