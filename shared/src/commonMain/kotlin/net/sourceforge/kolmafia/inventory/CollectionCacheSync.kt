package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.ash.CollectionCache
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.StorageRequest

/** Seeds and refreshes pref-backed collection caches used by `closet_amount`/`storage_amount` ASH. */
object CollectionCacheSync {

    fun saveFromSources(
        preferences: Preferences,
        closet: Map<Int, Int>,
        storage: Map<Int, Int>,
        freepulls: Map<Int, Int>,
        stash: Map<Int, Int>,
    ) {
        saveCloset(preferences, closet)
        saveStorage(preferences, storage, freepulls)
        saveStash(preferences, stash)
    }

    fun saveCloset(preferences: Preferences, closet: Map<Int, Int>) {
        CollectionCache.save(preferences, Preferences.CACHED_CLOSET, closet)
    }

    fun saveStorage(
        preferences: Preferences,
        storage: Map<Int, Int>,
        freepulls: Map<Int, Int>,
    ) {
        CollectionCache.save(preferences, Preferences.CACHED_STORAGE, storage)
        CollectionCache.save(preferences, Preferences.CACHED_FREEPULLS, freepulls)
    }

    fun saveStash(preferences: Preferences, stash: Map<Int, Int>) {
        CollectionCache.save(preferences, Preferences.CACHED_STASH, stash)
    }

    fun saveDisplay(preferences: Preferences, display: Map<Int, Int>) {
        CollectionCache.save(preferences, Preferences.CACHED_DISPLAY, display)
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
        saveStorage(preferences, classified.storage, classified.freepulls)
    }

    suspend fun refreshStash(clanStashRequest: ClanStashRequest, preferences: Preferences) {
        saveStash(preferences, clanStashRequest.fetchContents())
    }

    suspend fun refreshDisplay(displayCaseRequest: DisplayCaseRequest, preferences: Preferences) {
        saveDisplay(preferences, displayCaseRequest.fetchContents())
    }
}
