package net.sourceforge.kolmafia.character

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.EquipmentSlot.HAT
import net.sourceforge.kolmafia.character.EquipmentSlot.WEAPON
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest

class CharacterStatusRefreshTest {

    @AfterTest
    fun tearDown() {
        CharpaneValhallaSync.reset()
    }

    @Test
    fun needsCharpaneFallback_noobcore() {
        val state = CharacterState(challengePath = AscensionPath.GELATINOUS_NOOB.apiName)
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_pokefam() {
        val state = CharacterState(challengePath = AscensionPath.POKEFAM.apiName)
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_disguise() {
        val state = CharacterState(challengePath = AscensionPath.DISGUISES_DELIMIT.apiName)
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_spelunkyLimitMode() {
        val state = CharacterState(limitMode = "spelunky")
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_batmanLimitMode() {
        val state = CharacterState(limitMode = "batman")
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_transfunctionerEquipped() {
        val state = CharacterState(
            equipment = mapOf(HAT to CharpaneStatusSync.TRANSFUNCTIONER_NAME),
        )
        assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_normalStandardPath() {
        val state = CharacterState(
            challengePath = AscensionPath.STANDARD.apiName,
            limitMode = "none",
            equipment = mapOf(WEAPON to "titanium assault umbrella"),
        )
        assertFalse(CharacterStatusRefresh.needsCharpaneFallback(state))
    }

    @Test
    fun needsCharpaneFallback_inValhalla() {
        try {
            CharpaneValhallaSync.apply(
                KoLCharacter(),
                """<img src="otherimages/spirit.gif">""",
                preferences = null,
                effectManager = null,
            )
            val state = CharacterState(
                challengePath = AscensionPath.STANDARD.apiName,
                limitMode = "none",
            )
            assertTrue(CharacterStatusRefresh.needsCharpaneFallback(state))
        } finally {
            CharpaneValhallaSync.reset()
        }
    }

    @Test
    fun refresh_valhallaHtml_appliesSpiritState() = runBlocking {
        CharpaneValhallaSync.apply(
            KoLCharacter(),
            """<img src="otherimages/spirit.gif">""",
            preferences = null,
            effectManager = null,
        )
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(level = "15", buffedmus = "100", meat = "5000", adventures = "40"),
        )
        val prefs = Preferences(MapSettings())
        val html = """
            <br>Lvl. <img src="otherimages/spirit.gif">
            Karma: <b>77</b>
        """.trimIndent()
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("charpane.php") ->
                    respond(html, HttpStatusCode.OK)
                else -> respond("{}", HttpStatusCode.OK)
            }
        })
        val ok = CharacterStatusRefresh.refresh(
            characterRequest = CharacterRequest(client),
            character = character,
            effectManager = null,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(CharpaneValhallaSync.inValhalla)
        assertEquals(1, character.state.value.buffedMusc)
        assertEquals(0, character.state.value.meat)
        assertEquals(77, prefs.getInt("bankedKarma"))
    }

    @Test
    fun refresh_inValhallaFlag_usesCharpaneNotApi() = runBlocking {
        CharpaneValhallaSync.apply(
            KoLCharacter(),
            """<img src="otherimages/spirit.gif">""",
            preferences = null,
            effectManager = null,
        )
        var apiStatusCalls = 0
        var charpaneCalls = 0
        val character = KoLCharacter()
        character.updateFromApiResponse(CharacterApiResponse(level = "15"))
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.parameters["what"] == "status" -> {
                    apiStatusCalls++
                    respond("{}", HttpStatusCode.OK)
                }
                request.url.encodedPath.endsWith("charpane.php") -> {
                    charpaneCalls++
                    respond(
                        """<img src="otherimages/spirit.gif"> Karma: <b>10</b>""",
                        HttpStatusCode.OK,
                    )
                }
                else -> respond("{}", HttpStatusCode.OK)
            }
        })
        CharacterStatusRefresh.refresh(
            characterRequest = CharacterRequest(client),
            character = character,
            effectManager = null,
            preferences = null,
        )
        assertEquals(0, apiStatusCalls)
        assertEquals(1, charpaneCalls)
    }
}
