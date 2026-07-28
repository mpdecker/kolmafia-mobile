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

    /** Desktop [LimitMode.limitRecovery]: block mood/recovery automation outside normal play. */
    fun limitRecovery(limitMode: String): Boolean =
        when (limitMode.lowercase()) {
            "", "none", "bird", "roach", "mole", "astral" -> false
            else -> true
        }

    /** Desktop [LimitMode.limitEating/limitDrinking/limitSpleening]: block consumption in avatar modes. */
    fun limitEating(limitMode: String): Boolean = limitsConsumption(limitMode)
    fun limitDrinking(limitMode: String): Boolean = limitsConsumption(limitMode)
    fun limitSpleening(limitMode: String): Boolean = limitsConsumption(limitMode)

    private fun limitsConsumption(limitMode: String): Boolean =
        when (limitMode.lowercase()) {
            "spelunky", "spelunk", "batman", "edunder", "ed" -> true
            else -> false
        }

    /** Desktop LimitMode.limitZone — mobile limit modes do not zone-lock knoll/beach yet. */
    fun limitZone(zone: String, limitMode: String): Boolean = false
}
