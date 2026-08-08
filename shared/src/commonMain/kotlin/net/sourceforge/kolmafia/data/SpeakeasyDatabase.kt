package net.sourceforge.kolmafia.data

/** Desktop ClanLoungeRequest.SpeakeasyDrink — static clan VIP speakeasy registry. */
object SpeakeasyDatabase {

    data class SpeakeasyEntry(
        val loungeId: Int,
        val name: String,
        val itemId: Int,
        val inebriety: Int,
        val cost: Int,
    )

    /** Order matches desktop SpeakeasyDrink enum. */
    val entries: List<SpeakeasyEntry> = listOf(
        SpeakeasyEntry(1, "glass of &quot;milk&quot;", 7589, 1, 250),
        SpeakeasyEntry(2, "cup of &quot;tea&quot;", 7590, 1, 250),
        SpeakeasyEntry(3, "thermos of &quot;whiskey&quot;", 7591, 1, 250),
        SpeakeasyEntry(4, "Lucky Lindy", 7592, 1, 500),
        SpeakeasyEntry(5, "Bee's Knees", 7593, 2, 500),
        SpeakeasyEntry(6, "Sockdollager", 7594, 2, 500),
        SpeakeasyEntry(7, "Ish Kabibble", 7595, 2, 500),
        SpeakeasyEntry(8, "Hot Socks", 7596, 3, 5000),
        SpeakeasyEntry(9, "Phonus Balonus", 7597, 3, 10000),
        SpeakeasyEntry(10, "Flivver", 7598, 2, 20000),
        SpeakeasyEntry(11, "Sloppy Jalopy", 7599, 5, 100000),
    )

    private const val SPEAKEASY_DRINKS_DRUNK_PREF = "_speakeasyDrinksDrunk"
    private const val DAILY_LIMIT = 3

    private val indexByName: Map<String, Int> =
        entries.withIndex().associate { (index, entry) -> entry.name.lowercase() to index }

    private val itemIds: Set<Int> = entries.map { it.itemId }.toSet()

    fun isSpeakeasyDrink(name: String): Boolean =
        indexByName.containsKey(name.lowercase())

    fun isSpeakeasyDrink(itemId: Int): Boolean =
        itemId in itemIds

    fun nameToIndex(name: String): Int =
        indexByName[name.lowercase()] ?: -1

    fun loungeIdToIndex(loungeId: Int): Int =
        entries.indexOfFirst { it.loungeId == loungeId }

    fun indexToName(index: Int): String? =
        entries.getOrNull(index)?.name

    fun nameToCost(name: String): Int {
        val index = nameToIndex(name)
        return if (index < 0) -1 else entries[index].cost
    }

    fun nameToInebriety(name: String): Int {
        val index = nameToIndex(name)
        return if (index < 0) -1 else entries[index].inebriety
    }

    /** Desktop UseItemEnqueuePanel speakeasy 3/day queue gate. */
    fun canQueueSpeakeasyDrink(quantity: Int, context: ConcoctionQueueContext): Boolean {
        if (quantity <= 0) return false
        val drunk = context.intPref(SPEAKEASY_DRINKS_DRUNK_PREF)
        return ConcoctionQueueBudget.queuedSpeakeasyDrink + drunk + quantity <= DAILY_LIMIT
    }
}
