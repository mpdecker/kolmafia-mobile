package net.sourceforge.kolmafia.data

/** Desktop ClanLoungeRequest.HOTDOG_DATA — static clan VIP hot dog stand registry. */
object HotDogDatabase {

    data class HotDogEntry(
        val name: String,
        val cafeId: Int,
        val fullness: Int,
        val unlockItemId: Int? = null,
        val unlockCount: Int? = null,
    )

    /** Order matches desktop HOTDOG_DATA; index > 0 marks fancy hot dogs. */
    val entries: List<HotDogEntry> = listOf(
        HotDogEntry("basic hot dog", -92, 1),
        HotDogEntry("savage macho dog", -93, 2),
        HotDogEntry("one with everything", -94, 2),
        HotDogEntry("sly dog", -95, 2),
        HotDogEntry("devil dog", -96, 3),
        HotDogEntry("chilly dog", -97, 3),
        HotDogEntry("ghost dog", -98, 3),
        HotDogEntry("junkyard dog", -99, 3),
        HotDogEntry("wet dog", -100, 3),
        HotDogEntry("optimal dog", -102, 1),
        HotDogEntry("sleeping dog", -101, 2),
        HotDogEntry("video games hot dog", -103, 3),
    )

    private val indexByName: Map<String, Int> =
        entries.withIndex().associate { (index, entry) -> entry.name.lowercase() to index }

    fun isHotDog(name: String): Boolean =
        indexByName.containsKey(name.lowercase())

    fun isFancyHotDog(name: String): Boolean {
        val index = nameToIndex(name)
        return index > 0
    }

    fun nameToIndex(name: String): Int =
        indexByName[name.lowercase()] ?: -1

    fun indexToName(index: Int): String? =
        entries.getOrNull(index)?.name

    fun indexToCafeId(index: Int): Int =
        entries.getOrNull(index)?.cafeId ?: -1

    fun nameToFullness(name: String): Int {
        val index = nameToIndex(name)
        return if (index < 0) -1 else entries[index].fullness
    }

    fun cafeIdToIndex(cafeId: Int): Int =
        entries.indexOfFirst { it.cafeId == cafeId }

    fun cafeIdToName(cafeId: Int): String? =
        entries.firstOrNull { it.cafeId == cafeId }?.name

    private const val FANCY_HOT_DOG_EATEN_PREF = "_fancyHotDogEaten"

    /** Desktop UseItemEnqueuePanel fancy-dog daily limit gate. */
    fun canQueueFancyDog(context: ConcoctionQueueContext): Boolean =
        !ConcoctionQueueBudget.queuedFancyDog &&
            !context.booleanPref(FANCY_HOT_DOG_EATEN_PREF)
}
