package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.EquipmentSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EquipmentRequestTest {

    @Test
    fun wearOutfit_succeedsOnPutOnMessage() = runTest {
        val client = HttpClient(MockEngine { respond("You put on your Mining Gear.", HttpStatusCode.OK) })
        assertTrue(EquipmentRequest(client).wearOutfit(8).isSuccess)
    }

    @Test
    fun wearOutfit_failsWithoutPutOnMessage() = runTest {
        val client = HttpClient(MockEngine { respond("You can't wear that.", HttpStatusCode.OK) })
        assertTrue(EquipmentRequest(client).wearOutfit(8).isFailure)
    }

    @Test
    fun unequipAll_succeeds() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        assertTrue(EquipmentRequest(client).unequipAll().isSuccess)
    }

    @Test
    fun parseOutfitOptions_extractsIdsAndNames() {
        val html = """
            <select name=whichoutfit>
              <option value='8'>Mining Gear</option>
              <option value='-3'>My Custom</option>
              <option value='-99'>Your Previous Outfit</option>
            </select>
        """.trimIndent()
        val options = EquipmentRequest(HttpClient(MockEngine { respond("") })).parseOutfitOptions(html)
        assertEquals(3, options.size)
        assertEquals(-3, options[1].first)
        assertEquals("My Custom", options[1].second)
    }

    @Test
    fun equipItem_succeeds() = runTest {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        assertTrue(EquipmentRequest(client).equipItem(123, EquipmentSlot.HAT).isSuccess)
    }

    @Test
    fun saveCustomOutfit_returnsNegativeId() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                "Your custom outfit has been saved <!-- outfitid: 42 -->",
                HttpStatusCode.OK
            )
        })
        val result = EquipmentRequest(client).saveCustomOutfit("Backup")
        assertTrue(result.isSuccess)
        assertEquals(-42, result.getOrNull())
    }
}
