package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.TransferItemSync

/**
 * Selective inventory.php action matrix (Phases 2286–2300).
 */
object InventoryActionSync {
    fun parse(
        url: String,
        html: String,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
        preferences: Preferences? = null,
    ): Boolean {
        if (!url.contains("inventory.php", ignoreCase = true)) return false
        val action = url.substringAfter("action=", "").substringBefore('&').lowercase()
        return when {
            action.contains("closetpush") || action == "closetpush" -> {
                val itemId = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
                    .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return false
                val qty = Regex("""qty=(\d+)""", RegexOption.IGNORE_CASE)
                    .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                TransferItemSync.transferItems(
                    itemId, qty,
                    TransferItemSync.Bucket.INVENTORY,
                    TransferItemSync.Bucket.CLOSET,
                    inventory,
                )
                true
            }
            action.contains("closetpull") || action == "closetpull" -> {
                val itemId = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
                    .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return false
                val qty = Regex("""qty=(\d+)""", RegexOption.IGNORE_CASE)
                    .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                TransferItemSync.transferItems(
                    itemId, qty,
                    TransferItemSync.Bucket.CLOSET,
                    TransferItemSync.Bucket.INVENTORY,
                    inventory,
                )
                true
            }
            action.contains("pullall") -> {
                // Closet empty — leave inventory refresh to caller
                preferences?.setBoolean("_inventoryPullAllSeen", true)
                html.isNotBlank()
            }
            action.contains("timepose") -> {
                preferences?.setBoolean("_timeSpinnerUsed", true)
                true
            }
            action.contains("requestdrop") || action.contains("breakbricko") -> {
                ResultProcessor.processResults(false, html, inventory, character, preferences)
                true
            }
            action.contains("candy") || action.contains("robooze") ||
                action.contains("ghost") || action.contains("hobo") ||
                action.contains("slime") -> {
                ResultProcessor.processResults(false, html, inventory, character, preferences)
                true
            }
            else -> false
        }
    }
}
