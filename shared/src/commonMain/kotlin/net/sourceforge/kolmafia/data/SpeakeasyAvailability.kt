package net.sourceforge.kolmafia.data

/** Desktop ClanLoungeRequest.availableSpeakeasyDrinks — lounge visit availability set. */
object SpeakeasyAvailability {

    private val availableLoungeIds = mutableSetOf<Int>()

    private val DRINK_ROW_PATTERN = Regex("""name="drink"\s+value="(\d+)"""")

    fun reset() {
        availableLoungeIds.clear()
    }

    fun addLoungeId(id: Int) {
        if (SpeakeasyDatabase.loungeIdToIndex(id) >= 0) {
            availableLoungeIds.add(id)
        }
    }

    fun addFromHtml(html: String) {
        for (match in DRINK_ROW_PATTERN.findAll(html)) {
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { addLoungeId(it) }
        }
    }

    fun isAvailable(name: String): Boolean {
        val index = SpeakeasyDatabase.nameToIndex(name)
        if (index < 0) return false
        val loungeId = SpeakeasyDatabase.entries[index].loungeId
        return loungeId in availableLoungeIds
    }

    fun isAvailableItemId(itemId: Int): Boolean {
        val entry = SpeakeasyDatabase.entries.firstOrNull { it.itemId == itemId } ?: return false
        return entry.loungeId in availableLoungeIds
    }

    internal fun resetForTest() {
        reset()
    }

    internal fun loungeIdsForTest(): Set<Int> = availableLoungeIds.toSet()
}
