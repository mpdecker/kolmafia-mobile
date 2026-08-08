package net.sourceforge.kolmafia.data

/** Snapshot for desktop Concoction.calculate2 / canMake during refreshConcoctionsNow. */
data class ConcoctionCreatableContext(
    val initialCount: (String) -> Int,
    val isPermitted: (ConcoctionData) -> Boolean = { true },
    val limitPools: ConcoctionLimitPools? = null,
    val inGLover: Boolean = false,
    val priceFor: (ConcoctionData) -> Int = { 0 },
    val knollAvailable: Boolean = false,
    val inZombiecore: Boolean = false,
    val coinmasterAcquirable: (Int) -> Int = { 0 },
    val availableCountById: (Int) -> Int = { id ->
        ItemDatabase.getById(id)?.name?.let { name -> initialCount(name.lowercase()) } ?: 0
    },
    val ingredientPriceFor: (Int) -> Int = ConcoctionInterchangeableIngredients::defaultPriceFor,
) {
    fun initialFor(concoction: ConcoctionData): Int = initialCount(concoction.result.lowercase())

    fun availableMeat(): Int = limitPools?.meatLimit?.initial ?: 0

    companion object {
        fun fromRuntime(initialCount: (String) -> Int): ConcoctionCreatableContext =
            ConcoctionCreatableContext(initialCount = initialCount)
    }
}

/** Mutable allocation state during a calculate2 binary-search pass (desktop Concoction.allocated). */
class ConcoctionAllocationTracker(
    private val limitPools: ConcoctionLimitPools? = null,
) {
    private val allocated = mutableMapOf<String, Int>()
    private val visited = mutableSetOf<String>()

    fun allocated(name: String): Int = allocated[name.lowercase()] ?: 0

    fun addAllocated(name: String, amount: Int) {
        val key = name.lowercase()
        allocated[key] = (allocated[key] ?: 0) + amount
    }

    fun subtractAllocated(name: String, amount: Int) {
        val key = name.lowercase()
        allocated[key] = (allocated[key] ?: 0) - amount
    }

    fun markVisited(name: String) {
        visited.add(name.lowercase())
    }

    fun resetAllocatedForVisited() {
        for (key in visited) {
            allocated[key] = 0
        }
        limitPools?.resetAllocated()
    }

    fun clear() {
        allocated.clear()
        visited.clear()
        limitPools?.resetAllocated()
    }
}

private fun runCreatableSearch(
    concoction: ConcoctionData,
    context: ConcoctionCreatableContext,
    turnFreeOnly: Boolean,
): Int {
    val initial = context.initialFor(concoction)
    if (concoction.ingredients.isEmpty() && context.priceFor(concoction) <= 0) {
        return initial
    }

    var maxSuccess = initial
    var minFailure = Int.MAX_VALUE
    var guess = maxSuccess + 1
    val tracker = ConcoctionAllocationTracker(context.limitPools)

    while (true) {
        var res = canMake(concoction, guess, context, tracker, turnFreeOnly)

        if (res >= guess) {
            maxSuccess = guess
        } else {
            minFailure = guess
            res = maxOf(res, (maxSuccess + minFailure) / 2)
        }

        if (maxSuccess + 1 >= minFailure) break

        guess = minOf(maxOf(res, maxSuccess + 1), minFailure - 1)
        tracker.resetAllocatedForVisited()
    }

    tracker.clear()
    return maxSuccess
}

/** Desktop Concoction.calculate2 post-adjust — creatable minus affordable meat buys. */
fun adjustCreatableForMeatPrice(
    total: Int,
    initial: Int,
    concoction: ConcoctionData,
    context: ConcoctionCreatableContext,
): Int {
    var creatable = (total - initial).coerceAtLeast(0)
    val price = context.priceFor(concoction)
    if (price <= 0) return creatable
    val itemId = ItemDatabase.getByName(concoction.result)?.id ?: return creatable
    if (ConcoctionBuyables.excludesCreatableMeatSubtract(itemId)) return creatable
    creatable = (creatable - context.availableMeat() / price).coerceAtLeast(0)
    return creatable
}

/** Desktop Concoction.calculate2 — binary search for max feasible total count. */
fun calculateCreatableTotal(
    concoction: ConcoctionData,
    context: ConcoctionCreatableContext,
): Int = runCreatableSearch(concoction, context, turnFreeOnly = false)

/** Desktop Concoction.calculate3 — turn-free creatable total (freeTotal). */
fun calculateCreatableFreeTotal(
    concoction: ConcoctionData,
    context: ConcoctionCreatableContext,
): Int = runCreatableSearch(concoction, context, turnFreeOnly = true)

/**
 * Desktop Concoction.canMake — determine if [requested] amount can be made from available ingredients.
 * Return value is >= requested when feasible, < requested when not; also used as search hint.
 */
