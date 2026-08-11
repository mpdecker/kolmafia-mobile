package net.sourceforge.kolmafia.character

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.PokefamDatabase
import net.sourceforge.kolmafia.data.PokefamMoveRegistry
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class FamTeamSyncTest {

    @AfterTest
    fun tearDown() {
        PokefamMoveRegistry.resetForTest()
        PokefamDatabase.resetForTest()
    }

    private fun teamTable(
        image: String,
        name: String,
        level: Int,
        race: String,
        moveSpans: List<String> = emptyList(),
    ): String {
        val spans = moveSpans.joinToString("&nbsp;&nbsp;") { move ->
            """<span title="desc">[$move]</span>"""
        }
        val moveRow = if (moveSpans.isNotEmpty()) {
            """<tr><td></td><td colspan="5" class="small">$spans</td></tr>"""
        } else {
            ""
        }
        return """
            <table>
            <tbody>
            <tr>
            <td rowspan="2"><img src="https://images.kingdomofloathing.com/itemimages/$image"></td>
            <td class="tiny" width="150">$name</td>
            <td rowspan="2" width="120"></td>
            <td rowspan="2" align="center" width="60"></td>
            <td rowspan="2" width="150"></td>
            </tr>
            <tr><td class=tiny>Lv. $level $race</td></tr>
            <tr><td height="10"></td></tr>
            $moveRow
            </tbody>
            </table>
        """.trimIndent()
    }

    private fun activeSlot(pos: Int, familiarId: Int, table: String = ""): String =
        """<div class="slot active" data-pos="$pos"><div class="fambox" data-id="$familiarId">$table</div></div>"""

    @Test
    fun parse_activeSlotsWithEmptySlot() = runTest {
        FamiliarDefinitionDatabase.load()
        val globmuleTable = teamTable("pokefam1.gif", "Globmule", 12, "Globmule")
        val bluzzardTable = teamTable("pokefam2.gif", "Bluzzard", 8, "Bluzzard")
        val html = buildString {
            append(activeSlot(1, 215, globmuleTable))
            append(activeSlot(2, 0))
            append(activeSlot(3, 216, bluzzardTable))
        }
        val team = FamTeamSync.parse(html)
        assertEquals(215, team[0].familiarId)
        assertEquals("Globmule", team[0].name)
        assertEquals(12, team[0].level)
        assertTrue(team[1].isEmpty)
        assertEquals(216, team[2].familiarId)
        assertEquals("Bluzzard", team[2].name)
    }

    @Test
    fun apply_bullpenUpsertWithoutChangingActiveSlots() = runTest {
        FamiliarDefinitionDatabase.load()
        val globmuleTable = teamTable("pokefam1.gif", "Globmule", 12, "Globmule")
        val bullpenTable = teamTable("pokefam3.gif", "Faux", 5, "Faux")
        val html = buildString {
            append(activeSlot(1, 215, globmuleTable))
            append(activeSlot(2, 0))
            append(activeSlot(3, 0))
            append("""<div class="fambox" data-id="217" style="">$bullpenTable</div>""")
        }
        val char = KoLCharacter().also { it.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(path = "Pocket Familiars")
        ) }
        val manager = FamiliarManager(HttpClient(MockEngine { respond("ok") }), GameEventBus())
        FamTeamSync.apply(char, html, manager)
        assertEquals(215, char.state.value.pokeTeam[0].familiarId)
        assertTrue(char.state.value.pokeTeam[1].isEmpty)
        assertTrue(char.state.value.pokeTeam[2].isEmpty)
        val owned = manager.state.value.ownedFamiliars
        assertEquals(2, owned.size)
        assertTrue(owned.any { it.id == 215 })
        assertTrue(owned.any { it.id == 217 && it.pokeLevel == 5 })
    }

    @Test
    fun parse_spanMovesUpdateDatabase() = runTest {
        FamiliarDefinitionDatabase.load()
        PokefamDatabase.load()
        val table = teamTable(
            image = "pokefam1.gif",
            name = "Globmule",
            level = 12,
            race = "Globmule",
            moveSpans = listOf("Bonk", "Embarrass", "ULTIMATE: Vulgar Display"),
        )
        val html = activeSlot(1, 215, table)
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        FamTeamSync.parse(html, sessionLogger = logger)
        val entry = PokefamDatabase.getByName("Globmule")
        assertEquals("Bonk", entry?.move1)
        assertEquals("Embarrass", entry?.move2)
        assertEquals("Vulgar Display", entry?.move3)
    }

    @Test
    fun registerRequest_bareVisitReturnsTrueWithoutLog() = runTest {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        assertTrue(FamTeamSync.registerRequest("famteam.php", logger))
        assertTrue(logger.recentLines().isEmpty())
    }

    @Test
    fun registerRequest_feedLogsSessionLine() = runTest {
        FamiliarDefinitionDatabase.load()
        ItemDatabase.load()
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val url = "famteam.php?action=feed&fam=215&iid=9748"
        assertTrue(FamTeamSync.registerRequest(url, logger))
        assertEquals(
            listOf("Feeding metandienone to Globmule"),
            logger.recentLines(),
        )
    }

    @Test
    fun registerRequest_slotLogsSessionLine() = runTest {
        FamiliarDefinitionDatabase.load()
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val url = "famteam.php?action=slot&fam=216&slot=2"
        assertTrue(FamTeamSync.registerRequest(url, logger))
        assertEquals(
            listOf("Putting  Bluzzard into slot 2 of your Pokefam team"),
            logger.recentLines(),
        )
    }

    @Test
    fun registerRequest_unknownActionReturnsFalse() {
        assertFalse(FamTeamSync.registerRequest("famteam.php?action=unknown", null))
    }

    @Test
    fun registerRequest_nonFamteamUrlReturnsFalse() {
        assertFalse(FamTeamSync.registerRequest("familiar.php", null))
    }
}
