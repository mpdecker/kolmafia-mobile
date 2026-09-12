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
    ConcoctionDatabase.ensureRefreshed()
    val name = gameDatabase?.item(itemId)?.name
        ?: ItemDatabase.getById(itemId)?.name
        ?: return false
    val concoction = ConcoctionDatabase.getByResult(name)
    // Create router residual: prefer typed CreateItemRequest path when supported.
    if (concoction?.isCreateSupported() == true) {
        val created = concoctionCreateRequest?.create(
            name,
            count,
            state = character?.state?.value,
            preferences = preferences,
        )
        if (created?.isSuccess == true) {
            ConcoctionDatabase.markRefreshNeeded()
            return true
        }
        // Specialty residual: fall through to retrieve when typed hub missing/failed
    }
    // Fallback retrieve (includes nested craft) when no typed create hub is wired.
    val retrieved = (retrieveItemService?.retrieve(itemId, count) ?: 0) >= count
    if (retrieved) ConcoctionDatabase.markRefreshNeeded()
    return retrieved
}
