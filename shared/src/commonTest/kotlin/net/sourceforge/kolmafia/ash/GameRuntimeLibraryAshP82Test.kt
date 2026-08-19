package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest
import net.sourceforge.kolmafia.session.BreakfastItemIds
import net.sourceforge.kolmafia.session.CargoCultManager
import net.sourceforge.kolmafia.session.CargoPocketSync
import net.sourceforge.kolmafia.session.YegDemonNameSync

class GameRuntimeLibraryAshP82Test {

    @BeforeTest
    fun loadData() = runTest {
        MonsterDatabase.load()
        EffectDatabase.load()
        ItemDatabase.load()
        PocketDatabase.applyParseForTest(
            PocketDatabase.parseForTest(
                """
                30	Monster	bookbat
                5	Effect	Super Vision (40)
                27	Item	baconstone
                12	Stats	80	90	120
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun revision_phase141() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pocket_monster_returnsMonsterEntity() {
        val lib = GameRuntimeLibrary()
        assertEquals("bookbat", outputLib(lib, """print(pocket_monster(30));"""))
    }

    @Test
    fun monster_pockets_includesMonsterPocket() {
        val lib = GameRuntimeLibrary()
        assertEquals("1", outputLib(lib, """print(count(monster_pockets()));"""))
    }

    @Test
    fun available_pocket_monster_returnsUnpickedPocket() {
        val lib = GameRuntimeLibrary()
        assertEquals("30", outputLib(lib, """print(available_pocket(to_monster("bookbat")));"""))
    }

    @Test
    fun available_pocket_effect_returnsFirstSortedUnpicked() {
        val lib = GameRuntimeLibrary()
        assertEquals("5", outputLib(lib, """print(available_pocket(to_effect("Super Vision")));"""))
    }

    @Test
    fun pick_pocket_byNumber_delegatesToManager() = runTest {
        val prefs = Preferences(MapSettings())
        var picked = 0
        val request = object : CargoCultistShortsRequest(HttpClient(MockEngine { respond("done") })) {
            override suspend fun pickPocket(pocket: Int): Result<String> {
                picked = pocket
                return Result.success("done")
            }
        }
        val pocketSync = CargoPocketSync(prefs, YegDemonNameSync(prefs))
        val mgr = CargoCultManager(
            preferences = prefs,
            request = request,
            pocketSync = pocketSync,
            yegDemonNameSync = YegDemonNameSync(prefs),
            inventoryManager = null,
        )
        val inv = InventoryState(
            items = mapOf(
                BreakfastItemIds.CARGO_CULTIST_SHORTS_ID to InventoryItem(
                    BreakfastItemIds.CARGO_CULTIST_SHORTS_ID,
                    "Cargo Cultist Shorts",
                    1,
                    ItemType.PANTS,
                ),
            ),
        )
        val invManager = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            init {
                _state.value = inv
            }
        }
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            inventoryManager = invManager,
            cargoPocketSync = pocketSync,
            cargoCultManager = mgr,
        )
        assertEquals("true", outputLib(lib, """print(pick_pocket(30));"""))
        assertEquals(30, picked)
    }

    @Test
    fun pick_pocket_unknownPocket_returnsFalse() {
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, """print(pick_pocket(999));"""))
    }

    @Test
    fun available_pocket_afterPick_skipsPicked() {
        val prefs = Preferences(MapSettings())
        val pocketSync = CargoPocketSync(prefs, YegDemonNameSync(prefs))
        pocketSync.parsePocketPick(5, "ok")
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            cargoPocketSync = pocketSync,
        )
        assertEquals("0", outputLib(lib, """print(available_pocket(to_effect("Super Vision")));"""))
    }
}
