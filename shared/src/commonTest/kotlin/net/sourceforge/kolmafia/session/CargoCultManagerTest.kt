package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.CultShortsDatabase
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest

class CargoCultManagerTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun manager(
        prefs: Preferences = prefs(),
        request: CargoCultistShortsRequest = CargoCultistShortsRequest(HttpClient(MockEngine { respond("ok") })),
        pocketSync: CargoPocketSync = CargoPocketSync(prefs, YegDemonNameSync(prefs)),
    ): CargoCultManager = CargoCultManager(
        preferences = prefs,
        request = request,
        pocketSync = pocketSync,
        yegDemonNameSync = YegDemonNameSync(prefs),
        inventoryManager = null,
    )

    @Test
    fun emptyCommand_printsEmptyMessage() = runTest {
        val out = mutableListOf<String>()
        manager().run("", InventoryState(), null) { out += it }
        assertTrue(out.any { it.contains("haven't emptied any pockets") })
    }

    @Test
    fun pick_rejectsWithoutShorts() = runTest {
        val out = mutableListOf<String>()
        manager().run("pick 7", InventoryState(), null) { out += it }
        assertTrue(out.any { it.contains("Cargo Cultist Shorts") })
    }

    @Test
    fun demon_listsKnownScraps() = runTest {
        CultShortsDatabase.injectForTest(listOf(373, 7))
        val prefs = prefs()
        val yeg = YegDemonNameSync(prefs)
        yeg.saveScrapPockets(mapOf(373 to "Ga"))
        val out = mutableListOf<String>()
        manager(
            prefs = prefs,
            pocketSync = CargoPocketSync(prefs, yeg),
        ).run("demon", InventoryState(), null) { out += it }
        assertTrue(out.any { it.contains("Pocket #373: Ga") })
        assertTrue(out.any { it.contains("Pocket #7: unknown") })
        CultShortsDatabase.resetForTest()
    }

    @Test
    fun pocket_describesContents() = runTest {
        MonsterDatabase.load()
        PocketDatabase.applyParseForTest(
            PocketDatabase.parseForTest("30\tMonster\tbookbat"),
        )
        val out = mutableListOf<String>()
        manager().run("pocket 30", InventoryState(), null) { out += it }
        assertTrue(out.any { it.contains("Pocket #30") && it.contains("bookbat") })
        PocketDatabase.resetForTest()
    }

    @Test
    fun count_monster_reportsOnePocket() = runTest {
        MonsterDatabase.load()
        PocketDatabase.applyParseForTest(
            PocketDatabase.parseForTest("30\tMonster\tbookbat"),
        )
        val out = mutableListOf<String>()
        manager().run("count monster bookbat", InventoryState(), null) { out += it }
        assertTrue(out.any { it.contains("one pocket") })
        PocketDatabase.resetForTest()
    }

    @Test
    fun list_effect_printsMatchingPockets() = runTest {
        EffectDatabase.load()
        PocketDatabase.applyParseForTest(
            PocketDatabase.parseForTest("5\tEffect\tSuper Vision (40)"),
        )
        val out = mutableListOf<String>()
        manager().run("list effect Super Vision", InventoryState(), null) { out += it }
        assertTrue(out.any { it.contains("Pocket #5") })
        PocketDatabase.resetForTest()
    }

    @Test
    fun monster_pick_routesToHttpPick() = runTest {
        MonsterDatabase.load()
        PocketDatabase.applyParseForTest(
            PocketDatabase.parseForTest("30\tMonster\tbookbat"),
        )
        val prefs = prefs()
        var picked = 0
        val request = object : CargoCultistShortsRequest(HttpClient(MockEngine { respond("done") })) {
            override suspend fun pickPocket(pocket: Int): Result<String> {
                picked = pocket
                return Result.success("done")
            }
        }
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
        val out = mutableListOf<String>()
        manager(prefs, request).run("monster bookbat", inv, null) { out += it }
        assertEquals(30, picked)
        assertTrue(out.any { it.contains("Emptied pocket #30") })
        PocketDatabase.resetForTest()
    }

    @Test
    fun pick_withShorts_callsRequest() = runTest {
        val prefs = prefs()
        var picked = false
        val request = object : CargoCultistShortsRequest(HttpClient(MockEngine { respond("done") })) {
            override suspend fun pickPocket(pocket: Int): Result<String> {
                picked = true
                return Result.success("done")
            }
        }
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
        val out = mutableListOf<String>()
        manager(prefs, request).run("pick 373", inv, null) { out += it }
        assertTrue(picked)
        assertTrue(out.any { it.contains("Emptied pocket #373") })
    }
}
