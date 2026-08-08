package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.craftMode
import net.sourceforge.kolmafia.data.isAutoCraftable
import net.sourceforge.kolmafia.data.isStationCraftable
import net.sourceforge.kolmafia.data.isSuseCraftable
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.item.RetrieveItemService

/** Desktop CreateItemRequest v1 — station/SUSE craft by concoction result name. */
class ConcoctionCreateRequest(
    private val retrieveItemService: RetrieveItemService?,
    private val craftRequest: CraftRequest?,
    private val useItemRequest: UseItemRequest?,
    private val gameDatabase: GameDatabase?,
) {

    suspend fun create(concoctionName: String, quantity: Int): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        val concoction = ConcoctionDatabase.getByResult(concoctionName)
            ?: return Result.failure(IllegalStateException("No concoction for: $concoctionName"))
        if (!concoction.isAutoCraftable()) {
            return Result.failure(IllegalStateException("Concoction not auto-craftable: $concoctionName"))
        }

        val retrieve = retrieveItemService
            ?: return Result.failure(IllegalStateException("RetrieveItemService not configured"))

        return try {
            val created = when {
                concoction.isSuseCraftable() -> createSuse(concoction, quantity, retrieve)
                concoction.isStationCraftable() -> createStation(concoction, quantity, retrieve)
                else -> 0
            }
            if (created < quantity) {
                Result.failure(IllegalStateException("Could not create $quantity of $concoctionName (got $created)"))
            } else {
                Result.success(created)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createSuse(
        concoction: ConcoctionData,
        quantity: Int,
        retrieve: RetrieveItemService,
    ): Int {
        val use = useItemRequest ?: return 0
        val sourceName = concoction.ingredients.firstOrNull()?.name ?: return 0
        val sourceId = gameDatabase?.item(sourceName)?.id ?: ItemDatabase.getByName(sourceName)?.id ?: return 0
        var created = 0
        repeat(quantity) {
            if (retrieve.retrieve(sourceId, 1) < 1) return created
            if (use.use(sourceId, 1).isFailure) return created
            created++
        }
        return created
    }

    private suspend fun createStation(
        concoction: ConcoctionData,
        quantity: Int,
        retrieve: RetrieveItemService,
    ): Int {
        val mode = concoction.craftMode()?.apiAction ?: return 0
        val craft = craftRequest ?: return 0
        val ing1Name = concoction.ingredients.getOrNull(0)?.name ?: return 0
        val ing2Name = concoction.ingredients.getOrNull(1)?.name ?: return 0
        val ing1 = gameDatabase?.item(ing1Name)?.id ?: ItemDatabase.getByName(ing1Name)?.id ?: return 0
        val ing2 = gameDatabase?.item(ing2Name)?.id ?: ItemDatabase.getByName(ing2Name)?.id ?: return 0

        var created = 0
        var remaining = quantity
        while (remaining > 0) {
            for (ingredient in concoction.ingredients) {
                val ingId = gameDatabase?.item(ingredient.name)?.id
                    ?: ItemDatabase.getByName(ingredient.name)?.id
                    ?: return created
                if (retrieve.retrieve(ingId, ingredient.quantity) < ingredient.quantity) {
                    return created
                }
            }
            val batch = remaining
            val crafted = craft.craft(mode, batch, ing1, ing2)
            if (crafted <= 0) break
            val gained = crafted.coerceAtMost(remaining)
            created += gained
            remaining -= gained
        }
        return created
    }
}
