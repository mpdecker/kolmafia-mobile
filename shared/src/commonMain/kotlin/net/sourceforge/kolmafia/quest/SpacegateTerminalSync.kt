package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.parseSpacegateTerminal] HTML → `_spacegate*` prefs.
 */
object SpacegateTerminalSync {

    data class Hazard(val terminal: String, val adventure: String, val gear: String)

    val HAZARDS = listOf(
        Hazard("toxic atmosphere", "Toxic environment", "filter helmet"),
        Hazard("high gravity", "Extremely high gravity", "exo-servo leg braces"),
        Hazard("irradiated", "High radiation levels", "rad cloak"),
        Hazard("magnetic storms", "High levels of magnetic interference", "gate transceiver"),
        Hazard("high winds", "Intense winds", "high-friction boots"),
    )

    private val PLANET = Regex("""<td>Current planet: Planet Name: ([^<]+)<br>""", RegexOption.IGNORE_CASE)
    private val COORDINATES = Regex("""<br>Coordinates: ([^<]+)<br>""", RegexOption.IGNORE_CASE)
    private val HAZARDS_PATTERN =
        Regex("""<br><p>Environmental Hazards:<[Bb]r>(.*)<br>Plant Life:""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val PLANT =
        Regex("""<br>Plant Life: (?:<font color=\w+>)?([^<]+)(?:</font>)? ?(?:<font color=\w+>(\(hostile\))</font>)?<br>""", RegexOption.IGNORE_CASE)
    private val ANIMAL =
        Regex("""<br>Animal Life: (?:<font color=\w+>)?([^<]+)(?:</font>)? ?(?:<font color=\w+>(\(hostile\))</font>)?<br>""", RegexOption.IGNORE_CASE)
    private val INTELLIGENT =
        Regex("""<br>Intelligent Life: (?:<font color=\w+>)?([^<]+) ?(?:</font>)?(?:<font color=\w+>(\(hostile\))</font>)?<br>""", RegexOption.IGNORE_CASE)
    private val SPANT = Regex("""<b>Spant</b>""", RegexOption.IGNORE_CASE)
    private val MURDERBOT = Regex("""<b>Murderbot</b>""", RegexOption.IGNORE_CASE)
    private val RUINS = Regex("""<br>ALERT: ANCIENT RUINS DETECTED<br>""", RegexOption.IGNORE_CASE)
    private val TURNS =
        Regex("""<p>Spacegate Energy remaining: <b><font size=\+2>(\d+) </font>""", RegexOption.IGNORE_CASE)

    fun applyFromTerminal(
        url: String?,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        val location = url.orEmpty()
        val isTerminal =
            location.contains("action=sg_Terminal", ignoreCase = true) ||
                (location.contains("forceoption=0") && html.contains("Spacegate Terminal")) ||
                html.contains("Spacegate Terminal")
        if (!isTerminal && !html.contains("Spacegate Terminal")) return false
        if (!html.contains("Spacegate Terminal")) return false

        PLANET.find(html)?.groupValues?.getOrNull(1)?.trim()?.let {
            preferences.setString("_spacegatePlanetName", it)
        } ?: preferences.setString("_spacegatePlanetName", "")

        val coordinates = COORDINATES.find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        preferences.setString("_spacegateCoordinates", coordinates)
        val index = if (coordinates.isNotEmpty()) (coordinates[0].code - 'A'.code) else 0
        preferences.setInt("_spacegatePlanetIndex", index.coerceIn(0, 25))

        var hazards = HAZARDS_PATTERN.find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        hazards = hazards.replace("&nbsp;", "").replace(Regex("<br>", RegexOption.IGNORE_CASE), "|").trim()
        preferences.setString("_spacegateHazards", hazards)

        val gear = HAZARDS.filter { hazards.contains(it.terminal) }.joinToString("|") { it.gear }
        preferences.setString("_spacegateGear", gear)

        preferences.setString("_spacegatePlantLife", lifeString(PLANT.find(html)))
        preferences.setString("_spacegateAnimalLife", lifeString(ANIMAL.find(html)))
        preferences.setString("_spacegateIntelligentLife", lifeString(INTELLIGENT.find(html)))

        preferences.setBoolean("_spacegateSpant", SPANT.containsMatchIn(html))
        preferences.setBoolean("_spacegateMurderbot", MURDERBOT.containsMatchIn(html))
        preferences.setBoolean("_spacegateRuins", RUINS.containsMatchIn(html))

        preferences.setString(
            "_spacegateTurnsLeft",
            TURNS.find(html)?.groupValues?.getOrNull(1) ?: "0",
        )
        return true
    }

    private fun lifeString(match: MatchResult?): String {
        if (match == null) return "none"
        val base = match.groupValues.getOrNull(1)?.trim().orEmpty()
        val hostile = match.groupValues.getOrNull(2)
        return if (hostile.isNullOrBlank()) base else "$base $hostile"
    }
}
