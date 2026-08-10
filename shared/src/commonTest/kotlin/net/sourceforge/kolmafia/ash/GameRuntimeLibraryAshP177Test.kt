package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StorageBucketMigration
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.shop.TimeTowerSync

class GameRuntimeLibraryAshP177Test {

    @Test
    fun revision_phase195() {
        assertEquals("phase400", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun timeTowerOpen_migratesToolbeltFromStorageToFreepullCache() {
        val toolbeltId = StoragePullRules.TIME_TWITCHING_TOOLBELT
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.CACHED_STORAGE, "$toolbeltId:1|9999:2")
        prefs.setString(Preferences.CACHED_FREEPULLS, "8888:3")

        TimeTowerSync.syncFromChronerShopHtml("Chroner merch is open for business.", prefs)

        assertEquals(true, prefs.getBoolean(TimeTowerSync.PREF, false))
        assertEquals("9999:2", prefs.getString(Preferences.CACHED_STORAGE, ""))
        assertEquals("8888:3|$toolbeltId:1", prefs.getString(Preferences.CACHED_FREEPULLS, ""))
    }

    @Test
    fun timeTowerClose_migratesToolbeltFromFreepullToStorageCache() {
        val toolbeltId = StoragePullRules.TIME_TWITCHING_TOOLBELT
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(TimeTowerSync.PREF, true)
        prefs.setString(Preferences.CACHED_STORAGE, "9999:2")
        prefs.setString(Preferences.CACHED_FREEPULLS, "8888:3|$toolbeltId:1")

        TimeTowerSync.syncFromChronerShopHtml("That store isn't there anymore.", prefs)

        assertEquals(false, prefs.getBoolean(TimeTowerSync.PREF, true))
        assertEquals("9999:2|$toolbeltId:1", prefs.getString(Preferences.CACHED_STORAGE, ""))
        assertEquals("8888:3", prefs.getString(Preferences.CACHED_FREEPULLS, ""))
    }

    @Test
    fun migrateToolbelt_noOpWhenToolbeltAbsentFromSourceBucket() {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.CACHED_STORAGE, "100:1")
        prefs.setString(Preferences.CACHED_FREEPULLS, "200:2")

        StorageBucketMigration.migrateToolbelt(timeTowerAvailable = true, prefs)

        assertEquals("100:1", prefs.getString(Preferences.CACHED_STORAGE, ""))
        assertEquals("200:2", prefs.getString(Preferences.CACHED_FREEPULLS, ""))
    }
}
