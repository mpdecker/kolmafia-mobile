package net.sourceforge.kolmafia.character

/** Desktop `FamTeamRequest.PokeBoost` pokepill boost types. */
enum class PokeBoost(val label: String) {
    NONE("None"),
    POWER("Power"),
    HP("HP"),
    ARMOR("Armor"),
    REGENERATING("Regenerating"),
    SMART("Smart"),
    SPIKED("Spiked"),
    ;

    override fun toString(): String = label

    companion object {
        const val METANDIENONE = 9748
        const val RIBOFLAVIN = 9749
        const val BRONZE = 9750
        const val PIRACETAM = 9751
        const val ULTRACALCIUM = 9752
        const val GINSENG = 9753

        private val itemIdToBoost = mapOf(
            METANDIENONE to POWER,
            RIBOFLAVIN to HP,
            BRONZE to ARMOR,
            PIRACETAM to SMART,
            ULTRACALCIUM to SPIKED,
            GINSENG to REGENERATING,
        )

        private val labelToBoost = entries.associateBy { it.label }

        fun fromLabel(label: String): PokeBoost = labelToBoost[label] ?: NONE

        fun fromItemId(itemId: Int): PokeBoost? = itemIdToBoost[itemId]

        fun itemIdFor(boost: PokeBoost): Int? =
            itemIdToBoost.entries.firstOrNull { it.value == boost }?.key
    }
}
