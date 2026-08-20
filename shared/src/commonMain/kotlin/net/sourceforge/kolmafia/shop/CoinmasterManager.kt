package net.sourceforge.kolmafia.shop

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.adventure.choice.OutfitPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.IslandWarCampSync
import net.sourceforge.kolmafia.quest.IslandWarVisitLogSync
import net.sourceforge.kolmafia.quest.IslandWarVisitSync
import net.sourceforge.kolmafia.session.SessionLogger

open class CoinmasterManager(
    private val coinmasterRequest: CoinmasterRequest,
    private val inventoryManager: InventoryManager?,
    private val gameDatabase: GameDatabase?,
    private val client: HttpClient,
    private val character: KoLCharacter? = null,
    private val preferences: Preferences? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    open fun resolveMaster(value: String): CoinmasterData? =
        CoinmasterRegistry.findByNickname(value)
            ?: CoinmasterRegistry.all.firstOrNull {
                it.masterName.equals(value, ignoreCase = true)
            }

    open fun findMasterForBuyItem(itemId: Int): Pair<CoinmasterData, ShopRow>? =
        CoinmasterRegistry.findBuyRowForItem(itemId)

    open suspend fun visit(master: CoinmasterData): Boolean {
        val shopId = master.shopId
        val buyUrl = master.buyUrl
        if (shopId == null && buyUrl.isNullOrBlank()) return false
        return try {
            val (html, url) = if (shopId != null) {
                val visitUrl = "$KOL_BASE_URL/shop.php?whichshop=$shopId"
                val body = client.get("$KOL_BASE_URL/shop.php") {
                    parameter("whichshop", shopId)
                }.bodyAsText()
                body to visitUrl
            } else {
                val path = buyUrl!!.removePrefix("/")
                val isSwagger = master.nickname.equals("swagger", ignoreCase = true)
                val visitUrl = if (isSwagger) {
                    "$KOL_BASE_URL/$path?place=shop"
                } else {
                    "$KOL_BASE_URL/$path"
                }
                val body = client.get("$KOL_BASE_URL/$path") {
                    if (isSwagger) {
                        parameter("place", "shop")
                    }
                }.bodyAsText()
                body to visitUrl
            }
            if (master.nickname.equals("swagger", ignoreCase = true)) {
                SwaggerShopSync.applyVisitShop(html, url, preferences, null, character?.state?.value)
            } else if (shopId != null) {
                ShopInventorySync.parseAndLearn(
                    html = html,
                    url = url,
                    prefs = preferences,
                    state = character?.state?.value,
                )
            }
            val ascension = character?.state?.value?.ascensionNumber ?: 0
            NpcShopSync.applyShopVisit(html, url, preferences, ascension)
            true
        } catch (_: Exception) {
            false
        }
    }

    open suspend fun buy(master: CoinmasterData, itemId: Int, quantity: Int): Int {
        if (quantity <= 0) return 0
        val row = master.buyRowFor(itemId) ?: return 0
        val before = inventoryCount(itemId)
        val rowOrItemId = if (master.useItemField) itemId else row.rowId
        val response = coinmasterRequest.buy(master, rowOrItemId, quantity)
        if (response.isFailure) return 0
        val html = response.getOrThrow()
        if (master.nickname.equals("swagger", ignoreCase = true)) {
            val url = "${master.buyUrl ?: "peevpee.php"}?place=shop&action=buy&whichitem=$itemId&howmany=$quantity"
            SwaggerShopSync.applyBuy(html, url, preferences, inventoryManager, sessionLogger)
        }
        applyCampCoinmasterResponse(master, master.buyAction, itemId, quantity, html)
        inventoryManager?.fetchInventory()
        val after = inventoryCount(itemId)
        val bought = (after - before).coerceAtLeast(0)
        if (bought > 0) {
            CoinmasterPurchasePrefs.applyPurchasedItem(master, itemId, preferences, gameDatabase)
        }
        return bought
    }

    open suspend fun sell(master: CoinmasterData, itemId: Int, quantity: Int): Int {
        if (quantity <= 0) return 0
        val row = master.sellRowFor(itemId) ?: return 0
        val before = inventoryCount(itemId)
        val rowOrItemId = if (master.useItemField) itemId else row.rowId
        val response = coinmasterRequest.sell(master, rowOrItemId, quantity)
        if (response.isFailure) return 0
        applyCampCoinmasterResponse(master, master.sellAction, itemId, quantity, response.getOrThrow())
        inventoryManager?.fetchInventory()
        val after = inventoryCount(itemId)
        return (before - after).coerceAtLeast(0)
    }

    open suspend fun buyItem(itemId: Int, quantity: Int): Int {
        val (master, _) = findMasterForBuyItem(itemId) ?: return 0
        return buy(master, itemId, quantity)
    }

    open fun buysItem(master: CoinmasterData, itemId: Int): Boolean =
        master.buyRowFor(itemId) != null

    open fun buyPrice(master: CoinmasterData, itemId: Int): Int {
        val row = master.buyRowFor(itemId) ?: return 0
        if (row.price > 0) return row.price
        val cost = row.costs.firstOrNull() ?: return 0
        return cost.count
    }

    open fun sellPrice(master: CoinmasterData, itemId: Int): Int {
        val row = master.sellRowFor(itemId) ?: return 0
        return row.price
    }

    open fun sellsItem(master: CoinmasterData, itemId: Int): Boolean {
        val shopId = master.shopId?.lowercase()
        if (shopId != null && CoinmasterVisitInventory.hasVisitSellOverlay(shopId)) {
            return CoinmasterVisitInventory.containsSellItem(shopId, itemId)
        }
        return master.sellRowFor(itemId) != null
    }

    open fun isAccessible(master: CoinmasterData): Boolean {
        if (!master.isAccessible()) return false
        val char = character?.state?.value ?: return true
        return CoinmasterAccessibility.isAccessible(master, char, preferences)
    }

    open fun inaccessibleReason(master: CoinmasterData): String {
        if (!master.isAccessible()) return "Shop not available"
        val char = character?.state?.value
        if (char != null) {
            CoinmasterAccessibility.inaccessibleReason(master, char, preferences)?.let { return it }
        }
        return ""
    }

    private fun applyCampCoinmasterResponse(
        master: CoinmasterData,
        action: String,
        itemId: Int,
        quantity: Int,
        html: String,
    ) {
        if (!IslandWarCampSync.isCampCoinmaster(master)) return
        val prefs = preferences ?: return
        val url = IslandWarCampSync.buildCampTransactionUrl(master, action, itemId, quantity)
        val context = islandVisitContext()
        IslandWarVisitLogSync.register(url, html, prefs, context, sessionLogger)
        IslandWarCampSync.parseCampResponse(url, html, prefs, context, sessionLogger)
        if (master.tokenItemId() == null) {
            ConcoctionDatabase.markRefreshNeeded()
        }
    }

    private fun islandVisitContext(): IslandWarVisitSync.IslandVisitContext {
        val equipment = character?.state?.value?.equipment ?: emptyMap()
        return IslandWarVisitSync.IslandVisitContext(
            hasItemId = { id ->
                inventoryManager?.state?.value?.items?.containsKey(id) == true
            },
            consumeItem = { itemId, qty ->
                inventoryManager?.consumeItemLocally(itemId, qty)
            },
            isWearingWarHippyOutfit = {
                val outfit = OutfitDatabase.getById(OutfitPool.WAR_HIPPY_OUTFIT)
                    ?: return@IslandVisitContext false
                OutfitManager.isWearingPieces(outfit.equipment, equipment)
            },
            ascensionNumber = character?.state?.value?.ascensionNumber ?: 0,
        )
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
}
