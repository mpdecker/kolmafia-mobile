package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.TransferItemRequest] closet/storage/display hub. */
object TransferItemRequest {
    fun parseResponse(
        url: String,
        html: String,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
        preferences: Preferences? = null,
        itemId: Int = 0,
        quantity: Int = 1,
    ) {
        when {
            url.contains("closet.php", ignoreCase = true) ->
                TransferItemSync.parseClosetTransfer(url, html, itemId, quantity, inventory, character)
            url.contains("storage.php", ignoreCase = true) ->
                TransferItemSync.parseStorageTransfer(
                    url, html, itemId, quantity, inventory, character, preferences,
                )
            url.contains("managecollection.php", ignoreCase = true) ->
                TransferItemSync.parseDisplayTransfer(url, html, itemId, quantity, inventory)
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("closet.php", ignoreCase = true) ||
            url.contains("storage.php", ignoreCase = true) ||
            url.contains("managecollection.php", ignoreCase = true) ||
            url.contains("clan_stash.php", ignoreCase = true)
}
