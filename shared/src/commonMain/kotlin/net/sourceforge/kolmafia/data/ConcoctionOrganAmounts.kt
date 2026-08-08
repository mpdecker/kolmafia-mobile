package net.sourceforge.kolmafia.data

/** Desktop Concoction fullness/inebriety/spleen resolution for craft queue organ budgeting. */
object ConcoctionOrganAmounts {

    data class OrganHit(val fullness: Int, val inebriety: Int, val spleenHit: Int)

    enum class QueueBucket {
        FOOD,
        BOOZE,
        SPLEEN,
        POTION,
        CRAFT,
    }

    fun organHit(resultName: String): OrganHit = OrganHit(
        fullness = ConsumableDatabase.getFullnessByName(resultName),
        inebriety = ConsumableDatabase.getInebrietyByName(resultName),
        spleenHit = ConsumableDatabase.getSpleenByName(resultName),
    )

    fun queueBucket(
        resultName: String,
        item: ItemData? = ItemDatabase.getByName(resultName),
        context: ConcoctionQueueContext? = null,
    ): QueueBucket {
        val itemId = item?.id
        if (itemId != null && context != null) {
            if (ConcoctionMayoQueue.canQueueFood(itemId, context)) return QueueBucket.FOOD
            if (ConcoctionMayoQueue.canQueueBooze(itemId)) return QueueBucket.BOOZE
        }
        val hit = organHit(resultName)
        val primaryUse = item?.primaryUse
        return when {
            hit.fullness > 0 ||
                primaryUse == ItemPrimaryUse.FOOD ||
                primaryUse == ItemPrimaryUse.FOOD_HELPER ->
                QueueBucket.FOOD
            hit.inebriety > 0 ||
                primaryUse == ItemPrimaryUse.DRINK ||
                primaryUse == ItemPrimaryUse.DRINK_HELPER ->
                QueueBucket.BOOZE
            hit.spleenHit > 0 || primaryUse == ItemPrimaryUse.SPLEEN ->
                QueueBucket.SPLEEN
            hit.fullness == 0 && hit.inebriety == 0 && hit.spleenHit == 0 ->
                QueueBucket.CRAFT
            else ->
                QueueBucket.POTION
        }
    }
}
