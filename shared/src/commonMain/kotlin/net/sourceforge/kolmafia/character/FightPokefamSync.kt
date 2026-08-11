package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.data.PokefamDatabase
import net.sourceforge.kolmafia.data.PokefamMoveRegistry
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Parses round-1 Pokefam fight HTML (desktop `FightRequest.parsePokefam`). */
object FightPokefamSync {

    private const val ULTIMATE_PREFIX = "ULTIMATE: "

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
    private val moveInputPattern = Regex(
        """<input[^>]*class="[^"]*button[^"]*"[^>]*title="([^"]*)"[^>]*value="([^"]*)"[^>]*name="famaction\[([^-\]]+)-\d+\]"[^>]*>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val bracketMovePattern = Regex("""\[([^\]]+)\]""")

    fun isPokefamFight(html: String, inPokefam: Boolean): Boolean =
        inPokefam && html.contains(" Team:") && html.contains("Your Team")

    fun isRoundOne(html: String): Boolean = !roundLaterPattern.containsMatchIn(html)

    fun parse(
        html: String,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
    ): List<PokefamTeamSlot> {
        val tables = collectFightTables(html)
        if (tables.isEmpty()) return PokefamTeamSlot.EMPTY_TEAM

        val yourTeam = mutableListOf<PokefamTeamSlot>()
        tables.forEachIndexed { index, tableHtml ->
            val tableIndex = index + 1
            val myTeam = tableIndex > 3
            parseTable(
                tableHtml = tableHtml,
                myTeam = myTeam,
                moveSpans = !myTeam,
                preferences = preferences,
                sessionLogger = sessionLogger,
            )?.let { slot ->
                if (myTeam) yourTeam.add(slot)
            }
        }
        return List(3) { index -> yourTeam.getOrNull(index) ?: PokefamTeamSlot.EMPTY }
    }

    fun apply(
        character: KoLCharacter,
        html: String,
        familiarManager: FamiliarManager? = null,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
    ) {
        val state = character.state.value
        if (!isPokefamFight(html, state.inPokefam) || !isRoundOne(html)) return
        val team = parse(html, preferences, sessionLogger)
        if (team.none { !it.isEmpty }) return
        character.updatePokeTeam(team)
        familiarManager?.mergePokeTeam(team)
    }

    internal fun collectFightTables(html: String): List<String> {
        val teamMarker = html.indexOf(" Team:")
        if (teamMarker < 0) return emptyList()
        val section = html.substring(teamMarker)
        return tableBlockPattern.findAll(section).map { it.value }.take(6).toList()
    }

    internal fun parseFamteamTable(
        tableHtml: String,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
    ): PokefamTeamSlot? = parseTable(
        tableHtml = tableHtml,
        myTeam = true,
        moveSpans = true,
        preferences = preferences,
        sessionLogger = sessionLogger,
    )

    internal fun parseTable(
        tableHtml: String,
        myTeam: Boolean,
        moveSpans: Boolean,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ): PokefamTeamSlot? {
        val image = imagePattern.find(tableHtml)?.groupValues?.get(1)?.trim().orEmpty()
        val familiarId = FamiliarDefinitionDatabase.getByImage(image)?.id ?: 0
        if (familiarId <= 0) return null

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
        val (power, hp, adjustedAttributes) = if (myTeam) {
            PokefamBoostSync.adjustStats(
                race = race,
                power = rawPower,
                hp = rawHp,
                attributes = attributes,
                preferences = preferences,
            )
        } else {
            Triple(rawPower, rawHp, attributes)
        }
        val attribute = adjustedAttributes.firstOrNull() ?: "None"

        val moveSection = moveSectionHtml(tableHtml)
        val parsedMoves = if (moveSpans) {
            parseSpanMoves(moveSection)
        } else {
            parseInputMoves(moveSection)
        }

        for (slot in 1..3) {
            val move = parsedMoves.moves[slot - 1]
            val action = parsedMoves.actions[slot - 1]
            val description = parsedMoves.descriptions[slot - 1]
            PokefamMoveRegistry.registerMove(slot, move, action, description, sessionLogger)
        }

        PokefamDatabase.registerFromFight(
            race = race,
            level = level,
            power = power,
            hp = hp,
            attribute = attribute,
            move1 = parsedMoves.moves[0],
            move2 = parsedMoves.moves[1],
            move3 = parsedMoves.moves[2],
            sessionLogger = sessionLogger,
        )

        if (!myTeam) {
            sessionLogger?.appendRawLine("$name, Lv. $level $race")
            return null
        }

        return PokefamTeamSlot(
            familiarId = familiarId,
            name = name,
            level = level,
            power = power,
            hp = hp,
            attributes = adjustedAttributes,
        )
    }

    private fun moveSectionHtml(tableHtml: String): String {
        var trIndex = 0
        var pos = 0
        while (trIndex < 4 && pos < tableHtml.length) {
            val idx = tableHtml.indexOf("<tr", pos, ignoreCase = true)
            if (idx < 0) break
            trIndex++
            pos = idx + 3
        }
        return if (pos > 0 && pos < tableHtml.length) tableHtml.substring(pos) else tableHtml
    }

    private data class ParsedMoves(
        val moves: Array<String?>,
        val actions: Array<String?>,
        val descriptions: Array<String?>,
    )

    private fun parseInputMoves(section: String): ParsedMoves {
        val moves = arrayOfNulls<String>(3)
        val actions = arrayOfNulls<String>(3)
        val descriptions = arrayOfNulls<String>(3)
        var moveIndex = 0
        for (match in moveInputPattern.findAll(section)) {
            if (moveIndex >= 3) break
            descriptions[moveIndex] = match.groupValues[1]
            var move = match.groupValues[2].trim()
            actions[moveIndex] = match.groupValues[3].trim()
            if (moveIndex == 2 && move.startsWith(ULTIMATE_PREFIX, ignoreCase = true)) {
                move = move.substring(ULTIMATE_PREFIX.length)
            }
            moves[moveIndex] = move
            moveIndex++
        }
        return ParsedMoves(moves, actions, descriptions)
    }

    private fun parseSpanMoves(section: String): ParsedMoves {
        val moves = arrayOfNulls<String>(3)
        val actions = arrayOfNulls<String>(3)
        val descriptions = arrayOfNulls<String>(3)
        val candidates = mutableListOf<String>()
        val spanBlockPattern = Regex(
            """<span[^>]*>(.*?)</span>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
        for (match in spanBlockPattern.findAll(section)) {
            val inner = match.groupValues[1]
            val bracket = bracketMovePattern.find(inner)?.groupValues?.get(1)?.trim()
            if (!bracket.isNullOrBlank()) {
                candidates.add(bracket)
            }
        }
        for (index in 0 until minOf(3, candidates.size)) {
            var move = candidates[index]
            if (index == 2 && move.startsWith(ULTIMATE_PREFIX, ignoreCase = true)) {
                move = move.substring(ULTIMATE_PREFIX.length)
            }
            moves[index] = move
        }
        return ParsedMoves(moves, actions, descriptions)
    }
}
