package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.maximizer.MaximizerManager
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

    @Test
    fun corpus_locationModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val penalty = outputLib(
            lib,
            """print(to_string(numeric_modifier(to_location("The Briny Deeps"), "Item Drop Penalty")));""",
        )
        assertEquals("-25.0", penalty)
    }

    @Test
    fun corpus_pathModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1.0",
            outputLib(lib, """print(to_string(numeric_modifier(to_path("You, Robot"), "Energy")));"""),
        )
    }

    @Test
    fun corpus_maximizerModifierSnippet() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val out = outputLib(
            lib,
            """
            print(modifier_name("Item Drop Penalty"));
            print(to_string(numeric_modifier(to_location("The Briny Deeps"), "Item Drop Penalty")));
            """.trimIndent(),
        )
        assertTrue(out.contains("Item Drop Penalty"))
        assertTrue(out.contains("-25"))
    }

    @Test
    fun corpus_outfitModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "25.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier("Outfit:Antique Nutcracker Outfit", "Muscle")));""",
            ),
        )
    }

    @Test
    fun corpus_signModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "1.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier("Sign:Marmot", "Cold Resistance")));""",
            ),
        )
    }

    @Test
    fun corpus_currentNumericModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        char.updateEquipment(net.sourceforge.kolmafia.character.EquipmentSlot.ACC1, "Jarlsberg's earring")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "10.0",
            outputLib(lib, """print(to_string(numeric_modifier("Mysticality")));"""),
        )
    }

    @Test
    fun corpus_elementModifier_live() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(sign = "Marmot"))
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "1.0",
            outputLib(lib, """print(to_string(numeric_modifier(to_element("cold"), "Cold Resistance")));"""),
        )
    }

    @Test
    fun corpus_numericsModifier_effectDuration() = runBlocking {
        val effectsJson = """{"1":{"name":"Adventurerlike","duration":10}}"""
        val client = HttpClient(MockEngine {
            respond(effectsJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val effectMgr = EffectManager(client, net.sourceforge.kolmafia.event.GameEventBus())
        effectMgr.fetchEffects()
        val lib = GameRuntimeLibrary(effectManager = effectMgr)
        assertTrue(outputLib(lib, """print(to_string(numerics_modifier("Effect Duration")));""").contains("10"))
    }
}
