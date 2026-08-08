package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ChezSnooteeDatabase
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConsumableQuality
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.data.HellKitchenDatabase
import net.sourceforge.kolmafia.data.MicroBreweryDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class CafePurchaseRequestTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun hellKitchenDatabase_resolvesPriceAndWhichItem() {
        val entry = HellKitchenDatabase.resolve("Imp Ale")
        assertEquals(470, entry?.whichItem)
        assertEquals(75, entry?.price)
        assertEquals(HellKitchenDatabase.CAFE_ID, entry?.cafeId)
    }

    @Test
    fun chezSnooteeDatabase_resolvesNegativeWhichItem() {
        val entry = ChezSnooteeDatabase.resolve("As Jus Gezund Heit")
        assertEquals(-2, entry?.whichItem)
        assertEquals(75, entry?.price)
    }

    @Test
    fun microBreweryDatabase_resolvesNegativeWhichItem() {
        val entry = MicroBreweryDatabase.resolve("Scrawny Stout")
        assertEquals(-2, entry?.whichItem)
        assertEquals(75, entry?.price)
    }

    @Test
    fun purchase_hellsKitchen_postsCafePhp() = runTest {
        registerConcoction("Jumbo Dr. Lucifer", ConsumableType.FOOD)
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You gain some stats.")
        })
        val purchase = buildPurchase(client)
        val state = CharacterState(zodiacSign = "Bad Moon", meat = 500)

        val result = purchase.purchase(
            name = "Jumbo Dr. Lucifer",
            type = ConcoctionConsumptionType.EAT,
            state = state,
            prefs = null,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, bodies.size)
        assertTrue(bodies.single().contains("cafeid=3"))
        assertTrue(bodies.single().contains("whichitem=571"))
    }

    @Test
    fun purchase_notOnMenu_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("ok") })
        val purchase = buildPurchase(client)

        val result = purchase.purchase(
            name = "unknown cafe food",
            type = ConcoctionConsumptionType.EAT,
            state = CharacterState(meat = 500),
            prefs = null,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CafeNotOnMenuException)
    }

    @Test
    fun purchase_insufficientMeat_failsBeforeHttp() = runTest {
        registerConcoction("Peche a la Frog", ConsumableType.FOOD)
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("ok")
        })
        val purchase = buildPurchase(client)
        val state = CharacterState(zodiacSign = "Blender", meat = 10)

        val result = purchase.purchase(
            name = "Peche a la Frog",
            type = ConcoctionConsumptionType.EAT,
            state = state,
            prefs = null,
        )

        assertTrue(result.isFailure)
        assertEquals(0, bodies.size)
    }

    private fun buildPurchase(client: HttpClient): CafePurchaseRequest {
        val cafeRequest = CafeRequest(client)
        val hellKitchen = HellKitchenRequest(cafeRequest)
        return CafePurchaseRequest(
            hellKitchenRequest = hellKitchen,
            chezSnooteeRequest = ChezSnooteeRequest(hellKitchen),
            microBreweryRequest = MicroBreweryRequest(hellKitchen),
        )
    }

    private fun registerConcoction(name: String, type: ConsumableType) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
                type = type,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = name,
                resultQuantity = 1,
                methods = emptySet(),
                ingredients = emptyList(),
            ),
        )
    }
}
