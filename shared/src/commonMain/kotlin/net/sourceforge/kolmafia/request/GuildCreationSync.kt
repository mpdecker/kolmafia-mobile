package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop guild.php `malussmash` / `makestaff` post-response ingredient accounting. */
object GuildCreationSync {
    private val WHICH_ITEM_PATTERN = Regex("""whichitem=(\d+)""")
    private val WHICH_STAFF_PATTERN = Regex("""whichstaff=(\d+)""")
    private val QUANTITY_PATTERN = Regex("""quantity=(\d+)""")
    private const val MALUS_INGREDIENT_MULTIPLIER = 5
    private const val MISSING_STAFF_INGREDIENTS_MESSAGE =
        "You don't have all of the items I'll need to make that Chefstaff."

    fun parseFromVisit(
        url: String,
        responseText: String,
        eventBus: GameEventBus? = null,
        sessionLogger: SessionLogger? = null,
    ) {
        if (!url.contains("guild.php", ignoreCase = true)) return
        when (actionFromUrl(url)) {
            "malussmash" -> parseMalus(url, responseText, eventBus)
            "makestaff" -> parseStaff(url, responseText, eventBus, sessionLogger)
        }
    }

    fun parseMalus(
        url: String,
        responseText: String,
        eventBus: GameEventBus? = null,
    ) {
        if (!responseText.contains("You acquire", ignoreCase = true)) return

        val ingredientId = WHICH_ITEM_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return
        val concoction = ConcoctionDatabase.malusByIngredientItemId(ingredientId) ?: return

        val smashCount = QUANTITY_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
        val totalIngredientCount = smashCount * MALUS_INGREDIENT_MULTIPLIER

        for (ingredient in concoction.ingredients) {
            val itemId = ItemDatabase.getByName(ingredient.name)?.id ?: continue
            emitConsumed(eventBus, itemId, totalIngredientCount)
        }
    }

    fun parseStaff(
        url: String,
        responseText: String,
        eventBus: GameEventBus? = null,
        sessionLogger: SessionLogger? = null,
    ) {
        if (responseText.contains(MISSING_STAFF_INGREDIENTS_MESSAGE, ignoreCase = true)) {
            return
        }
        if (!responseText.contains("You acquire", ignoreCase = true)) return

        val baseStaffId = WHICH_STAFF_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return
        val concoction = ConcoctionDatabase.chefStaffByBaseItemId(baseStaffId) ?: return

        registerStaffRequest(url, sessionLogger)

        for (ingredient in concoction.ingredients) {
            val itemId = ItemDatabase.getByName(ingredient.name)?.id ?: continue
            emitConsumed(eventBus, itemId, ingredient.quantity)
        }
    }

    fun registerStaffRequest(url: String, sessionLogger: SessionLogger?) {
        val baseStaffId = WHICH_STAFF_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return
        val concoction = ConcoctionDatabase.chefStaffByBaseItemId(baseStaffId) ?: return

        val ingredients = concoction.ingredients.joinToString(", ") { ingredient ->
            "${ingredient.quantity} ${ingredient.name}"
        }
        sessionLogger?.appendRawLine("Chefstaff $ingredients")
    }

    private fun actionFromUrl(url: String): String? =
        Regex("""action=([^&]+)""").find(url)?.groupValues?.getOrNull(1)?.lowercase()

    private fun emitConsumed(eventBus: GameEventBus?, itemId: Int, quantity: Int) {
        if (quantity <= 0) return
        eventBus?.tryEmit(GameEvent.ItemConsumed(itemId, quantity))
    }
}
