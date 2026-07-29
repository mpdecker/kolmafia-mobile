package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.PulverizeRequest
import net.sourceforge.kolmafia.request.UntinkerRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

class CleanupJunkRunnerTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
        NpcStoreDatabase.resetForTest()
        UntinkerRequest.resetForTest()
    }

    @Test
    fun cleanup_untinkersBeforeAutosell() = runTest {
        registerItem(COMBINE_ITEM, "combining item")
        registerItem(SELL_ITEM, "sell me")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "combining item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("meat paste", 1)),
            ),
        )

        val calls = mutableListOf<String>()
        val inventory = fakeInventory(
            mapOf(
                COMBINE_ITEM to item(COMBINE_ITEM, "combining item", 2),
                SELL_ITEM to item(SELL_ITEM, "sell me", 3),
            ),
        )
        val junkList = JunkListManager(GameDatabase()).also {
            it.loadFromNamesForTest(listOf("combining item", "sell me"))
        }
        val runner = runner(
            junkList = junkList,
            inventory = inventory,
            calls = calls,
            canUntinker = true,
        )

        runner.cleanup()

        assertTrue(calls.indexOf("untinker:$COMBINE_ITEM") < calls.indexOf("autosell:$SELL_ITEM"))
    }

    @Test
    fun cleanup_pulverizeRequiresPowerGate() = runTest {
        registerWeapon(WEAK_WEAPON, "weak sword", power = 50)
        registerWeapon(STRONG_WEAPON, "strong sword", power = 120)
        registerItem(PulverizeRequest.TENDER_HAMMER, "tenderizing hammer")

        val calls = mutableListOf<String>()
        val inventory = fakeInventory(
            mapOf(
                WEAK_WEAPON to item(WEAK_WEAPON, "weak sword", 1),
                STRONG_WEAPON to item(STRONG_WEAPON, "strong sword", 1),
                PulverizeRequest.TENDER_HAMMER to item(
                    PulverizeRequest.TENDER_HAMMER,
                    "tenderizing hammer",
                    1,
                ),
            ),
        )
        val junkList = JunkListManager(GameDatabase()).also {
            it.loadFromNamesForTest(listOf("weak sword", "strong sword"))
        }
        val runner = runner(
            junkList = junkList,
            inventory = inventory,
            calls = calls,
            hasPulverize = true,
            characterClass = CharacterClass.PASTAMANCER.id,
        )

        runner.cleanup()

        assertEquals(false, calls.any { it == "pulverize:$WEAK_WEAPON" })
        assertEquals(true, calls.any { it == "pulverize:$STRONG_WEAPON" })
    }

    @Test
    fun cleanup_roninKeepsOneWhenAutoselling() = runTest {
        registerItem(SELL_ITEM, "sell me")

        val calls = mutableListOf<Int>()
        val inventory = fakeInventory(
            mapOf(SELL_ITEM to item(SELL_ITEM, "sell me", 4)),
        )
        val junkList = JunkListManager(GameDatabase()).also {
            it.loadFromNamesForTest(listOf("sell me"))
        }
        val character = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Player",
                    classId = CharacterClass.SEAL_CLUBBER.id.toString(),
                    roninleft = "5",
                ),
            )
        }
        val runner = runner(
            junkList = junkList,
            inventory = inventory,
            calls = calls,
            character = character,
            trackAutosellQty = true,
        )

        runner.cleanup()

        assertEquals(listOf(3), calls)
    }

    @Test
    fun cleanup_stashesSingletonInClosetBeforeAutosell() = runTest {
        registerItem(SINGLETON_ITEM, "potted sporeling")
        registerItem(SELL_ITEM, "sell me")

        val closetPutCalls = mutableListOf<Pair<Int, Int>>()
        val closet = object : ClosetRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
            override suspend fun fetchContents(): Map<Int, Int> = emptyMap()
            override suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
                closetPutCalls += itemId to quantity
                return Result.success("ok")
            }
        }

        val calls = mutableListOf<String>()
        val inventory = fakeInventory(
            mapOf(
                SINGLETON_ITEM to item(SINGLETON_ITEM, "potted sporeling", 1),
                SELL_ITEM to item(SELL_ITEM, "sell me", 1),
            ),
        )
        val junkList = JunkListManager(GameDatabase()).also {
            it.loadListsForTest(
                junkNames = listOf("potted sporeling", "sell me"),
                singletonNames = listOf("potted sporeling"),
            )
        }
        val runner = runner(
            junkList = junkList,
            inventory = inventory,
            calls = calls,
            closetRequest = closet,
        )

        runner.cleanup()

        assertEquals(listOf(SINGLETON_ITEM to 1), closetPutCalls)
        assertTrue(calls.any { it == "autosell:$SELL_ITEM" })
    }

    @Test
    fun cleanup_skipsMementoInAutosell() = runTest {
        registerItem(MEMENTO_ITEM, "black onyx pendant")

        val calls = mutableListOf<String>()
        val inventory = fakeInventory(
            mapOf(MEMENTO_ITEM to item(MEMENTO_ITEM, "black onyx pendant", 2)),
        )
        val junkList = JunkListManager(GameDatabase()).also {
            it.loadListsForTest(
                junkNames = listOf("black onyx pendant"),
                mementoNames = listOf("black onyx pendant"),
            )
        }
        val runner = runner(
            junkList = junkList,
            inventory = inventory,
            calls = calls,
        )

        runner.cleanup()

        assertEquals(emptyList(), calls)
    }

    private fun runner(
        junkList: JunkListManager,
        inventory: InventoryManager,
        calls: MutableList<out Any>,
        canUntinker: Boolean = false,
        hasPulverize: Boolean = false,
        characterClass: Int = CharacterClass.PASTAMANCER.id,
        character: KoLCharacter = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Player",
                    classId = characterClass.toString(),
                ),
            )
        },
        trackAutosellQty: Boolean = false,
        closetRequest: ClosetRequest? = null,
    ): CleanupJunkRunner {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val untinker = object : UntinkerRequest(client, inventory, gameDatabase = GameDatabase()) {
            override suspend fun canUntinker(): Boolean = canUntinker
            override suspend fun untinker(itemId: Int, quantity: Int): Result<Int> {
                (calls as MutableList<String>).add("untinker:$itemId")
                inventory.consumeItemLocally(itemId, quantity)
                return Result.success(quantity)
            }
        }
        val pulverize = object : PulverizeRequest(client, inventory) {
            override suspend fun pulverize(itemId: Int, quantity: Int): Result<Int> {
                (calls as MutableList<String>).add("pulverize:$itemId")
                return Result.success(quantity)
            }
        }
        val useItem = object : UseItemRequest(client) {
            override suspend fun use(itemId: Int, quantity: Int): Result<String> {
                (calls as MutableList<String>).add("use:$itemId")
                return Result.success("ok")
            }
        }
        val autosell = object : AutosellRequest(client) {
            override suspend fun autosell(itemId: Int, quantity: Int): Result<String> {
                if (trackAutosellQty) {
                    (calls as MutableList<Int>).add(quantity)
                } else {
                    (calls as MutableList<String>).add("autosell:$itemId")
                }
                return Result.success("ok")
            }
        }
        val skills = if (hasPulverize) {
            listOf(
                SkillData(
                    id = 1016,
                    name = "Pulverize",
                    type = SkillType.NONCOMBAT,
                    mpCost = 0,
                    dailyLimit = 0,
                    timesCast = 0,
                ),
            )
        } else {
            emptyList()
        }
        val skillManager = fakeSkillManager(skills, client)

        return CleanupJunkRunner(
            junkListManager = junkList,
            inventoryManager = inventory,
            untinkerRequest = untinker,
            pulverizeRequest = pulverize,
            useItemRequest = useItem,
            autosellRequest = autosell,
            skillManager = skillManager,
            character = character,
            gameDatabase = GameDatabase(),
            closetRequest = closetRequest,
        )
    }

    private fun fakeSkillManager(skills: List<SkillData>, client: HttpClient): SkillManager {
        val manager = SkillManager(client, SkillCastRequest(client), GameEventBus())
        skills.forEach { manager.learnLocalSkill(it) }
        return manager
    }

    private fun fakeInventory(items: Map<Int, InventoryItem>): InventoryManager =
        object : InventoryManager(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }), GameEventBus()) {
            private val flow = MutableStateFlow(InventoryState(items = items))
            override val state = flow.asStateFlow()
            override suspend fun fetchInventory() { /* no-op */ }
            override fun consumeItemLocally(itemId: Int, quantity: Int) {
                if (quantity <= 0) return
                val current = flow.value
                val item = current.items[itemId] ?: return
                val remaining = item.quantity - quantity
                val updated = current.items.toMutableMap()
                if (remaining <= 0) {
                    updated.remove(itemId)
                } else {
                    updated[itemId] = item.copy(quantity = remaining)
                }
                flow.value = current.copy(items = updated)
            }
        }

    private fun item(id: Int, name: String, qty: Int) =
        InventoryItem(id, name, qty, ItemType.OTHER)

    private fun registerItem(id: Int, name: String, primaryUse: ItemPrimaryUse = ItemPrimaryUse.NONE) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = primaryUse,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun registerWeapon(id: Int, name: String, power: Int) {
        registerItem(id, name, ItemPrimaryUse.WEAPON)
        EquipmentDatabase.registerForTest(
            id,
            net.sourceforge.kolmafia.data.EquipmentData(name, power, null, 1, "sword"),
        )
    }

    companion object {
        private const val COMBINE_ITEM = 9001
        private const val SELL_ITEM = 9002
        private const val WEAK_WEAPON = 9003
        private const val STRONG_WEAPON = 9004
        private const val SINGLETON_ITEM = 9005
        private const val MEMENTO_ITEM = 9006
    }
}
