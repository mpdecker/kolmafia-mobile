package net.sourceforge.kolmafia.servant

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class EdServantCharpaneSyncTest {

    @Test
    fun syncFromCharpane_updatesActiveServantPref() {
        val prefs = Preferences(MapSettings())
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(name = "Test", classId = "7", path = AscensionPath.ACTUALLY_ED_THE_UNDYING.apiName),
            )
        }
        val manager = EdServantManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), prefs, char)
        val html = """
            <b>Servant:</b> <a target="mainpane">Mittens (lvl 3)</a><img src="edserv1.gif">
        """.trimIndent()
        manager.syncFromCharpane(html)
        assertEquals("Cat", prefs.getString(EdServantManager.ACTIVE_SERVANT_PREF, ""))
    }

    @Test
    fun myServant_prefersEdServantManagerActiveType() {
        val prefs = Preferences(MapSettings())
        prefs.setString(EdServantManager.ACTIVE_SERVANT_PREF, "Priest")
        prefs.setString("_currentServant", "Cat")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(name = "Test", classId = "7", path = AscensionPath.ACTUALLY_ED_THE_UNDYING.apiName),
            )
        }
        val manager = EdServantManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), prefs, char)
        val lib = net.sourceforge.kolmafia.ash.GameRuntimeLibrary(
            preferences = prefs,
            character = char,
            edServantManager = manager,
        )
        assertEquals(
            "Priest",
            net.sourceforge.kolmafia.ash.outputLib(lib, "print(my_servant());").trim(),
        )
    }

    @Test
    fun parseActiveServantType_compactCharpane() {
        val html = """
            <b>Servant:</b> your loyal <a href="choice.php" target="mainpane">Fluffy (lvl 5)</a>
            <img src="itemimages/edserv1.gif">
        """.trimIndent()
        assertEquals("Cat", EdServantCharpaneSync.parseActiveServantType(html))
    }

    @Test
    fun parseActiveServantType_expandedCharpane() {
        val html = """
            <b>Servant:</b> your loyal <a href="choice.php" target="mainpane">Priestess the 12 level</a>
            <img src="https://images.kingdomofloathing.com/itemimages/edserv6.gif">
        """.trimIndent()
        assertEquals("Priest", EdServantCharpaneSync.parseActiveServantType(html))
    }

    @Test
    fun parseActiveServantType_returnsNullWhenMissing() {
        assertNull(EdServantCharpaneSync.parseActiveServantType("<html><body>No servant here</body></html>"))
    }
}
