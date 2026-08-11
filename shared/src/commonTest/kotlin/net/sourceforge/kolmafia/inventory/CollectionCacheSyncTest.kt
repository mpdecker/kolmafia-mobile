package net.sourceforge.kolmafia.inventory

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.ash.CollectionCache
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.StorageRequest

class CollectionCacheSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun saveFromSources_persistsAllCollectionPrefs() {
        val p = prefs()
        CollectionCacheSync.saveFromSources(
            p,
            closet = mapOf(42 to 3),
            storage = mapOf(99 to 7),
            freepulls = mapOf(99 to 2),
            stash = mapOf(1 to 5),
        )
        assertEquals(3, CollectionCache.load(p, Preferences.CACHED_CLOSET)[42])
        assertEquals(7, CollectionCache.load(p, Preferences.CACHED_STORAGE)[99])
        assertEquals(2, CollectionCache.load(p, Preferences.CACHED_FREEPULLS)[99])
        assertEquals(5, CollectionCache.load(p, Preferences.CACHED_STASH)[1])
    }

    @Test
    fun refreshCloset_refetchesAndSaves() = runBlocking {
        var fetchCount = 0
        val closet = object : ClosetRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun fetchContents(): Map<Int, Int> {
                fetchCount++
                return mapOf(42 to 9)
            }
        }
        val p = prefs()
        CollectionCacheSync.refreshCloset(closet, p)
        assertEquals(1, fetchCount)
        assertEquals(9, CollectionCache.load(p, Preferences.CACHED_CLOSET)[42])
    }

    @Test
    fun refreshStorage_refetchesClassifiedAndSaves() = runBlocking {
        val storage = object : StorageRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun fetchClassifiedContents(
                characterState: net.sourceforge.kolmafia.character.CharacterState?,
                prefs: Preferences?,
            ): StoragePullRules.StorageContents =
                StoragePullRules.StorageContents(
                    storage = mapOf(99 to 4),
                    freepulls = mapOf(99 to 1),
                )
        }
        val p = prefs()
        CollectionCacheSync.refreshStorage(storage, null, p)
        assertEquals(4, CollectionCache.load(p, Preferences.CACHED_STORAGE)[99])
        assertEquals(1, CollectionCache.load(p, Preferences.CACHED_FREEPULLS)[99])
    }

    @Test
    fun refreshStash_refetchesAndSaves() = runBlocking {
        val stash = object : ClanStashRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun fetchContents(): Map<Int, Int> = mapOf(1 to 6)
        }
        val p = prefs()
        CollectionCacheSync.refreshStash(stash, p)
        assertEquals(6, CollectionCache.load(p, Preferences.CACHED_STASH)[1])
    }

    @Test
    fun refreshDisplay_refetchesAndSaves() = runBlocking {
        var fetchCount = 0
        val display = object : DisplayCaseRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun fetchContents(): Map<Int, Int> {
                fetchCount++
                return mapOf(7 to 4)
            }
        }
        val p = prefs()
        CollectionCacheSync.refreshDisplay(display, p)
        assertEquals(1, fetchCount)
        assertEquals(4, CollectionCache.load(p, Preferences.CACHED_DISPLAY)[7])
    }
}
