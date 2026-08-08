package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConsumableQuality
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.preferences.Preferences

class StillSuitRequestTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun isDistillate_matchesName() {
        assertTrue(StillSuitRequest.isDistillate("stillsuit distillate"))
        assertFalse(StillSuitRequest.isDistillate("Imp Ale"))
    }

    @Test
    fun canMake_requiresStillsuitAndSweat() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", 10)
        assertTrue(
            StillSuitRequest.canMake(
                prefs = prefs,
                inventoryCountById = { if (it == StillSuitRequest.STILLSUIT_ITEM_ID) 1 else 0 },
                isEquipped = { false },
            ).isSuccess,
        )
        assertTrue(
            StillSuitRequest.canMake(
                prefs = prefs,
                inventoryCountById = { 0 },
                isEquipped = { it == StillSuitRequest.STILLSUIT_ITEM_ID },
            ).isSuccess,
        )
        assertTrue(
            StillSuitRequest.canMake(
                prefs = prefs,
                inventoryCountById = { 0 },
                isEquipped = { false },
            ).isFailure,
        )
        prefs.setInt("familiarSweat", 5)
        assertTrue(
            StillSuitRequest.canMake(
                prefs = prefs,
                inventoryCountById = { 1 },
                isEquipped = { false },
            ).isFailure,
        )
    }

    @Test
    fun distill_postsDistillThenChoice() = runTest {
        registerDistillateConcoction()
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            urls += req.url.toString()
            respond("ok")
        })
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", 10)
        val request = StillSuitRequest(client)

        val result = request.distill(
            name = StillSuitRequest.DISTILLATE_NAME,
            type = ConcoctionConsumptionType.DRINK,
            state = CharacterState(),
            prefs = prefs,
            inventoryCountById = { if (it == StillSuitRequest.STILLSUIT_ITEM_ID) 1 else 0 },
        )

        assertTrue(result.isSuccess)
        assertEquals(2, urls.size)
        assertTrue(urls[0].contains("inventory.php"))
        assertTrue(urls[0].contains("action=distill"))
        assertTrue(urls[1].contains("choice.php"))
        assertTrue(urls[1].contains("whichchoice=1476"))
        assertTrue(urls[1].contains("option=1"))
    }

    private fun registerDistillateConcoction() {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = StillSuitRequest.DISTILLATE_NAME,
                type = ConsumableType.DRINK,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.CRAPPY,
                advMin = 0,
                advMax = 0,
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
                result = StillSuitRequest.DISTILLATE_NAME,
                resultQuantity = 1,
                methods = setOf("STILLSUIT"),
                ingredients = emptyList(),
            ),
        )
    }
}
