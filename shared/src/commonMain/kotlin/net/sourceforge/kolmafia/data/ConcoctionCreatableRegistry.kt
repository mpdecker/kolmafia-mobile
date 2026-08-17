package net.sourceforge.kolmafia.data

/** Desktop ConcoctionDatabase.creatableList — incremental creatable snapshot after refresh. */
object ConcoctionCreatableRegistry {

    private val entriesByResult = linkedMapOf<String, ConcoctionCreatableEntry>()

    fun isRegistryEligible(concoction: ConcoctionData): Boolean {
        if (concoction.methods.isEmpty()) return false
        return ItemDatabase.getByName(concoction.result) != null
    }

    fun updateFromRefresh() {
        for (key in ConcoctionDatabase.byResult.keys.sorted()) {
            val concoction = ConcoctionDatabase.getByResult(key) ?: continue
            val runtime = ConcoctionDatabase.getRuntime(concoction.result) ?: continue
            val itemId = ItemDatabase.getByName(concoction.result)?.id ?: continue
            if (!isRegistryEligible(concoction)) continue

            val creatable = maxOf(runtime.creatable, 0)
            val pullable = maxOf(runtime.pullable, 0)
            val available = creatable + pullable
            val entry = ConcoctionCreatableEntry(
                resultName = concoction.result,
                itemId = itemId,
                creatable = creatable,
                pullable = pullable,
                methods = concoction.methods,
            )

            when {
                available == 0 && runtime.wasPossible -> {
                    entriesByResult.remove(key)
                    ConcoctionDatabase.updateRuntimeWasPossible(concoction.result, false)
                }
                available > 0 && !runtime.wasPossible -> {
                    entriesByResult[key] = entry
                    ConcoctionDatabase.updateRuntimeWasPossible(concoction.result, true)
                }
                available > 0 -> {
                    entriesByResult[key] = entry
                }
            }
        }
    }

    fun entries(): List<ConcoctionCreatableEntry> =
        entriesByResult.values.sortedBy { it.resultName.lowercase() }

    fun get(resultName: String): ConcoctionCreatableEntry? =
        entriesByResult[resultName.lowercase()]

    internal fun seedForTest(entry: ConcoctionCreatableEntry) {
        entriesByResult[entry.resultName.lowercase()] = entry
    }

    internal fun resetForTest() {
        entriesByResult.clear()
    }
}
