package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.inventory.AccessCountContext
import net.sourceforge.kolmafia.inventory.AccessibleItemCount
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.NpcShopSync

/** Desktop CreateItemRequest.makeDough — rolling/unrolling pin dough conversion. */
class RollingPinCreateRequest(
    private val useItemRequest: UseItemRequest?,
    private val retrieveItemService: RetrieveItemService?,
    private val gameDatabase: GameDatabase?,
    private val npcBuyRequest: NpcBuyRequest? = null,
    private val preferences: Preferences? = null,
    private val inventoryManager: InventoryManager? = null,
    private val character: KoLCharacter? = null,
    private val accessibleCountFn: (suspend (Int, String) -> Int)? = null,
) {
    suspend fun create(concoction: ConcoctionData, quantity: Int): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        val outputId = gameDatabase?.item(concoction.result)?.id
            ?: ItemDatabase.getByName(concoction.result)?.id
            ?: return Result.failure(IllegalStateException("Unknown roll output: ${concoction.result}"))
        val recipe = DOUGH_RECIPES.firstOrNull { it.outputId == outputId }
            ?: return Result.failure(IllegalStateException("Unsupported roll output: ${concoction.result}"))
        val retrieve = retrieveItemService
            ?: return Result.failure(IllegalStateException("RetrieveItemService not configured"))
        val use = useItemRequest
            ?: return Result.failure(IllegalStateException("UseItemRequest not configured"))

        var totalCreated = 0
        var remaining = quantity
        while (remaining > 0) {
            val created = makeDoughBatch(recipe, remaining, retrieve, use).getOrElse { return Result.failure(it) }
            if (created <= 0) {
                break
            }
            totalCreated += created
            remaining -= created
        }
        return Result.success(totalCreated)
    }

    private suspend fun makeDoughBatch(
        recipe: DoughRecipe,
        quantity: Int,
        retrieve: RetrieveItemService,
        use: UseItemRequest,
    ): Result<Int> {
        var needed = quantity
        val available = accessibleCount(recipe.inputId, recipe.inputName)
        val retrieveQty = minOf(available, needed)
        var retrieved = 0
        if (retrieveQty > 0) {
            retrieved = retrieve.retrieve(recipe.inputId, retrieveQty)
        }

        var purchase = needed - if (retrieved > 0) retrieved else retrieveQty
        if (retrieveQty > 0 && retrieved < retrieveQty) {
            purchase = needed - inventoryCount(recipe.inputId)
        }

        if (purchase > 0) {
            buyWadsFromNpc(purchase)
            if (recipe.outputId == WAD_OF_DOUGH) {
                needed -= purchase
            }
        }

        if (needed <= 0) {
            return Result.success(quantity)
        }

        val toolName = ItemDatabase.getById(recipe.toolId)?.name ?: "tool"
        val usedTool = if (needed > 10) {
            if (retrieve.retrieve(recipe.toolId, 1) < 1) {
                return Result.failure(IllegalStateException("Please purchase a $toolName first."))
            }
            true
        } else {
            retrieve.retrieve(recipe.toolId, 1) >= 1
        }

        val beforeOutput = inventoryCount(recipe.outputId)

        if (usedTool) {
            val response = use.use(recipe.toolId, 1)
            response.exceptionOrNull()?.let { return Result.failure(it) }
            val body = response.getOrThrow()
            if (!body.contains("You acquire")) {
                return Result.failure(IllegalStateException("Rolling pin creation was unsuccessful."))
            }
        } else {
            repeat(needed) {
                val response = use.use(recipe.inputId, 1)
                response.exceptionOrNull()?.let { return Result.failure(it) }
                val body = response.getOrThrow()
                if (!body.contains("You acquire")) {
                    return Result.failure(IllegalStateException("Rolling pin creation was unsuccessful."))
                }
            }
        }

        val created = (inventoryCount(recipe.outputId) - beforeOutput).coerceAtLeast(
            if (usedTool) 1 else needed,
        )
        return Result.success(created.coerceAtMost(quantity))
    }

    private suspend fun buyWadsFromNpc(quantity: Int) {
        if (quantity <= 0) return
        val npcBuy = npcBuyRequest ?: return
        val store = gameDatabase?.npcStoreFor(WAD_OF_DOUGH_NAME)
            ?: NpcStoreDatabase.storeForItem(WAD_OF_DOUGH_NAME)
            ?: return
        val prefs = preferences
        val state = character?.state?.value
        if (prefs != null && state != null && NpcShopSync.needsSync(store.storeKey)) {
            npcBuy.visitStore(store.storeKey, prefs, state.ascensionNumber)
        }
        npcBuy.buy(store.storeKey, WAD_OF_DOUGH, quantity, prefs)
        inventoryManager?.fetchInventory()
    }

    private suspend fun accessibleCount(itemId: Int, itemName: String): Int {
        accessibleCountFn?.let { return it(itemId, itemName) }
        val state = character?.state?.value
        return AccessibleItemCount.physicalCount(
            itemId = itemId,
            itemName = itemName,
            inventoryManager = inventoryManager,
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            equipment = state?.equipment ?: emptyMap(),
            context = AccessCountContext(
                characterState = state,
                gameDatabase = gameDatabase,
                preferences = preferences,
            ),
        )
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

    private data class DoughRecipe(
        val outputId: Int,
        val inputId: Int,
        val inputName: String,
        val inputQuantity: Int,
        val toolId: Int,
    )

    companion object {
        private const val WAD_OF_DOUGH = 159
        private const val WAD_OF_DOUGH_NAME = "wad of dough"
        private const val FLAT_DOUGH = 301
        private const val ROLLING_PIN = 873
        private const val UNROLLING_PIN = 874

        private val DOUGH_RECIPES = listOf(
            DoughRecipe(
                outputId = FLAT_DOUGH,
                inputId = WAD_OF_DOUGH,
                inputName = WAD_OF_DOUGH_NAME,
                inputQuantity = 1,
                toolId = ROLLING_PIN,
            ),
            DoughRecipe(
                outputId = WAD_OF_DOUGH,
                inputId = FLAT_DOUGH,
                inputName = "flat dough",
                inputQuantity = 1,
                toolId = UNROLLING_PIN,
            ),
        )
    }
}
