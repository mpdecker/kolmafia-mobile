package net.sourceforge.kolmafia.character

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.preferences.Preferences

class FightPokefamSyncTest {

    private fun yourTeamTable(
        image: String,
        name: String,
        level: Int,
        race: String,
        power: Int,
        hp: Int,
        attributeTitle: String = "Armor: reduces damage",
    ): String {
        val swords = (1..power).joinToString("") {
            """<img src="https://images.kingdomofloathing.com/itemimages/blacksword.gif">"""
        }
        val hearts = (1..hp).joinToString("") {
            """<img src="https://images.kingdomofloathing.com/itemimages/blackheart.gif">"""
        }
        return """
            <table>
            <tbody>
            <tr>
            <td rowspan="2"><img src="https://images.kingdomofloathing.com/itemimages/$image"></td>
            <td class="tiny" width="150">$name</td>
            <td rowspan="2" width="120">$swords</td>
            <td rowspan="2" align="center" width="60">
            <img src="https://images.kingdomofloathing.com/itemimages/whiteshield.gif" title="$attributeTitle">
            </td>
            <td rowspan="2" width="150">$hearts</td>
            </tr>
            <tr><td class="tiny">Lv. $level $race</td></tr>
            </tbody>
            </table>
        """.trimIndent()
    }

    private fun fightHtml(vararg yourTeamTables: String, round: Int = 1): String = buildString {
        append("""<center><b>Some Opponent's Team:</b>""")
        append("""<table><tr><td>enemy</td></tr></table>""")
        append("""<b>Your Team</b>""")
        yourTeamTables.forEach { append(it) }
        append("""Round $round of combat""")
    }

    @Test
    fun parse_yourTeam_extractsPowerHpLevelAndAttributes() = runTest {
        FamiliarDefinitionDatabase.load()
        val html = fightHtml(
            yourTeamTable("pokefam1.gif", "Globmule", 12, "Globmule", power = 3, hp = 4),
            yourTeamTable("pokefam2.gif", "Bluzzard", 8, "Bluzzard", power = 2, hp = 2),
        )
        val team = FightPokefamSync.parse(html)
        assertEquals(215, team[0].familiarId)
        assertEquals("Globmule", team[0].name)
        assertEquals(12, team[0].level)
        assertEquals(3, team[0].power)
        assertEquals(4, team[0].hp)
        assertEquals(listOf("Armor"), team[0].attributes)
        assertEquals(216, team[1].familiarId)
        assertEquals(8, team[1].level)
        assertEquals(PokefamTeamSlot.EMPTY, team[2])
    }

    @Test
    fun isRoundOne_rejectsLaterRounds() {
        assertTrue(FightPokefamSync.isRoundOne(fightHtml(yourTeamTable("pokefam1.gif", "Globmule", 5, "Globmule", 1, 1))))
        assertFalse(FightPokefamSync.isRoundOne(fightHtml(yourTeamTable("pokefam1.gif", "Globmule", 5, "Globmule", 1, 1), round = 2)))
    }

    @Test
    fun apply_updatesCharacterAndOwnedFamiliarsOnRoundOne() = runTest {
        FamiliarDefinitionDatabase.load()
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(
                path = AscensionPath.POKEFAM.apiName,
            ),
        )
        val client = HttpClient(MockEngine { respond("ok") })
        val manager = FamiliarManager(client, GameEventBus())
        val html = fightHtml(yourTeamTable("pokefam1.gif", "Globmule", 12, "Globmule", 3, 4))
        FightPokefamSync.apply(character, html, manager)
        val team = character.state.value.pokeTeam
        assertEquals(12, team[0].level)
        assertEquals(3, team[0].power)
        assertEquals(4, team[0].hp)
        val owned = manager.state.value.ownedFamiliars.single()
        assertEquals(215, owned.id)
        assertEquals(12, owned.pokeLevel)
    }

    @Test
    fun parse_appliesPokeBoostAdjustmentWhenPrefSet() = runTest {
        FamiliarDefinitionDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(PokefamBoostSync.POKEFAM_BOOSTS_PREF, "Globmule:Power")
        val html = fightHtml(
            yourTeamTable("pokefam1.gif", "Globmule", 12, "Globmule", power = 3, hp = 4),
        )
        val team = FightPokefamSync.parse(html, prefs)
        assertEquals(2, team[0].power)
        assertEquals(4, team[0].hp)
    }

    @Test
    fun apply_skipsWhenNotRoundOne() = runTest {
        FamiliarDefinitionDatabase.load()
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(
                path = AscensionPath.POKEFAM.apiName,
            ),
        )
        val html = fightHtml(
            yourTeamTable("pokefam1.gif", "Globmule", 12, "Globmule", 3, 4),
            round = 3,
        )
        FightPokefamSync.apply(character, html, null)
        assertTrue(character.state.value.pokeTeam.isEmpty())
    }
}
