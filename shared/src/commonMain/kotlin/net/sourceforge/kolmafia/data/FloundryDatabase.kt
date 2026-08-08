package net.sourceforge.kolmafia.data

/** Desktop ClanLoungeRequest.FLOUNDRY_DATA static registry. */
object FloundryDatabase {

    private val items = listOf(
        FloundryItem(9001, "carpe", "carp"),
        FloundryItem(9002, "codpiece", "cod"),
        FloundryItem(9003, "troutsers", "trout"),
        FloundryItem(9004, "bass clarinet", "bass"),
        FloundryItem(9005, "fish hatchet", "hatchetfish"),
        FloundryItem(9006, "tunac", "tuna"),
    )

    private val byName = items.associateBy { it.name.lowercase() }
    private val byId = items.associateBy { it.itemId }
    private val byFish = items.associateBy { it.fish.lowercase() }

    fun isFloundryItem(name: String): Boolean = byName.containsKey(name.lowercase())

    fun itemIdForName(name: String): Int? = byName[name.lowercase()]?.itemId

    fun nameForItemId(itemId: Int): String? = byId[itemId]?.name

    fun itemNameForFish(fish: String): String? = byFish[fish.lowercase()]?.name

    fun fishForItemName(name: String): String? = byName[name.lowercase()]?.fish

    fun locationPrefForFish(fish: String): String? = when (fish.lowercase()) {
            "carp" -> "_floundryCarpLocation"
            "cod" -> "_floundryCodLocation"
            "trout" -> "_floundryTroutLocation"
            "bass" -> "_floundryBassLocation"
            "hatchetfish" -> "_floundryHatchetfishLocation"
            "tuna" -> "_floundryTunaLocation"
            else -> null
        }

    fun allItems(): List<FloundryItem> = items

    data class FloundryItem(val itemId: Int, val name: String, val fish: String)
}
