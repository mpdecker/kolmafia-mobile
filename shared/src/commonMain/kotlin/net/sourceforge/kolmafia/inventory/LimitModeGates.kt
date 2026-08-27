package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ZoneParentDatabase

/** Desktop [LimitMode] mall/NPC/coinmaster/storage/clan/campground/zone/skill gates. */
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

    private val LIMIT_ZONE_ROOTS = listOf(
        "Batfellow Area",
        "Spelunky Area",
        "Shape of Mole",
        "Astral",
    )

    private fun normalize(limitMode: String): Mode =
        when (limitMode.lowercase()) {
            "", "none" -> Mode.NONE
            "spelunky", "spelunk" -> Mode.SPELUNKY
            "batman" -> Mode.BATMAN
            "edunder", "ed" -> Mode.ED
            "bird" -> Mode.BIRD
            "roach", "cockroach" -> Mode.ROACH
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
            else -> false
        }

    fun limitNPCStores(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN, Mode.ED -> true
            else -> false
        }

    fun limitCoinmasters(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            else -> false
        }

    fun limitClan(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN, Mode.ED -> true
            else -> false
        }

    fun limitCampground(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN, Mode.ED -> true
            else -> false
        }

    fun limitFamiliars(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            else -> false
        }

    fun limitStorage(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            else -> false
        }

    /** Desktop [LimitMode.limitRecovery]: block mood/recovery automation outside normal play. */
    fun limitRecovery(limitMode: String): Boolean =
        when (limitMode.lowercase()) {
            "", "none", "bird", "roach", "cockroach", "mole", "astral" -> false
            else -> true
        }

    fun limitEating(limitMode: String): Boolean = limitsConsumption(limitMode)
    fun limitDrinking(limitMode: String): Boolean = limitsConsumption(limitMode)
    fun limitSpleening(limitMode: String): Boolean = limitsConsumption(limitMode)

    private fun limitsConsumption(limitMode: String): Boolean =
        when (limitMode.lowercase()) {
            "spelunky", "spelunk", "batman", "edunder", "ed" -> true
            else -> false
        }

    /** Desktop [LimitMode.limitSkill]. */
    fun limitSkill(limitMode: String, skillId: Int): Boolean =
        when (normalize(limitMode)) {
            Mode.UNKNOWN, Mode.NONE, Mode.ED -> false
            Mode.SPELUNKY -> skillId < 7238 || skillId > 7244
            Mode.BATMAN -> true
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL -> false
        }

    /** Desktop [LimitMode.limitSlot]. */
    fun limitSlot(limitMode: String, slot: EquipmentSlot): Boolean =
        when (normalize(limitMode)) {
            Mode.UNKNOWN, Mode.NONE -> false
            Mode.SPELUNKY -> when (slot) {
                EquipmentSlot.HAT,
                EquipmentSlot.WEAPON,
                EquipmentSlot.OFFHAND,
                EquipmentSlot.CONTAINER,
                EquipmentSlot.ACC1,
                -> false
                else -> true
            }
            Mode.ED, Mode.BATMAN -> true
            Mode.BIRD, Mode.ROACH, Mode.MOLE, Mode.ASTRAL -> false
        }

    fun limitOutfits(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            else -> false
        }

    fun limitMeat(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            else -> false
        }

    fun limitPickpocket(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            else -> false
        }

    fun limitMCD(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            else -> false
        }

    /**
     * Desktop [LimitMode.limitZone] — true when [zone] is **blocked**.
     * Uses [ZoneParentDatabase] parent walk when loaded; falls back to name equality.
     */
    fun limitZone(zone: String, limitMode: String): Boolean {
        if (zone.isBlank()) return false
        val root = getRootZone(zone)
        val mode = normalize(limitMode)
        when (root) {
            "Astral" -> {
                // Without chosen trip prefs, treat like desktop empty-trip gate
                // (caller may still allow via limitAdventure).
            }
            "Shape of Mole" -> { /* llama form gate deferred to adventure */ }
        }
        return when (mode) {
            Mode.UNKNOWN -> false
            Mode.NONE -> LIMIT_ZONE_ROOTS.any { it.equals(root, ignoreCase = true) }
            Mode.SPELUNKY -> !root.equals("Spelunky Area", ignoreCase = true)
            Mode.BATMAN -> !root.equals("Batfellow Area", ignoreCase = true)
            Mode.ED -> true
            Mode.BIRD ->
                zone.equals("Shape of Mole", ignoreCase = true) ||
                    root.equals("Astral", ignoreCase = true)
            Mode.ROACH -> false
            Mode.MOLE -> !zone.equals("Shape of Mole", ignoreCase = true)
            Mode.ASTRAL -> !zone.equals("Astral", ignoreCase = true) &&
                !root.equals("Astral", ignoreCase = true)
        }
    }

    /**
     * Desktop [LimitMode.limitAdventure] — true when adventure in [zone] is blocked.
     * [adventureId] is snarfblat/id string when known.
     */
    fun limitAdventure(
        zone: String,
        limitMode: String,
        adventureId: String = "",
        currentAstralTrip: String = "",
    ): Boolean {
        if (normalize(limitMode) == Mode.ASTRAL) {
            val chosen = currentAstralTrip.isNotBlank()
            return when (adventureId) {
                "96" -> chosen && currentAstralTrip != "Bad Trip" // BAD_TRIP
                "97" -> chosen && currentAstralTrip != "Mediocre Trip"
                "98" -> chosen && currentAstralTrip != "Great Trip"
                else -> true
            }
        }
        return limitZone(zone, limitMode)
    }

    /** Walk zonelist parents until hitting a known limit root or top-level. */
    fun getRootZone(zoneName: String): String {
        var current = zoneName.trim()
        if (current.isEmpty()) return current
        val seen = mutableSetOf<String>()
        while (current.isNotEmpty() && seen.add(current.lowercase())) {
            if (LIMIT_ZONE_ROOTS.any { it.equals(current, ignoreCase = true) }) {
                return LIMIT_ZONE_ROOTS.first { it.equals(current, ignoreCase = true) }
            }
            val parent = ZoneParentDatabase.getByName(current)?.parent ?: break
            if (parent.isBlank() || parent.equals(current, ignoreCase = true)) break
            // Top-level parents like "Item-Driven" mean current is the root area name
            if (parent.equals("Item-Driven", ignoreCase = true) ||
                parent.equals("World", ignoreCase = true)
            ) {
                return current
            }
            current = parent
        }
        return current
    }

    /** Desktop [LimitMode.requiresCharPane] — api.php status is incomplete in these modes. */
    fun requiresCharPane(limitMode: String): Boolean =
        when (normalize(limitMode)) {
            Mode.SPELUNKY, Mode.BATMAN -> true
            else -> false
        }
}
