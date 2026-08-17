package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameRuntimeLibraryAshP504Test {

    @Test
    fun revision_phase504() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    private fun libWithScript(name: String, source: String): GameRuntimeLibrary {
        val p = prefs()
        p.setString(
            ScriptManager.SCRIPTS_PREF_KEY,
            Json.encodeToString(listOf(ScriptEntry(name, source))),
        )
        return GameRuntimeLibrary(preferences = p)
    }

    @Test
    fun call_runsSavedScript() {
        val lib = libWithScript("demo", """print("called");""")
        val out = outputLib(lib, """cli_execute("call demo");""")
        assertTrue(out.contains("called"))
    }

    @Test
    fun exec_execute_load_start_aliasesRunSavedScript() {
        val lib = libWithScript("demo", """print("aliased");""")
        for (cmd in listOf("exec", "execute", "load", "start")) {
            val out = outputLib(lib, """cli_execute("$cmd demo");""")
            assertTrue(out.contains("aliased"), cmd)
        }
    }

    @Test
    fun call_nx_runsThatManyTimes() {
        val lib = libWithScript("demo", """print("tick");""")
        val out = outputLib(lib, """cli_execute("call 2x demo");""")
        assertEquals(2, Regex("tick").findAll(out).count())
    }

    @Test
    fun call_zeroX_isNoOp() {
        val lib = libWithScript("demo", """print("tick");""")
        val out = outputLib(lib, """cli_execute("call 0x demo");""")
        assertFalse(out.contains("tick"))
        assertFalse(out.contains("not found"))
    }

    @Test
    fun call_missingScript_printsNotFound() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val out = outputLib(lib, """cli_execute("call missing");""")
        assertTrue(out.contains("Script 'missing' not found"))
    }
}
