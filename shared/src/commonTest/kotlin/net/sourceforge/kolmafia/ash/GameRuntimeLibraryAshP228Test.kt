package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.PulverizeRequest
import net.sourceforge.kolmafia.request.UntinkerRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.session.CleanupJunkRunner
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class GameRuntimeLibraryAshP228Test {

    @Test
    fun revision_isphase222() {
        assertEquals("phase253", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cleanupJunk_cliInvokesRunner() = runTest {
        ItemDatabase.registerForTest(
            ItemData(
                id = JUNK_ITEM,
                name = "batgut",
                descId = "d$JUNK_ITEM",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )

        val inventory = object : InventoryManager(
            HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        JUNK_ITEM to InventoryItem(JUNK_ITEM, "batgut", 2, ItemType.OTHER),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
            override suspend fun fetchInventory() { /* no-op */ }
        }

        var autosold = 0
        val client = HttpClient(MockEngine { respond("sold", HttpStatusCode.OK) })
        val junkList = JunkListManager(GameDatabase()).also {
            it.loadFromNamesForTest(listOf("batgut"))
        }
        val runner = CleanupJunkRunner(
            junkListManager = junkList,
            inventoryManager = inventory,
            untinkerRequest = UntinkerRequest(client, inventory, gameDatabase = GameDatabase()),
            pulverizeRequest = PulverizeRequest(client, inventory),
            useItemRequest = UseItemRequest(client),
            autosellRequest = object : AutosellRequest(client) {
                override suspend fun autosell(itemId: Int, quantity: Int): Result<String> {
                    autosold = quantity
                    return Result.success("ok")
                }
            },
            skillManager = SkillManager(client, SkillCastRequest(client), GameEventBus()),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            gameDatabase = GameDatabase(),
        )
        val lib = GameRuntimeLibrary(
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
            cleanupJunkRunner = runner,
            preferences = prefs(),
        )

        runLib(lib, """cli_execute("cleanup junk");""")

        assertEquals(2, autosold)
    }

    companion object {
        private const val JUNK_ITEM = 9100
    }
}
