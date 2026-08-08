package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.preferences.Preferences

class ClipArtCreateRequestTest {

    @Test
    fun create_postsCombineClipArtsFormFields() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("You acquire an item: <b>Ur-Donut</b>", HttpStatusCode.OK)
        })
        val request = ClipArtCreateRequest(client)
        val concoction = ConcoctionData(
            result = "Ur-Donut",
            resultQuantity = 1,
            methods = setOf("CLIPART"),
            ingredients = emptyList(),
            param = 0x020304,
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isSuccess)
        val body = bodies.single()
        assertTrue(body.contains("preaction=combinecliparts"), body)
        assertTrue(body.contains("clip1=2"), body)
        assertTrue(body.contains("clip2=3"), body)
        assertTrue(body.contains("clip3=4"), body)
    }

    @Test
    fun create_incrementsClipArtSummonPref() = runTest {
        val client = HttpClient(MockEngine { respond("You acquire an item.", HttpStatusCode.OK) })
        val prefs = Preferences(MapSettings())
        val request = ClipArtCreateRequest(client)
        val concoction = ConcoctionData(
            result = "Ur-Donut",
            resultQuantity = 1,
            methods = setOf("CLIPART"),
            ingredients = emptyList(),
            param = 0x010101,
        )

        request.create(concoction, 1, state = null, prefs)

        assertEquals(1, prefs.getInt("_clipartSummons", 0))
    }

    @Test
    fun create_missingAcquireText_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("nothing happened", HttpStatusCode.OK) })
        val request = ClipArtCreateRequest(client)
        val concoction = ConcoctionData(
            result = "Ur-Donut",
            resultQuantity = 1,
            methods = setOf("CLIPART"),
            ingredients = emptyList(),
            param = 0x010101,
        )

        val result = request.create(concoction, 1, state = null, preferences = null)

        assertTrue(result.isFailure)
    }

    @Test
    fun create_notPermitted_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("You acquire an item.", HttpStatusCode.OK) })
        val request = ClipArtCreateRequest(client)
        val concoction = ConcoctionData(
            result = "Ur-Donut",
            resultQuantity = 1,
            methods = setOf("CLIPART"),
            ingredients = emptyList(),
            param = 0x010101,
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("_clipartSummons", 3)

        val result = request.create(
            concoction,
            1,
            CharacterState(),
            prefs,
        )

        assertTrue(result.isFailure)
    }
}
