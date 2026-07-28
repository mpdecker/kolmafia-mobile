package net.sourceforge.kolmafia.inventory

/** Desktop [LimitMode] mall/NPC/coinmaster/storage/clan/campground gates. */
object LimitModeGates {

    private enum class Mode {
        NONE,
        SPELUNKY,
        BATMAN,
        ED,
        UNKNOWN,
    }

    private fun normalize(limitMode: String): Mode =
        when (limitMode.lowercase()) {
            "", "none" -> Mode.NONE
            "spelunky", "spelunk" -> Mode.SPELUNKY
            "batman" -> Mode.BATMAN
            "edunder", "ed" -> Mode.ED
            "unknown" -> Mode.UNKNOWN
            else -> Mode.NONE
        }

    fun limitMall(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            Mode.NONE, Mode.ED, Mode.UNKNOWN -> false
        }

    fun limitNPCStores(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN, Mode.ED -> true
            Mode.NONE, Mode.UNKNOWN -> false
        }

    fun limitCoinmasters(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            Mode.NONE, Mode.ED, Mode.UNKNOWN -> false
        }

    fun limitClan(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN, Mode.ED -> true
            Mode.NONE, Mode.UNKNOWN -> false
        }

    fun limitCampground(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN, Mode.ED -> true
            Mode.NONE, Mode.UNKNOWN -> false
        }

    fun limitStorage(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            Mode.NONE, Mode.ED, Mode.UNKNOWN -> false
        }
}
