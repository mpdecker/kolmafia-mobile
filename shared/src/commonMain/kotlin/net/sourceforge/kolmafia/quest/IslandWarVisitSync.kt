package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [IslandManager.parseBigIsland] / [IslandManager.parseBattlefield] visit hooks. */
object IslandWarVisitSync {

    private val MAP_PATTERN = Regex("""bfleft(\d*).*bfright(\d*)""", RegexOption.DOT_MATCHES_ALL)

    // Crowther spaded threshold table — desktop IslandManager.IMAGES
    private val IMAGES = intArrayOf(
        0, // Image 0
        3, // Image 1
        9, // Image 2
        17, // Image 3
        28, // Image 4
        40, // Image 5
        52, // Image 6
        64, // Image 7
        80, // Image 8
        96, // Image 9
        114, // Image 10
        132, // Image 11
        152, // Image 12
        172, // Image 13
        192, // Image 14
        224, // Image 15
        258, // Image 16
        294, // Image 17
        332, // Image 18
        372, // Image 19
        414, // Image 20
        458, // Image 21
        506, // Image 22
        556, // Image 23
        606, // Image 24
        658, // Image 25
        711, // Image 26
        766, // Image 27
        822, // Image 28
        880, // Image 29
        939, // Image 30
        999, // Image 31
        1000, // Image 32
    )

    fun applyFromBigIslandVisit(html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        var changed = false
        if (prefs.getString("warProgress", "unstarted") != "started") {
            prefs.setString("warProgress", "started")
            changed = true
        }
        if (parseBattlefield(html, prefs)) {
            changed = true
        }
        return changed
    }

    internal fun parseBattlefield(html: String, preferences: Preferences): Boolean {
        val match = MAP_PATTERN.find(html) ?: return false
        val fratboyImage = match.groupValues[1].toIntOrNull() ?: 0
        val hippyImage = match.groupValues[2].toIntOrNull() ?: 0

        var changed = false
        imageRange(fratboyImage)?.let { (min, max) ->
            changed = clampCounter(preferences, "fratboysDefeated", min, max) || changed
        }
        imageRange(hippyImage)?.let { (min, max) ->
            changed = clampCounter(preferences, "hippiesDefeated", min, max) || changed
        }
        return changed
    }

    internal fun imageRange(image: Int): Pair<Int, Int>? {
        if (image !in 0..32) return null
        val min = IMAGES[image]
        val max = if (min == 1000) 1000 else IMAGES[image + 1] - 1
        return min to max
    }

    private fun clampCounter(
        preferences: Preferences,
        prefKey: String,
        min: Int,
        max: Int,
    ): Boolean {
        val current = preferences.getInt(prefKey, 0)
        return when {
            current < min -> {
                preferences.setInt(prefKey, min)
                true
            }
            current > max -> {
                preferences.setInt(prefKey, max)
                true
            }
            else -> false
        }
    }
}
