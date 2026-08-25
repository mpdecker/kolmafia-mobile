package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop ClanStashRequest.parseTransfer + SendMailRequest.parseTransfer (Phases 2286–2300).
 */
object ClanStashSync {
    @Volatile
    var stashCounts: MutableMap<Int, Int>? = null

    fun parseTransfer(
        url: String,
        html: String,
        itemId: Int,
        quantity: Int,
        inventory: InventoryManager?,
    ): Boolean {
        if (!url.contains("clan_stash.php", ignoreCase = true)) return false
        val action = url.substringAfter("action=", "").substringBefore('&').lowercase()
        return when {
            action.contains("contribute") || action.contains("addgoodie") -> {
                if (html.isNotBlank()) {
                    inventory?.consumeItemLocally(itemId, quantity)
                    adjustStash(itemId, quantity)
                    true
                } else false
            }
            action.contains("take") || action.contains("takegoodie") -> {
                if (html.isNotBlank()) {
                    inventory?.gainItemLocally(itemId, quantity)
                    adjustStash(itemId, -quantity)
                    true
                } else false
            }
            else -> false
        }
    }

    fun resetForTest() {
        stashCounts = null
    }

    private fun adjustStash(itemId: Int, delta: Int) {
        val map = stashCounts ?: return
        val next = (map[itemId] ?: 0) + delta
        if (next <= 0) map.remove(itemId) else map[itemId] = next
    }
}

object SendMailSync {
    fun parseTransfer(
        url: String,
        html: String?,
        attachments: List<Pair<Int, Int>> = emptyList(),
        meat: Long = 0,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
    ): Boolean {
        if (!url.contains("sendmessage.php", ignoreCase = true)) return false
        val body = html.orEmpty()
        val ok = body.contains("<center>Message ", ignoreCase = true) ||
            body.contains("Message sent", ignoreCase = true) ||
            (body.isNotBlank() && !body.contains("doesn't exist", ignoreCase = true))
        if (!ok) return false
        for ((itemId, qty) in attachments) {
            inventory?.consumeItemLocally(itemId, qty)
        }
        if (meat > 0) {
            ResultProcessor.processMeat(-meat, character)
        }
        return true
    }
}

object ManageStoreSync {
    fun parseResponse(
        url: String,
        html: String,
        inventory: InventoryManager? = null,
        preferences: Preferences? = null,
    ): Boolean {
        if (!url.contains("backoffice.php", ignoreCase = true) &&
            !url.contains("manageprices.php", ignoreCase = true) &&
            !url.contains("mallstore.php", ignoreCase = true)
        ) {
            return false
        }
        val action = url.substringAfter("action=", "").substringBefore('&').lowercase()
        val itemId = Regex("""itemid=h?(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""price\[(\d+)]""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1)?.toIntOrNull()
        val qty = Regex("""quantity=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""qty=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: 1
        when {
            action.contains("additem") && itemId != null -> {
                inventory?.consumeItemLocally(itemId, qty)
                preferences?.setBoolean("_mallStoreDirty", true)
                return true
            }
            action.contains("removeitem") && itemId != null -> {
                inventory?.gainItemLocally(itemId, qty)
                preferences?.setBoolean("_mallStoreDirty", true)
                return true
            }
            action.contains("updateinv") -> {
                preferences?.setBoolean("_mallStoreDirty", true)
                return html.isNotBlank()
            }
        }
        return html.isNotBlank()
    }
}
