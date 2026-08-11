package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.ash.CollectionCache
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.outputLib
import net.sourceforge.kolmafia.ash.prefs
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.preferences.Preferences

class SessionManagerCollectionCacheTest {

    @Test
    fun loginStyleSaveFromSources_seedsClosetAmountWithoutLiveFetch() {
        val p = prefs()
        CollectionCacheSync.saveFromSources(
            p,
            closet = mapOf(42 to 8),
            storage = mapOf(99 to 3),
            freepulls = emptyMap(),
            stash = emptyMap(),
        )
        val db = object : GameDatabase() {
            private val shiny = ItemData(
                42, "shiny item", "desc", "item.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null,
            )
            private val haggard = ItemData(
                99, "haggard item", "desc", "hag.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null,
            )
            override fun item(id: Int): ItemData? = when (id) {
                42 -> shiny
                99 -> haggard
                else -> null
            }
            override fun item(name: String): ItemData? = when {
                name.equals("shiny item", ignoreCase = true) -> shiny
                name.equals("haggard item", ignoreCase = true) -> haggard
                else -> null
            }
        }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = db, closetRequest = null)
        assertEquals("8", outputLib(lib, """print(to_string(closet_amount(to_item("shiny item"))));"""))
        assertEquals("3", outputLib(lib, """print(to_string(storage_amount(to_item("haggard item"))));"""))
        assertEquals(8, CollectionCache.load(p, Preferences.CACHED_CLOSET)[42])
    }

    @Test
    fun loginStyleSaveDisplay_seedsDisplayAmountWithoutLiveFetch() {
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
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = db, displayCaseRequest = null)
        assertEquals("6", outputLib(lib, """print(to_string(display_amount(to_item("trophy item"))));"""))
        assertEquals(6, CollectionCache.load(p, Preferences.CACHED_DISPLAY)[55])
    }
}
