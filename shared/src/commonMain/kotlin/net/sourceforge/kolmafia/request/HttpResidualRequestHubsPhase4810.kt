package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [net.sourceforge.kolmafia.request.Crimbo21TreeRequest] — crimbo21tree.php ammo throw.
 * Item ids: big rock=30; Black/White Crimbo balls resolved by name.
 */
object Crimbo21TreeRequest {
    private val AMMO_PATTERN = Regex("""[?&]c=([^&]*)""", RegexOption.IGNORE_CASE)
    private val ACTION_PATTERN = Regex("""[?&]action=([^&]*)""", RegexOption.IGNORE_CASE)

    private const val BIG_ROCK_ID = 30

    fun ammoItemName(url: String): String? {
        val c = AMMO_PATTERN.find(url)?.groupValues?.get(1)?.let { decodeField(it) } ?: return null
        return when (c) {
            "1" -> "big rock"
            "2" -> "Black Crimbo ball"
            "3" -> "White Crimbo ball"
            else -> null
        }
    }

    fun parseResponse(
        url: String,
        html: String,
        inventoryManager: InventoryManager?,
        resolveItemId: (String) -> Int? = { null },
    ) {
        if (!url.contains("crimbo21tree.php", ignoreCase = true)) return
        val action = ACTION_PATTERN.find(url)?.groupValues?.get(1) ?: return
        if (!action.equals("b", ignoreCase = true)) return
        val name = ammoItemName(url) ?: return
        val id = when (name) {
            "big rock" -> BIG_ROCK_ID
            else -> resolveItemId(name) ?: return
        }
        inventoryManager?.consumeItemLocally(id, 1)
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo21tree.php", ignoreCase = true)) return false
        val action = ACTION_PATTERN.find(url)?.groupValues?.get(1)
        if (action.equals("j", ignoreCase = true)) return true
        val itemName = ammoItemName(url) ?: "something"
        sessionLogger?.appendRawLine("Throwing $itemName at the Crimbo tree")
        return true
    }

    private fun decodeField(raw: String): String =
        raw.replace("+", " ")
            .replace(Regex("%([0-9A-Fa-f]{2})")) { m ->
                m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
            }
}
