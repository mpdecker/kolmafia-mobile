package net.sourceforge.kolmafia.ash

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AshCompatibilityCorpusTest {

    @Test
    fun corpus_basicLocationAndCollectionSnippets() {
        val p = prefs()
        p.setString(Preferences.LAST_LOCATION, "The Spooky Forest")
        CollectionCache.save(p, Preferences.CACHED_CLOSET, mapOf(42 to 2))
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("The Spooky Forest", outputLib(lib, "print(my_location());"))
        assertEquals("false", outputLib(lib, """print(to_string(pvp_attack("someone")));"""))
        assertEquals("false", outputLib(lib, "print(to_string(ranked_fam()));"))
    }

    @Test
    fun corpus_combatScriptSnippet() {
        val scripts = listOf(
            ScriptEntry("fight", """set_ccs_action("attack;");""", type = ScriptType.COMBAT),
        )
        val p = prefs()
        p.setString(ScriptManager.SCRIPTS_PREF_KEY, Json.encodeToString(scripts))
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("attack;", outputLib(lib, """print(get_ccs_action());"""))
    }

    @Test
    fun corpus_cliHighTrafficSnippets() {
        val p = prefs()
        p.registerCounterName("kills")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Testy",
                    playerid = "99",
                    level = "5",
                    classId = "5",
                    adventures = "12",
                    meat = "1000",
                ),
            )
        }
        val lib = GameRuntimeLibrary(preferences = p, character = char)
        runLib(lib, """cli_execute("counter kills add 3");""")
        assertEquals("3", outputLib(lib, """cli_execute("counter kills");"""))
        assertTrue(outputLib(lib, """cli_execute("show all");""").contains("Testy"))
        assertEquals("That's funny.", outputLib(lib, """cli_execute("joke");""").trim())
        assertFalse(outputLib(lib, """cli_execute("pvp attack rival");""").contains("[cli]"))
        assertEquals("false", outputLib(lib, """cli_execute("is_adventuring");""").trim())
        assertEquals("false", outputLib(lib, """cli_execute("has_queued_commands");""").trim())
        assertTrue(outputLib(lib, """print(to_string(my_path_id()));""").trim().toLongOrNull() != null)
        assertEquals("Muscle", outputLib(lib, """print(modifier_name("Muscle"));""").trim())
    }

    @Test
    fun corpus_entityConversionStubs() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("skeleton", outputLib(lib, """print(to_servant("skeleton"));"""))
        assertEquals("vykea", outputLib(lib, """print(to_vykea("vykea"));"""))
    }
}
