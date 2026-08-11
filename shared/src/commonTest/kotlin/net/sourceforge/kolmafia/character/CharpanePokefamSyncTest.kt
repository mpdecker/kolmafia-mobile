package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager

class CharpanePokefamSyncTest {

    @Test
    fun parse_threeSlotTeam_resolvesImageToFamiliarId() = runTest {
        FamiliarDefinitionDatabase.load()
        val html = """
            <img align="absmiddle" src="https://images.kingdomofloathing.com/itemimages/pokefam1.gif">&nbsp;Globmule (Lvl 12)
            <img align="absmiddle" src="https://d2uyhvukfffg5a.cloudfront.net/itemimages/pokefam2.gif">&nbsp;Bluzzard (Lvl 8)
            <img align="absmiddle" src="https://images.kingdomofloathing.com/itemimages/pokefam3.gif">&nbsp;Faux (Lvl 5)
        """.trimIndent()
        val team = CharpanePokefamSync.parse(html)
        assertEquals(3, team.size)
        assertEquals(215, team[0].familiarId)
        assertEquals("Globmule", team[0].name)
        assertEquals(12, team[0].level)
        assertEquals(216, team[1].familiarId)
        assertEquals("Bluzzard", team[1].name)
        assertEquals(8, team[1].level)
        assertEquals(217, team[2].familiarId)
        assertEquals("Faux", team[2].name)
        assertEquals(5, team[2].level)
    }

    @Test
    fun parse_partialTeam_padsRemainingWithEmpty() = runTest {
        FamiliarDefinitionDatabase.load()
        val html = """
            <img align="absmiddle" src="https://images.kingdomofloathing.com/itemimages/pokefam49.gif">&nbsp;Bowlet (Lvl 3)
        """.trimIndent()
        val team = CharpanePokefamSync.parse(html)
        assertEquals(263, team[0].familiarId)
        assertEquals("Bowlet", team[0].name)
        assertEquals(3, team[0].level)
        assertEquals(PokefamTeamSlot.EMPTY, team[1])
        assertEquals(PokefamTeamSlot.EMPTY, team[2])
    }

    @Test
    fun apply_updatesCharacterStateAndMergesOwnedFamiliars() = runTest {
        FamiliarDefinitionDatabase.load()
        val character = KoLCharacter()
        val client = HttpClient(MockEngine { respond("ok") })
        val manager = FamiliarManager(client, GameEventBus())
        val html = """
            <img align="absmiddle" src="https://images.kingdomofloathing.com/itemimages/pokefam1.gif">&nbsp;Globmule (Lvl 4)
        """.trimIndent()
        CharpanePokefamSync.apply(character, html, manager)
        val team = character.state.value.pokeTeam
        assertEquals(215, team[0].familiarId)
        assertEquals(4, team[0].level)
        assertEquals(PokefamTeamSlot.EMPTY, team[1])
        assertEquals(PokefamTeamSlot.EMPTY, team[2])
        val owned = manager.state.value.ownedFamiliars.single()
        assertEquals(215, owned.id)
        assertEquals("Globmule", owned.name)
        assertEquals(4, owned.pokeLevel)
    }
}
