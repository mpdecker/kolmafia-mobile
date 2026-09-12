package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [net.sourceforge.kolmafia.request.UpdateSuppressedRequest] —
 * GenericRequest variant that suppresses result/charpane update follow-ups.
 * Headless marker: callers that need silent navigation can tag URLs via [shouldSuppress].
 */
object UpdateSuppressedRequest {
    fun shouldSuppress(url: String): Boolean =
        url.contains("ajax=1", ignoreCase = true) ||
            url.contains("api.php", ignoreCase = true)

    fun shouldFollowRedirect(): Boolean = true
}

/**
 * Desktop [net.sourceforge.kolmafia.request.concoction.shop.Crimbo12Request] —
 * shop.php?whichshop=crimbo12 create register / ingredient session-log.
 */
object Crimbo12Request {
    private val CREATE_PATTERN = Regex(
        """shop\.php.*whichshop=crimbo12.*whichitem=(\d+).*quantity=(\d+)""",
        RegexOption.IGNORE_CASE,
    )

    fun registerRequest(
        url: String,
        sessionLogger: SessionLogger? = null,
        ingredientSummary: ((itemId: Int, quantity: Int) -> String?)? = null,
    ): Boolean {
        val m = CREATE_PATTERN.find(url) ?: return false
        val itemId = m.groupValues[1].toIntOrNull() ?: return false
        val quantity = m.groupValues[2].toIntOrNull() ?: 1
        val line = ingredientSummary?.invoke(itemId, quantity)
            ?: "Create $quantity item #$itemId at Crimbo12 shop"
        sessionLogger?.appendRawLine(line)
        return true
    }
}

/**
 * Desktop [net.sourceforge.kolmafia.request.concoction.WaxGlobRequest] —
 * choice 1218 wax glob craft register.
 */
object WaxGlobRequest {
    private const val CHOICE = 1218
    const val WAX_GLOB = 9310
    private const val WAX_HAND = 9305
    private const val MINIATURE_CANDLE = 9306
    private const val WAX_PANCAKE = 9307
    private const val WAX_FACE = 9308
    private const val WAX_BOOZE = 9309

    fun optionForItemId(itemId: Int): String = when (itemId) {
        MINIATURE_CANDLE -> "1"
        WAX_HAND -> "2"
        WAX_FACE -> "3"
        WAX_PANCAKE -> "4"
        WAX_BOOZE -> "5"
        else -> "6"
    }

    fun optionToName(option: Int): String = when (option) {
        1 -> "miniature candle"
        2 -> "wax hand"
        3 -> "wax face"
        4 -> "wax pancake"
        5 -> "wax booze"
        else -> "unknown"
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("choice.php", ignoreCase = true)) return false
        if (!url.contains("whichchoice=$CHOICE")) return false
        val option = Regex("""[?&]option=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()
        if (option != null && option in 1..5) {
            sessionLogger?.appendRawLine("Creating ${optionToName(option)} from wax glob")
        }
        return true
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("whichchoice=$CHOICE")) return false
        if (!html.contains("You acquire", ignoreCase = true)) return false
        preferences?.setBoolean("_waxGlobCrafted", true)
        return true
    }
}
