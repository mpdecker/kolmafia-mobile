package net.sourceforge.kolmafia.ash

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.event.GameEventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScriptManagerTest {

    private fun managerWithPref(scripts: List<ScriptEntry>): ScriptManager {
        val p = prefs()
        p.setString(ScriptManager.SCRIPTS_PREF_KEY, Json.encodeToString(scripts))
        return ScriptManager(GameRuntimeLibrary.forTesting(), p, GameEventBus())
    }

    @Test
    fun activeAutoscript_returnsFirstAutoscriptEntry() {
        val mgr = managerWithPref(listOf(
            ScriptEntry("daily", "print(\"daily\");", type = ScriptType.NORMAL),
            ScriptEntry("auto", "print(\"auto\");", type = ScriptType.AUTOSCRIPT),
        ))
        mgr.initialize()
        assertEquals("auto", mgr.activeAutoscript()?.name)
    }

    @Test
    fun activeCombatScript_returnsFirstCombatEntry() {
        val mgr = managerWithPref(listOf(
            ScriptEntry("ccs", "print(\"ccs\");", type = ScriptType.COMBAT),
        ))
        mgr.initialize()
        assertEquals("ccs", mgr.activeCombatScript()?.name)
    }

    @Test
    fun runScriptSync_executesAndSetsOutput() {
        val mgr = managerWithPref(listOf(
            ScriptEntry("auto", "print(\"hook\");", type = ScriptType.AUTOSCRIPT),
        ))
        mgr.initialize()
        mgr.runScriptSync("auto")
        assertTrue(mgr.state.value.output.contains("hook"))
    }
}
