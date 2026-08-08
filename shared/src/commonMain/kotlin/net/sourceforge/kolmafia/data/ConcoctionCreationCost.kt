package net.sourceforge.kolmafia.data

/** Desktop ConcoctionDatabase.CREATION_COST / ADVENTURE_USAGE for primary craft methods. */
object ConcoctionCreationCost {

    private val CREATION_COST = mapOf(
        "COMBINE" to 10L,
        "ACOMBINE" to 10L,
        "SMITH" to 0L,
        "SSMITH" to 0L,
        "COOK" to 0L,
        "COOK_FANCY" to 0L,
        "MIX" to 0L,
        "MIX_FANCY" to 0L,
        "STILL" to 0L,
        "SUSE" to 0L,
        "MUSE" to 0L,
        "PHINEAS" to 0L,
        "SEWER" to 0L,
        "STAR" to 0L,
        "SUGAR" to 0L,
        "PIXEL" to 0L,
        "ROLL" to 0L,
        "TINKER" to 0L,
        "STAFF" to 0L,
        "SUSHI" to 0L,
        "JEWEL" to 0L,
        "MALUS" to 0L,
        "VYKEA" to 0L,
        "FLOUNDRY" to 0L,
        "BARREL" to 0L,
        "GNOME_TINKER" to 0L,
        "GNOME_PART" to 0L,
        "BURNING_LEAVES" to 0L,
        "WAX" to 0L,
        "NEWSPAPER" to 0L,
        "METEOROID" to 0L,
        "WOOL" to 0L,
        "TERMINAL" to 0L,
        "SPACEGATE" to 0L,
        "FANTASY_REALM" to 0L,
        "STILLSUIT" to 0L,
        "MAYAM" to 0L,
        "PHOTO_BOOTH" to 0L,
        "TAKERSPACE" to 0L,
        "SPEAKEASY" to 0L,
        "HOT_DOG" to 0L,
    )

    private val ADVENTURE_USAGE = mapOf(
        "COMBINE" to 0,
        "ACOMBINE" to 0,
        "SMITH" to 1,
        "SSMITH" to 1,
        "COOK" to 0,
        "COOK_FANCY" to 1,
        "MIX" to 0,
        "MIX_FANCY" to 1,
        "STILL" to 0,
        "SUSE" to 0,
    )

    private val METHOD_PRIORITY = listOf(
        "COMBINE", "ACOMBINE", "COOK", "COOK_FANCY", "MIX", "MIX_FANCY",
        "SMITH", "SSMITH", "STILL", "SUSE", "MUSE", "PHINEAS", "SEWER",
        "STAR", "SUGAR", "PIXEL", "ROLL", "TINKER", "STAFF", "SUSHI",
        "JEWEL", "MALUS", "COINMASTER", "VYKEA", "FLOUNDRY", "BARREL", "GNOME_TINKER",
        "GNOME_PART", "BURNING_LEAVES", "WAX", "NEWSPAPER", "METEOROID", "WOOL",
        "TERMINAL", "SPACEGATE", "FANTASY_REALM", "STILLSUIT", "MAYAM", "PHOTO_BOOTH", "TAKERSPACE",
        "SPEAKEASY", "HOT_DOG",
        "CLIPART", "JEWELRY", "ROLLING_PIN", "SAUSAGE_O_MATIC", "MULTI_USE", "SINGLE_USE",
    )

    fun primaryMethod(methods: Set<String>): String? {
        val normalized = ConcoctionMethodAliases.normalize(methods)
        return METHOD_PRIORITY.firstOrNull { it in normalized }
    }

    fun creationCost(methods: Set<String>): Long {
        val method = primaryMethod(methods) ?: return 0L
        return CREATION_COST[method] ?: 0L
    }

    fun adventureUsage(methods: Set<String>): Int {
        val method = primaryMethod(methods) ?: return 0
        return ADVENTURE_USAGE[method] ?: 0
    }
}
