package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.servant.EdServantManager
import net.sourceforge.kolmafia.servant.EdServantRecord
import net.sourceforge.kolmafia.servant.EdServantState

class GameRuntimeLibraryAshP32Test {

    @Test
    fun servantBracketLevel_readsStoredRecord() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(name = "Test", classId = "7", path = "Actually Ed the Undying"),
            )
        }
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 14, 221))
        val manager = EdServantManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), prefs, char)
        val lib = GameRuntimeLibrary(preferences = prefs, character = char, edServantManager = manager)
        assertEquals("14", outputLib(lib, """print(to_servant("Cat")["level"]);""").trim())
        assertEquals("221", outputLib(lib, """print(to_servant("Cat")["experience"]);""").trim())
        assertEquals("Hethys", outputLib(lib, """print(to_servant("Cat")["name"]);""").trim())
        assertEquals("1", outputLib(lib, """print(to_servant("Cat")["id"]);""").trim())
    }

    @Test
    fun servantBracketAbility_readsCatalog() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """print(to_servant("Priest")["level7_ability"]);""").trim()
        assertEquals("Improves evocation spells", out)
    }
}
