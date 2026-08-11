package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.PokeBoost
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode

class FamTeamRequestTest {

    private fun pokefamCharacter(): KoLCharacter =
        KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Pocket Familiars"))
        }

    @Test
    fun parseVisit_appliesTeamWhenInPokefam() = runTest {
        FamiliarDefinitionDatabase.load()
        val char = pokefamCharacter()
        val manager = FamiliarManager(HttpClient(MockEngine { respond("ok") }), GameEventBus())
        val html = """
            <div class="slot active" data-pos="1"><div class="fambox" data-id="215"><table><tbody><tr><td class=tiny>Lv. 12 Globmule</td></tr></tbody></table></div></div>
        """.trimIndent()
        FamTeamRequest.parseVisit(
            url = "famteam.php",
            html = html,
            character = char,
            familiarManager = manager,
        )
        assertEquals(215, char.pokeFamSlot(0).familiarId)
        assertEquals("Globmule", char.pokeFamSlot(0).name)
    }

    @Test
    fun parseVisit_skipsWhenNotInPokefam() = runTest {
        FamiliarDefinitionDatabase.load()
        val char = KoLCharacter()
        val html = """
            <div class="slot active" data-pos="1"><div class="fambox" data-id="215"><table><tbody><tr><td class=tiny>Lv. 12 Globmule</td></tr></tbody></table></div></div>
        """.trimIndent()
        FamTeamRequest.parseVisit(
            url = "famteam.php",
            html = html,
            character = char,
        )
        assertTrue(char.pokeFamSlot(0).isEmpty)
        assertTrue(char.pokeFamSlot(1).isEmpty)
        assertTrue(char.pokeFamSlot(2).isEmpty)
    }

    @Test
    fun visit_fetchesAndParsesFamteamPage() = runTest {
        FamiliarDefinitionDatabase.load()
        val char = pokefamCharacter()
        val html = """
            <div class="slot active" data-pos="1"><div class="fambox" data-id="216"><table><tbody><tr><td class=tiny>Lv. 8 Bluzzard</td></tr></tbody></table></div></div>
        """.trimIndent()
        val client = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })
        val result = FamTeamRequest.visit(client, char)
        assertTrue(result.isSuccess)
        assertEquals(216, char.pokeFamSlot(0).familiarId)
    }

    @Test
    fun feed_buildsPostAndSyncsBoostPref() = runTest {
        FamiliarDefinitionDatabase.load()
        val char = pokefamCharacter()
        val prefs = Preferences(MapSettings())
        val responseHtml = "<html><center>Familiar powered up.</center></html>"
        val client = HttpClient(MockEngine { respond(responseHtml, HttpStatusCode.OK) })
        val result = FamTeamRequest.feed(
            client = client,
            race = "Globmule",
            boost = PokeBoost.POWER,
            character = char,
            preferences = prefs,
        )
        assertTrue(result.isSuccess)
        assertEquals("Globmule:Power", prefs.getString("pokefamBoosts"))
    }
}
