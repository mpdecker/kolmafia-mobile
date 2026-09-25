package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [UseItemRequest.parseAbsorb]. A Gelatinous Noob `inventory.php?absorb=`
 * success increments the absorb count, logs `Absorbing …`, and consumes the item.
 * Failure text leaves the count and the item alone.
 */
object UseItemAbsorbSync {
    private val absorbPattern = Regex("""absorb=(\d+)""", RegexOption.IGNORE_CASE)

    fun apply(
        url: String,
        responseText: String,
        character: KoLCharacter?,
        inventory: InventoryManager? = null,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        itemName: (Int) -> String = { id ->
            ItemDatabase.getItemName(id).ifBlank { "item #$id" }
        },
    ): Boolean {
        val state = character?.state?.value ?: return true
        if (!state.inNoobcore) return true
        val itemId = absorbedItemId(url) ?: return true
        val success = responseText.contains("absorb some new knowledge") ||
            responseText.contains("You absorb the") ||
            responseText.contains("absorbing that item")
        if (!success) return true
        character.incrementAbsorbs(1, preferences)
        val message = "Absorbing ${itemName(itemId)}"
        RequestLogger.updateSessionLog(message, sessionLogger)
        inventory?.consumeItemLocally(itemId, 1)
        return true
    }

    fun absorbedItemId(url: String): Int? {
        val path = inventoryPath(url)
        if (!path.startsWith("inventory.php")) return null
        return absorbPattern.find(path)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun inventoryPath(url: String): String {
        val noHash = url.substringBefore('#')
        val afterHost = if (noHash.contains("://")) {
            noHash.substringAfter("://").substringAfter('/')
        } else {
            noHash.trimStart('/')
        }
        return afterHost
    }
}
