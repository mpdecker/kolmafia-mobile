package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Detects mushroom plot ownership and 4×4 grid from knoll_mushrooms.php HTML.
 * Mirrors desktop [MushroomManager.parsePlot].
 */
object MushroomPlotSync {

    const val MUSHROOM_PLOT_SQUARES_PREF = "_mushroomPlotSquares"

    private val PLOT_PATTERN = Regex(
        """<b>Your Mushroom Plot:</b><p><table>(.*?)</table>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val SQUARE_PATTERN = Regex("""<td>(.*?)</td>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val IMAGE_PATTERN = Regex("""([^/"']+\.gif)""", RegexOption.IGNORE_CASE)

    private data class MushroomGif(val id: Int, val filename: String, val shorthand: String)

    private val MUSHROOMS = listOf(
        MushroomGif(0, "dirt1.gif", "__"),
        MushroomGif(1, "mushsprout.gif", ".."),
        MushroomGif(303, "mushroom.gif", "kb"),
        MushroomGif(723, "bmushroom.gif", "kn"),
        MushroomGif(724, "spooshroom.gif", "sp"),
        MushroomGif(749, "flatshroom.gif", "wa"),
        MushroomGif(751, "plaidroom.gif", "co"),
        MushroomGif(753, "tallshroom.gif", "po"),
        MushroomGif(755, "fireshroom.gif", "fl"),
        MushroomGif(756, "iceshroom.gif", "fr"),
        MushroomGif(757, "stinkshroo.gif", "st"),
        MushroomGif(1266, "blackshroo.gif", "gl"),
    )

    fun hasPlot(html: String): Boolean =
        html.contains("<b>Your Mushroom Plot:</b>", ignoreCase = true)

    fun parseSquares(html: String): Array<String> {
        val grid = Array(16) { "__" }
        val plotMatch = PLOT_PATTERN.find(html) ?: return grid
        val squareMatcher = SQUARE_PATTERN.findAll(plotMatch.groupValues[1]).iterator()
        for (index in 0 until 16) {
            if (!squareMatcher.hasNext()) break
            grid[index] = shorthandForSquare(squareMatcher.next().groupValues[1])
        }
        return grid
    }

    fun plotGrid(prefs: Preferences?): Array<Array<String>> {
        val flat = prefs?.getString(MUSHROOM_PLOT_SQUARES_PREF, "") ?: ""
        if (flat.length != 32) {
            return Array(4) { Array(4) { "__" } }
        }
        return Array(4) { row ->
            Array(4) { col ->
                flat.substring((row * 4 + col) * 2, (row * 4 + col) * 2 + 2)
            }
        }
    }

    fun squareAt(prefs: Preferences?, row: Int, col: Int): String {
        if (row !in 0..3 || col !in 0..3) return "__"
        return plotGrid(prefs)[row][col]
    }

    fun apply(preferences: Preferences?, character: KoLCharacter, html: String, url: String?) {
        if (url == null || !url.contains("knoll_mushrooms.php", ignoreCase = true)) return
        if (!hasPlot(html)) return
        preferences?.setInt("lastMushroomPlot", character.state.value.ascensionNumber)
        val squares = parseSquares(html)
        preferences?.setString(MUSHROOM_PLOT_SQUARES_PREF, squares.joinToString(""))
    }

    private fun shorthandForSquare(cellHtml: String): String {
        val gif = IMAGE_PATTERN.find(cellHtml)?.groupValues?.getOrNull(1) ?: return "__"
        return MUSHROOMS.firstOrNull { it.filename.equals(gif, ignoreCase = true) }?.shorthand ?: "__"
    }
}
