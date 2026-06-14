package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.adventure.AdventureParser
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryCombatScriptTest {

    @Test
    fun getCcsAction_runsCombatScriptAndReturnsAction() {
        val scripts = listOf(
            ScriptEntry("my ccs", """set_ccs_action("skill 3004;");""", type = ScriptType.COMBAT),
        )
        val p = prefs()
        p.setString(ScriptManager.SCRIPTS_PREF_KEY, Json.encodeToString(scripts))
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("skill 3004;", outputLib(lib, """print(get_ccs_action());"""))
    }

    @Test
    fun getCcsAction_emptyWhenNoCombatScript() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("", outputLib(lib, """print(get_ccs_action());"""))
    }

    @Test
    fun setCcsAction_storesActionOnRuntime() {
        val runtime = runLib(GameRuntimeLibrary.forTesting(), """set_ccs_action("skill 1;");""")
        assertEquals("skill 1;", runtime.lastCombatAction())
    }

    @Test
    fun resolveCombatMacro_prefersCcsOverPrefs() {
        val scripts = listOf(
            ScriptEntry("fight", """set_ccs_action("attack;");""", type = ScriptType.COMBAT),
        )
        val p = prefs()
        p.setString(ScriptManager.SCRIPTS_PREF_KEY, Json.encodeToString(scripts))
        p.setString("combatMacroDefault", "skill 9999")
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("attack;", lib.resolveCombatMacro("1"))
    }

    @Test
    fun resolveCombatMacro_fallsBackToPrefsWhenNoCcs() {
        val p = prefs()
        p.setString("combatMacro_1", "skill 3005")
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("skill 3005", lib.resolveCombatMacro("1"))
    }

    @Test
    fun canStillSteal_trueWhenPickpocketClassAndStealButtonPresent() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(classId = CharacterClass.DISCO_BANDIT.id.toString()))
        }
        val mgr = stubAdventureManager(char)
        mgr.testSetLastFightHtml(
            """<input type="submit" value="Pick His Pocket">"""
        )
        val lib = GameRuntimeLibrary(adventureManager = mgr)
        assertEquals("true", outputLib(lib, "print(to_string(can_still_steal()));"))
    }

    @Test
    fun canStillSteal_falseWithoutPickpocketClass() {
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(classId = CharacterClass.SEAL_CLUBBER.id.toString()))
        }
        val mgr = stubAdventureManager(char)
        mgr.testSetLastFightHtml(
            """<input type="submit" value="Pick His Pocket">"""
        )
        val lib = GameRuntimeLibrary(adventureManager = mgr)
        assertEquals("false", outputLib(lib, "print(to_string(can_still_steal()));"))
    }

    @Test
    fun canStillSteal_falseWithoutAdventureManager() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, "print(to_string(can_still_steal()));"))
    }

    @Test
    fun adventureParser_canStillSteal_matchesDesktopPattern() {
        val html = """<input type="submit" value="Steal (for research)">"""
        assertTrue(AdventureParser.canStillSteal(html))
        assertFalse(AdventureParser.canStillSteal("<p>You win the fight</p>"))
    }

    private fun stubAdventureManager(character: KoLCharacter) =
        net.sourceforge.kolmafia.adventure.AdventureManager(
            adventureRequest = net.sourceforge.kolmafia.adventure.AdventureRequest(
                HttpClient(MockEngine { respond("") }),
            ),
            fightRequest = net.sourceforge.kolmafia.adventure.FightRequest(
                HttpClient(MockEngine { respond("") }),
            ),
            choiceRequest = net.sourceforge.kolmafia.adventure.ChoiceRequest(
                HttpClient(MockEngine { respond("") }),
            ),
            characterRequest = net.sourceforge.kolmafia.request.CharacterRequest(
                HttpClient(MockEngine { respond("") }),
            ),
            character = character,
            preferences = prefs(),
            eventBus = GameEventBus(),
        )
}
