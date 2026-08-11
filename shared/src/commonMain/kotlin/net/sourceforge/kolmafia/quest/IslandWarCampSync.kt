package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shop.CoinmasterData
import kotlin.math.max

/** Desktop [IslandManager.parseCamp] / [CoinMasterRequest.parseResponse] camp transaction hooks. */
object IslandWarCampSync {

    fun isCampCoinmaster(master: CoinmasterData): Boolean =
        master.nickname == "dimemaster" || master.nickname == "quartersmaster"

    fun buildCampTransactionUrl(
        master: CoinmasterData,
        action: String,
        itemId: Int,
        quantity: Int,
    ): String {
        val whichcamp = when (master.nickname) {
            "dimemaster" -> "1"
            "quartersmaster" -> "2"
            else -> return ""
        }
        return "bigisland.php?place=camp&whichcamp=$whichcamp&action=$action&whichitem=$itemId&howmany=$quantity"
    }

    private val ACTION_PATTERN = Regex("""action=([^&]+)""")
    private val WHICHITEM_PATTERN = Regex("""whichitem=(\d+)""")
    private val QUANTITY_PATTERN = Regex("""quantity=(\d+)""")
    private val HOWMANY_PATTERN = Regex("""howmany=(\d+)""")

    fun parseCampResponse(
        url: String,
        html: String,
        preferences: Preferences,
        context: IslandWarVisitSync.IslandVisitContext = IslandWarVisitSync.IslandVisitContext(),
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        if (!url.contains("whichcamp")) return false
        val coinmaster = IslandWarVisitSync.findCampMaster(url) ?: return false

        var changed = false
        when (getAction(url)) {
            coinmaster.buyAction -> {
                if (completeCampPurchase(coinmaster, url, html, preferences, sessionLogger)) {
                    changed = true
                }
            }
            coinmaster.sellAction -> {
                if (completeCampSale(coinmaster, url, html, preferences, context, sessionLogger)) {
                    changed = true
                }
            }
        }
        if (IslandWarVisitSync.parseCampTokenBalance(coinmaster, html, preferences)) {
            changed = true
        }
        return changed
    }

    internal fun getAction(url: String): String? =
        ACTION_PATTERN.find(url)?.groupValues?.getOrNull(1)

    internal fun extractItemId(url: String): Int? =
        WHICHITEM_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()

    internal fun extractCount(url: String): Int {
        QUANTITY_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        HOWMANY_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        return 1
    }

    internal fun completeCampPurchase(
        coinmaster: CoinmasterData,
        url: String,
        html: String,
        preferences: Preferences,
        sessionLogger: SessionLogger?,
    ): Boolean {
        if (html.contains("You don't have enough") || html.contains("Huh?")) {
            return false
        }
        val property = coinmaster.property ?: return false
        val itemId = extractItemId(url) ?: return false
        val count = extractCount(url).coerceAtLeast(1)
        val price = coinmaster.buyRowFor(itemId)?.price ?: return false
        val cost = price * count
        val current = preferences.getInt(property, 0)
        preferences.setInt(property, max(0, current - cost))
        sessionLogger?.appendRawLine(
            "trading $cost ${tokenPlural(coinmaster, cost)} for $count ${itemName(itemId, count)}",
        )
        return true
    }

    internal fun completeCampSale(
        coinmaster: CoinmasterData,
        url: String,
        html: String,
        preferences: Preferences,
        context: IslandWarVisitSync.IslandVisitContext,
        sessionLogger: SessionLogger?,
    ): Boolean {
        if (html.contains("You don't have that many")) {
            return false
        }
        val property = coinmaster.property ?: return false
        val itemId = extractItemId(url) ?: return false
        val count = extractCount(url).coerceAtLeast(1)
        val price = coinmaster.sellRowFor(itemId)?.price ?: return false
        val gain = price * count
        context.consumeItem(itemId, count)
        preferences.setInt(property, preferences.getInt(property, 0) + gain)
        val token = coinmaster.token ?: "token"
        val tokenLabel = if (gain == 1) token else "${token}s"
        sessionLogger?.appendRawLine(
            "trading $count ${itemName(itemId, count)} for $gain $tokenLabel",
        )
        sessionLogger?.appendRawLine(
            "You acquire $gain $token${if (gain == 1) "" else "s"}",
        )
        return true
    }

    private fun itemName(itemId: Int, count: Int): String =
        if (count == 1) {
            ItemDatabase.getItemName(itemId).ifBlank { "item $itemId" }
        } else {
            ItemDatabase.getPluralName(itemId).ifBlank { "items $itemId" }
        }

    private fun tokenPlural(coinmaster: CoinmasterData, cost: Int): String {
        val token = coinmaster.token ?: "token"
        return if (cost == 1) token else "${token}s"
    }
}
