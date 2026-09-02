package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.maximizer.MaximizerManager
import net.sourceforge.kolmafia.request.EquipmentRequest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LastMaximizerSucceededTest {

    @AfterTest
    fun resetModifiers() {
        ModifierDatabase.resetForTest()
    }

    private class StubDb : GameDatabase() {
        override fun item(id: Int): ItemData? = when (id) {
            1 -> ItemData(1, "myst hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
            2 -> ItemData(2, "plain hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
            else -> null
        }
        override fun item(name: String): ItemData? = when (name.lowercase()) {
            "myst hat" -> item(1)
            "plain hat" -> item(2)
            else -> null
        }
        override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
            "myst hat" -> ModifierEntry("Item", "myst hat", "Mysticality: +5")
            "plain hat" -> ModifierEntry("Item", "plain hat", "Mysticality: +1")
            else -> null
        }

        fun syncTestItemModifiers() {
            runBlocking { ModifierDatabase.load() }
            for (name in listOf("myst hat", "plain hat")) {
                itemModifier(name)?.let { entry ->
                    ModifierDatabase.overrideModifier("Item", entry.name, entry.modifiers)
                }
            }
        }
    }

    private fun inventory(vararg items: InventoryItem): InventoryManager =
        object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = items.associateBy { it.itemId }),
            )
        }

    private fun equipment(character: KoLCharacter): EquipmentRequest =
        object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> =
                Result.success(Unit)
        }

    private fun library(
        equippedHat: String,
        items: Array<InventoryItem> = arrayOf(
            InventoryItem(1, "myst hat", 1, ItemType.HAT),
            InventoryItem(2, "plain hat", 1, ItemType.HAT),
        ),
    ): GameRuntimeLibrary {
        val db = StubDb().also { it.syncTestItemModifiers() }
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.HAT, equippedHat)
        val mgr = MaximizerManager(db, inventory(*items), equipment(character), character)
        return GameRuntimeLibrary(maximizerManager = mgr)
    }

    private fun lastSucceeded(lib: GameRuntimeLibrary): String =
        outputLib(lib, "print(to_string(last_maximizer_succeeded()));")

    @Test
    fun lastMaximizerSucceeded_falseWhenNeverRun() {
        val client = HttpClient(MockEngine { respond("ok") })
        val character = KoLCharacter()
        val mgr = object : MaximizerManager(
            GameDatabase(),
            InventoryManager(client, GameEventBus()),
            EquipmentRequest(client, character = character),
            character,
        ) {}
        val lib = GameRuntimeLibrary(maximizerManager = mgr)
        assertEquals("false", lastSucceeded(lib))
    }

    @Test
    fun lastMaximizerSucceeded_falseWithoutManager() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", lastSucceeded(lib))
    }

    @Test
    fun lastMaximizerSucceeded_falseOnInvalidGoal() {
        val lib = library(equippedHat = "plain hat")
        assertEquals("false", outputLib(lib, """print(to_string(maximize("")));"""))
        assertEquals("false", lastSucceeded(lib))
    }

    @Test
    fun lastMaximizerSucceeded_trueOnSuccessfulMaximize() {
        val lib = library(equippedHat = "plain hat")
        assertEquals("true", outputLib(lib, """print(to_string(maximize("mysticality")));"""))
        assertEquals("true", lastSucceeded(lib))
    }

    @Test
    fun lastMaximizerSucceeded_speculateNoImprovement_stillSucceededWhenScored() {
        val lib = library(
            equippedHat = "myst hat",
            items = arrayOf(InventoryItem(1, "myst hat", 1, ItemType.HAT)),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(maximize("mysticality", 0, 0, 0, "equip")));"""),
        )
        assertEquals("true", lastSucceeded(lib))
    }

    @Test
    fun lastMaximizerSucceeded_falseWhenMinNotHit() {
        val lib = library(equippedHat = "myst hat")
        outputLib(lib, """maximize("500 min, mysticality");""")
        assertEquals("false", lastSucceeded(lib))
    }

    @Test
    fun lastMaximizerSucceeded_resetsOnSubsequentInvalidGoal() {
        val lib = library(equippedHat = "plain hat")
        assertEquals("true", outputLib(lib, """print(to_string(maximize("mysticality")));"""))
        assertEquals("true", lastSucceeded(lib))
        assertEquals("false", outputLib(lib, """print(to_string(maximize("")));"""))
        assertEquals("false", lastSucceeded(lib))
    }
}
