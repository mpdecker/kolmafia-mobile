package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AutumnatonChoiceSync
import net.sourceforge.kolmafia.quest.ColdMedicineChoiceSync

/** Phases 1011–1022 IoTM facility CLI corpus. */
class GameRuntimeLibraryIotmCliTest {

    private fun invWith(items: Map<Int, InventoryItem>): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(InventoryState(items = items))
            override val state = flow.asStateFlow()
        }

    @Test
    fun autumnaton_status_ready() {
        val p = Preferences(MapSettings())
        p.setBoolean("hasAutumnaton", true)
        val inv = invWith(
            mapOf(
                AutumnatonChoiceSync.AUTUMNATON_ITEM_ID to InventoryItem(
                    AutumnatonChoiceSync.AUTUMNATON_ITEM_ID, "autumn-aton", 1, ItemType.OTHER,
                ),
            ),
        )
        val lib = GameRuntimeLibrary(preferences = p, inventoryManager = inv, character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("autumnaton");""")
        assertTrue(out.contains("ready to be sent"), out)
    }

    @Test
    fun fallguy_alias_dispatches() {
        val p = Preferences(MapSettings())
        p.setBoolean("hasAutumnaton", true)
        p.setString("autumnatonQuestLocation", "The Haunted Ballroom")
        p.setInt("autumnatonQuestTurn", 100)
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("fallguy");""")
        assertTrue(out.contains("Haunted Ballroom"), out)
    }

    @Test
    fun cmc_status_printsConsults() {
        val p = Preferences(MapSettings())
        p.setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, ColdMedicineChoiceSync.CABINET_ITEM_ID)
        p.setInt(ColdMedicineChoiceSync.CONSULTS_PREF, 2)
        p.setInt(ColdMedicineChoiceSync.EQUIPMENT_PREF, 1)
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("cmc");""")
        assertTrue(out.contains("consults used: 2"), out)
        assertTrue(out.contains("Equipment taken: 1"), out)
    }

    @Test
    fun leaves_status_printsBurned() {
        val p = Preferences(MapSettings())
        p.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_BURNING_LEAVES_PREF, true)
        p.setInt("_leavesBurned", 42)
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("leaves");""")
        assertTrue(out.contains("42"), out)
    }

    @Test
    fun teatree_status_whenUnused() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("teatree");""")
        assertTrue(out.contains("used today: false"), out)
    }

    @Test
    fun mummery_requiresTrunk() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()), character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("mummery meat");""")
        assertTrue(out.contains("mumming trunk"), out)
    }

    @Test
    fun timespinner_requiresItem() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(lib, """cli_execute("timespinner");""")
        assertTrue(out.contains("Time-Spinner"), out)
    }

    @Test
    fun florist_requiresAvailability() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        val out = outputLib(lib, """cli_execute("florist plant War Lily");""")
        assertTrue(out.contains("Florist Friar"), out)
    }

    @Test
    fun leprecondo_status() {
        val p = Preferences(MapSettings())
        p.setString("leprecondoInstalled", "1,2,3,4")
        p.setInt("_leprecondoRearrangements", 1)
        val inv = invWith(mapOf(11861 to InventoryItem(11861, "Leprecondo", 1, ItemType.OTHER)))
        val lib = GameRuntimeLibrary(preferences = p, inventoryManager = inv)
        val out = outputLib(lib, """cli_execute("leprecondo");""")
        assertTrue(out.contains("1,2,3,4"), out)
        assertTrue(out.contains("1 / 3"), out)
    }

    @Test
    fun heist_requiresCatBurglar() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()), character = KoLCharacter())
        val out = outputLib(lib, """cli_execute("heist");""")
        assertTrue(out.contains("Cat Burglar"), out)
    }

    @Test
    fun help_listsIotmVerbs() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("help autumn");""")
        assertTrue(out.contains("autumnaton"), out)
        val out2 = outputLib(lib, """cli_execute("help cmc");""")
        assertTrue(out2.contains("cmc"), out2)
    }
}
