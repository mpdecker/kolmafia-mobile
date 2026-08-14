package net.sourceforge.kolmafia.data

/** Resolved cafe menu row for purchase HTTP. */
data class CafeMenuEntry(
    val name: String,
    val cafeId: String,
    val whichItem: Int,
    val price: Int,
    val type: ConsumableType,
)

internal object CafeMenuLookup {
    fun find(
        query: String,
        entries: List<CafeMenuEntry>,
        resolve: (String) -> CafeMenuEntry?,
    ): CafeMenuEntry? {
        val q = query.trim()
        if (q.isEmpty()) return null
        resolve(q)?.let { return it }
        val lower = q.lowercase()
        return entries.firstOrNull { it.name.lowercase().contains(lower) }
    }
}
