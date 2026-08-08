package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.item.FreeCraftingTurns
import net.sourceforge.kolmafia.preferences.Preferences

/** Budget deltas applied by a single craft-queue push (desktop QueuedConcoction metadata). */
data class ConcoctionQueueReservation(
    val resultName: String,
    val quantity: Int,
    val adventuresUsed: Int = 0,
    val freeCraftingTurns: Int = 0,
    val stillsUsed: Int = 0,
    val tomesUsed: Int = 0,
    val extrudesUsed: Int = 0,
    val meatSpent: Int = 0,
    val pullsUsed: Int = 0,
    val fullnessUsed: Int = 0,
    val inebrietyUsed: Int = 0,
    val grossInebrietyUsed: Int = 0,
    val spleenHitUsed: Int = 0,
    val mimeShotglassUsed: Boolean = false,
    val mayodiolSwapApplied: Boolean = false,
    val queueBucket: ConcoctionOrganAmounts.QueueBucket = ConcoctionOrganAmounts.QueueBucket.CRAFT,
    val ingredientBucket: ConcoctionQueuedIngredients.Bucket? = null,
    val ingredientCredits: Map<Int, Int> = emptyMap(),
    val specialQueueDelta: SpecialQueueDelta = SpecialQueueDelta("", 0),
    val pseudoIngredientDelta: PseudoIngredientDelta = PseudoIngredientDelta(),
    val preferences: Preferences? = null,
)

/** Desktop Concoction.queue — reserve/release craft budget counters in [ConcoctionQueueBudget]. */
object ConcoctionQueueReserve {

    private const val PLASTIC_SWORD = 1
    private const val BORIS_KEY = 326
    private const val JARLSBERG_KEY = 327
    private const val SNEAKY_PETE_KEY = 328

    fun reserve(
        concoction: ConcoctionData,
        quantity: Int,
        runtime: ConcoctionRuntimeState,
        context: ConcoctionQueueContext,
        adjustQueued: Boolean = true,
    ): ConcoctionQueueReservation {
        if (quantity <= 0) {
            return ConcoctionQueueReservation(concoction.result, 0)
        }

        val ingredientCredits = mutableMapOf<Int, Int>()
        val before = snapshotBudget()
        val ingredientBucket = queueInternal(
            concoction,
            quantity,
            runtime,
            context,
            adjustQueued,
            ingredientBucket = null,
            ingredientCredits = ingredientCredits,
        )
        val after = snapshotBudget()

        val pseudoDelta = PseudoIngredientDelta(
            adventures = after.adventuresUsed - before.adventuresUsed,
            meat = after.meatSpent - before.meatSpent,
            pulls = after.pullsUsed - before.pullsUsed,
            tomes = after.tomesUsed - before.tomesUsed,
            stills = after.stillsUsed - before.stillsUsed,
            extrudes = after.extrudesUsed - before.extrudesUsed,
            freeCrafts = after.freeCraftingTurns - before.freeCraftingTurns,
        )
        if (ingredientBucket != null && pseudoDelta != PseudoIngredientDelta()) {
            ConcoctionQueuedPseudoIngredients.track(ingredientBucket, pseudoDelta)
        }

        return ConcoctionQueueReservation(
            resultName = concoction.result,
            quantity = quantity,
            adventuresUsed = after.adventuresUsed - before.adventuresUsed,
            freeCraftingTurns = after.freeCraftingTurns - before.freeCraftingTurns,
            stillsUsed = after.stillsUsed - before.stillsUsed,
            tomesUsed = after.tomesUsed - before.tomesUsed,
            extrudesUsed = after.extrudesUsed - before.extrudesUsed,
            meatSpent = after.meatSpent - before.meatSpent,
            pullsUsed = after.pullsUsed - before.pullsUsed,
            ingredientBucket = ingredientBucket,
            ingredientCredits = ingredientCredits.toMap(),
            pseudoIngredientDelta = pseudoDelta,
        )
    }

    fun release(reservation: ConcoctionQueueReservation) {
        ConcoctionQueueBudget.adventuresUsed -= reservation.adventuresUsed
        ConcoctionQueueBudget.freeCraftingTurns -= reservation.freeCraftingTurns
        ConcoctionQueueBudget.stillsUsed -= reservation.stillsUsed
        ConcoctionQueueBudget.tomesUsed -= reservation.tomesUsed
        ConcoctionQueueBudget.extrudesUsed -= reservation.extrudesUsed
        ConcoctionQueueBudget.meatSpent -= reservation.meatSpent
        ConcoctionQueueBudget.pullsUsed -= reservation.pullsUsed

        val bucket = reservation.ingredientBucket
        if (bucket != null) {
            if (reservation.pseudoIngredientDelta != PseudoIngredientDelta()) {
                ConcoctionQueuedPseudoIngredients.release(bucket, reservation.pseudoIngredientDelta)
            }
            for ((itemId, quantity) in reservation.ingredientCredits) {
                ConcoctionQueuedIngredients.releaseConsumption(bucket, itemId, quantity)
            }
        }
    }

