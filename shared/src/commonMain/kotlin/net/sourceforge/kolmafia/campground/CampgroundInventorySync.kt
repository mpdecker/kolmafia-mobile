package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.ash.CollectionCache
import net.sourceforge.kolmafia.preferences.Preferences

/** Pref-backed campground item inventory for `get_campground()` ASH. */
object CampgroundInventorySync {

    private data class GifItem(val gif: String, val itemId: Int, val count: Int = 1)

    private val OUTSIDE_ITEMS = listOf(
        GifItem("telescope.gif", 2599),
        GifItem("pagoda.gif", 502),
        GifItem("scarecrow.gif", 104),
        GifItem("golem.gif", 101),
        GifItem("doghouse.gif", 8639),
        GifItem("chesstable.gif", 8989),
        GifItem("campterminal.gif", 9033),
        GifItem("sourceterminal.gif", 9033),
        GifItem("monolith.gif", 11268),
        GifItem("burningleaves.gif", 11340),
        GifItem("campground/leaves", 11340),
        GifItem("maid.gif", 1000),
        GifItem("maid2.gif", 1113),
        GifItem("butler.gif", 11262),
        GifItem("jetmaid.gif", 11377),
    )

    fun load(prefs: Preferences?): Map<Int, Int> {
        if (prefs == null) return emptyMap()
        return CollectionCache.load(prefs, Preferences.CACHED_CAMPGROUND)
    }

    fun setItem(prefs: Preferences?, itemId: Int, count: Int) {
        if (prefs == null || itemId < 0) return
        val updated = load(prefs).toMutableMap()
        if (count == 0) {
            updated.remove(itemId)
        } else {
            updated[itemId] = count
        }
        CollectionCache.save(prefs, Preferences.CACHED_CAMPGROUND, updated)
    }

    fun syncFromHtml(html: String, prefs: Preferences?) {
        if (prefs == null) return
        DwellingSync.applyFromHtml(html, prefs)

        for ((gif, itemId) in CampgroundItemSync.WORKSHED_GIF_TO_ID) {
            if (html.contains(gif, ignoreCase = true)) {
                setItem(prefs, itemId, 1)
            }
        }
        if (html.contains("Looks like the doctors are out for the day.", ignoreCase = true)) {
            setItem(prefs, 10815, 1)
        }

        for (entry in OUTSIDE_ITEMS) {
            if (html.contains(entry.gif, ignoreCase = true)) {
                setItem(prefs, entry.itemId, entry.count)
            }
        }

        if (html.contains("Burn some Leaves", ignoreCase = true)) {
            setItem(prefs, 11340, 1)
        }

        syncTelescope(html, prefs)
        syncKitchen(html, prefs)
        syncJungGate(html, prefs)
    }

    private fun syncTelescope(html: String, prefs: Preferences) {
        if (!html.contains("telescope.gif", ignoreCase = true)) return
        val upgrades = prefs.getInt("telescopeUpgrades", 0).coerceAtLeast(1)
        setItem(prefs, 2599, upgrades)
    }

    private fun syncKitchen(html: String, prefs: Preferences) {
        if (html.contains("ezcook.gif", ignoreCase = true)) setItem(prefs, 4707, 1)
        if (html.contains("oven.gif", ignoreCase = true)) setItem(prefs, 157, 1)
        if (html.contains("shaker.gif", ignoreCase = true)) setItem(prefs, 4708, 1)
        if (html.contains("cocktailkit.gif", ignoreCase = true)) setItem(prefs, 236, 1)
        if (html.contains("chefinbox.gif", ignoreCase = true)) setItem(prefs, 438, 1)
        if (html.contains("cchefbox.gif", ignoreCase = true)) setItem(prefs, 1112, 1)
        if (html.contains("bartinbox.gif", ignoreCase = true)) setItem(prefs, 440, 1)
        if (html.contains("cbartbox.gif", ignoreCase = true)) setItem(prefs, 1111, 1)
    }

    private fun syncJungGate(html: String, prefs: Preferences) {
        val match = Regex("""junggate_(\d+)""").find(html) ?: return
        val link = match.groupValues[1].toIntOrNull() ?: return
        val jarId = when (link) {
            1 -> 5898
            2 -> 5899
            3 -> 5900
            4 -> 5901
            5 -> 5902
            6 -> 5903
            11 -> 5905
            else -> return
        }
        setItem(prefs, jarId, 1)
    }
}
