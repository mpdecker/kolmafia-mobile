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
import net.sourceforge.kolmafia.data.PokefamDatabase
import net.sourceforge.kolmafia.data.PokefamMoveRegistry
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

class FightPokefamSyncTest {

    @AfterTest
    fun tearDown() {
        PokefamMoveRegistry.resetForTest()
        PokefamDatabase.resetForTest()
    }

    private fun invalidEnemyTable(): String = """<table><tr><td>enemy</td></tr></table>"""

    private fun teamTable(
        image: String,
        name: String,
        level: Int,
        race: String,
        power: Int,
        hp: Int,
        attributeTitle: String = "Armor: reduces damage",
        moveInputs: List<Triple<String, String, String>>? = null,
        moveSpans: List<String>? = null,
    ): String {
        val swords = (1..power).joinToString("") {
            """<img src="https://images.kingdomofloathing.com/itemimages/blacksword.gif">"""
        }
        val hearts = (1..hp).joinToString("") {
            """<img src="https://images.kingdomofloathing.com/itemimages/blackheart.gif">"""
        }
        val moveRow = when {
            moveInputs != null -> {
                val inputs = moveInputs.joinToString("") { (title, value, action) ->
                    """<input class="button" title="$title" value="$value" name="famaction[$action-1]">"""
                }
                """<tr><td></td><td colspan="5" class="small">$inputs</td></tr>"""
            }
            moveSpans != null -> {
                val spans = moveSpans.joinToString("&nbsp;&nbsp;") { move ->
                    """<span title="desc">[$move]</span>"""
                }
                """<tr><td></td><td colspan="5" class="small">$spans</td></tr>"""
            }
            else -> ""
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
            <tr><td height="10"></td></tr>
            $moveRow
            </tbody>
            </table>
        """.trimIndent()
    }

    private fun yourTeamTable(
        image: String,
        name: String,
        level: Int,
        race: String,
        power: Int,
        hp: Int,
        attributeTitle: String = "Armor: reduces damage",
    ): String = teamTable(image, name, level, race, power, hp, attributeTitle)

    private fun fightHtml(
        vararg yourTeamTables: String,
        enemyTables: List<String>? = null,
        round: Int = 1,
    ): String = buildString {
        append("""<center><b>Some Opponent's Team:</b>""")
        val enemies = enemyTables ?: List(3) { invalidEnemyTable() }
        enemies.forEach { append(it) }
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

    @Test
    fun parse_enemyTable_appendsSessionLogLine() = runTest {
        FamiliarDefinitionDatabase.load()
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val enemy = teamTable(
            image = "pokefam1.gif",
            name = "Enemy Glob",
            level = 5,
            race = "Globmule",
            power = 2,
            hp = 3,
            moveSpans = listOf("Punch", "Tackle", "ULTIMATE: Deluxe Impale"),
        )
        val html = fightHtml(
            yourTeamTable("pokefam2.gif", "Bluzzard", 8, "Bluzzard", 1, 1),
            enemyTables = listOf(enemy, invalidEnemyTable(), invalidEnemyTable()),
        )
        FightPokefamSync.parse(html, sessionLogger = logger)
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertEquals(true, log.contains("Enemy Glob, Lv. 5 Globmule"))
    }

    @Test
    fun parse_enemySpanMoves_stripsUltimatePrefix() = runTest {
        FamiliarDefinitionDatabase.load()
        val enemy = teamTable(
            image = "pokefam1.gif",
            name = "Enemy Glob",
            level = 5,
            race = "Globmule",
            power = 2,
            hp = 3,
            moveSpans = listOf("Punch", "Tackle", "ULTIMATE: Deluxe Impale"),
        )
        val html = fightHtml(
            enemyTables = listOf(enemy, invalidEnemyTable(), invalidEnemyTable()),
        )
        FightPokefamSync.parse(html)
        assertEquals("punch", PokefamMoveRegistry.moveToAction(1, "Punch"))
        assertEquals("tackle", PokefamMoveRegistry.moveToAction(2, "Tackle"))
        assertEquals("ult_impale", PokefamMoveRegistry.moveToAction(3, "Deluxe Impale"))
    }

    @Test
    fun parse_yourTeamMoveInputs_registerMoveAndDatabase() = runTest {
        FamiliarDefinitionDatabase.load()
        PokefamDatabase.load()
        val table = teamTable(
            image = "pokefam1.gif",
            name = "Globmule",
            level = 2,
            race = "Globmule",
            power = 3,
            hp = 4,
            moveInputs = listOf(
                Triple("Deal damage", "Punch", "punch"),
                Triple("Knock back", "Tackle", "tackle"),
                Triple("Ultimate", "ULTIMATE: Deluxe Impale", "ult_impale"),
            ),
        )
        val html = fightHtml(table)
        FightPokefamSync.parse(html)
        assertEquals("punch", PokefamMoveRegistry.moveToAction(1, "Punch"))
        assertEquals("tackle", PokefamMoveRegistry.moveToAction(2, "Tackle"))
        assertEquals("ult_impale", PokefamMoveRegistry.moveToAction(3, "Deluxe Impale"))
        val data = PokefamDatabase.getByName("Globmule")
        assertEquals("Punch", data?.move1)
        assertEquals("Tackle", data?.move2)
        assertEquals("Vulgar Display", data?.move3)
        assertEquals(3, data?.power2)
        assertEquals(4, data?.hp2)
    }
}
