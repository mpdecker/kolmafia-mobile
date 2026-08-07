package net.sourceforge.kolmafia.data

/** Desktop ConcoctionDatabase queuedFullness/Inebriety/SpleenHit reservation on craft queue push/pop. */
object ConcoctionOrganQueueReserve {

    private const val MIME_SHOTGLASS = 9676
    private const val MIME_PREF = "_mimeArmyShotglassUsed"

    fun reserve(resultName: String, quantity: Int, context: ConcoctionQueueContext): OrganReservationDelta {
        if (quantity <= 0) {
            return OrganReservationDelta(resultName = resultName, quantity = 0)
        }

        val item = ItemDatabase.getByName(resultName)
        val itemId = item?.id ?: 0
        val bucket = ConcoctionOrganAmounts.queueBucket(resultName, item, context)
        val hit = ConcoctionOrganAmounts.organHit(resultName)

        var mayodiolSwapApplied = false
        if (bucket == ConcoctionOrganAmounts.QueueBucket.FOOD &&
            ConcoctionQueueBudget.lastQueuedMayo == ConcoctionMayoQueue.MAYODIOL
        ) {
            ConcoctionQueueBudget.queuedFullness--
            ConcoctionQueueBudget.queuedInebriety++
            mayodiolSwapApplied = true
        }

        val fullnessUsed = hit.fullness * quantity
        val grossInebriety = hit.inebriety * quantity
        val spleenHitUsed = hit.spleenHit * quantity

        ConcoctionQueueBudget.queuedFullness += fullnessUsed
        ConcoctionQueueBudget.queuedInebriety += grossInebriety
        ConcoctionQueueBudget.queuedSpleenHit += spleenHitUsed

        var mimeShotglassUsed = false
        var inebrietyUsed = grossInebriety
        if (grossInebriety == 1 &&
            !ConcoctionQueueBudget.queuedMimeShotglass &&
            context.availableCountById(MIME_SHOTGLASS) > 0 &&
            !context.getBooleanPref(MIME_PREF)
        ) {
            ConcoctionQueueBudget.queuedInebriety--
            ConcoctionQueueBudget.queuedMimeShotglass = true
            mimeShotglassUsed = true
            inebrietyUsed = 0
        }

        if (ConcoctionMayoQueue.isMayo(itemId)) {
            ConcoctionQueueBudget.lastQueuedMayo = itemId
        } else {
            ConcoctionQueueBudget.lastQueuedMayo = 0
        }

        return OrganReservationDelta(
            resultName = resultName,
            quantity = quantity,
            itemId = itemId,
            fullnessUsed = fullnessUsed,
            inebrietyUsed = inebrietyUsed,
            grossInebrietyUsed = grossInebriety,
            spleenHitUsed = spleenHitUsed,
            mimeShotglassUsed = mimeShotglassUsed,
            mayodiolSwapApplied = mayodiolSwapApplied,
            queueBucket = bucket,
            baseInebrietyPerUnit = hit.inebriety,
        )
    }

    fun release(
        delta: OrganReservationDelta,
        remainingReservations: List<ConcoctionQueueReservation> = emptyList(),
    ) {
        ConcoctionQueueBudget.queuedFullness -= delta.fullnessUsed
        ConcoctionQueueBudget.queuedInebriety -= delta.grossInebrietyUsed
        ConcoctionQueueBudget.queuedSpleenHit -= delta.spleenHitUsed

        if (delta.mayodiolSwapApplied) {
            ConcoctionQueueBudget.queuedFullness++
            ConcoctionQueueBudget.queuedInebriety--
        }

        if (delta.mimeShotglassUsed && delta.queueBucket == ConcoctionOrganAmounts.QueueBucket.BOOZE) {
            val hasRemainingOneInebrietyDrink = remainingReservations.any { entry ->
                entry.queueBucket == ConcoctionOrganAmounts.QueueBucket.BOOZE &&
                    ConcoctionOrganAmounts.organHit(entry.resultName).inebriety == 1
            }
            if (!hasRemainingOneInebrietyDrink) {
                ConcoctionQueueBudget.queuedInebriety++
                ConcoctionQueueBudget.queuedMimeShotglass = false
            }
        }

        if (ConcoctionMayoQueue.isMayo(delta.itemId)) {
            ConcoctionQueueBudget.lastQueuedMayo = 0
        }

        val tail = remainingReservations.lastOrNull()
        if (tail != null) {
            val tailId = ItemDatabase.getByName(tail.resultName)?.id ?: 0
            if (ConcoctionMayoQueue.isMayo(tailId)) {
                ConcoctionQueueBudget.lastQueuedMayo = tailId
                if (tailId == ConcoctionMayoQueue.MAYODIOL) {
                    ConcoctionQueueBudget.queuedFullness++
                    ConcoctionQueueBudget.queuedInebriety--
                }
            }
        }
    }
}

/** Organ budget deltas for a single craft-queue push (merged into [ConcoctionQueueReservation]). */
data class OrganReservationDelta(
    val resultName: String,
    val quantity: Int,
    val itemId: Int = 0,
    val fullnessUsed: Int = 0,
    val inebrietyUsed: Int = 0,
    val grossInebrietyUsed: Int = 0,
    val spleenHitUsed: Int = 0,
    val mimeShotglassUsed: Boolean = false,
    val mayodiolSwapApplied: Boolean = false,
    val queueBucket: ConcoctionOrganAmounts.QueueBucket = ConcoctionOrganAmounts.QueueBucket.CRAFT,
    val baseInebrietyPerUnit: Int = 0,
)
