package net.sourceforge.kolmafia.servant

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class EdServantHtmlFormatterTest {

    @Test
    fun buildServantsTable_includesAllTypesAndAbilities() {
        val html = EdServantHtmlFormatter.buildServantsTable(edManager())
        assertContains(html, "<table border=2")
        assertContains(html, ">Cat<")
        assertContains(html, ">Priest<")
        assertContains(html, "Level 1: Gives unpleasant gifts")
        assertContains(html, "Level 21:")
    }

    @Test
    fun buildServantsTable_showsSummonedRecord() {
        val prefs = prefs()
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 14, 221))
        val html = EdServantHtmlFormatter.buildServantsTable(
            EdServantManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), prefs, edCharacter()),
        )
        assertContains(html, "Hethys")
    }

    @Test
    fun buildCurrentServantLine_activeRecord() {
        val prefs = prefs()
        prefs.setString(EdServantManager.ACTIVE_SERVANT_PREF, "Cat")
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 14, 221))
        val line = EdServantHtmlFormatter.buildCurrentServantLine(
            EdServantManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), prefs, edCharacter()),
        )
        assertContains(line, "Your current servant is Hethys")
    }

    private fun prefs() = Preferences(MapSettings())

    private fun edCharacter(): KoLCharacter = KoLCharacter().also {
        it.updateFromApiResponse(
            CharacterApiResponse(
                name = "Test",
                classId = "7",
                path = AscensionPath.ACTUALLY_ED_THE_UNDYING.apiName,
            ),
        )
    }

    private fun edManager(): EdServantManager =
        EdServantManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), prefs(), edCharacter())
}
