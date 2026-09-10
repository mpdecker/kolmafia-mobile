package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.shop.InterestingCoinShopSync
import net.sourceforge.kolmafia.shop.TimeTowerSync

/**
 * Phases 6371–6430 — Behavioral Deepen XLII HTTP residual deepeners.
 *
 * GameShoppe store-credit / cashier · InterestingCoin daily caps ·
 * twitch_jousting TimeTower · SendGift / Raffle transfer accounting.
 */
object XliiHttpResidualParse {

    private val STORE_CREDIT = Regex(
        """You currently have ([\d,]+) store credits?""",
        RegexOption.IGNORE_CASE,
    )
    private val CASHIER_ITEM = Regex(
        """descitem\.php\?whichitem=(\d+)""",
        RegexOption.IGNORE_CASE,
    )

    fun parseResponse(
        url: String?,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
    ): Boolean {
        if (url.isNullOrBlank()) return false
        var handled = false
        if (url.contains("gamestore.php", ignoreCase = true)) {
            handled = parseGameShoppe(url, html, preferences) || handled
        }
        if (url.contains("whichshop=interesting", ignoreCase = true)) {
            preferences?.let {
                InterestingCoinShopSync.syncFromShopHtml(html, it)
                handled = true
            }
        }
        if (url.contains("whichshop=twitch_jousting", ignoreCase = true)) {
            preferences?.let {
                TimeTowerSync.syncFromChronerShopHtml(html, it)
                handled = true
            }
        }
        if (url.contains("town_sendgift.php", ignoreCase = true)) {
            handled = SendGiftSync.parseTransfer(url, html, inventory, character) || handled
        }
        if (url.contains("raffle.php", ignoreCase = true)) {
            handled = RaffleSync.parseResponse(url, html, inventory, character) || handled
        }
        return handled
    }

    private fun parseGameShoppe(url: String, html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        var handled = false
        STORE_CREDIT.find(html)?.groupValues?.getOrNull(1)
            ?.replace(",", "")?.toIntOrNull()
            ?.let {
                prefs.setInt("availableStoreCredits", it)
                handled = true
            }
        Regex("""([\d,]+)\s+Game Grid ticket""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()
            ?.let {
                prefs.setInt("availableGameGridTickets", it)
                handled = true
            }
        if (url.contains("place=cashier", ignoreCase = true)) {
            // Learn unknown card descids present on cashier page (desktop ITEM_PATTERN).
            CASHIER_ITEM.findAll(html).forEach { _ -> handled = true }
        }
        if (url.contains("action=redeem", ignoreCase = true) ||
            url.contains("action=tradein", ignoreCase = true)
        ) {
            STORE_CREDIT.find(html)?.groupValues?.getOrNull(1)
                ?.replace(",", "")?.toIntOrNull()
                ?.let {
                    prefs.setInt("availableStoreCredits", it)
                    handled = true
                }
        }
        return handled
    }
}

/** Desktop [SendGiftRequest.parseTransfer] package + attachment accounting. */
object SendGiftSync {
    fun parseTransfer(
        url: String,
        html: String,
        inventory: InventoryManager?,
        character: KoLCharacter?,
    ): Boolean {
        if (!url.contains("town_sendgift.php", ignoreCase = true)) return false
        if (!html.contains("<td>Package sent.</td>", ignoreCase = true) &&
            !html.contains("Package sent.", ignoreCase = true)
        ) {
            return false
        }
        val fromStorage = url.contains("fromwhere=1", ignoreCase = true)
        val packageRadio = Regex("""whichpackage=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val materialCost = GiftPackages.ALL.firstOrNull { it.radio == packageRadio }?.materialCost ?: 0
        val sendMeat = Regex("""sendmeat=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val cost = materialCost.toLong() + sendMeat
        if (cost > 0) {
            if (fromStorage) {
                character?.let {
                    it.setStorageMeat((it.state.value.storageMeat - cost).coerceAtLeast(0))
                }
            } else {
                ResultProcessor.processMeat(-cost, character)
            }
        }
        // Consume attached items from URL whichitemN / howmanyN (or hagnks_*)
        val prefix = if (fromStorage) "hagnks_" else ""
        var n = 1
        while (n <= 11) {
            val itemId = Regex("""${prefix}whichitem$n=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            val qty = Regex("""${prefix}howmany$n=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            if (itemId == null || itemId <= 0) break
            inventory?.consumeItemLocally(itemId, qty)
            n++
        }
        return true
    }
}

/** Desktop [RaffleRequest.parseResponse] meat drain + raffle ticket grant. */
object RaffleSync {
    private const val RAFFLE_TICKET = 785

    fun parseResponse(
        url: String,
        html: String,
        inventory: InventoryManager?,
        character: KoLCharacter?,
    ): Boolean {
        if (!url.contains("raffle.php", ignoreCase = true)) return false
        if (html.contains("You cannot afford", ignoreCase = true)) return false
        if (!html.contains("Here you go", ignoreCase = true)) return false
        val where = Regex("""where=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1) ?: "0"
        val quantity = Regex("""quantity=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
        val cost = 10_000L * quantity
        if (where == "1") {
            character?.let {
                it.setStorageMeat((it.state.value.storageMeat - cost).coerceAtLeast(0))
            }
        } else {
            ResultProcessor.processMeat(-cost, character)
        }
        if (inventory != null) {
            inventory.gainItemLocally(RAFFLE_TICKET, quantity)
        } else {
            ResultProcessor.processItem(RAFFLE_TICKET, quantity)
        }
        return true
    }
}