fun canMake(
    concoction: ConcoctionData,
    requested: Int,
    context: ConcoctionCreatableContext,
    tracker: ConcoctionAllocationTracker,
    turnFreeOnly: Boolean = false,
): Int {
    tracker.markVisited(concoction.result)

    val initial = context.initialFor(concoction)
    var alreadyHave = initial - tracker.allocated(concoction.result)
    if (alreadyHave < 0 || requested <= 0) {
        return 0
    }

    tracker.addAllocated(concoction.result, requested)
    var needToMake = requested - alreadyHave

    // Meat is a pseudo-ingredient when price > 0 (desktop Concoction.canMake ~1018–1035).
    if (needToMake > 0) {
        val price = context.priceFor(concoction)
        if (price > 0) {
            val pool = context.limitPools?.meatLimit ?: run {
                tracker.subtractAllocated(concoction.result, requested)
                return alreadyHave
            }
            val buyable = pool.canMake(minOf(needToMake * price, Int.MAX_VALUE)) / price
            alreadyHave += buyable
            val applied = minOf(buyable, needToMake)
            tracker.subtractAllocated(concoction.result, applied)
            needToMake -= applied
        }
    }

    // NOCREATE — no recipe ingredients.
    if (concoction.ingredients.isEmpty()) {
        return alreadyHave
    }

    if (!context.isPermitted(concoction)) {
        return alreadyHave
    }

    if (ConcoctionCreationCost.primaryMethod(concoction.methods) == "COINMASTER") {
        val itemId = ItemDatabase.getByName(concoction.result)?.id
        tracker.subtractAllocated(concoction.result, requested)
        return if (itemId != null) {
            alreadyHave + context.coinmasterAcquirable(itemId)
        } else {
            alreadyHave
        }
    }

    // Exotic method branches deferred (FLOUNDRY, etc.) — standard ingredient trees below.
    if (needToMake <= 0) {
        return alreadyHave
    }

    val yield = concoction.craftYield.coerceAtLeast(1)
    needToMake = (needToMake + yield - 1) / yield
    var minMake = Int.MAX_VALUE

    val resultItemId = ItemDatabase.getByName(concoction.result)?.id
    val ingredients = ConcoctionInterchangeableIngredients.resolve(
        concoction,
        resultItemId,
        context.availableCountById,
        context.ingredientPriceFor,
    )
    var i = 0
    var len = ingredients.size
    while (minMake > 0 && i < len) {
        val ingredient = ingredients[i]
        val child = ConcoctionDatabase.getByResult(ingredient.name)
        if (child == null) {
            i++
            continue
        }
        var count = ingredient.quantity

        if (i == 0 && len == 2 &&
            ingredients[1].name.equals(ingredient.name, ignoreCase = true) &&
            ingredients[1].quantity == ingredient.quantity
        ) {
            count += ingredients[1].quantity
            len = 1
        }

        if (count <= 0) {
            minMake = 0
            break
        }

        val childMake = canMake(child, needToMake * count, context, tracker, turnFreeOnly)
        minMake = minOf(minMake, childMake / count)
        i++
    }

    if (minMake == Int.MAX_VALUE) {
        minMake = 0
    }

    // Implicit meat paste for COMBINE/ACOMBINE (desktop Concoction.canMake ~1189–1204).
    if (minMake > 0 &&
        ("COMBINE" in concoction.methods || "ACOMBINE" in concoction.methods) &&
        (!context.knollAvailable || context.inZombiecore)
    ) {
        val paste = ConcoctionDatabase.getByResult("meat paste")
        if (paste != null) {
            val pasteMake = canMake(paste, needToMake, context, tracker, turnFreeOnly)
            minMake = minOf(minMake, pasteMake)
        }
    }

    // Adventures are also considered an ingredient (desktop Concoction.canMake ~1206–1230).
    if (minMake > 0) {
        val advs = ConcoctionCreationCost.adventureUsage(concoction.methods)
        if (advs > 0) {
            val pool = context.limitPools?.poolFor(concoction.methods, turnFreeOnly)
            if (pool != null) {
                val advMake = pool.canMake(needToMake * advs)
                minMake = minOf(minMake, advMake / advs)
            }
        }
    }

    // Stills / clip art / terminal extrudes (desktop Concoction.canMake ~1241–1282).
    if (minMake > 0) {
        when (val method = ConcoctionCreationCost.primaryMethod(concoction.methods)) {
            "STILL", "TERMINAL" -> {
                val pool = context.limitPools?.methodPool(method)
                if (pool != null) {
                    minMake = minOf(minMake, pool.canMake(needToMake))
                }
            }
            "CLIPART" -> {
                if (context.inGLover) {
                    tracker.subtractAllocated(concoction.result, requested)
                    return alreadyHave
                }
                val pool = context.limitPools?.clipArtLimit
                if (pool != null) {
                    minMake = minOf(minMake, pool.canMake(needToMake))
                }
            }
            else -> Unit
        }
    }

    tracker.subtractAllocated(concoction.result, minOf(minMake, needToMake) * yield)
    return alreadyHave + minMake * yield
}
