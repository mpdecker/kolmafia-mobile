package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
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
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP499Test {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    private fun registerStolenAccordion() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 11,
                name = "stolen accordion",
                descId = "d11",
                image = "accordion.gif",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    @Test
    fun revision_phase499() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun accordions_emptyPref_starterNoNo() {
        registerStolenAccordion()
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(lib, """cli_execute("accordions");""")
        assertTrue(out.lines().any { it.contains("stolen accordion | no/no | starter item") })
    }

    @Test
    fun accordions_inventoryAndPref_starterYesYes() {
        registerStolenAccordion()
        val prefs = Preferences(MapSettings())
        prefs.setString("_stolenAccordions", "11")
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(
                    items = mapOf(11 to InventoryItem(11, "stolen accordion", 1, ItemType.WEAPON)),
                ),
            ).asStateFlow()
        }
        val lib = GameRuntimeLibrary(preferences = prefs, inventoryManager = inv)
        val out = outputLib(lib, """cli_execute("accordions");""")
        assertTrue(out.lines().any { it.contains("stolen accordion | yes/yes | starter item") })
    }
}
