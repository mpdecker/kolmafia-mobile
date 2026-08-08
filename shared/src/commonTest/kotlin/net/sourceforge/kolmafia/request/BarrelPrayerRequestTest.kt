package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.preferences.Preferences

class BarrelPrayerRequestTest {

    @AfterTest
    fun tearDown() {
        StandardRequest.resetForTest()
    }

    private fun formParam(body: String, key: String): String? =
        Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)

    private fun prefs(unlocked: Boolean = true) = Preferences(MapSettings()).apply {
        setBoolean("barrelShrineUnlocked", unlocked)
    }

    @Test
    fun findPrayer_resolvesNameAndRewardAliases() {
        assertEquals(BarrelChoiceMapper.OPTION_PROTECTION, BarrelChoiceMapper.findPrayer("protection"))
        assertEquals(BarrelChoiceMapper.OPTION_PROTECTION, BarrelChoiceMapper.findPrayer("barrel lid"))
        assertEquals(BarrelChoiceMapper.OPTION_GLAMOUR, BarrelChoiceMapper.findPrayer("GLAMOUR"))
        assertEquals(BarrelChoiceMapper.OPTION_VIGOR, BarrelChoiceMapper.findPrayer("bankruptcy barrel"))
        assertEquals(BarrelChoiceMapper.OPTION_BUFF, BarrelChoiceMapper.findPrayer("buff"))
        assertEquals(BarrelChoiceMapper.OPTION_BUFF, BarrelChoiceMapper.findPrayer("class buff"))
        assertEquals(0, BarrelChoiceMapper.findPrayer("unknown"))
        assertEquals(0, BarrelChoiceMapper.findPrayer(""))
    }

    @Test
    fun pray_option1_success_setsDailyAndAscensionPrefs() = runTest {
        val shrineVisits = mutableListOf<String>()
        val choicePosts = mutableListOf<Pair<Int, Int>>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("da.php?barrelshrine=1") -> {
                    shrineVisits += request.url.toString()
                    respond("shrine", HttpStatusCode.OK)
                }
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") -> {
                    val body = request.body.toByteArray().decodeToString()
                    val choiceId = formParam(body, "whichchoice")?.toIntOrNull() ?: -1
                    val option = formParam(body, "option")?.toIntOrNull() ?: -1
                    choicePosts += choiceId to option
                    respond("You acquire an item: <b>barrel lid</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val preferences = prefs()
        val request = BarrelPrayerRequest(client, ChoiceRequest(client))

        val result = request.pray(BarrelChoiceMapper.OPTION_PROTECTION, state = null, preferences)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
        assertEquals(1, shrineVisits.size)
        assertEquals(
            listOf(BarrelChoiceMapper.CHOICE_ID to BarrelChoiceMapper.OPTION_PROTECTION),
            choicePosts,
        )
        assertTrue(preferences.getBoolean("_barrelPrayer", false))
        assertTrue(preferences.getBoolean("prayedForProtection", false))
    }

    @Test
    fun pray_option4_success_setsDailyPrayerOnly() = runTest {
        val choicePosts = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("da.php?barrelshrine=1") ->
                    respond("shrine", HttpStatusCode.OK)
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") -> {
                    val body = request.body.toByteArray().decodeToString()
                    choicePosts += formParam(body, "option")?.toIntOrNull() ?: -1
                    respond("You feel barrel-chested.", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val preferences = prefs()
        val request = BarrelPrayerRequest(client, ChoiceRequest(client))

        val result = request.pray(BarrelChoiceMapper.OPTION_BUFF, state = null, preferences)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
        assertEquals(listOf(BarrelChoiceMapper.OPTION_BUFF), choicePosts)
        assertTrue(preferences.getBoolean("_barrelPrayer", false))
        assertFalse(preferences.getBoolean("prayedForProtection", false))
        assertFalse(preferences.getBoolean("prayedForGlamour", false))
        assertFalse(preferences.getBoolean("prayedForVigor", false))
    }

    @Test
    fun pray_shrineNotInstalled_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val preferences = prefs(unlocked = false)
        val request = BarrelPrayerRequest(client, ChoiceRequest(client))

        val result = request.pray(BarrelChoiceMapper.OPTION_BUFF, state = null, preferences)

        assertTrue(result.isFailure)
        assertEquals("Barrel Shrine not installed", result.exceptionOrNull()?.message)
    }

    @Test
    fun pray_alreadyPrayedToday_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val preferences = prefs().apply { setBoolean("_barrelPrayer", true) }
        val request = BarrelPrayerRequest(client, ChoiceRequest(client))

        val result = request.pray(BarrelChoiceMapper.OPTION_BUFF, state = null, preferences)

        assertTrue(result.isFailure)
        assertEquals(
            "You have already prayed to the Barrel God today.",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun pray_alreadyPrayedForProtection_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val preferences = prefs().apply { setBoolean("prayedForProtection", true) }
        val request = BarrelPrayerRequest(client, ChoiceRequest(client))

        val result = request.pray(BarrelChoiceMapper.OPTION_PROTECTION, state = null, preferences)

        assertTrue(result.isFailure)
        assertEquals(
            "You have already prayed for that item this ascension.",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun pray_kingdomOfExploathing_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val preferences = prefs()
        val state = CharacterState(challengePath = AscensionPath.KINGDOM_OF_EXPLOATHING.apiName)
        val request = BarrelPrayerRequest(client, ChoiceRequest(client))

        val result = request.pray(BarrelChoiceMapper.OPTION_BUFF, state, preferences)

        assertTrue(result.isFailure)
        assertEquals(
            "The barrel shrine has been blown to smithereens",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun pray_standardRestriction_returnsFailure() = runTest {
        StandardRequest.parseResponse(
            """
            <b>Items</b><p><span class="i">shrine to the Barrel god,</span><p>
            """.trimIndent(),
        )
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val preferences = prefs()
        val state = CharacterState(isHardcore = true, roninLeft = 0)
        val request = BarrelPrayerRequest(client, ChoiceRequest(client))

        val result = request.pray(BarrelChoiceMapper.OPTION_BUFF, state, preferences)

        assertTrue(result.isFailure)
        assertEquals(
            "Standard restrictions preclude you from approaching the Barrel Shrine",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun pray_option1_noAcquireResponse_returnsFalse() = runTest {
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("da.php?barrelshrine=1") ->
                    respond("shrine", HttpStatusCode.OK)
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") ->
                    respond("You kneel before the barrel.", HttpStatusCode.OK)
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val preferences = prefs()
        val request = BarrelPrayerRequest(client, ChoiceRequest(client))

        val result = request.pray(BarrelChoiceMapper.OPTION_PROTECTION, state = null, preferences)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
        assertFalse(preferences.getBoolean("_barrelPrayer", false))
    }
}
