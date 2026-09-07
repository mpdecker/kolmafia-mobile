package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.Crimbo09Request] — crimbo09.php. */
object Crimbo09Request {
    private const val ELF_RESISTANCE_BUTTON = 4363
    private const val CRIMBUCK = 4343

    private val ACTION_PATTERN = Regex("""[?&]action=([^&]*)""", RegexOption.IGNORE_CASE)
    private val HOWMANY_PATTERN = Regex("""[?&]howmany=(\d+)""", RegexOption.IGNORE_CASE)
    private val WHICHBET_PATTERN = Regex("""[?&]whichbet=(\d+)""", RegexOption.IGNORE_CASE)

    fun parseResponse(
        url: String,
        html: String,
        inventoryManager: InventoryManager?,
        preferences: Preferences? = null,
    ) {
        if (!url.contains("crimbo09.php", ignoreCase = true)) return
        val action = ACTION_PATTERN.find(url)?.groupValues?.get(1) ?: return
        when (action.lowercase()) {
            "buygift" -> return // coinmaster cartel — consolidated elsewhere
            "tradearmbands" -> {
                val count = inventoryManager?.getCount(ELF_RESISTANCE_BUTTON) ?: 0
                if (count > 0) inventoryManager?.consumeItemLocally(ELF_RESISTANCE_BUTTON, count)
            }
            "new11", "double11", "slotmachine" -> {
                val bet = WHICHBET_PATTERN.find(url)?.groupValues?.get(1)?.toIntOrNull()
                    ?: preferences?.getInt("_crimbo09Bet", 0)
                    ?: 0
                if (bet > 0) inventoryManager?.consumeItemLocally(CRIMBUCK, bet)
            }
        }
        @Suppress("UNUSED_VARIABLE")
        val unused = html
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo09.php", ignoreCase = true)) return false
        val action = ACTION_PATTERN.find(url)?.groupValues?.get(1)
        val message = when {
            action == null && url.contains("place=don", ignoreCase = true) ->
                "Visiting Don Crimbo in Crimbo Town"
            action == null && url.contains("place=store", ignoreCase = true) ->
                "Visiting the Crimbo Cartel in Crimbo Town"
            action.equals("tradearmbands", ignoreCase = true) ->
                "Trading elf resistance buttons for Crimbux"
            action.equals("new11", ignoreCase = true) ->
                "Playing Crimbo 11"
            action.equals("slotmachine", ignoreCase = true) -> {
                val n = HOWMANY_PATTERN.find(url)?.groupValues?.get(1) ?: "1"
                "Playing the Crimbo slot machine $n time(s)"
            }
            action.equals("buygift", ignoreCase = true) -> null // cartel claims
            else -> null
        }
        if (message != null) sessionLogger?.appendRawLine(message)
        return true
    }
}

/** Desktop [net.sourceforge.kolmafia.request.Crimbo10Request] — crimbo10.php. */
object Crimbo10Request {
    fun parseResponse(url: String, html: String) {
        if (!url.contains("crimbo10.php", ignoreCase = true)) return
        // buygift routed to CRIMBCO gift shop coinmaster; no local inventory side effects here
        @Suppress("UNUSED_VARIABLE")
        val unused = html
    }

    fun locationName(url: String): String? = when {
        url.contains("place=office", ignoreCase = true) -> "Mr. Mination's Office"
        url.contains("place=giftshop", ignoreCase = true) -> "the Gift Shop"
        else -> null
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("crimbo10.php", ignoreCase = true)) return false
        val action = Regex("""[?&]action=([^&]*)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)
        if (action.equals("buygift", ignoreCase = true)) return true
        if (action != null) return false
        val place = locationName(url) ?: return true
        sessionLogger?.appendRawLine("Visiting $place in CRIMBCO Headquarters")
        return true
    }
}

/**
 * Desktop [net.sourceforge.kolmafia.request.concoction.CombineMeatRequest] —
 * craft.php?action=makepaste meat→paste/stack.
 */
object CombineMeatRequest {
    private const val MEAT_PASTE = 25
    private const val MEAT_STACK = 88
    private const val DENSE_STACK = 258

    fun getCost(itemId: Int): Int = when (itemId) {
        MEAT_PASTE -> 10
        MEAT_STACK -> 100
        DENSE_STACK -> 1000
        else -> 0
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("action=makepaste", ignoreCase = true) &&
            !(url.contains("inventory.php", ignoreCase = true) && url.contains("makepaste", ignoreCase = true))
        ) {
            return false
        }
        val itemId = Regex("""[?&]whichitem=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        val cost = getCost(itemId)
        if (cost == 0) return false
        val qty = Regex("""[?&]qty=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        sessionLogger?.appendRawLine("Create $qty meat paste/stack (item #$itemId)")
        return true
    }

    fun parseResponse(
        url: String,
        html: String,
        deductMeat: (Int) -> Unit = {},
    ) {
        if (!registerRequest(url)) return
        val itemId = Regex("""[?&]whichitem=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return
        val cost = getCost(itemId)
        val qty = Regex("""[?&]qty=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        // inventory.php path does not echo meat loss in HTML — deduct locally
        if (url.contains("inventory.php", ignoreCase = true)) {
            deductMeat(cost * qty)
        }
        @Suppress("UNUSED_VARIABLE")
        val unused = html
    }
}
