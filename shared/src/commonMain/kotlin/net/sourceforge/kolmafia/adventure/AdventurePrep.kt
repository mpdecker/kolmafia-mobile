package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UseItemRequest

/**
 * Zone prep before adventuring — wears disguises and retrieves prep items when known.
 * Preference overrides:
 * - `zoneOutfit_<location>` → outfit name
 * - `zoneFamiliar_<location>` → familiar species name
 * - `zoneItem_<location>` → comma-separated item names to retrieve (qty 1 each)
 * - `zoneUse_<location>` → comma-separated item names to use before adventuring
 */
object AdventurePrep {

    /** Returns false when the character cannot adventure at [locationName] (zone rules + adventures left). */
    fun canAdventureAt(
        locationName: String,
        character: CharacterState?,
        zone: AdventureZone? = AdventureDatabase.getByName(locationName),
    ): Boolean {
        if ((character?.adventuresLeft ?: 0) <= 0) return false
        val cs = character ?: return true
        zone ?: return true

        if (zone.isOverdrunk && cs.inebriety <= 0) return false

        if (cs.isInLimitMode) {
            val mode = cs.limitMode.lowercase()
            val inZone = zone.urlParams.lowercase().contains(mode) ||
                zone.zoneName.lowercase().contains(mode) ||
                zone.locationName.lowercase().contains(mode)
            if (!inZone) return false
        }

        return true
    }
    private val ZONE_OUTFITS = mapOf(
        "The Mine Office" to "Mining Gear",
        "Dwarf Factory" to "Mining Gear",
        "The Frat House (Disguised)" to "Frat Boy Ensemble",
        "The Hippy Camp (Disguised)" to "Filthy Hippy Disguise",
        "The Wartime Frat House (Disguised)" to "War Hippy Fatigues",
        "The Wartime Hippy Camp (Disguised)" to "Frat Warrior Fatigues",
        "The Battlefield (Cloaca-Cola)" to "Cloaca-Cola Uniform",
        "The Battlefield (Dyspepsi-Cola)" to "Dyspepsi-Cola Uniform",
        "Mer-kin Elementary School" to "Crappy Mer-Kin Disguise",
        "Mer-kin Gymnasium" to "Crappy Mer-Kin Disguise",
        "Mer-kin Library" to "Mer-kin Scholar's Vestments",
        "Mer-kin Colosseum" to "Mer-kin Gladiatorial Gear",
        "Mer-kin Study" to "Mer-kin Scholar's Vestments",
        "Mer-kin Deep Temple" to "Crappy Mer-Kin Disguise",
        "The Poop Deck" to "Swashbuckling Getup",
        "Below Deck" to "Swashbuckling Getup",
    )

    suspend fun prepareForAdventure(
        locationName: String,
        outfitManager: OutfitManager?,
        preferences: Preferences?,
        retrieveItemService: RetrieveItemService? = null,
        useItemRequest: UseItemRequest? = null,
        gameDatabase: net.sourceforge.kolmafia.data.GameDatabase? = null,
        familiarManager: FamiliarManager? = null,
        character: CharacterState? = null,
    ): Boolean {
        val familiarName = preferences?.getString("zoneFamiliar_$locationName", "")?.takeIf { it.isNotBlank() }
        if (familiarName != null) {
            val fm = familiarManager ?: return false
            if (fm.setFamiliar(familiarName).isFailure) return false
        }

        val outfitName = preferences?.getString("zoneOutfit_$locationName", "")?.takeIf { it.isNotBlank() }
            ?: ZONE_OUTFITS[locationName]
            ?: ZONE_OUTFITS.entries.firstOrNull { (zone, _) ->
                locationName.equals(zone, ignoreCase = true) ||
                    locationName.contains(zone, ignoreCase = true)
            }?.value

        if (outfitName != null) {
            val manager = outfitManager ?: return false
            if (!manager.wearOutfit(outfitName)) return false
        }

        val itemPref = preferences?.getString("zoneItem_$locationName", "")?.takeIf { it.isNotBlank() }
        if (itemPref != null && retrieveItemService != null && gameDatabase != null) {
            for (raw in itemPref.split(',')) {
                val name = raw.trim()
                if (name.isEmpty()) continue
                val id = gameDatabase.item(name)?.id ?: return false
                if (retrieveItemService.retrieve(id, 1) < 1) return false
            }
        }

        val usePref = preferences?.getString("zoneUse_$locationName", "")?.takeIf { it.isNotBlank() }
        if (usePref != null && useItemRequest != null && gameDatabase != null) {
            for (raw in usePref.split(',')) {
                val name = raw.trim()
                if (name.isEmpty()) continue
                val id = gameDatabase.item(name)?.id ?: return false
                if (retrieveItemService?.retrieve(id, 1) ?: 0 < 1) return false
                if (useItemRequest.use(id, 1).isFailure) return false
            }
        }

        return true
    }
}
