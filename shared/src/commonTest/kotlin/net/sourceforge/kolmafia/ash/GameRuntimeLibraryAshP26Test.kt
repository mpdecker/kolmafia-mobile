package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.servant.EdServantManager
import net.sourceforge.kolmafia.servant.EdServantRecord
import net.sourceforge.kolmafia.servant.EdServantState

class GameRuntimeLibraryAshP26Test {

    @Test
    fun servantIsValid_acceptsCanonicalType() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_servant("Cat"))));""").trim())
        assertEquals("false", outputLib(lib, """print(to_string(is_valid(to_servant("skeleton"))));""").trim())
    }

    @Test
    fun toServant_resolvesKnownServant() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("Cat", outputLib(lib, """print(to_servant("Cat"));""").trim())
        assertEquals("", outputLib(lib, """print(to_servant("skeleton"));""").trim())
    }

    @Test
    fun haveServant_falseWithoutRegisteredRecord() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(to_string(have_servant(to_servant("Maid"))));""").trim())
        assertEquals("false", outputLib(lib, """print(to_string(have_servant(to_servant("bogus"))));""").trim())
    }

    @Test
    fun haveServant_trueAfterChoice1053SyncWithoutManualPref() = runBlocking {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(name = "Test", classId = "7", path = "Actually Ed the Undying"),
            )
        }
        val manager = EdServantManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), prefs, char)
        val html = """
            <b>Freed, but Lazy Servants</b><table>
            <tr><td valign=top><img src="itemimages/edserv1.gif"></td>
            <td>Hethys, the Cat<br><span></span></td>
            <td valign=top>(level 14, 221 xp)</td></tr>
            </table>
        """.trimIndent()
        manager.syncFromChoice1053(html)
        val lib = GameRuntimeLibrary(preferences = prefs, character = char, edServantManager = manager)
        assertEquals("true", outputLib(lib, """print(to_string(have_servant(to_servant("Cat"))));""").trim())
        assertEquals("false", outputLib(lib, """print(to_string(have_servant(to_servant("Maid"))));""").trim())
    }

    @Test
    fun toVykea_resolvesLevelCouch() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("level 3 couch", outputLib(lib, """print(to_vykea("level 3 couch"));""").trim())
    }

    @Test
    fun vykeaNumericModifier_couchMeatDrop() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "30.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_vykea("level 3 couch"), "Meat Drop")));""",
            ).trim(),
        )
    }

    @Test
    fun servantTypeOf_returnsServant() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("servant", outputLib(lib, """print(type_of(to_servant("Priest")));""").trim())
    }

    @Test
    fun vykeaTypeOf_returnsVykea() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("vykea", outputLib(lib, """print(type_of(to_vykea("level 1 lamp")));""").trim())
    }

    @Test
    fun useServant_ed_switchesActiveServant() = runBlocking {
        val prefs = Preferences(MapSettings())
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 1, 0))
        prefs.setString(EdServantManager.SERVANTS_PREF, "Cat")
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val client = HttpClient(engine)
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(name = "Test", classId = "7", path = "Actually Ed the Undying"),
            )
        }
        val manager = EdServantManager(client, prefs, char)
        val lib = GameRuntimeLibrary(preferences = prefs, character = char, edServantManager = manager)
        val out = outputLib(lib, """print(to_string(use_servant(to_servant("Cat"))));""")
        assertTrue(out.endsWith("true"), "expected success, got: $out")
        assertEquals("Cat", prefs.getString(EdServantManager.ACTIVE_SERVANT_PREF, ""))
    }
}
