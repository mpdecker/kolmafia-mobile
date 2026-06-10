package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TEST_ITEM = "test widget"
private const val TEST_ITEM_ID = 42

private fun stubDb(): GameDatabase = object : GameDatabase() {
    override fun item(id: Int) = if (id == TEST_ITEM_ID) ItemData(
        id = TEST_ITEM_ID, name = TEST_ITEM, descId = "", image = "",
        primaryUse = ItemPrimaryUse.NONE, secondaryUses = emptySet(),
        access = setOf('t', 'd'), autosellPrice = 10, plural = null
    ) else null
    override fun item(name: String) = if (name == TEST_ITEM) item(TEST_ITEM_ID) else null
}

class GameRuntimeLibraryCliOutfitBuyTest {

    @Test
    fun cliOutfit_wearCallsOutfitManager() = runTest {
        var worn: String? = null
        val manager = object : OutfitManager(
            retrieveItemService = null,
            equipmentRequest = EquipmentRequest(HttpClient(MockEngine { respond("You put on") })),
            customOutfitRequest = net.sourceforge.kolmafia.request.CustomOutfitRequest(HttpClient(MockEngine { respond("") })),
            character = KoLCharacter(),
            gameDatabase = stubDb(),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = null,
        ) {
            override suspend fun wearOutfit(name: String, postWear: ((String) -> Unit)?): Boolean {
                worn = name
                return true
            }
        }
        val lib = GameRuntimeLibrary(outfitManager = manager)
        runLib(lib, """cli_execute("outfit Test Mining");""")
        assertEquals("Test Mining", worn)
    }

    @Test
    fun cliBuy_wrapsPurchaseInCheckpoint() = runTest {
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, "old hat")
        var buyCount = 0
        var restored = false
        val dummyClient = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val mall = object : MallManager(MallSearchRequest(dummyClient), MallPurchaseRequest(dummyClient), null) {
            override suspend fun buy(itemId: Int, count: Int, maxPrice: Int): Int {
                buyCount = count
                return count
            }
        }
        val equip = object : EquipmentRequest(dummyClient) {
            override suspend fun unequipSlot(slot: EquipmentSlot): Result<Unit> {
                restored = true
                return Result.success(Unit)
            }
        }
        val lib = GameRuntimeLibrary(
            character = char,
            gameDatabase = stubDb(),
            mallManager = mall,
            equipmentRequest = equip,
        )
        runLib(lib, """cli_execute("buy 2 test widget@500");""")
        assertEquals(2, buyCount)
        assertTrue(restored, "Checkpoint should restore equipment after buy")
    }
}
