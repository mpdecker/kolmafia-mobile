package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [MushroomManager] plant/pick/harvest, breeding forecast, spore catalog
 * (Phases 3381–3395).
 */
object MushroomManager {

    const val EMPTY = 0
    const val SPROUT = 1
    const val SPOOKY = 724
    const val KNOB = 303
    const val KNOLL = 723
    const val WARM = 749
    const val COOL = 751
    const val POINTY = 753
    const val FLAMING = 755
    const val FROZEN = 756
    const val STINKY = 757
    const val GLOOMY = 1266

    data class Mushroom(
        val id: Int,
        val filename: String,
        val spore: String,
        val mushroom: String,
        val index: Int,
        val name: String,
    )

    val MUSHROOMS = arrayOf(
        Mushroom(EMPTY, "dirt1.gif", "__", "__", 0, "empty"),
        Mushroom(SPROUT, "mushsprout.gif", "..", "..", 0, "unknown"),
        Mushroom(KNOB, "mushroom.gif", "kb", "KB", 1, "knob"),
        Mushroom(KNOLL, "bmushroom.gif", "kn", "KN", 2, "knoll"),
        Mushroom(SPOOKY, "spooshroom.gif", "sp", "SP", 3, "spooky"),
        Mushroom(WARM, "flatshroom.gif", "wa", "WA", 4, "warm"),
        Mushroom(COOL, "plaidroom.gif", "co", "CO", 5, "cool"),
        Mushroom(POINTY, "tallshroom.gif", "po", "PO", 6, "pointy"),
        Mushroom(FLAMING, "fireshroom.gif", "fl", "FL", 7, "flaming"),
        Mushroom(FROZEN, "iceshroom.gif", "fr", "FR", 8, "frozen"),
        Mushroom(STINKY, "stinkshroo.gif", "st", "ST", 9, "stinky"),
        Mushroom(GLOOMY, "blackshroo.gif", "gl", "GL", 10, "gloomy"),
    )

    private val SPORE_DATA = arrayOf(
        intArrayOf(SPOOKY, 30, 1),
        intArrayOf(KNOB, 40, 2),
        intArrayOf(KNOLL, 50, 3),
    )

