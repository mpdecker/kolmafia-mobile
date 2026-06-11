package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryScriptTest {

    private fun libWithScript(name: String, source: String): GameRuntimeLibrary {
        val p = prefs()
        p.setString(
            ScriptManager.SCRIPTS_PREF_KEY,
            Json.encodeToString(listOf(ScriptEntry(name, source))),
        )
        return GameRuntimeLibrary(preferences = p)
    }

    @Test
    fun runscript_executesSavedScript() {
        val lib = libWithScript("hello", """print("from script");""")
        val out = outputLib(lib, """
            print(to_string(runscript("hello")));
            print("done");
        """)
        assertTrue(out.contains("true"))
        assertTrue(out.contains("from script"))
        assertTrue(out.contains("done"))
    }

    @Test
    fun runscript_returnsFalseWhenMissing() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("false", outputLib(lib, """print(to_string(runscript("nope")));"""))
    }

    @Test
    fun sync_quests_callsQuestLogSync() {
        var synced = false
        val questLog = object : net.sourceforge.kolmafia.request.QuestLogRequest(
            HttpClient(MockEngine { respond("") }),
            QuestDatabase(prefs()),
        ) {
            override suspend fun syncAll() {
                synced = true
            }
        }
        val lib = GameRuntimeLibrary(questLogRequest = questLog)
        assertEquals("true", outputLib(lib, """print(to_string(sync_quests()));"""))
        assertTrue(synced)
    }

    @Test
    fun sync_quests_returnsFalseWithoutQuestLog() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, """print(to_string(sync_quests()));"""))
    }

    @Test
    fun maximize_returnsFalseStub() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, """print(to_string(maximize()));"""))
    }
}
