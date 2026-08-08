package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.preferences.Preferences

class BarrelCreateRequestTest {

    private fun formParam(body: String, key: String): String? =
        Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)

    private fun barrelLidConcoction() = ConcoctionData(
        result = "barrel lid",
        resultQuantity = 1,
        methods = setOf("BARREL"),
        ingredients = emptyList(),
    )

    @Test
    fun create_barrelLid_visitsShrineAndPostsChoice1100Option1() = runTest {
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
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("barrelShrineUnlocked", true)
        }
        val request = BarrelCreateRequest(
            client = client,
            choiceRequest = ChoiceRequest(client),
            preferences = prefs,
        )

        val result = request.create(barrelLidConcoction(), 1, state = null, preferences = prefs)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(1, shrineVisits.size)
        assertTrue(shrineVisits.single().contains("barrelshrine=1"))
        assertEquals(listOf(BarrelChoiceMapper.CHOICE_ID to BarrelChoiceMapper.OPTION_PROTECTION), choicePosts)
        assertTrue(prefs.getBoolean("_barrelPrayer", false))
        assertTrue(prefs.getBoolean("prayedForProtection", false))
    }

    @Test
    fun create_quantityTwo_capsToOneChoice() = runTest {
        var choiceCount = 0
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("da.php?barrelshrine=1") ->
                    respond("shrine", HttpStatusCode.OK)
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") -> {
                    choiceCount++
                    respond("You acquire an item: <b>barrel lid</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("barrelShrineUnlocked", true)
        }
        val request = BarrelCreateRequest(
            client = client,
            choiceRequest = ChoiceRequest(client),
        )

        val result = request.create(barrelLidConcoction(), 2, state = null, preferences = prefs)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        assertEquals(1, choiceCount)
    }

    @Test
    fun create_alreadyPrayedForProtection_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("barrelShrineUnlocked", true)
            setBoolean("prayedForProtection", true)
        }
        val request = BarrelCreateRequest(
            client = client,
            choiceRequest = ChoiceRequest(client),
        )

        val result = request.create(barrelLidConcoction(), 1, state = null, preferences = prefs)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not available") == true)
    }

    @Test
    fun create_barrelPrayerAlreadyUsed_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("barrelShrineUnlocked", true)
            setBoolean("_barrelPrayer", true)
        }
        val request = BarrelCreateRequest(
            client = client,
            choiceRequest = ChoiceRequest(client),
        )

        val result = request.create(barrelLidConcoction(), 1, state = null, preferences = prefs)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("not available") == true)
    }

    @Test
    fun create_noAcquireResponse_returnsZero() = runTest {
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("da.php?barrelshrine=1") ->
                    respond("shrine", HttpStatusCode.OK)
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") ->
                    respond("You kneel before the barrel.", HttpStatusCode.OK)
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val prefs = Preferences(MapSettings()).apply {
            setBoolean("barrelShrineUnlocked", true)
        }
        val request = BarrelCreateRequest(
            client = client,
            choiceRequest = ChoiceRequest(client),
        )

        val result = request.create(barrelLidConcoction(), 1, state = null, preferences = prefs)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
        assertFalse(prefs.getBoolean("_barrelPrayer", false))
    }
}
