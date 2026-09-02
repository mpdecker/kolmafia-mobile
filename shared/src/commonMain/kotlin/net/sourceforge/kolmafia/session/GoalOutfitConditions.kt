package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase

/** Desktop EquipmentManager.addOutfitConditions — location outfit → item goals. */
object GoalOutfitConditions {

    private val LOCATION_ALIASES = mapOf(
        "guard" to "cobb's knob treasury",
        "elite" to "cobb's knob treasury",
        "elite guard" to "cobb's knob treasury",
        "rift" to "cola wars battlefield",
        "cloaca-cola" to "cola wars battlefield (cloaca uniform)",
        "cloaca cola" to "cola wars battlefield (cloaca uniform)",
        "dyspepsi-cola" to "cola wars battlefield (dyspepsi uniform)",
        "dyspepsi cola" to "cola wars battlefield (dyspepsi uniform)",
    )

    private val OUTFIT_BY_LOCATION = listOf(
        "cobb's knob barracks" to 5,
        "cobb's knob harem" to 4,
        "cobb's knob treasury" to 5,
        "itznotyerzitz mine" to 8,
        "extreme slope" to 7,
        "hippy camp" to 2,
        "orcish frat house" to 3,
        "obligatory pirate's cove" to 9,
        "cola wars battlefield (cloaca uniform)" to 23,
        "cola wars battlefield (dyspepsi uniform)" to 24,
        "cola wars battlefield" to 23,
    )

    fun normalizeLocation(raw: String): String {
        val trimmed = raw.trim().lowercase()
        return LOCATION_ALIASES[trimmed] ?: trimmed
    }

    fun outfitIdForLocation(location: String): Int? {
        val normalized = normalizeLocation(location)
        AdventureDatabase.getByName(normalized)?.let { zone ->
            outfitIdForLocationName(zone.locationName.lowercase())?.let { return it }
        }
        return outfitIdForLocationName(normalized)
    }

    private fun outfitIdForLocationName(name: String): Int? {
        OUTFIT_BY_LOCATION.firstOrNull { (key, _) -> name.contains(key) }?.second?.let { return it }
        return null
    }

    fun addOutfitConditions(
        location: String,
        manager: GoalManager,
        mode: GoalManager.ConditionMode,
        isEquipped: (String) -> Boolean = { false },
    ): Boolean {
        val outfitId = outfitIdForLocation(location) ?: return false
        val outfit = OutfitDatabase.getById(outfitId) ?: return false
        when (mode) {
            GoalManager.ConditionMode.REMOVE -> {
                for (piece in outfit.equipment) manager.removeGoal(piece)
                manager.outfitGoalActive = false
            }
            GoalManager.ConditionMode.SET -> {
                manager.clearGoals()
                addMissingPieces(outfit.equipment, manager, isEquipped)
                manager.outfitGoalActive = true
            }
            GoalManager.ConditionMode.ADD -> {
                addMissingPieces(outfit.equipment, manager, isEquipped)
                manager.outfitGoalActive = true
            }
        }
        return true
    }

    private fun addMissingPieces(
        pieces: List<String>,
        manager: GoalManager,
        isEquipped: (String) -> Boolean,
    ) {
        for (piece in pieces) {
            if (!isEquipped(piece)) manager.addItemGoalByName(piece)
        }
    }
}
