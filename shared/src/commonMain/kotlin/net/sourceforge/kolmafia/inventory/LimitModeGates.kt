package net.sourceforge.kolmafia.inventory

/** Desktop [LimitMode] mall/NPC/coinmaster/storage/clan/campground gates. */
object LimitModeGates {

    private enum class Mode {
        NONE,
        SPELUNKY,
        BATMAN,
        ED,
        BIRD,
        ROACH,
        MOLE,
        ASTRAL,
        UNKNOWN,
    }

    private fun normalize(limitMode: String): Mode =
        when (limitMode.lowercase()) {
            "", "none" -> Mode.NONE
            "spelunky", "spelunk" -> Mode.SPELUNKY
            "batman" -> Mode.BATMAN
            "edunder", "ed" -> Mode.ED
            "bird" -> Mode.BIRD
            "roach" -> Mode.ROACH
            "mole" -> Mode.MOLE
            "astral" -> Mode.ASTRAL
            "unknown" -> Mode.UNKNOWN
            else -> Mode.NONE
        }

    /** Desktop [LimitMode.limitItem] — block item use outside allowed id ranges/modes. */
    fun limitItem(limitMode: String, itemId: Int): Boolean =
        when (normalize(limitMode)) {
            Mode.UNKNOWN, Mode.NONE -> false
            Mode.SPELUNKY -> itemId < 8040 || itemId > 8062
            Mode.BATMAN -> itemId < 8797 || itemId > 8815 || itemId == 8800
            Mode.ED -> true
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL ->
                itemId == 3353 || itemId == 1622 // GONG, ASTRAL_MUSHROOM
        }

    fun limitMall(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            Mode.NONE, Mode.ED, Mode.UNKNOWN,
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL,
            -> false
        }

    fun limitNPCStores(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN, Mode.ED -> true
            Mode.NONE, Mode.UNKNOWN,
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL,
            -> false
        }

    fun limitCoinmasters(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            Mode.NONE, Mode.ED, Mode.UNKNOWN,
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL,
            -> false
        }

    fun limitClan(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN, Mode.ED -> true
            Mode.NONE, Mode.UNKNOWN,
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL,
            -> false
        }

    fun limitCampground(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN, Mode.ED -> true
            Mode.NONE, Mode.UNKNOWN,
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL,
            -> false
        }

    fun limitFamiliars(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            Mode.NONE, Mode.ED, Mode.UNKNOWN,
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL,
            -> false
        }

    fun limitStorage(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            Mode.NONE, Mode.ED, Mode.UNKNOWN,
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL,
            -> false
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
