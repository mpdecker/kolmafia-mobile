package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.preferences.Preferences

/** Parses round-1 Pokefam fight HTML for your team (desktop `FightRequest.parsePokefam`). */
object FightPokefamSync {

    private val roundLaterPattern = Regex("""Round\s+[2-9]\d*""", RegexOption.IGNORE_CASE)
    private val tableBlockPattern = Regex(
        """<table\b[^>]*>.*?</table>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val imagePattern = Regex(
        """itemimages/([^"?]+\.(?:gif|png|jpg))""",
        RegexOption.IGNORE_CASE,
    )
    private val namePattern = Regex(
        """<td[^>]*class="tiny"[^>]*width="150"[^>]*>\s*([^<\n]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val levelRacePattern = Regex(
        """Lv\.\s*(\d+)\s+([^<\n]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val attributeTitlePattern = Regex("""title="([^:"]+):""")

    fun isPokefamFight(html: String, inPokefam: Boolean): Boolean =
        inPokefam && html.contains(" Team:") && html.contains("Your Team")

    fun isRoundOne(html: String): Boolean = !roundLaterPattern.containsMatchIn(html)

    fun parse(html: String, preferences: Preferences? = null): List<PokefamTeamSlot> {
        val yourTeamSection = html.substringAfter("Your Team", "")
        if (yourTeamSection.isEmpty()) return PokefamTeamSlot.EMPTY_TEAM

        val tables = tableBlockPattern.findAll(yourTeamSection).map { it.value }.take(3).toList()
        return List(3) { index ->
            tables.getOrNull(index)?.let { parseTable(it, preferences) } ?: PokefamTeamSlot.EMPTY
        }
    }

    fun apply(
        character: KoLCharacter,
        html: String,
        familiarManager: FamiliarManager? = null,
        preferences: Preferences? = null,
    ) {
        val state = character.state.value
        if (!isPokefamFight(html, state.inPokefam) || !isRoundOne(html)) return
        val team = parse(html, preferences)
        if (team.none { !it.isEmpty }) return
        character.updatePokeTeam(team)
        familiarManager?.mergePokeTeam(team)
    }

    private fun parseTable(tableHtml: String, preferences: Preferences?): PokefamTeamSlot {
        val image = imagePattern.find(tableHtml)?.groupValues?.get(1)?.trim().orEmpty()
        val familiarId = FamiliarDefinitionDatabase.getByImage(image)?.id ?: 0
        if (familiarId <= 0) return PokefamTeamSlot.EMPTY

        val name = namePattern.find(tableHtml)?.groupValues?.get(1)?.trim().orEmpty()
        val levelMatch = levelRacePattern.find(tableHtml)
        val level = levelMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val race = levelMatch?.groupValues?.get(2)?.trim().orEmpty()

        val row1End = tableHtml.indexOf("<tr", startIndex = tableHtml.indexOf("<tr") + 1)
        val attributeSection = if (row1End > 0) tableHtml.substring(0, row1End) else tableHtml
        val attributes = attributeTitlePattern.findAll(attributeSection)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

        val rawPower = Regex("""blacksword\.gif""", RegexOption.IGNORE_CASE).findAll(tableHtml).count()
        val rawHp = Regex("""blackheart\.gif""", RegexOption.IGNORE_CASE).findAll(tableHtml).count()
        val (power, hp, adjustedAttributes) = PokefamBoostSync.adjustStats(
            race = race,
            power = rawPower,
            hp = rawHp,
            attributes = attributes,
            preferences = preferences,
        )

        return PokefamTeamSlot(
            familiarId = familiarId,
            name = name,
            level = level,
            power = power,
            hp = hp,
            attributes = adjustedAttributes,
        )
    }
}
