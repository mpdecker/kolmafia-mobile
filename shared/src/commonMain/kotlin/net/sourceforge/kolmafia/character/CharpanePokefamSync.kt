package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.familiar.FamiliarManager

/** Parses Pokefam path active team from charpane HTML (desktop `CharPaneRequest.checkPokeFam`). */
object CharpanePokefamSync {

    private val pokeFamPattern = Regex(
        """img align="absmiddle" src="[^"]*(?:cloudfront.net|images.kingdomofloathing.com)/itemimages/([^"]+)"[^>]*>&nbsp;(.*?) \(Lvl (\d+)\)""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(html: String): List<PokefamTeamSlot> {
        val matches = pokeFamPattern.findAll(html).iterator()
        return List(3) { slot ->
            if (matches.hasNext()) {
                val match = matches.next()
                val image = match.groupValues[1].trim()
                val name = match.groupValues[2].trim()
                val level = match.groupValues[3].toIntOrNull() ?: 0
                val familiarId = FamiliarDefinitionDatabase.getByImage(image)?.id ?: 0
                PokefamTeamSlot(familiarId = familiarId, name = name, level = level)
            } else {
                PokefamTeamSlot.EMPTY
            }
        }
    }

    fun apply(character: KoLCharacter, html: String, familiarManager: FamiliarManager? = null) {
        val team = parse(html)
        character.updatePokeTeam(team)
        familiarManager?.mergePokeTeam(team)
    }
}
