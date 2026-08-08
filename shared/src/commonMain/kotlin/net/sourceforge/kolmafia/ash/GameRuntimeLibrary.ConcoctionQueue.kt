package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.isCreateSupported

internal suspend fun GameRuntimeLibrary.drainQueue(
    bucket: QueueBucket,
    type: ConcoctionConsumptionType,
): Boolean {
    val runner = concoctionQueueRunner ?: return false
    return runner.handleQueue(
        bucket = bucket,
        type = type,
        preferences = preferences,
        state = character?.state?.value,
    ).isSuccess
}

internal suspend fun GameRuntimeLibrary.drainCreateQueues(): Boolean {
    val buckets = listOf(
        QueueBucket.FOOD,
        QueueBucket.BOOZE,
        QueueBucket.SPLEEN,
        QueueBucket.POTION,
    )
    for (bucket in buckets) {
        if (!drainQueue(bucket, ConcoctionConsumptionType.NONE)) return false
    }
    return true
}

internal suspend fun GameRuntimeLibrary.createItem(itemId: Int, count: Int): Boolean {
    if (count <= 0) return true
    val name = gameDatabase?.item(itemId)?.name
        ?: ItemDatabase.getById(itemId)?.name
        ?: return false
    val concoction = ConcoctionDatabase.getByResult(name)
    if (concoction?.isCreateSupported() == true) {
        return concoctionCreateRequest?.create(
            name,
            count,
            state = character?.state?.value,
            preferences = preferences,
        )?.isSuccess == true
    }
    return (retrieveItemService?.retrieve(itemId, count) ?: 0) >= count
}
