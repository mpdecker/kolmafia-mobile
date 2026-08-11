package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Parses famteam.php HTML (desktop `FamTeamRequest.parseResponse`). */
object FamTeamSync {

    private val activePattern = Regex(
        """<div class="(slot[^"]+)" data-pos="(\d+)"><div class="fambox" data-id="(\d+)">(.*?)</div></div>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val bullpenPattern = Regex(
        """<div class="fambox" data-id="(\d+)"[^>]+>(.*?)</div>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val famTypePattern = Regex(
        """class=tiny>Lv\.\s*(\d+)\s+([^<]+)</td>""",
        RegexOption.IGNORE_CASE,
    )
    private val tableBlockPattern = Regex(
        """<table\b[^>]*>.*?</table>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val famParamPattern = Regex("""[?&]fam=(\d+)""", RegexOption.IGNORE_CASE)
    private val itemParamPattern = Regex("""[?&]iid=(\d+)""", RegexOption.IGNORE_CASE)
    private val slotParamPattern = Regex("""[?&]slot=(\d+)""", RegexOption.IGNORE_CASE)
    private val actionParamPattern = Regex("""[?&]action=([^&]+)""", RegexOption.IGNORE_CASE)

    /** Desktop `FamTeamRequest.registerRequest`. */
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        val normalized = normalizeUrl(url)
        if (!normalized.startsWith("famteam.php", ignoreCase = true)) return false

        if (normalized.equals("famteam.php", ignoreCase = true)) {
            return true
        }

        val action = actionParamPattern.find(normalized)?.groupValues?.getOrNull(1) ?: return false
        return when (action.lowercase()) {
            "feed" -> {
                val familiarId = famParamPattern.find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: return false
                val itemId = itemParamPattern.find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: return false
                val race = FamiliarDefinitionDatabase.getById(familiarId)?.name ?: return false
                val item = ItemDatabase.getItemName(itemId)
                sessionLogger?.appendRawLine("Feeding $item to $race")
                true
            }
            "slot" -> {
                val familiarId = famParamPattern.find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: return false
                val slot = slotParamPattern.find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: return false
                val race = FamiliarDefinitionDatabase.getById(familiarId)?.name ?: return false
                sessionLogger?.appendRawLine("Putting  $race into slot $slot of your Pokefam team")
                true
            }
            else -> false
        }
    }

    private fun normalizeUrl(url: String): String =
        url.removePrefix(KOL_BASE_URL).trimStart('/')

    fun parse(
        html: String,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
    ): List<PokefamTeamSlot> {
        val slots = MutableList(3) { PokefamTeamSlot.EMPTY }
        val activeIds = mutableSetOf<Int>()

        for (match in activePattern.findAll(html)) {
            val slotIndex = match.groupValues[2].toIntOrNull()?.minus(1) ?: continue
            if (slotIndex !in 0..2) continue
            val familiarId = match.groupValues[3].toIntOrNull() ?: continue
            if (familiarId <= 0) {
                slots[slotIndex] = PokefamTeamSlot.EMPTY
                continue
            }
            activeIds.add(familiarId)
            val famTable = match.groupValues[4]
            val typeMatch = famTypePattern.find(famTable)
            val level = typeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val race = typeMatch?.groupValues?.get(2)?.trim().orEmpty()
            val parsed = extractTable(famTable)?.let { tableHtml ->
                FightPokefamSync.parseFamteamTable(tableHtml, preferences, sessionLogger)
            }
            slots[slotIndex] = when {
                parsed != null -> parsed.copy(
                    familiarId = familiarId,
                    name = race.ifBlank { parsed.name },
                    level = level.takeIf { it > 0 } ?: parsed.level,
                )
                else -> PokefamTeamSlot(
                    familiarId = familiarId,
                    name = race,
                    level = level,
                )
            }
        }
        return slots
    }

    fun applyBullpen(
        html: String,
        activeIds: Set<Int>,
        familiarManager: FamiliarManager?,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
    ) {
        if (familiarManager == null) return
        for (match in bullpenPattern.findAll(html)) {
            val familiarId = match.groupValues[1].toIntOrNull() ?: continue
            if (familiarId <= 0 || familiarId in activeIds) continue
            val famTable = match.groupValues[2]
            val typeMatch = famTypePattern.find(famTable)
            val level = typeMatch?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val race = typeMatch?.groupValues?.get(2)?.trim().orEmpty()
            if (race.isBlank()) continue
            extractTable(famTable)?.let { tableHtml ->
                FightPokefamSync.parseFamteamTable(tableHtml, preferences, sessionLogger)
            }
            familiarManager.registerPokefamFamiliar(familiarId, race, level)
        }
    }

    fun apply(
        character: KoLCharacter,
        html: String,
        familiarManager: FamiliarManager? = null,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
    ) {
        if (!character.state.value.inPokefam) return
        val team = parse(html, preferences, sessionLogger)
        character.updatePokeTeam(team)
        familiarManager?.mergePokeTeam(team)
        val activeIds = team.mapNotNull { slot -> slot.familiarId.takeIf { it > 0 } }.toSet()
        applyBullpen(html, activeIds, familiarManager, preferences, sessionLogger)
    }

    private fun extractTable(section: String): String? =
        tableBlockPattern.find(section)?.value
}
