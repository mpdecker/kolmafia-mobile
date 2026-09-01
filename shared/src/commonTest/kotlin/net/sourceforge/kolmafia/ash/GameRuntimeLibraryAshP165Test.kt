package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.shop.TimeTowerSync

class GameRuntimeLibraryAshP165Test {

    @Test
    fun revision_phase185() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun getFreePulls_fetchesAndCachesFreepullBucket() {
        val fakeStorage = object : StorageRequest(
            HttpClient(MockEngine { respond("") }),
        ) {
            override suspend fun fetchClassifiedContents(
                characterState: net.sourceforge.kolmafia.character.CharacterState?,
                prefs: Preferences?,
            ): StoragePullRules.StorageContents =
                StoragePullRules.StorageContents(
                    storage = mapOf(7566 to 1),
                    freepulls = mapOf(7566 to 2),
                )
        }
        val db = object : GameDatabase() {
            private val toolbelt = ItemData(
                7566,
                "time-twitching toolbelt",
                "desc",
                "belt.gif",
                ItemPrimaryUse.NONE,
                emptySet(),
                setOf('t', 'd'),
                0,
                null,
            )
            override fun item(id: Int): ItemData? = if (id == 7566) toolbelt else null
            override fun item(name: String): ItemData? =
                if (name.equals("time-twitching toolbelt", ignoreCase = true)) toolbelt else null
        }
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(storageRequest = fakeStorage, gameDatabase = db, preferences = p)
        assertEquals("1", outputLib(lib, "print(to_string(count(get_free_pulls())));"))
        assertEquals(2, CollectionCache.load(p, Preferences.CACHED_FREEPULLS)[7566])
    }

    @Test
    fun getCachedFreePulls_readsPreferenceSnapshot() {
        val p = Preferences(MapSettings())
        CollectionCache.save(p, Preferences.CACHED_FREEPULLS, mapOf(7566 to 4))
        val db = object : GameDatabase() {
            private val toolbelt = ItemData(
                7566,
                "time-twitching toolbelt",
                "desc",
                "belt.gif",
                ItemPrimaryUse.NONE,
                emptySet(),
                setOf('t', 'd'),
                0,
                null,
            )
            override fun item(id: Int): ItemData? = if (id == 7566) toolbelt else null
            override fun item(name: String): ItemData? =
                if (name.equals("time-twitching toolbelt", ignoreCase = true)) toolbelt else null
        }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = db)
        assertEquals("1", outputLib(lib, "print(to_string(count(get_cached_free_pulls())));"))
    }

    @Test
    fun timeTowerClose_migratesToolbeltToStorageCache() {
        val toolbeltId = StoragePullRules.TIME_TWITCHING_TOOLBELT
        val p = Preferences(MapSettings())
        p.setString(Preferences.CACHED_FREEPULLS, "$toolbeltId:2")
        p.setBoolean(TimeTowerSync.PREF, true)
        TimeTowerSync.syncFromChronerShopHtml("That store isn't there anymore.", p)
        assertEquals(false, p.getBoolean(TimeTowerSync.PREF, true))
        assertEquals("$toolbeltId:2", p.getString(Preferences.CACHED_STORAGE, "unset"))
        assertEquals("", p.getString(Preferences.CACHED_FREEPULLS, "unset"))
    }
}
