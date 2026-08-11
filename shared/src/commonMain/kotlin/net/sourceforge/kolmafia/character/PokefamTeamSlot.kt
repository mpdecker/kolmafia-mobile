package net.sourceforge.kolmafia.character

/** One slot on the Pokefam path active team (desktop `KoLCharacter.currentPokeFam`). */
data class PokefamTeamSlot(
    val familiarId: Int = 0,
    val name: String = "",
    val level: Int = 0,
    val power: Int = 0,
    val hp: Int = 0,
    val attributes: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = familiarId <= 0

    companion object {
        val EMPTY = PokefamTeamSlot()
        val EMPTY_TEAM = List(3) { EMPTY }
    }
}
