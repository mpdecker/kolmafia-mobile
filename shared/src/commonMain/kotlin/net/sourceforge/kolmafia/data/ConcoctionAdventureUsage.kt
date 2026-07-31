package net.sourceforge.kolmafia.data

/** Desktop ConcoctionDatabase.getAdventureUsage — v1 static method-token turn costs. */
object ConcoctionAdventureUsage {

    private val usageByMethod: Map<String, Int> = mapOf(
        "SMITH" to 1,
        "SSMITH" to 1,
        "COMBINE" to 0,
        "ACOMBINE" to 0,
        "COOK" to 0,
        "COOK_FANCY" to 0,
        "MIX" to 0,
        "MIX_FANCY" to 0,
        "STILL" to 0,
        "SUSE" to 0,
        "CLIPART" to 0,
        "JEWELRY" to 0,
        "MANUAL" to 0,
    )

    fun adventureUsageForMethod(token: String): Int = usageByMethod[token] ?: 0

    fun adventureUsageForConcoction(concoction: ConcoctionData): Int =
        concoction.methods.maxOfOrNull(::adventureUsageForMethod) ?: 0

    /** Method token with highest adventure usage (stable tie-break by token sort). */
    fun primaryAdventureMethod(concoction: ConcoctionData): String? =
        concoction.methods
            .sorted()
            .maxByOrNull(::adventureUsageForMethod)
            ?.takeIf { adventureUsageForMethod(it) > 0 }
            ?: concoction.methods.minOrNull()
}
