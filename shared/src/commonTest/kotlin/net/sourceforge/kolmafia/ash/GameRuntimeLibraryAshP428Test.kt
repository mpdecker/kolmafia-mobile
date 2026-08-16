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
import net.sourceforge.kolmafia.request.DisplayCaseRequest

class GameRuntimeLibraryAshP428Test {

    @Test
    fun revision_phase480() {
        assertEquals("phase485", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun takeDisplayCli_refreshesCachedDisplayAmount() = runBlocking {
        var displayContents = mapOf(55 to 6)
        val fakeDisplay = object : DisplayCaseRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
                displayContents = displayContents.toMutableMap().apply {
                    this[itemId] = (this[itemId] ?: 0) - quantity
                    if (get(itemId) == 0) remove(itemId)
                }
                return Result.success("ok")
            }

            override suspend fun fetchContents(): Map<Int, Int> = displayContents
        }
        val p = prefs()
        CollectionCacheSync.saveDisplay(p, mapOf(55 to 6))
        val db = object : GameDatabase() {
            private val trophy = ItemData(
                55, "trophy item", "desc", "trophy.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null,
            )
            override fun item(id: Int): ItemData? = if (id == 55) trophy else null
            override fun item(name: String): ItemData? =
                if (name.equals("trophy item", ignoreCase = true)) trophy else null
        }
        val lib = GameRuntimeLibrary(
            preferences = p,
            gameDatabase = db,
            displayCaseRequest = fakeDisplay,
        )
        outputLib(lib, """cli_execute("display take 2 trophy item");""")
        assertEquals(4, CollectionCache.load(p, Preferences.CACHED_DISPLAY)[55])
        assertEquals(
            "4",
            outputLib(lib, """print(to_string(display_amount(to_item("trophy item"))));"""),
        )
    }
}
