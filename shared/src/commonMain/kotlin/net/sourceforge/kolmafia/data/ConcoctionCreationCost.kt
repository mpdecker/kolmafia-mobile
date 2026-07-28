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
        "JEWEL", "MALUS", "COINMASTER",
    )

    fun primaryMethod(methods: Set<String>): String? =
        METHOD_PRIORITY.firstOrNull { it in methods }

    fun creationCost(methods: Set<String>): Long {
        val method = primaryMethod(methods) ?: return 0L
        return CREATION_COST[method] ?: 0L
    }

    fun adventureUsage(methods: Set<String>): Int {
        val method = primaryMethod(methods) ?: return 0
        return ADVENTURE_USAGE[method] ?: 0
    }
}
