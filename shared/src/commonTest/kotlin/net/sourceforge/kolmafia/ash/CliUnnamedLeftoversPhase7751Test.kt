package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.GoalManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliUnnamedLeftoversPhase7751Test {

    private fun lib(): GameRuntimeLibrary {
        val prefs = Preferences(MapSettings())
        return GameRuntimeLibrary(preferences = prefs, goalManager = GoalManager())
    }

    @Test
    fun enableDisable_usesDisabledFeaturesNotPrefs() {
        val library = lib()
        val out = mutableListOf<String>()
        val rt = object : AshRuntimeContext {
            override fun print(msg: String) { out += msg }
        }
        library.runEnableDisableCli(enable = false, parameters = "breakfast, maximize", rt = rt)
        assertTrue(library.isCliCommandDisabled("breakfast"))
        assertTrue(library.isCliCommandDisabled("maximize"))
        library.runEnableDisableCli(enable = true, parameters = "all", rt = rt)
        assertFalse(library.isCliCommandDisabled("breakfast"))
    }

    @Test
    fun help_listsNewVerbs() {
        assertTrue(IMPLEMENTED_CLI_COMMANDS.contains("enable"))
        assertTrue(IMPLEMENTED_CLI_COMMANDS.contains("neweffect"))
        assertTrue(IMPLEMENTED_CLI_COMMANDS.contains("update"))
        assertTrue(IMPLEMENTED_CLI_COMMANDS.contains("login"))
        assertTrue(IMPLEMENTED_CLI_COMMANDS.contains("fortune"))
        assertTrue(IMPLEMENTED_CLI_COMMANDS.contains("goals"))
        assertTrue(IMPLEMENTED_CLI_COMMANDS.contains("aprilband"))
    }

    @Test
    fun conditionCheck_reportsNoGoals() {
        val library = lib()
        val out = mutableListOf<String>()
        val rt = object : AshRuntimeContext {
            override fun print(msg: String) { out += msg }
        }
        library.runConditionCheckCli(rt)
        assertTrue(out.any { it.contains("No conditions", ignoreCase = true) })
    }

    @Test
    fun registerRuntimeEffect_learnsByDescId() {
        EffectDatabase.resetForTest()
        val id = EffectDatabase.registerRuntimeEffect("Test Effect", "desc999")
        assertTrue(id < 0)
        assertEquals("Test Effect", EffectDatabase.getByDescId("desc999")?.name)
        EffectDatabase.resetForTest()
    }

    @Test
    fun update_unknownArgsPrintsHint() {
        val library = lib()
        val out = mutableListOf<String>()
        val rt = object : AshRuntimeContext {
            override fun print(msg: String) { out += msg }
        }
        library.runUpdateDataCli("dailybuild", rt)
        assertTrue(out.any { it.contains("doesn't do what you think") })
    }
}
