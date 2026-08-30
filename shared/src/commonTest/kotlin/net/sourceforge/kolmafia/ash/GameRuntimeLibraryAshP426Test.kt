package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClosetRequest

class GameRuntimeLibraryAshP426Test {

    @Test
    fun revision_phase480() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun takeClosetCli_refreshesCachedClosetAmount() = runBlocking {
        var closetContents = mapOf(42 to 5)
        val fakeCloset = object : ClosetRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
                closetContents = closetContents.toMutableMap().apply {
                    this[itemId] = (this[itemId] ?: 0) - quantity
                    if (get(itemId) == 0) remove(itemId)
                }
                return Result.success("ok")
            }

            override suspend fun fetchContents(): Map<Int, Int> = closetContents
        }
        val p = prefs()
        CollectionCacheSync.saveCloset(p, mapOf(42 to 5))
        val db = object : GameDatabase() {
            private val shiny = ItemData(
                42, "shiny item", "desc", "item.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null,
            )
            override fun item(id: Int): ItemData? = if (id == 42) shiny else null
            override fun item(name: String): ItemData? =
                if (name.equals("shiny item", ignoreCase = true)) shiny else null
        }
        val lib = GameRuntimeLibrary(
            preferences = p,
            gameDatabase = db,
            closetRequest = fakeCloset,
        )
        outputLib(lib, """cli_execute("take_closet 2 shiny item");""")
        assertEquals(3, CollectionCache.load(p, Preferences.CACHED_CLOSET)[42])
        assertEquals(
            "3",
            outputLib(lib, """print(to_string(closet_amount(to_item("shiny item"))));"""),
        )
    }
}
