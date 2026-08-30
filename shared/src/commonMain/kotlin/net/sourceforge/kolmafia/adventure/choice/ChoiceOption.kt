package net.sourceforge.kolmafia.adventure.choice

/**
 * Desktop [ChoiceOption] — spoiler label plus optional explicit option index and item names
 * used by goal matching / "complete the outfit".
 */
data class ChoiceOption(
    val name: String,
    val option: Int = 0,
    val itemNames: List<String> = emptyList(),
) {
    fun decision(defaultIndex1Based: Int): Int = if (option == 0) defaultIndex1Based else option

    override fun toString(): String = name
}
