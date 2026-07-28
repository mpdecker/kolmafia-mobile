package net.sourceforge.kolmafia.inventory

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.ThriftyRequest
import net.sourceforge.kolmafia.request.TrendyRequest

class AccessibleItemCountTest {

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
    }

    private class FakeClosetRequest(
        private val contents: Map<Int, Int>,
    ) : ClosetRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
        override suspend fun fetchContents(): Map<Int, Int> = contents
    }

    private class FakeStorageRequest(
        private val contents: Map<Int, Int>,
    ) : StorageRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
        override suspend fun fetchRawContents(): Map<Int, Int> = contents
    }

    @Test
    fun physicalCount_sumsInventoryClosetStorageAndEquipped() = runTest {
        val itemId = 42
        val inv = TestInventoryManager(
            mapOf(itemId to InventoryItem(itemId, "test item", 2, ItemType.OTHER)),
        )
        val total = AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = "test item",
            inventoryManager = inv,
            closetRequest = FakeClosetRequest(mapOf(itemId to 3)),
            storageRequest = FakeStorageRequest(mapOf(itemId to 5)),
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = mapOf(EquipmentSlot.WEAPON to "test item"),
        )
        assertEquals(11, total)
    }

    @Test
    fun physicalCount_worthlessItemId_sumsComponents() = runTest {
        val inv = TestInventoryManager(
            mapOf(
                HermitRequest.WORTHLESS_TRINKET_ID to InventoryItem(
                    HermitRequest.WORTHLESS_TRINKET_ID, "trinket", 2, ItemType.OTHER,
                ),
                HermitRequest.WORTHLESS_GEWGAW_ID to InventoryItem(
                    HermitRequest.WORTHLESS_GEWGAW_ID, "gewgaw", 1, ItemType.OTHER,
                ),
            ),
        )
        val total = AccessibleItemCount.physicalCount(
            itemId = HermitRequest.WORTHLESS_ITEM_ID,
            itemName = "worthless item",
            inventoryManager = inv,
            closetRequest = FakeClosetRequest(mapOf(HermitRequest.WORTHLESS_KNICK_KNACK_ID to 3)),
            storageRequest = FakeStorageRequest(emptyMap()),
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = emptyMap(),
        )
        assertEquals(6, total)
    }

    @Test
    fun physicalCount_lolPath_excludesNonPullableStorage() = runTest {
        val itemId = 501
        val weapon = ItemData(
            id = itemId,
            name = "blocked sword",
            descId = "desc501",
            image = "s.gif",
            primaryUse = ItemPrimaryUse.WEAPON,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = if (id == itemId) weapon else null
        }
        val inv = TestInventoryManager(
            mapOf(itemId to InventoryItem(itemId, "blocked sword", 1, ItemType.OTHER)),
        )
        val context = AccessCountContext(
            characterState = CharacterState(challengePath = "Legacy of Loathing"),
            gameDatabase = db,
        )
        val total = AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = "blocked sword",
            inventoryManager = inv,
            closetRequest = null,
            storageRequest = FakeStorageRequest(mapOf(itemId to 8)),
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = emptyMap(),
            context = context,
        )
        assertEquals(1, total)
    }

    @Test
    fun physicalCount_hatTrick_addsExtraHatIds() = runTest {
        val hatId = 8801
        val hat = ItemData(
            id = hatId,
            name = "fancy hat",
            descId = "desc8801",
            image = "h.gif",
            primaryUse = ItemPrimaryUse.HAT,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = if (id == hatId) hat else null
        }
        val context = AccessCountContext(
            characterState = CharacterState(
                challengePath = AscensionPath.HAT_TRICK.apiName,
                hatTrickHatIds = listOf(hatId, hatId),
            ),
            gameDatabase = db,
        )
        val total = AccessibleItemCount.physicalCount(
            itemId = hatId,
            itemName = "fancy hat",
            inventoryManager = TestInventoryManager(emptyMap()),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = emptyMap(),
            context = context,
        )
        assertEquals(2, total)
    }

    @Test
    fun physicalCount_inactiveFamiliarEquipment_countsCopy() = runTest {
        val itemId = 9901
        val familiarItem = InventoryItem(itemId, "pet sweater", 1, ItemType.FAMILIAR_ITEM)
        val mgr = FamiliarManager(
            HttpClient(MockEngine { respond("[]", HttpStatusCode.OK) }),
            GameEventBus(),
        )
        mgr.testSetState(
            FamiliarState(
                activeFamiliar = FamiliarData(
                    id = 1, name = "Active", race = "Grue", weight = 10, experience = 0, kills = 0,
                ),
                ownedFamiliars = listOf(
                    FamiliarData(
                        id = 1, name = "Active", race = "Grue", weight = 10, experience = 0, kills = 0,
                    ),
                    FamiliarData(
                        id = 2, name = "Idle", race = "Bunny", weight = 5, experience = 0, kills = 0,
                        equipment = familiarItem,
                    ),
                ),
            ),
        )
        val context = AccessCountContext(
            characterState = CharacterState(familiarId = 1),
            familiarManager = mgr,
        )
        val total = AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = "pet sweater",
            inventoryManager = TestInventoryManager(emptyMap()),
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = emptyMap(),
            context = context,
        )
        assertEquals(1, total)
    }

    @Test
    fun physicalCount_thriftyPath_excludesNonAllowedStorage() = runTest {
        ThriftyRequest.resetForTest()
        ThriftyRequest.parseResponse(
            """<b>Items</b><p><span class="i">allowed food,</span><span class="i">other thing</span><p>""",
        )
        val itemId = 601
        val item = ItemData(
            id = itemId,
            name = "blocked thrifty item",
            descId = "desc601",
            image = "x.gif",
            primaryUse = ItemPrimaryUse.USABLE,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = if (id == itemId) item else null
        }
        val context = AccessCountContext(
            characterState = CharacterState(challengePath = "Thrifty"),
            gameDatabase = db,
        )
        val total = AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = "blocked thrifty item",
            inventoryManager = TestInventoryManager(emptyMap()),
            closetRequest = null,
            storageRequest = FakeStorageRequest(mapOf(itemId to 4)),
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = emptyMap(),
            context = context,
        )
        assertEquals(0, total)
    }

    @Test
    fun physicalCount_roninFreepull_countsFreepullStorage() = runTest {
        val itemId = 3220
        ItemDatabase.registerForTest(
            ItemData(
                id = itemId,
                name = "hobo code binder",
                descId = "desc3220",
                image = "book2.gif",
                primaryUse = ItemPrimaryUse.OFFHAND,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest("Item", "hobo code binder", "Free Pull")
        val context = AccessCountContext(
            characterState = CharacterState(isHardcore = true, roninLeft = 3),
        )
        val total = AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = "hobo code binder",
            inventoryManager = TestInventoryManager(emptyMap()),
            closetRequest = null,
            storageRequest = FakeStorageRequest(mapOf(itemId to 2)),
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = emptyMap(),
            context = context,
        )
        assertEquals(2, total)
    }

    @Test
    fun physicalCount_restrictedItem_returnsZero() = runTest {
        StandardRequest.resetForTest()
        StandardRequest.parseResponse(
            """<b>Items</b><p><span class="i">banned widget,</span><span class="i">other</span><p>""",
        )
        val itemId = 701
        val item = ItemData(
            id = itemId,
            name = "banned widget",
            descId = "desc701",
            image = "w.gif",
            primaryUse = ItemPrimaryUse.USABLE,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals("banned widget", ignoreCase = true)) item else null
            override fun item(id: Int): ItemData? = if (id == itemId) item else null
        }
        val total = AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = "banned widget",
            inventoryManager = TestInventoryManager(
                mapOf(itemId to InventoryItem(itemId, "banned widget", 5, ItemType.OTHER)),
            ),
            closetRequest = FakeClosetRequest(mapOf(itemId to 2)),
            storageRequest = FakeStorageRequest(mapOf(itemId to 9)),
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = emptyMap(),
            context = AccessCountContext(
                characterState = CharacterState(isHardcore = true, roninLeft = 0),
                gameDatabase = db,
            ),
        )
        assertEquals(0, total)
    }

    @Test
    fun physicalCount_trendyExpiredItem_returnsZero() = runTest {
        TrendyRequest.resetForTest()
        TrendyRequest.parseResponse(
            """
            <tr class="expired">
            <td>2004-12</td><td>Items</td><td>old trendy widget</td></tr>
            """.trimIndent(),
        )
        val itemId = 702
        val item = ItemData(
            id = itemId,
            name = "old trendy widget",
            descId = "desc702",
            image = "w.gif",
            primaryUse = ItemPrimaryUse.USABLE,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals("old trendy widget", ignoreCase = true)) item else null
            override fun item(id: Int): ItemData? = if (id == itemId) item else null
        }
        val total = AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = "old trendy widget",
            inventoryManager = TestInventoryManager(
                mapOf(itemId to InventoryItem(itemId, "old trendy widget", 3, ItemType.OTHER)),
            ),
            closetRequest = null,
            storageRequest = FakeStorageRequest(mapOf(itemId to 4)),
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = emptyMap(),
            context = AccessCountContext(
                characterState = CharacterState(challengePath = "Trendy", kingLiberated = false),
                gameDatabase = db,
            ),
        )
        assertEquals(0, total)
    }

    @Test
    fun physicalCount_thriftySeasonalItem_returnsZero() = runTest {
        ModifierDatabase.injectForTest(
            "Item",
            "seasonal widget",
            """Last Available: "2019-03"""",
        )
        val itemId = 703
        val item = ItemData(
            id = itemId,
            name = "seasonal widget",
            descId = "desc703",
            image = "w.gif",
            primaryUse = ItemPrimaryUse.USABLE,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals("seasonal widget", ignoreCase = true)) item else null
            override fun item(id: Int): ItemData? = if (id == itemId) item else null
        }
        val total = AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = "seasonal widget",
            inventoryManager = TestInventoryManager(
                mapOf(itemId to InventoryItem(itemId, "seasonal widget", 2, ItemType.OTHER)),
            ),
            closetRequest = null,
            storageRequest = FakeStorageRequest(mapOf(itemId to 6)),
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = emptyMap(),
            context = AccessCountContext(
                characterState = CharacterState(challengePath = "Thrifty"),
                gameDatabase = db,
            ),
        )
        assertEquals(0, total)
    }
}
