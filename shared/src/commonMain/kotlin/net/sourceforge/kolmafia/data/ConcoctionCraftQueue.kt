package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket

/** Desktop ConcoctionDatabase push/pop — per-bucket craft queue with budget reservation. */
object ConcoctionCraftQueue {

    private val buckets = linkedMapOf(
        QueueBucket.FOOD to mutableListOf<ConcoctionQueueReservation>(),
        QueueBucket.BOOZE to mutableListOf(),
        QueueBucket.SPLEEN to mutableListOf(),
        QueueBucket.POTION to mutableListOf(),
    )
    private val globalOrder = mutableListOf<ConcoctionQueueReservation>()

    private val clearOrder = listOf(
        QueueBucket.FOOD,
        QueueBucket.BOOZE,
        QueueBucket.SPLEEN,
        QueueBucket.POTION,
    )

    fun push(resultName: String, quantity: Int, context: ConcoctionQueueContext): Boolean {
        if (quantity <= 0) return false
        val concoction = ConcoctionDatabase.getByResult(resultName) ?: return false
        val runtime = context.runtimeFor(concoction.result) ?: return false
        if (context.isFancyDog(resultName) && !HotDogDatabase.canQueueFancyDog(context)) {
            return false
        }
        val itemId = ItemDatabase.getByName(resultName)?.id ?: 0
        if (itemId != 0 &&
            ConcoctionSpecialQueue.isSpeakeasyDrink(itemId) &&
            !SpeakeasyDatabase.canQueueSpeakeasyDrink(quantity, context)
        ) {
            return false
        }
        val specialDelta = ConcoctionSpecialQueue.reserve(resultName, quantity, context)
        val organDelta = ConcoctionOrganQueueReserve.reserve(resultName, quantity, context)
        val reservation = ConcoctionQueueReserve.reserve(concoction, quantity, runtime, context).copy(
            fullnessUsed = organDelta.fullnessUsed,
            inebrietyUsed = organDelta.inebrietyUsed,
            grossInebrietyUsed = organDelta.grossInebrietyUsed,
            spleenHitUsed = organDelta.spleenHitUsed,
            mimeShotglassUsed = organDelta.mimeShotglassUsed,
            mayodiolSwapApplied = organDelta.mayodiolSwapApplied,
            queueBucket = organDelta.queueBucket,
            specialQueueDelta = specialDelta,
            preferences = context.preferences,
        )
        val store = storageBucket(reservation.queueBucket)
        buckets.getValue(store).add(reservation)
        globalOrder.add(reservation)
        applyQueuedDelta(reservation, sign = 1)
        ConcoctionDatabase.refreshAfterQueueMutation()
        return true
    }

    fun pop(): ConcoctionQueueReservation? {
        val reservation = globalOrder.removeLastOrNull() ?: return null
        buckets.getValue(storageBucket(reservation.queueBucket)).remove(reservation)
        return releaseReservation(reservation)
    }

    fun pop(bucket: QueueBucket): ConcoctionQueueReservation? {
        val store = storageBucket(bucket)
        val reservation = buckets.getValue(store).removeLastOrNull() ?: return null
        globalOrder.remove(reservation)
        return releaseReservation(reservation, remainingInBucket = buckets.getValue(store).toList())
    }

    fun clear() {
        for (bucket in clearOrder) {
            while (depth(bucket) > 0) {
                pop(bucket)
            }
        }
    }

    fun depth(bucket: QueueBucket): Int = buckets.getValue(storageBucket(bucket)).size

    fun entries(bucket: QueueBucket): List<ConcoctionQueueReservation> =
        buckets.getValue(storageBucket(bucket)).toList()

    fun entries(): List<ConcoctionQueueReservation> =
        clearOrder.flatMap { buckets.getValue(it) }

    internal fun resetForTest() {
        for (stack in buckets.values) {
            stack.clear()
        }
        globalOrder.clear()
    }

    internal fun storageBucket(bucket: QueueBucket): QueueBucket = when (bucket) {
        QueueBucket.CRAFT -> QueueBucket.POTION
        else -> bucket
    }

    private fun releaseReservation(
        reservation: ConcoctionQueueReservation,
        remainingInBucket: List<ConcoctionQueueReservation>? = null,
    ): ConcoctionQueueReservation {
        val bucketRemainder = remainingInBucket
            ?: buckets.getValue(storageBucket(reservation.queueBucket)).toList()
        ConcoctionQueueReserve.release(reservation)
        ConcoctionOrganQueueReserve.release(
            OrganReservationDelta(
                resultName = reservation.resultName,
                quantity = reservation.quantity,
                itemId = ItemDatabase.getByName(reservation.resultName)?.id ?: 0,
                fullnessUsed = reservation.fullnessUsed,
                inebrietyUsed = reservation.inebrietyUsed,
                grossInebrietyUsed = reservation.grossInebrietyUsed,
                spleenHitUsed = reservation.spleenHitUsed,
                mimeShotglassUsed = reservation.mimeShotglassUsed,
                mayodiolSwapApplied = reservation.mayodiolSwapApplied,
                queueBucket = reservation.queueBucket,
            ),
            remainingReservations = bucketRemainder,
        )
        ConcoctionSpecialQueue.release(
            reservation.specialQueueDelta,
            ConcoctionQueueContext(preferences = reservation.preferences),
        )
        applyQueuedDelta(reservation, sign = -1)
        ConcoctionDatabase.refreshAfterQueueMutation()
        return reservation
    }

    private fun applyQueuedDelta(reservation: ConcoctionQueueReservation, sign: Int) {
        val key = reservation.resultName.lowercase()
        val current = ConcoctionDatabase.getRuntime(reservation.resultName) ?: return
        ConcoctionDatabase.setRuntimeForTest(
            key,
            current.copy(
                queued = (current.queued + sign * reservation.quantity).coerceAtLeast(0),
                queuedPulls = (current.queuedPulls + sign * reservation.pullsUsed).coerceAtLeast(0),
            ),
        )
    }
}
