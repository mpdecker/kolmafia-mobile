package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.craftMode
import net.sourceforge.kolmafia.data.isAutoCraftable
import net.sourceforge.kolmafia.data.isCreateSupported
import net.sourceforge.kolmafia.data.isStationCraftable
import net.sourceforge.kolmafia.data.isSuseCraftable
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemRestriction
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.RestrictionListRefresh
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.ThriftyRequest
import net.sourceforge.kolmafia.request.TrendyRequest
import net.sourceforge.kolmafia.request.UntinkerRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.shop.NpcShopSync
import net.sourceforge.kolmafia.request.ConcoctionCreateRequest

/**
 * Desktop [InventoryManager.doRetrieveItem] compound acquisition chain
 * (Phases 2511–2570): autoSatisfy source gates, freepull-before-storage,
 * useEquipped / familiar steal, create-vs-buy pricing.
 */
open class RetrieveItemService(
    private val inventoryManager: InventoryManager?,
    private val closetRequest: ClosetRequest?,
    private val storageRequest: StorageRequest?,
    private val displayCaseRequest: DisplayCaseRequest? = null,
    private val clanStashRequest: ClanStashRequest? = null,
    private val npcBuyRequest: NpcBuyRequest?,
    private val mallManager: MallManager?,
    private val coinmasterManager: CoinmasterManager? = null,
    private val craftRequest: CraftRequest? = null,
    private val useItemRequest: UseItemRequest? = null,
    private val gameDatabase: GameDatabase?,
    private val hermitRequest: HermitRequest? = null,
    private val familiarRequest: FamiliarRequest? = null,
    private val character: KoLCharacter? = null,
    private val preferences: Preferences? = null,
    private val standardRequest: StandardRequest? = null,
    private val thriftyRequest: ThriftyRequest? = null,
    private val trendyRequest: TrendyRequest? = null,
    private val specialtyCreateProvider: (() -> ConcoctionCreateRequest)? = null,
    private val createItemIngredientsProvider: (() -> CreateItemIngredients)? = null,
    private val equipmentRequest: EquipmentRequest? = null,
    private val familiarManager: FamiliarManager? = null,
    private val untinkerRequest: UntinkerRequest? = null,
    private val buyScriptRunner: ((String, List<String>) -> Boolean)? = null,
) {
    companion object {
        const val ABRIDGED_DICTIONARY = 534
        const val BRIDGE = 535
        const val WORTHLESS_ITEM = 13
    }

    open suspend fun retrieve(itemId: Int, qty: Int): Int =
        retrieve(itemId, qty, useEquipped = true)

    open suspend fun retrieve(itemId: Int, qty: Int, useEquipped: Boolean): Int {
        if (qty <= 0) return 0
        if (itemId <= 0) return 0

        if (ItemDatabase.isVirtualItem(itemId) &&
            ItemDatabase.haveVirtualItem(itemId, preferences)
        ) {
            return qty
        }

        val itemName = gameDatabase?.item(itemId)?.name
            ?: ItemDatabase.getItemName(itemId).ifEmpty { return 0 }
        var remaining = qty - inventoryCount(itemId)
        if (remaining <= 0) return qty

        val charState = character?.state?.value
        if (charState != null) {
            RestrictionListRefresh.ensureInitialized(
                charState,
                standardRequest,
                thriftyRequest,
                trendyRequest,
            )
        }
        val isRestricted = charState != null &&
            !ItemRestriction.isAllowed(itemId, itemName, charState, gameDatabase)
        if (isRestricted && !canCreateItem(itemId, itemName)) {
            return qty - remaining
        }

        // Bridge ← abridged dictionary untinker
        if (itemId == BRIDGE && remaining > 0 && inventoryCount(ABRIDGED_DICTIONARY) > 0) {
            remaining -= untinkerBridge()
            if (remaining <= 0) return qty
        }

        val isEquipment = ItemDatabase.getById(itemId)?.isEquipment == true

        // Familiar steal before unequip (desktop order)
        if (!isRestricted && isEquipment && remaining > 0 && familiarRequest != null) {
            remaining -= stealFromFamiliar(itemId, remaining)
            if (remaining <= 0) return qty
        }

        // Unequip worn copies when useEquipped
        if (!isRestricted && isEquipment && useEquipped && remaining > 0) {
            remaining -= unequipWorn(itemId, remaining)
            if (remaining <= 0) return qty
        }

        val prefs = preferences
        val canCloset = RetrieveSourceGates.canUseCloset(prefs, charState)
        val canStorage = RetrieveSourceGates.canUseStorage(prefs, charState)
        val canStash = RetrieveSourceGates.canUseClanStash(prefs, charState)
        val canDisplay = RetrieveSourceGates.canUseDisplay(prefs)
        val canNpc = RetrieveSourceGates.canUseNPCStores(prefs, charState)
        val tradeable = ItemDatabase.isTradeable(itemId)
        val canMall = RetrieveSourceGates.canUseMall(prefs, charState, tradeable)
        val canCoinmasters = RetrieveSourceGates.canUseCoinmasters(prefs, charState)

        if (remaining > 0 && canCloset && closetRequest != null) {
            remaining -= withdrawFromSource(itemId, remaining, CollectionBucket.CLOSET) { q ->
                closetRequest.takeOut(itemId, q)
            }
            if (remaining <= 0) return qty
        }

        // Freepull before storage (ronin/hardcore free pulls)
        if (!isRestricted && remaining > 0 && storageRequest != null) {
            remaining -= withdrawFreepull(itemId, remaining)
            if (remaining <= 0) return qty
        }

        if (!isRestricted && remaining > 0 && canStorage && storageRequest != null) {
            remaining -= withdrawFromSource(itemId, remaining, CollectionBucket.STORAGE) { q ->
                storageRequest.withdraw(itemId, q)
            }
            if (remaining <= 0) return qty
        }

        if (remaining > 0 && canDisplay && displayCaseRequest != null) {
            remaining -= withdrawFromSource(itemId, remaining, CollectionBucket.DISPLAY) { q ->
                displayCaseRequest.takeOut(itemId, q)
            }
            if (remaining <= 0) return qty
        }

        if (!isRestricted && remaining > 0 && canStash && clanStashRequest != null) {
            remaining -= withdrawFromSource(itemId, remaining, CollectionBucket.STASH) { q ->
                clanStashRequest.takeOut(itemId, q)
            }
            if (remaining <= 0) return qty
        }

        // Create-vs-buy: skip craft when buyScript / cheaperToBuy says buy
        val priceCtx = buildPriceContext()
        val defaultBuy = canMall && RetrievePricing.cheaperToBuy(itemId, remaining, priceCtx)
        val scriptSaysBuy = RetrievePricing.invokeBuyScript(
            prefs = prefs,
            itemName = itemName,
            qty = remaining,
            ingredientLevel = 2,
            defaultBuy = defaultBuy,
            runScript = buyScriptRunner,
        )

        if (remaining > 0 && !scriptSaysBuy) {
            remaining -= craftMissing(itemName, itemId, remaining)
            if (remaining <= 0) return qty
        }

        if (remaining > 0 && canCoinmasters && hermitRequest != null) {
            remaining -= withdrawFromHermit(itemId, remaining)
            if (remaining <= 0) return qty
        }

        if (remaining > 0 && canNpc && npcBuyRequest != null) {
            val npcStore = gameDatabase?.npcStoreFor(itemName)
                ?: NpcStoreDatabase.storeForItem(itemName)
            if (npcStore != null) {
                if (NpcShopSync.needsSync(npcStore.storeKey) && prefs != null && charState != null) {
                    npcBuyRequest.visitStore(
                        npcStore.storeKey,
                        prefs,
                        charState.ascensionNumber,
                    )
                }
                val before = inventoryCount(itemId)
                val bought = npcBuyRequest.buy(
                    npcStore.storeKey,
                    itemId,
                    remaining,
                    prefs,
                ).getOrDefault(0)
                inventoryManager?.fetchInventory()
                val gained = (inventoryCount(itemId) - before).coerceAtLeast(bought)
                remaining -= gained
                if (remaining <= 0) return qty
            }
        }

        if (remaining > 0 && canCoinmasters && coinmasterManager != null) {
            remaining -= coinmasterManager.buyItem(itemId, remaining)
            if (remaining <= 0) return qty
        }

        // Non-equipment familiar steal late (legacy mobile ordering fallback)
        if (remaining > 0 && !isEquipment && familiarRequest != null) {
            remaining -= stealFromFamiliar(itemId, remaining)
            if (remaining <= 0) return qty
        }

        if (remaining > 0 && canMall && mallManager != null) {
            val before = inventoryCount(itemId)
            val bought = mallManager.buy(itemId, remaining)
            inventoryManager?.fetchInventory()
            val gained = (inventoryCount(itemId) - before).coerceAtLeast(bought)
            remaining -= gained
        }

        return qty - remaining
    }

    private fun buildPriceContext(): RetrievePricing.PriceContext =
        RetrievePricing.PriceContext(
            inventoryCount = { inventoryCount(it) },
            mallPrice = { -1L }, // live mall lookup is async; ASH retrieve_price uses MallManager
            npcPrice = { id ->
                val name = ItemDatabase.getItemName(id)
                if (name.isBlank()) 0L else NpcStoreDatabase.npcPrice(name).toLong()
            },
            prefs = preferences,
            canCreate = { id ->
                val name = ItemDatabase.getItemName(id)
                name.isNotBlank() && canCreateItem(id, name)
            },
        )

    private suspend fun untinkerBridge(): Int {
        val before = inventoryCount(BRIDGE)
        val untinker = untinkerRequest ?: return 0
        untinker.untinker(ABRIDGED_DICTIONARY, 1)
        inventoryManager?.fetchInventory()
        return (inventoryCount(BRIDGE) - before).coerceAtLeast(0)
    }

    private suspend fun unequipWorn(itemId: Int, qty: Int): Int {
        val equipReq = equipmentRequest ?: return 0
        val itemName = ItemDatabase.getItemName(itemId)
        if (itemName.isBlank()) return 0
        var gained = 0
        // Dual-wield: unequip OFFHAND before WEAPON when both match
        val slots = EquipmentSlot.SEARCH_SLOTS.flatMap { slot ->
            when (slot) {
                EquipmentSlot.WEAPON -> listOf(EquipmentSlot.OFFHAND, EquipmentSlot.WEAPON)
                EquipmentSlot.OFFHAND -> emptyList() // already covered with WEAPON
                else -> listOf(slot)
            }
        }.distinct()
        for (slot in slots) {
            if (gained >= qty) break
            val equipped = character?.state?.value?.equipment?.get(slot).orEmpty()
            if (!equipped.equals(itemName, ignoreCase = true)) continue
            val before = inventoryCount(itemId)
            if (equipReq.unequipSlot(slot).isFailure) continue
            inventoryManager?.fetchInventory()
            val delta = (inventoryCount(itemId) - before).coerceAtLeast(0)
            if (delta <= 0) continue
            gained += delta
        }
        return gained.coerceAtMost(qty)
    }

    private suspend fun stealFromFamiliar(itemId: Int, qty: Int): Int {
        var gained = 0
        if (familiarRequest != null) {
            while (gained < qty) {
                val before = inventoryCount(itemId)
                if (familiarRequest.stealItem(itemId).isFailure) break
                inventoryManager?.fetchInventory()
                val delta = (inventoryCount(itemId) - before).coerceAtLeast(0)
                if (delta <= 0) break
                gained += delta
            }
        }
        val fams = familiarManager?.state?.value?.ownedFamiliars.orEmpty()
        val activeId = character?.state?.value?.familiarId
        for (fam in fams) {
            if (gained >= qty) break
            if (fam.id == activeId) continue
            if (fam.equipment?.itemId != itemId) continue
            val before = inventoryCount(itemId)
            if (familiarRequest?.unequipFamiliar(fam.id)?.isFailure != false) continue
            inventoryManager?.fetchInventory()
            val delta = (inventoryCount(itemId) - before).coerceAtLeast(0)
            if (delta > 0) gained += delta
        }
        return gained.coerceAtMost(qty)
    }

    private suspend fun withdrawFreepull(itemId: Int, qty: Int): Int {
        val storage = storageRequest ?: return 0
        val classified = storage.fetchClassifiedContents(character?.state?.value, preferences)
        val available = classified.freepulls[itemId] ?: 0
        if (available <= 0) return 0
        return withdrawFromSource(itemId, minOf(qty, available), CollectionBucket.STORAGE) { q ->
            storage.withdraw(itemId, q)
        }
    }

    private suspend fun withdrawFromHermit(itemId: Int, qty: Int): Int {
        val before = inventoryCount(itemId)
        if (hermitRequest!!.trade(itemId, qty).isFailure) return 0
        inventoryManager?.fetchInventory()
        return (inventoryCount(itemId) - before).coerceIn(0, qty)
    }

    private suspend fun withdrawFromSource(
        itemId: Int,
        qty: Int,
        bucket: CollectionBucket,
        withdraw: suspend (Int) -> Result<String>,
    ): Int {
        val before = inventoryCount(itemId)
        val result = withdraw(qty)
        if (result.isFailure) return 0
        inventoryManager?.fetchInventory()
        refreshCollectionCache(bucket)
        return (inventoryCount(itemId) - before).coerceIn(0, qty)
    }

    private suspend fun refreshCollectionCache(bucket: CollectionBucket) {
        val prefs = preferences ?: return
        when (bucket) {
            CollectionBucket.CLOSET ->
                closetRequest?.let { CollectionCacheSync.refreshCloset(it, prefs) }
            CollectionBucket.STORAGE ->
                storageRequest?.let {
                    CollectionCacheSync.refreshStorage(it, character?.state?.value, prefs)
                }
            CollectionBucket.STASH ->
                clanStashRequest?.let { CollectionCacheSync.refreshStash(it, prefs) }
            CollectionBucket.DISPLAY ->
                displayCaseRequest?.let { CollectionCacheSync.refreshDisplay(it, prefs) }
        }
    }

    private enum class CollectionBucket {
        CLOSET,
        STORAGE,
        STASH,
        DISPLAY,
    }

    private suspend fun craftMissing(itemName: String, itemId: Int, qty: Int): Int {
        val concoction = ConcoctionDatabase.getByResult(itemName) ?: return 0

        if (concoction.isAutoCraftable()) {
            if (concoction.isSuseCraftable() && useItemRequest != null) {
                return craftSuse(concoction, itemId, qty)
            }

            if (concoction.isStationCraftable()) {
                return craftAtStation(concoction, itemId, qty)
            }
        }

        if (concoction.isCreateSupported() && specialtyCreateProvider != null) {
            return craftSpecialty(concoction, itemId, qty)
        }

        return 0
    }

    private suspend fun craftSpecialty(
        concoction: net.sourceforge.kolmafia.data.ConcoctionData,
        itemId: Int,
        qty: Int,
    ): Int {
        val create = specialtyCreateProvider ?: return 0
        val before = inventoryCount(itemId)
        var remaining = qty
        while (remaining > 0) {
            val created = create.invoke()
                .create(concoction.result, 1)
                .getOrDefault(0)
            inventoryManager?.fetchInventory()
            if (created <= 0) break
            val gained = inventoryCount(itemId) - before
            remaining = qty - gained
            if (gained >= qty) break
        }
        return (inventoryCount(itemId) - before).coerceIn(0, qty)
    }

    private suspend fun craftSuse(
        concoction: net.sourceforge.kolmafia.data.ConcoctionData,
        itemId: Int,
        qty: Int,
    ): Int {
        val use = useItemRequest ?: return 0
        val helper = createItemIngredientsProvider?.invoke() ?: return 0
        val source = concoction.ingredients.firstOrNull()?.name ?: return 0
        val sourceId = gameDatabase?.item(source)?.id ?: return 0
        val state = character?.state?.value
        val before = inventoryCount(itemId)
        var attempts = 0
        while (inventoryCount(itemId) - before < qty && attempts < qty * 2) {
            attempts++
            if (!helper.makeIngredients(concoction, 1, state)) break
            if (use.use(sourceId, 1).isFailure) break
            inventoryManager?.fetchInventory()
        }
        return (inventoryCount(itemId) - before).coerceIn(0, qty)
    }

    private suspend fun craftAtStation(
        concoction: net.sourceforge.kolmafia.data.ConcoctionData,
        itemId: Int,
        qty: Int,
    ): Int {
        val mode = concoction.craftMode() ?: return 0
        val craft = craftRequest ?: return 0
        val helper = createItemIngredientsProvider?.invoke() ?: return 0
        val ing1 = gameDatabase?.item(concoction.ingredients[0].name)?.id ?: return 0
        val ing2 = gameDatabase?.item(concoction.ingredients[1].name)?.id ?: return 0
        val state = character?.state?.value
        val before = inventoryCount(itemId)
        var remaining = qty
        while (remaining > 0) {
            val batch = remaining.coerceAtMost(qty)
            if (!helper.makeIngredients(concoction, batch, state)) {
                return (inventoryCount(itemId) - before).coerceIn(0, qty)
            }
            val created = craft.craft(mode, batch, ing1, ing2)
            inventoryManager?.fetchInventory()
            if (created <= 0) break
            val gained = inventoryCount(itemId) - before
            remaining = qty - gained
            if (gained >= qty) break
        }
        return (inventoryCount(itemId) - before).coerceIn(0, qty)
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

    /** Desktop [InventoryManager.doRetrieveItem] restricted-item early exit helper. */
    private fun canCreateItem(itemId: Int, itemName: String): Boolean {
        val concoction = ConcoctionDatabase.getByResult(itemName)
        if (concoction?.isCreateSupported() == true) return true
        if (coinmasterManager != null && coinmasterManager.findMasterForBuyItem(itemId) != null) {
            return true
        }
        return false
    }
}
