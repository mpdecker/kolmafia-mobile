package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScriptHookRunnerTest {

    @Test
    fun normalizeScriptName_stripsPathAndExtension() {
        assertEquals("buff", ScriptHookRunner.normalizeScriptName("scripts/buff.ash"))
        assertEquals("buff", ScriptHookRunner.normalizeScriptName("buff.ash"))
        assertEquals("buff", ScriptHookRunner.normalizeScriptName("C:\\\\mafia\\\\buff.ash"))
        assertEquals("between", ScriptHookRunner.normalizeScriptName("between"))
    }

    @Test
    fun onBetweenBattle_runsPrefScript() {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        val bus = GameEventBus()
        val lib = GameRuntimeLibrary.forTesting()
        val scripts = ScriptManager(lib, prefs, bus)
        scripts.saveScript(
            ScriptEntry(
                name = "pre",
                source = "print(\"hi\");",
                type = ScriptType.NORMAL,
            ),
        )
        prefs.setString(Preferences.BETWEEN_BATTLE_SCRIPT, "pre.ash")
        val runner = ScriptHookRunner(scripts, prefs)
        runner.onBetweenBattle()
        assertTrue(scripts.state.value.output.contains("hi"))
    }

    @Test
    fun onTurnConsumed_runsAfterAdventureScript() {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        val bus = GameEventBus()
        val lib = GameRuntimeLibrary.forTesting()
        val scripts = ScriptManager(lib, prefs, bus)
        scripts.saveScript(
            ScriptEntry(
                name = "post",
                source = "print(\"bye\");",
                type = ScriptType.NORMAL,
            ),
        )
        prefs.setString(Preferences.AFTER_ADVENTURE_SCRIPT, "post")
        val runner = ScriptHookRunner(scripts, prefs)
        runner.onTurnConsumed()
        assertTrue(scripts.state.value.output.contains("bye"))
    }

    @Test
    fun onLogin_runsLoginScript() {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        val bus = GameEventBus()
        val lib = GameRuntimeLibrary.forTesting()
        val scripts = ScriptManager(lib, prefs, bus)
        scripts.saveScript(ScriptEntry(name = "login", source = "print(\"in\");"))
        prefs.setString(Preferences.LOGIN_SCRIPT, "call login.ash")
        val runner = ScriptHookRunner(scripts, prefs)
        runner.onLogin()
        assertTrue(scripts.state.value.output.contains("in"))
    }

    @Test
    fun onKingLiberated_runsKingScript() {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        val bus = GameEventBus()
        val lib = GameRuntimeLibrary.forTesting()
        val scripts = ScriptManager(lib, prefs, bus)
        scripts.saveScript(ScriptEntry(name = "freed", source = "print(\"king\");"))
        prefs.setString(Preferences.KING_LIBERATED_SCRIPT, "freed")
        val runner = ScriptHookRunner(scripts, prefs)
        runner.onKingLiberated()
        assertTrue(scripts.state.value.output.contains("king"))
    }

    @Test
    fun onChoiceAdventure_invokesNamedFunctionWithArgs() {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        val bus = GameEventBus()
        val lib = GameRuntimeLibrary.forTesting()
        val scripts = ScriptManager(lib, prefs, bus)
        scripts.saveScript(
            ScriptEntry(
                name = "choice",
                source = """
                    void handle(int choice, string page) {
                      print(choice);
                    }
                """.trimIndent(),
            ),
        )
        prefs.setString(Preferences.CHOICE_ADVENTURE_SCRIPT, "handle@choice.ash")
        val runner = ScriptHookRunner(scripts, prefs)
        assertTrue(runner.onChoiceAdventure(123, "<form>"))
        assertTrue(scripts.state.value.output.contains("123"))
    }

    @Test
    fun onCounter_returnsTrueWhenScriptReturnsNonZero() {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        val bus = GameEventBus()
        val lib = GameRuntimeLibrary.forTesting()
        val scripts = ScriptManager(lib, prefs, bus)
        scripts.saveScript(
            ScriptEntry(
                name = "counter",
                source = """
                    boolean main(string label, string turns) {
                      print(label);
                      return true;
                    }
                """.trimIndent(),
            ),
        )
        prefs.setString(Preferences.COUNTER_SCRIPT, "counter")
        val runner = ScriptHookRunner(scripts, prefs)
        assertTrue(runner.onCounter("Bee window end", 0))
        assertTrue(scripts.state.value.output.contains("Bee window end"))
    }

    @Test
    fun onBeforePvp_runsSavedAshScript() {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        val bus = GameEventBus()
        val lib = GameRuntimeLibrary.forTesting()
        val scripts = ScriptManager(lib, prefs, bus)
        scripts.saveScript(ScriptEntry(name = "prep", source = "print(\"pvp\");"))
        prefs.setString(Preferences.BEFORE_PVP_SCRIPT, "prep.ash")
        val runner = ScriptHookRunner(scripts, prefs)
        assertTrue(runner.onBeforePvp())
        assertTrue(scripts.state.value.output.contains("pvp"))
    }

    @Test
    fun normalizeScriptName_stripsCallPrefixViaPrefParse() {
        val settings = MapSettings()
        val prefs = Preferences(settings)
        val bus = GameEventBus()
        val lib = GameRuntimeLibrary.forTesting()
        val scripts = ScriptManager(lib, prefs, bus)
        scripts.saveScript(ScriptEntry(name = "buff", source = "print(\"ok\");"))
        prefs.setString(Preferences.LOGIN_SCRIPT, "call scripts/buff.ash")
        val runner = ScriptHookRunner(scripts, prefs)
        assertTrue(runner.onLogin())
        assertTrue(scripts.state.value.output.contains("ok"))
    }
}
