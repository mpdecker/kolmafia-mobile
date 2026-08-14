package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType

class FoldItemRequestTest {

    @AfterTest
    fun tearDown() {
        FoldGroupDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun fold_usesInventoryPeerViaInvUse() = runTest {
        FoldGroupDatabase.registerGroupForTest(FoldGroup(0, listOf("fold-a", "fold-b")))
        ItemDatabase.registerForTest(
            ItemData(1, "fold-a", "", "", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        ItemDatabase.registerForTest(
            ItemData(2, "fold-b", "", "", ItemPrimaryUse.USABLE, emptySet(), emptySet(), 0, null),
        )
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls += request.url.toString()
            respond("folded", HttpStatusCode.OK)
        }
        val inv = object : InventoryManager(HttpClient(engine), GameEventBus()) {
            override val state = MutableStateFlow(
                InventoryState(
                    items = mapOf(2 to InventoryItem(2, "fold-b", 1, ItemType.OTHER)),
                ),
            ).asStateFlow()
        }
        val request = FoldItemRequest(
            client = HttpClient(engine),
            inventoryManager = inv,
        )
        val result = request.fold(1)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(urls.any { it.contains("inv_use.php") && it.contains("whichitem=2") }, urls.toString())
    }
}
