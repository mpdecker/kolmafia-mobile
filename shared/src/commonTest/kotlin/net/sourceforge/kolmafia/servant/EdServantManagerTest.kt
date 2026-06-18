package net.sourceforge.kolmafia.servant

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class EdServantManagerTest {

    private fun edManager(prefs: Preferences, char: KoLCharacter): EdServantManager =
        EdServantManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), prefs, char)

    private fun edCharacter(): KoLCharacter = KoLCharacter().also {
        it.updateFromApiResponse(
            CharacterApiResponse(
                name = "Test",
                classId = "7",
                path = AscensionPath.ACTUALLY_ED_THE_UNDYING.apiName,
            ),
        )
    }

    @Test
    fun addCombatExperience_incrementsActiveServantOnWin() {
        val prefs = Preferences(MapSettings())
        val char = edCharacter()
        val manager = edManager(prefs, char)
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 1, 0))
        prefs.setString(EdServantManager.ACTIVE_SERVANT_PREF, "Cat")
        manager.addCombatExperience()
        assertEquals(1, manager.findEdServant("Cat")?.experience)
    }

    @Test
    fun printStatus_includesLevelAndXp() {
        val prefs = Preferences(MapSettings())
        val char = edCharacter()
        val manager = edManager(prefs, char)
        prefs.setString(EdServantManager.SERVANTS_PREF, "Cat")
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 14, 221))
        val lines = mutableListOf<String>()
        manager.printStatus { lines += it }
        assertEquals("Hethys, the Cat (level 14, 221 xp)", lines.first())
    }

    @Test
    fun hasRegisteredServant_requiresStoredRecord() {
        val prefs = Preferences(MapSettings())
        val char = edCharacter()
        val manager = edManager(prefs, char)
        prefs.setString(EdServantManager.SERVANTS_PREF, "Cat")
        assertEquals(false, manager.hasRegisteredServant("Cat"))
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 1, 0))
        assertEquals(true, manager.hasRegisteredServant("Cat"))
    }
}
