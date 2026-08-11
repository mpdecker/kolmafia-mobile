package net.sourceforge.kolmafia.session

/** Desktop [LimitMode.getName] canonical names for ASH [limit_mode]. */
object LimitModeNames {

    fun ashName(limitMode: String): String =
        when (limitMode.lowercase().trim()) {
            "", "none" -> ""
            "spelunky", "spelunk" -> "spelunky"
            "batman" -> "batman"
            "ed", "edunder" -> "edunder"
            "bird" -> "bird"
            "cockroach", "roach" -> "cockroach"
            "mole" -> "mole"
            "astral" -> "astral"
            "unknown" -> "unknown"
            else -> if (limitMode.isBlank()) "" else "unknown"
        }
}
