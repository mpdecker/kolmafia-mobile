package net.sourceforge.kolmafia.adventure

sealed class AdventureResult {
    data class Combat(
        val monster: String, val won: Boolean,
        val itemsGained: List<String> = emptyList(),
        val meatGained: Int = 0,
        val statsGained: Map<String, Int> = emptyMap()
    ) : AdventureResult()
    data class NonCombat(
        val encounterName: String, val text: String,
        val itemsGained: List<String> = emptyList(),
        val meatGained: Int = 0
    ) : AdventureResult()
    data class Choice(
        val choiceId: Int, val encounterName: String,
        val options: List<String> = emptyList(),
        val chosenOption: Int? = null
    ) : AdventureResult()
}
