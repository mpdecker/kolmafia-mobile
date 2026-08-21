package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [BeachManager] preference synchronization for choice 1388. */
object BeachCombManager {
    data class Coordinates(val beach: Int, val row: Int, val column: Int) {
        override fun toString(): String = "$row,${beach * 10 - column}"
    }

    private val minutesPattern =
        Regex("""You walk for ([\d,]+) minutes? and find a nice stretch of beach""")
    private val freeWalkPattern =
        Regex("""\(You have (\d+) free walks? down the beach left today\.\)""")
    private val beachHeadPattern = Regex("""Visit Beach Head #(\d+)""")
    private val effectPattern = Regex("""You acquire an effect:\s*<b>([^<]+)</b>""")
    private val mapPattern = Regex(
        """name=["']coords["']\s+value=["'](\d+),(\d+)["'].*?title=["']([^"']*).*?otherimages/beachcomb/([^."'/?]+)\.(?:gif|png)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val encodedCoordsPattern = Regex("""coords=(\d+)(?:%2C|,)(\d+)""", RegexOption.IGNORE_CASE)

    fun parseIdSet(raw: String): Set<Int> =
        raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSortedSet()

    fun setIdSet(preferences: Preferences, property: String, values: Set<Int>) {
        preferences.setString(property, values.sorted().joinToString(","))
    }

    fun stringToLayout(input: String): Map<Int, String> =
        input.split(',').mapNotNull { rowData ->
            val separator = rowData.indexOf(':')
            if (separator < 0) return@mapNotNull null
            val row = rowData.substring(0, separator).toIntOrNull() ?: return@mapNotNull null
            row to rowData.substring(separator + 1)
        }.toMap().toSortedMap()

    fun layoutToString(layout: Map<Int, String>): String =
        layout.toSortedMap().entries.joinToString(",") { (row, squares) -> "$row:$squares" }

    fun coordinatesFromUrl(url: String): Coordinates? {
        val match = encodedCoordsPattern.find(url) ?: return null
        val row = match.groupValues[1].toIntOrNull() ?: return null
        val encoded = match.groupValues[2].toIntOrNull() ?: return null
        val remainder = encoded % 10
        val beach = encoded / 10 + if (remainder == 0) 0 else 1
        val column = if (remainder == 0) 0 else 10 - remainder
        return Coordinates(beach, row, column)
    }

    fun parseCombUsage(html: String, preferences: Preferences): Boolean {
        if (!html.contains("to the start of the beach to find", ignoreCase = true)) return false
        val walksAvailable = freeWalkPattern.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        preferences.setInt("_freeBeachWalksUsed", (11 - walksAvailable).coerceIn(0, 11))

        val unlocked = parseIdSet(preferences.getString("beachHeadsUnlocked", "")).toMutableSet()
        val available = beachHeadPattern.findAll(html)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .toSortedSet()
        unlocked += available
        setIdSet(preferences, "beachHeadsUnlocked", unlocked)
        setIdSet(preferences, "_beachHeadsUsed", unlocked - available)
        return true
    }

    fun parseBeachHeadCombing(html: String, preferences: Preferences): Boolean {
        if (!html.contains("some kind of magical blessing", ignoreCase = true)) return false
        val effect = effectPattern.find(html)?.groupValues?.get(1) ?: return false
        val head = BeachHeadAvailability.BEACH_HEADS.firstOrNull {
            it.effect.equals(effect, ignoreCase = true)
        } ?: return false
        val unlocked = parseIdSet(preferences.getString("beachHeadsUnlocked", "")).toMutableSet()
        val used = parseIdSet(preferences.getString("_beachHeadsUsed", "")).toMutableSet()
        unlocked += head.id
        used += head.id
        setIdSet(preferences, "beachHeadsUnlocked", unlocked)
        setIdSet(preferences, "_beachHeadsUsed", used)
        return true
    }

    fun parseBeachMap(html: String, preferences: Preferences): Boolean {
        val minutes = minutesPattern.find(html)?.groupValues?.get(1)
            ?.replace(",", "")?.toIntOrNull()
        if (minutes == null) {
            preferences.setBoolean("_beachCombing", false)
            return false
        }
        val rows = sortedMapOf<Int, StringBuilder>()
        var twinkles = false
        for (match in mapPattern.findAll(html)) {
            val row = match.groupValues[1].toIntOrNull() ?: continue
            val title = match.groupValues[3]
            val image = match.groupValues[4]
            val square = when {
                title.equals("rough sand with a twinkle", ignoreCase = true) -> 't'
                title.equals("rough sand", ignoreCase = true) -> 'r'
                title.equals("combed sand", ignoreCase = true) -> 'c'
                title.equals("a beach head", ignoreCase = true) -> 'H'
                title.equals("a sand castle", ignoreCase = true) -> 'C'
                image.equals("whale", ignoreCase = true) -> 'W'
                else -> '?'
            }
            if (square == 't') twinkles = true
            rows.getOrPut(row) { StringBuilder() }.append(square)
        }
        if (twinkles) preferences.setBoolean("hasTwinkleVision", true)
        if (rows.isNotEmpty()) preferences.setInt("_beachTides", rows.firstKey() - 1)
        preferences.setBoolean("_beachCombing", true)
        preferences.setInt("_beachMinutes", minutes)
        preferences.setString("_beachLayout", layoutToString(rows.mapValues { it.value.toString() }))
        return true
    }

    fun markCombedSquare(url: String, html: String, preferences: Preferences): Boolean {
        if (!html.contains("You comb", ignoreCase = true)) return false
        val coords = coordinatesFromUrl(url) ?: return false
        val layout = stringToLayout(preferences.getString("_beachLayout", "")).toMutableMap()
        val squares = layout[coords.row] ?: return false
        if (coords.column !in squares.indices) return false
        layout[coords.row] = squares.replaceRange(coords.column, coords.column + 1, "c")
        preferences.setString("_beachLayout", layoutToString(layout))
        return true
    }
}