    private val BREEDING = arrayOf(
        intArrayOf(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
        intArrayOf(EMPTY, KNOB, COOL, WARM, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
        intArrayOf(EMPTY, COOL, KNOLL, POINTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
        intArrayOf(EMPTY, WARM, POINTY, SPOOKY, EMPTY, EMPTY, EMPTY, EMPTY, GLOOMY, EMPTY, EMPTY),
        intArrayOf(EMPTY, EMPTY, EMPTY, EMPTY, WARM, STINKY, FLAMING, EMPTY, EMPTY, EMPTY, EMPTY),
        intArrayOf(EMPTY, EMPTY, EMPTY, EMPTY, STINKY, COOL, FROZEN, EMPTY, EMPTY, EMPTY, EMPTY),
        intArrayOf(EMPTY, EMPTY, EMPTY, EMPTY, FLAMING, FROZEN, POINTY, EMPTY, EMPTY, EMPTY, EMPTY),
        intArrayOf(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, FLAMING, EMPTY, EMPTY, EMPTY),
        intArrayOf(EMPTY, EMPTY, EMPTY, GLOOMY, EMPTY, EMPTY, EMPTY, EMPTY, FROZEN, EMPTY, EMPTY),
        intArrayOf(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, STINKY, EMPTY),
        intArrayOf(EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY),
    )

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

    fun plotGrid(preferences: Preferences?): Array<Array<String>> =
        MushroomPlotSync.plotGrid(preferences)

    fun getMushroomManager(isDataOnly: Boolean, preferences: Preferences?): String {
        val plot = plotGrid(preferences)
        return formatPlot(isDataOnly, plot)
    }

    fun getForecastedPlot(isDataOnly: Boolean, preferences: Preferences?): String {
        val plot = plotGrid(preferences)
        return getForecastedPlot(isDataOnly, plot)
    }

    fun getForecastedPlot(isDataOnly: Boolean, plot: Array<Array<String>>): String {
        val changeList = Array(4) { BooleanArray(4) }
        val forecast = Array(4) { row -> plot[row].copyOf() }
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                if (plot[row][col] == "__") {
                    forecast[row][col] = getForecastSquare(row, col, plot)
                    changeList[row][col] = forecast[row][col] != "__"
                } else if (plot[row][col] == plot[row][col].lowercase()) {
                    forecast[row][col] = plot[row][col].uppercase()
                }
            }
        }
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                if (!changeList[row][col]) continue
                if (row > 0 && forecast[row - 1][col] == forecast[row - 1][col].uppercase()) {
                    forecast[row - 1][col] = "__"
                }
                if (row < 3 && forecast[row + 1][col] == forecast[row + 1][col].uppercase()) {
                    forecast[row + 1][col] = "__"
                }
                if (col > 0 && forecast[row][col - 1] == forecast[row][col - 1].uppercase()) {
                    forecast[row][col - 1] = "__"
                }
                if (col < 3 && forecast[row][col + 1] == forecast[row][col + 1].uppercase()) {
                    forecast[row][col + 1] = "__"
                }
            }
        }
        return formatPlot(isDataOnly, forecast)
    }

    fun getSporeDataByType(spore: Int): IntArray? =
        SPORE_DATA.firstOrNull { it[0] == spore }

    fun getSporeDataByIndex(index: Int): IntArray? =
        SPORE_DATA.firstOrNull { it[2] == index }

    fun getSporeName(data: IntArray): String = ItemDatabase.getById(data[0])?.name ?: "spore"

    fun getSporePrice(data: IntArray): Int = data[1]

    fun getSporeIndex(data: IntArray): Int = data[2]

    fun resolveSporeId(name: String): Int? {
        val normalized = name.trim()
        val withMushroom = if (normalized.contains("mushroom", ignoreCase = true)) {
            normalized
        } else {
            "$normalized mushroom"
        }
        return ItemDatabase.getByName(withMushroom)?.id
            ?: MUSHROOMS.firstOrNull { it.name.equals(normalized, ignoreCase = true) }?.id
    }

    fun plantMushroom(
        square: Int,
        sporeShorthand: String,
        preferences: Preferences?,
    ): Boolean {
        if (square !in 1..16 || sporeShorthand.length != 2) return false
        val prefs = preferences ?: return false
        val idx = square - 1
        val flat = prefs.getString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, "")
        val chars = if (flat.length == 32) flat.toCharArray() else CharArray(32) { '_' }
        if (flat.length != 32) {
            for (i in 0 until 16) {
                chars[i * 2] = '_'
                chars[i * 2 + 1] = '_'
            }
        }
        chars[idx * 2] = sporeShorthand[0]
        chars[idx * 2 + 1] = sporeShorthand[1]
        prefs.setString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, chars.concatToString())
        prefs.setBoolean("_mushroomPlanted", true)
        return true
    }

    fun pickMushroom(
        square: Int,
        preferences: Preferences?,
        pickSpores: Boolean = false,
    ): Boolean {
        if (square !in 1..16) return false
        val prefs = preferences ?: return false
        val idx = square - 1
        val flat = prefs.getString(MushroomPlotSync.MUSHROOM_PLOT_SQUARES_PREF, "")
        if (flat.length != 32) return false
        val current = flat.substring(idx * 2, idx * 2 + 2)
        val shouldPick = current != "__" && (current != current.lowercase() || pickSpores)
        if (!shouldPick) return true
        val chars = flat.toCharArray()
        chars[idx * 2] = '_'
        chars[idx * 2 + 1] = '_'
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

    fun shorthandForSporeId(sporeId: Int): String? =
        MUSHROOMS.firstOrNull { it.id == sporeId }?.spore?.takeIf { it != "__" }

    private fun getForecastSquare(row: Int, col: Int, plot: Array<Array<String>>): String {
        val touched = arrayOf(
            if (row == 0) "__" else plot[row - 1][col],
            if (row == 3) "__" else plot[row + 1][col],
            if (col == 0) "__" else plot[row][col - 1],
            if (col == 3) "__" else plot[row][col + 1],
        )
        val touchIndex = IntArray(4)
        var touchCount = 0
        for (value in touched) {
            if (value != "__" && value != "..") {
                touchIndex[touchCount] = MUSHROOMS.firstOrNull { it.mushroom == value }?.index ?: 0
                touchCount++
            }
        }
        if (touchCount == 2) {
            val breed = BREEDING.getOrNull(touchIndex[0])?.getOrNull(touchIndex[1]) ?: EMPTY
            if (breed != EMPTY) return getShorthand(breed, false)
        }
        return plot[row][col]
    }

    private fun getShorthand(mushroomType: Int, isAdult: Boolean): String =
        MUSHROOMS.firstOrNull { it.id == mushroomType }
            ?.let { if (isAdult) it.mushroom else it.spore }
            ?: "__"

    private fun formatPlot(isDataOnly: Boolean, plot: Array<Array<String>>): String =
        buildString {
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    if (!isDataOnly) append("  ")
                    append(plot[row][col])
                    if (isDataOnly) append(';')
                }
                if (!isDataOnly) append('\n')
            }
        }
}
