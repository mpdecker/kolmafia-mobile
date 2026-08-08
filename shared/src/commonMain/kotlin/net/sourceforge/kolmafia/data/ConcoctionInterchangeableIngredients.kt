package net.sourceforge.kolmafia.data

/** Desktop ConcoctionDatabase.getIngredients / getBetterIngredient — interchangeable pairs. */
object ConcoctionInterchangeableIngredients {

    const val NICE_WARM_BEER = 11097

    private const val SCHLITZ = 41
    private const val WILLER = 81
    private const val KETCHUP = 106
    private const val CATSUP = 107
    private const val DYSPEPSI_COLA = 347
    private const val CLOACA_COLA = 1334
    private const val TITANIUM_UMBRELLA = 596
    private const val GOATSKIN_UMBRELLA = 2451

    private val INTERCHANGEABLE_PAIR = mapOf(
        SCHLITZ to WILLER,
        WILLER to SCHLITZ,
        KETCHUP to CATSUP,
        CATSUP to KETCHUP,
        DYSPEPSI_COLA to CLOACA_COLA,
        CLOACA_COLA to DYSPEPSI_COLA,
        TITANIUM_UMBRELLA to GOATSKIN_UMBRELLA,
        GOATSKIN_UMBRELLA to TITANIUM_UMBRELLA,
    )

    fun defaultPriceFor(itemId: Int): Int = ItemDatabase.getById(itemId)?.autosellPrice ?: 0

    fun resolve(
        concoction: ConcoctionData,
        resultItemId: Int?,
        availableCount: (Int) -> Int,
        priceFor: (Int) -> Int = ::defaultPriceFor,
    ): List<ConcoctionIngredient> {
        if (concoction.ingredients.size > 2) return concoction.ingredients
        if (resultItemId == NICE_WARM_BEER) return concoction.ingredients

        return concoction.ingredients.map { ingredient ->
            val itemId = ItemDatabase.getByName(ingredient.name)?.id ?: return@map ingredient
            val resolvedId = resolveIngredientId(itemId, availableCount, priceFor)
            if (resolvedId == itemId) {
                ingredient
            } else {
                val resolvedName = ItemDatabase.getById(resolvedId)?.name ?: ingredient.name
                ingredient.copy(name = resolvedName)
            }
        }
    }

    internal fun resolveIngredientId(
        itemId: Int,
        availableCount: (Int) -> Int,
        priceFor: (Int) -> Int,
    ): Int {
        val pairId = INTERCHANGEABLE_PAIR[itemId] ?: return itemId
        return getBetterIngredient(itemId, pairId, availableCount, priceFor)
    }

    internal fun getBetterIngredient(
        itemId1: Int,
        itemId2: Int,
        availableCount: (Int) -> Int,
        priceFor: (Int) -> Int,
    ): Int {
        var diff = availableCount(itemId1) - availableCount(itemId2)
        if (diff == 0) {
            diff = priceFor(itemId2) - priceFor(itemId1)
        }
        return if (diff > 0) itemId1 else itemId2
    }
}
