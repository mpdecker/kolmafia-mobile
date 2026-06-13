package net.sourceforge.kolmafia.ash

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertTrue

class ScriptHookRunnerTest {

    @Test
    fun onTurnConsumed_runsAutoscriptWhenEnabled() {
        val prefs = prefs()
        prefs.setBoolean(Preferences.AUTO_SCRIPTING, true)
        prefs.setString(
            ScriptManager.SCRIPTS_PREF_KEY,
            Json.encodeToString(listOf(
                ScriptEntry("auto", "print(\"hook\");", type = ScriptType.AUTOSCRIPT),
            )),
        )
        val lib = GameRuntimeLibrary(preferences = prefs)
        val scriptManager = ScriptManager(lib, prefs, net.sourceforge.kolmafia.event.GameEventBus())
        scriptManager.initialize()
        val hooks = ScriptHookRunner(scriptManager, prefs)

        hooks.onTurnConsumed()

        assertTrue(scriptManager.state.value.output.contains("hook"))
    }
}
