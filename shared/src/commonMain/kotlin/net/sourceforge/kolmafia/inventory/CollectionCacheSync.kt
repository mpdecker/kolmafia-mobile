package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.ash.CollectionCache
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.StorageRequest

/**
 * Seeds and refreshes pref-backed collection caches used by `closet_amount`/`storage_amount` ASH.
 *
 * Retrieved flags mirror desktop [DisplayCaseManager.collectionRetrieved] /
 * [ClanManager.stashRetrieved] so `*_amount` can lazy-refresh when never seeded.
 */
object CollectionCacheSync {

    var closetRetrieved: Boolean = false
        private set
    var storageRetrieved: Boolean = false
        private set
    /** Desktop [DisplayCaseManager.collectionRetrieved]. */
    var collectionRetrieved: Boolean = false
        private set

    fun resetRetrievedFlags() {
        closetRetrieved = false
        storageRetrieved = false
        collectionRetrieved = false
    }

    fun saveFromSources(
        preferences: Preferences,
        closet: Map<Int, Int>,
        storage: Map<Int, Int>,
        freepulls: Map<Int, Int>,
        stash: Map<Int, Int>,
        nopulls: Map<Int, Int> = emptyMap(),
    ) {
        saveCloset(preferences, closet)
        saveStorage(preferences, storage, freepulls, nopulls)
        saveStash(preferences, stash)
    }

    fun saveCloset(preferences: Preferences, closet: Map<Int, Int>) {
        CollectionCache.save(preferences, Preferences.CACHED_CLOSET, closet)
        closetRetrieved = true
    }

    fun saveStorage(
        preferences: Preferences,
        storage: Map<Int, Int>,
        freepulls: Map<Int, Int>,
        nopulls: Map<Int, Int> = emptyMap(),
    ) {
        CollectionCache.save(preferences, Preferences.CACHED_STORAGE, storage)
        CollectionCache.save(preferences, Preferences.CACHED_FREEPULLS, freepulls)
        CollectionCache.save(preferences, Preferences.CACHED_NOPULLS, nopulls)
        storageRetrieved = true
    }

    fun saveNopulls(preferences: Preferences, nopulls: Map<Int, Int>) {
        CollectionCache.save(preferences, Preferences.CACHED_NOPULLS, nopulls)
    }

    fun saveStash(preferences: Preferences, stash: Map<Int, Int>) {
        CollectionCache.save(preferences, Preferences.CACHED_STASH, stash)
        ClanManager.setStashRetrieved()
    }

    fun saveDisplay(preferences: Preferences, display: Map<Int, Int>) {
        CollectionCache.save(preferences, Preferences.CACHED_DISPLAY, display)
        collectionRetrieved = true
    }

    /** Put/take write-back that preserves the retrieved flag. */
    fun adjustCloset(preferences: Preferences, itemId: Int, delta: Int) {
        CollectionCache.adjust(preferences, Preferences.CACHED_CLOSET, itemId, delta)
        closetRetrieved = true
    }

    fun adjustDisplay(preferences: Preferences, itemId: Int, delta: Int) {
        CollectionCache.adjust(preferences, Preferences.CACHED_DISPLAY, itemId, delta)
        collectionRetrieved = true
    }

    fun adjustStash(preferences: Preferences, itemId: Int, delta: Int) {
        CollectionCache.adjust(preferences, Preferences.CACHED_STASH, itemId, delta)
        ClanManager.setStashRetrieved()
    }

    fun adjustStorage(preferences: Preferences, itemId: Int, delta: Int) {
        CollectionCache.adjust(preferences, Preferences.CACHED_STORAGE, itemId, delta)
        storageRetrieved = true
    }

    /**
     * Desktop [KoLCharacter.liberateKing] storage merge:
     * freepulls + nopulls move into storage and those buckets clear.
     */
    fun mergeFreepullsIntoStorage(preferences: Preferences) {
        val storage = CollectionCache.load(preferences, Preferences.CACHED_STORAGE).toMutableMap()
        val freepulls = CollectionCache.load(preferences, Preferences.CACHED_FREEPULLS)
        val nopulls = CollectionCache.load(preferences, Preferences.CACHED_NOPULLS)
        for ((id, qty) in freepulls) {
            if (qty <= 0) continue
            storage[id] = (storage[id] ?: 0) + qty
        }
        for ((id, qty) in nopulls) {
            if (qty <= 0) continue
            storage[id] = (storage[id] ?: 0) + qty
        }
        saveStorage(preferences, storage, emptyMap(), emptyMap())
    }

    suspend fun refreshCloset(closetRequest: ClosetRequest, preferences: Preferences) {
        saveCloset(preferences, closetRequest.fetchContents())
    }

    suspend fun refreshStorage(
        storageRequest: StorageRequest,
        characterState: CharacterState?,
        preferences: Preferences,
    ) {
        val classified = storageRequest.fetchClassifiedContents(characterState, preferences)
        saveStorage(preferences, classified.storage, classified.freepulls, classified.nopulls)
    }

    suspend fun refreshStash(clanStashRequest: ClanStashRequest, preferences: Preferences) {
        saveStash(preferences, clanStashRequest.fetchContents())
    }

    suspend fun refreshDisplay(displayCaseRequest: DisplayCaseRequest, preferences: Preferences) {
        saveDisplay(preferences, displayCaseRequest.fetchContents())
    }
}