    fun reapply(reservation: ConcoctionQueueReservation) {
        ConcoctionQueueBudget.adventuresUsed += reservation.adventuresUsed
        ConcoctionQueueBudget.freeCraftingTurns += reservation.freeCraftingTurns
        ConcoctionQueueBudget.stillsUsed += reservation.stillsUsed
        ConcoctionQueueBudget.tomesUsed += reservation.tomesUsed
        ConcoctionQueueBudget.extrudesUsed += reservation.extrudesUsed
        ConcoctionQueueBudget.meatSpent += reservation.meatSpent
        ConcoctionQueueBudget.pullsUsed += reservation.pullsUsed

        val bucket = reservation.ingredientBucket
        if (bucket != null) {
            if (reservation.pseudoIngredientDelta != PseudoIngredientDelta()) {
                ConcoctionQueuedPseudoIngredients.track(bucket, reservation.pseudoIngredientDelta)
            }
            for ((itemId, quantity) in reservation.ingredientCredits) {
                ConcoctionQueuedIngredients.trackConsumption(bucket, itemId, quantity)
            }
        }
    }

    private data class BudgetSnapshot(
        val adventuresUsed: Int,
        val freeCraftingTurns: Int,
        val stillsUsed: Int,
        val tomesUsed: Int,
        val extrudesUsed: Int,
        val meatSpent: Int,
        val pullsUsed: Int,
    )

    private fun snapshotBudget(): BudgetSnapshot = BudgetSnapshot(
        adventuresUsed = ConcoctionQueueBudget.adventuresUsed,
        freeCraftingTurns = ConcoctionQueueBudget.freeCraftingTurns,
        stillsUsed = ConcoctionQueueBudget.stillsUsed,
        tomesUsed = ConcoctionQueueBudget.tomesUsed,
        extrudesUsed = ConcoctionQueueBudget.extrudesUsed,
        meatSpent = ConcoctionQueueBudget.meatSpent,
        pullsUsed = ConcoctionQueueBudget.pullsUsed,
    )

    private fun queueInternal(
        concoction: ConcoctionData,
        amount: Int,
        runtime: ConcoctionRuntimeState,
        context: ConcoctionQueueContext,
        adjustQueued: Boolean,
        ingredientBucket: ConcoctionQueuedIngredients.Bucket?,
        ingredientCredits: MutableMap<Int, Int>,
    ): ConcoctionQueuedIngredients.Bucket? {
        val organBucket = ConcoctionOrganAmounts.queueBucket(
            concoction.result,
            ItemDatabase.getByName(concoction.result),
            context,
        )
        val bucket = ingredientBucket
            ?: ConcoctionQueuedIngredients.fromOrganBucket(organBucket)

        val decrementAmount = minOf(runtime.initial, amount)
        val creatableAmount = maxOf(runtime.creatable, 0)
        val overAmount = minOf(creatableAmount, amount - decrementAmount)
        var pullAmount = (amount - decrementAmount - overAmount).coerceAtLeast(0)
        if (runtime.price > 0 || !context.isPermitted(concoction)) {
            pullAmount = 0
        }
        if (pullAmount > 0) {
            ConcoctionQueueBudget.pullsUsed += pullAmount
        }

        val itemId = ItemDatabase.getByName(concoction.result)?.id
        if (itemId != null && decrementAmount > 0 && !isNonConsumableQueueItem(itemId) && bucket != null) {
            ConcoctionQueuedIngredients.trackConsumption(bucket, itemId, decrementAmount)
            ingredientCredits[itemId] = (ingredientCredits[itemId] ?: 0) + decrementAmount
        }

        val advPerCraft = ConcoctionCreationCost.adventureUsage(concoction.methods)
        val advUnits = advPerCraft * overAmount
        if (advUnits > 0) {
            val freeCap = FreeCraftingTurns.freeCraftingTurns(context.freeCrafting)
            repeat(advUnits) {
                if (ConcoctionQueueBudget.freeCraftingTurns < freeCap) {
                    ConcoctionQueueBudget.freeCraftingTurns++
                } else {
                    ConcoctionQueueBudget.adventuresUsed++
                }
            }
        }

        when (ConcoctionCreationCost.primaryMethod(concoction.methods)) {
            "STILL" -> ConcoctionQueueBudget.stillsUsed += overAmount
            "CLIPART" -> ConcoctionQueueBudget.tomesUsed += overAmount
            "TERMINAL" -> ConcoctionQueueBudget.extrudesUsed += overAmount
        }

        if (runtime.price > 0) {
            ConcoctionQueueBudget.meatSpent += runtime.price * (amount - decrementAmount - overAmount)
        }

        if (runtime.price > 0 || !context.isPermitted(concoction)) {
            return bucket
        }

        val yield = concoction.craftYield.coerceAtLeast(1)
        val craftBatches = (overAmount + yield - 1) / yield
        if (craftBatches <= 0) {
            return bucket
        }

        val resultItemId = itemId
        val ingredients = ConcoctionInterchangeableIngredients.resolve(
            concoction,
            resultItemId,
            context.availableCountById,
        )
        for (ingredient in ingredients) {
            val child = ConcoctionDatabase.getByResult(ingredient.name) ?: continue
            val childRuntime = context.runtimeFor(child.result) ?: continue
            val childAmount = craftBatches * ingredient.quantity
            if (childAmount <= 0) continue
            queueInternal(
                child,
                childAmount,
                childRuntime,
                context,
                adjustQueued = false,
                ingredientBucket = bucket,
                ingredientCredits = ingredientCredits,
            )
        }
        return bucket
    }

    private fun isNonConsumableQueueItem(itemId: Int): Boolean =
        itemId == PLASTIC_SWORD ||
            itemId == BORIS_KEY ||
            itemId == JARLSBERG_KEY ||
            itemId == SNEAKY_PETE_KEY
}
