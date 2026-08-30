package net.sourceforge.kolmafia.session

/**
 * Desktop [CakeArenaManager] — arena opponent registry and event name helpers
 * (Phases 3231–3245).
 */
object CakeArenaManager {

    data class ArenaOpponent(
        val id: Int,
        val name: String,
        val race: String,
        val weight: Int,
    ) {
        val description: String get() = "$race ($weight lbs)"

        override fun toString(): String = description

        override fun equals(other: Any?): Boolean =
            other is ArenaOpponent && id == other.id

        override fun hashCode(): Int = id
    }

    private val opponents = mutableListOf<ArenaOpponent>()

    fun reset() {
        opponents.clear()
    }

    fun registerOpponent(opponentId: Int, name: String, race: String, weight: Int) {
        val ao = ArenaOpponent(opponentId, name, race, weight)
        val index = opponents.indexOfFirst { it.id == opponentId }
        if (index >= 0) {
            opponents[index] = ao
        } else {
            opponents.add(ao)
        }
    }

    fun getOpponentList(): List<ArenaOpponent> = opponents.toList()

    fun getOpponent(opponentId: Int): ArenaOpponent? =
        opponents.firstOrNull { it.id == opponentId }

    fun eventIdToName(eventId: Int): String = when (eventId) {
        1 -> "Ultimate Cage Match"
        2 -> "Scavenger Hunt"
        3 -> "Obstacle Course"
        4 -> "Hide and Seek"
        else -> "Unknown Event"
    }

    fun eventNameToId(eventName: String): Int = when (eventName) {
        "Ultimate Cage Match" -> 1
        "Scavenger Hunt" -> 2
        "Obstacle Course" -> 3
        "Hide and Seek" -> 4
        else -> 0
    }
}
