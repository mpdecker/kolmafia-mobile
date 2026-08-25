package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.ClosetMeatSync
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.StorageMeatSync
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop TransferItemRequest.parseTransfer hub for closet/storage/display (Phases 2246–2260).
 */
object TransferItemSync {
    enum class Bucket { INVENTORY, CLOSET, STORAGE, DISPLAY }

    /** Optional display-case quantity map (id → qty) mutated on put/take. */
    @Volatile
    var displayCounts: MutableMap<Int, Int>? = null

    /** Optional closet quantity map when api closet cache is not wired. */
    @Volatile
    var closetCounts: MutableMap<Int, Int>? = null

    /** Optional storage quantity map. */
    @Volatile
    var storageCounts: MutableMap<Int, Int>? = null

    fun transferItems(
        itemId: Int,
        quantity: Int,
        source: Bucket,
        destination: Bucket,
        inventory: InventoryManager?,
    ) {
        if (itemId <= 0 || quantity <= 0) return
        when (source) {
            Bucket.INVENTORY -> inventory?.consumeItemLocally(itemId, quantity)
            Bucket.CLOSET -> adjustMap(closetCounts, itemId, -quantity)
            Bucket.STORAGE -> adjustMap(storageCounts, itemId, -quantity)
            Bucket.DISPLAY -> adjustMap(displayCounts, itemId, -quantity)
        }
        when (destination) {
            Bucket.INVENTORY -> inventory?.gainItemLocally(itemId, quantity)
            Bucket.CLOSET -> adjustMap(closetCounts, itemId, quantity)
            Bucket.STORAGE -> adjustMap(storageCounts, itemId, quantity)
            Bucket.DISPLAY -> adjustMap(displayCounts, itemId, quantity)
        }
    }

    fun parseClosetTransfer(
        url: String,
        html: String,
        itemId: Int,
        quantity: Int,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
    ): Boolean {
        if (character != null) {
            ClosetMeatSync.apply(character, html, url)
        }
        val action = url.substringAfter("action=", "").substringBefore('&').lowercase()
        return when {
            action.contains("put") || action.contains("closetpush") -> {
                if (html.isNotBlank()) {
                    transferItems(itemId, quantity, Bucket.INVENTORY, Bucket.CLOSET, inventory)
                    true
                } else {
                    false
                }
            }
            action.contains("take") || action.contains("closetpull") -> {
                if (html.isNotBlank()) {
                    transferItems(itemId, quantity, Bucket.CLOSET, Bucket.INVENTORY, inventory)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    fun parseStorageTransfer(
        url: String,
        html: String,
        itemId: Int,
        quantity: Int,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
        preferences: Preferences? = null,
    ): Boolean {
        if (character != null) {
            StorageMeatSync.apply(character, html, url)
        }
        val action = url.substringAfter("action=", "").substringBefore('&').lowercase()
        return when {
            action.contains("pullitem") || action == "pull" -> {
                if (html.isNotBlank()) {
                    transferItems(itemId, quantity, Bucket.STORAGE, Bucket.INVENTORY, inventory)
                    if (preferences != null) {
                        val left = preferences.getInt("pulls_remaining", -1)
                        if (left > 0) {
                            preferences.setInt("pulls_remaining", (left - quantity).coerceAtLeast(0))
                        }
                    }
                    true
                } else {
                    false
                }
            }
            action.contains("storeitem") -> {
                if (html.isNotBlank()) {
                    transferItems(itemId, quantity, Bucket.INVENTORY, Bucket.STORAGE, inventory)
                    true
                } else {
                    false
                }
            }
            action.contains("takemeat") ->
                html.contains("Meat out of storage", ignoreCase = true) || html.isNotBlank()
            action.contains("pullall") ->
                html.contains("grab all of your stuff", ignoreCase = true) || html.isNotBlank()
            else -> false
        }
    }

    fun parseDisplayTransfer(
        url: String,
        html: String,
        itemId: Int,
        quantity: Int,
        inventory: InventoryManager? = null,
    ): Boolean {
        val action = url.substringAfter("action=", "").substringBefore('&').lowercase()
        return when {
            action.contains("put") -> {
                if (html.isNotBlank()) {
                    transferItems(itemId, quantity, Bucket.INVENTORY, Bucket.DISPLAY, inventory)
                    true
                } else {
                    false
                }
            }
            action.contains("take") -> {
                if (html.isNotBlank()) {
                    transferItems(itemId, quantity, Bucket.DISPLAY, Bucket.INVENTORY, inventory)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    /** Apply ResultProcessor item gains from transfer HTML when present. */
    fun applyAcquireHtml(html: String, inventory: InventoryManager?) {
        if (inventory == null) return
        ResultProcessor.processResults(
            adventureResults = false,
            html = html,
            inventory = inventory,
        )
    }

    fun resetForTest() {
        displayCounts = null
        closetCounts = null
        storageCounts = null
    }

    private fun adjustMap(map: MutableMap<Int, Int>?, itemId: Int, delta: Int) {
        if (map == null || delta == 0) return
        val next = (map[itemId] ?: 0) + delta
        if (next <= 0) map.remove(itemId) else map[itemId] = next
    }
}
