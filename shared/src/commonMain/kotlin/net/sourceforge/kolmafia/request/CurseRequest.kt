package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.CurseRequest] curse.php. */
object CurseRequest {
    private const val CRIMBO_TRAINING_MANUAL = 11046
    private const val SMORE_GUN = 11057

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences? = null,
        inventory: InventoryManager? = null,
        sessionLogger: SessionLogger? = null,
    ) {
        if (!url.contains("curse.php", ignoreCase = true)) return
        if (!url.contains("action=use", ignoreCase = true)) return
        val itemId = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return
        when (itemId) {
            CRIMBO_TRAINING_MANUAL -> {
                if (html.contains("You've already trained somebody today", ignoreCase = true) ||
                    html.contains("You train", ignoreCase = true)
                ) {
                    preferences?.setBoolean("_crimboTraining", true)
                }
            }
            ItemPool.PING_PONG_TABLE -> {
                if (html.contains("You play", ignoreCase = true) ||
                    html.contains("already played ping-pong", ignoreCase = true)
                ) {
                    preferences?.setBoolean("_pingPongGame", true)
                }
            }
            SMORE_GUN -> inventory?.consumeItemLocally(ItemPool.MARSHMALLOW, 1)
            else -> inventory?.consumeItemLocally(itemId, 1)
        }
        sessionLogger?.appendRawLine("curse.php whichitem=$itemId")
    }

    fun registerRequest(url: String): Boolean = url.contains("curse.php", ignoreCase = true)
}
