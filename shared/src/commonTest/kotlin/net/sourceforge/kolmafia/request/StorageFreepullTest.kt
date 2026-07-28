package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StorageBucketMigration

class StorageFreepullTest {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
        ModifierDatabase.resetForTest()
    }

    @Test
    fun classifyContents_ronin_splitsFreepullFromStorage() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 3220,
                name = "hobo code binder",
                descId = "desc3220",
                image = "book2.gif",
                primaryUse = ItemPrimaryUse.OFFHAND,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest(
            "Item",
            "hobo code binder",
            "Free Pull",
        )
        val freepullId = 3220
        val regularId = 9999
        val ronin = CharacterState(isHardcore = true, roninLeft = 2)
        val contents = StoragePullRules.classifyContents(
            mapOf(freepullId to 1, regularId to 5),
            ronin,
        )
        assertEquals(1, contents.freepulls[freepullId])
        assertEquals(5, contents.storage[regularId])
    }

    @Test
    fun classifyContents_canInteract_keepsAllInStorage() {
        val contents = StoragePullRules.classifyContents(
            mapOf(100 to 3, 200 to 1),
            CharacterState(kingLiberated = true, roninLeft = 0),
        )
        assertEquals(3, contents.storage[100])
        assertEquals(1, contents.storage[200])
        assertEquals(0, contents.freepulls.size)
    }

    @Test
    fun toolbelt_notFreePullWhenTimeTowerUnavailable() {
        val toolbeltId = StoragePullRules.TIME_TWITCHING_TOOLBELT
        ItemDatabase.registerForTest(
            ItemData(
                id = toolbeltId,
                name = "time-twitching toolbelt",
                descId = "desc$toolbeltId",
                image = "time_belt.gif",
                primaryUse = ItemPrimaryUse.ACCESSORY,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest(
            "Item",
            "time-twitching toolbelt",
            "Free Pull: [pref(timeTowerAvailable,true)]",
        )
        val ronin = CharacterState(isHardcore = true, roninLeft = 2)
        val prefs = Preferences(MapSettings())
        val withoutTower = StoragePullRules.classifyContents(
            mapOf(toolbeltId to 1),
            ronin,
            prefs,
        )
        assertEquals(1, withoutTower.storage[toolbeltId])
        assertEquals(0, withoutTower.freepulls.size)

        prefs.setBoolean("timeTowerAvailable", true)
        val withTower = StoragePullRules.classifyContents(
            mapOf(toolbeltId to 1),
            ronin,
            prefs,
        )
        assertEquals(1, withTower.freepulls[toolbeltId])
        assertEquals(0, withTower.storage.size)
    }

    @Test
    fun migrateToolbelt_openMovesFromStorageCacheToFreepullCache() {
        val toolbeltId = StoragePullRules.TIME_TWITCHING_TOOLBELT
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.CACHED_STORAGE, "$toolbeltId:2|500:1")
        prefs.setString(Preferences.CACHED_FREEPULLS, "")

        StorageBucketMigration.migrateToolbelt(timeTowerAvailable = true, prefs)

        assertEquals("500:1", prefs.getString(Preferences.CACHED_STORAGE, ""))
        assertEquals("$toolbeltId:2", prefs.getString(Preferences.CACHED_FREEPULLS, ""))
    }

    @Test
    fun migrateToolbelt_closeMovesFromFreepullCacheToStorageCache() {
        val toolbeltId = StoragePullRules.TIME_TWITCHING_TOOLBELT
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.CACHED_STORAGE, "500:1")
        prefs.setString(Preferences.CACHED_FREEPULLS, "$toolbeltId:2")

        StorageBucketMigration.migrateToolbelt(timeTowerAvailable = false, prefs)

        assertEquals("500:1|$toolbeltId:2", prefs.getString(Preferences.CACHED_STORAGE, ""))
        assertEquals("", prefs.getString(Preferences.CACHED_FREEPULLS, ""))
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
