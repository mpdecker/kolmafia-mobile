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
}
