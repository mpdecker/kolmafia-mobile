package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.campground.DwellingSync
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP432Test {

    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun campgroundDb(): GameDatabase = object : GameDatabase() {
        private val items = mapOf(
            30 to ItemData(30, "big rock", "desc", "bigrock.gif", ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null),
            143 to ItemData(143, "cottage", "desc", "cottage.gif", ItemPrimaryUse.USABLE, emptySet(), setOf('t', 'd'), 50, null),
            6967 to ItemData(6967, "Chemystery Box", "desc", "wbchemset.gif", ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null),
        )
        override fun item(id: Int): ItemData? = items[id]
        override fun item(name: String): ItemData? =
            items.values.find { it.name.equals(name, ignoreCase = true) }
    }

    @Test
    fun revision_phase480() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun get_dwelling_defaultsToBigRock() {
        val lib = GameRuntimeLibrary(preferences = prefs(), gameDatabase = campgroundDb())
        assertEquals("big rock", outputLib(lib, """print(get_dwelling());""").trim())
    }

    @Test
    fun get_dwelling_readsSeededPref() {
        val p = prefs { setInt(DwellingSync.CURRENT_DWELLING_ITEM_ID_PREF, 143) }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = campgroundDb())
        assertEquals("cottage", outputLib(lib, """print(get_dwelling());""").trim())
    }

    @Test
    fun get_campground_includesDwellingAndCachedItems() {
        val p = prefs {
            setInt(DwellingSync.CURRENT_DWELLING_ITEM_ID_PREF, 143)
            CampgroundInventorySync.setItem(this, 6967, 1)
        }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = campgroundDb())
        val out = outputLib(
            lib,
            """
            foreach key, val in get_campground() {
              print(key + "=" + val);
            }
            """.trimIndent(),
        ).trim()
        assertTrue(out.contains("cottage=1"))
        assertTrue(out.contains("Chemystery Box=1"))
    }

    @Test
    fun campgroundVisitHook_seedsDwellingAndInventory() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = campgroundDb())
        lib.processVisitResponseHooks(
            """<img src="/rest3.gif"><img src="wbchemset.gif">""",
            "https://www.kingdomofloathing.com/campground.php",
        )
        assertEquals("cottage", outputLib(lib, """print(get_dwelling());""").trim())
        assertTrue(outputLib(lib, """print(count(get_campground()));""").trim().toInt() >= 2)
    }
}
