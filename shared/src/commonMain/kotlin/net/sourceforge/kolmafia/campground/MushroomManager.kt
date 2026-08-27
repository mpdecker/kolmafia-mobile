package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop MushroomManager plant/pick subset (Phases 2346–2360).
 */
object MushroomManager {
    fun ownsPlot(preferences: Preferences?, ascensionNumber: Int): Boolean {
        val last = preferences?.getInt("lastMushroomPlot", -1) ?: -1
        return last == ascensionNumber
    }

    fun parsePlot(
        html: String,
        preferences: Preferences?,
        character: KoLCharacter?,
        url: String? = "knoll_mushrooms.php",
    ) {
        if (character == null) return
        MushroomPlotSync.apply(preferences, character, html, url)
    }

    /**
     * Record a plant action after successful HTTP.
     * [square] is 0–15; [sporeShorthand] is two-char code (e.g. "kb").
     */
    fun plantMushroom(
        square: Int,
        sporeShorthand: String,
        preferences: Preferences?,
    ): Boolean {
        if (square !in 0..15 || sporeShorthand.length != 2) return false
        val prefs = preferences ?: return false
        val flat = prefs.getString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, "")
        val chars = if (flat.length == 32) flat.toCharArray() else CharArray(32) { '_' }.also {
            for (i in it.indices step 2) {
                it[i] = '_'
                it[i + 1] = '_'
            }
        }
        // Normalize empty grid to "__" pairs
        if (flat.length != 32) {
            for (i in 0 until 16) {
                chars[i * 2] = '_'
                chars[i * 2 + 1] = '_'
            }
        }
        chars[square * 2] = sporeShorthand[0]
        chars[square * 2 + 1] = sporeShorthand[1]
        prefs.setString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, chars.concatToString())
        prefs.setBoolean("_mushroomPlanted", true)
        return true
    }

    fun pickMushroom(
        square: Int,
        preferences: Preferences?,
        pickSpores: Boolean = false,
    ): Boolean {
        if (square !in 0..15) return false
        val prefs = preferences ?: return false
        val flat = prefs.getString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, "")
        if (flat.length != 32) return false
        val chars = flat.toCharArray()
        chars[square * 2] = '_'
        chars[square * 2 + 1] = '_'
        prefs.setString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, chars.concatToString())
        prefs.setBoolean("_mushroomPicked", true)
        if (pickSpores) prefs.setBoolean("_mushroomSporesPicked", true)
        return true
    }

    fun squareShorthand(preferences: Preferences?, square: Int): String {
        if (square !in 0..15) return "__"
        val flat = preferences?.getString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, "") ?: ""
        if (flat.length != 32) return "__"
        return flat.substring(square * 2, square * 2 + 2)
    }
}
