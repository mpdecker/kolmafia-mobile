package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.adventure.prep.AdventureGateContext
import net.sourceforge.kolmafia.adventure.prep.AdventurePrepareActions
import net.sourceforge.kolmafia.adventure.prep.AdventureUnlockHelpers
import net.sourceforge.kolmafia.adventure.prep.AdventureZoneGates
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.UseItemRequest

/**
 * Zone unlock gates + prep before adventuring (Phases 14/18/20/22 + 1851–1910).
 *
 * Preference overrides:
 * - `zoneOutfit_<location>` → outfit name
 * - `zoneFamiliar_<location>` → familiar species name
 * - `zoneItem_<location>` → comma-separated item names to retrieve (qty 1 each)
 * - `zoneUse_<location>` → comma-separated item names to use before adventuring
 */
object AdventurePrep {

    var questDatabaseProvider: (() -> QuestDatabase?)? = null
    var inventoryProvider: (() -> InventoryManager?)? = null
    var hasEquipped: (Int) -> Boolean = { false }
    var hasCampground: () -> Boolean = { true }
    var visitUrl: (suspend (String) -> Boolean)? = null
    var equipItem: (suspend (Int) -> Boolean)? = null
    var unequipSlot: (suspend (String) -> Boolean)? = null
    var hasEffect: (String) -> Boolean = { false }
    var stenchResistanceLevels: (() -> Int)? = null
    var preferFamiliar: (suspend (String) -> Boolean)? = null

    fun resetForTest() {
        questDatabaseProvider = null
        inventoryProvider = null
        hasEquipped = { false }
        hasCampground = { true }
        visitUrl = null
        equipItem = null
        unequipSlot = null
        hasEffect = { false }
        stenchResistanceLevels = null
        preferFamiliar = null
    }

    fun buildContext(
        character: CharacterState?,
        preferences: Preferences?,
        questDatabase: QuestDatabase? = questDatabaseProvider?.invoke(),
    ): AdventureGateContext {
        val inv = inventoryProvider?.invoke()
        return AdventureGateContext(
            character = character,
            preferences = preferences,
            questDatabase = questDatabase ?: preferences?.let { QuestDatabase(it) },
            inventoryCount = { id -> inv?.state?.value?.items?.get(id)?.quantity ?: 0 },
            hasEquipped = hasEquipped,
            hasCampground = hasCampground(),
        )
    }

    /** Returns false when the character cannot adventure at [locationName] (zone rules + adventures left). */
    fun canAdventureAt(
        locationName: String,
        character: CharacterState?,
        zone: AdventureZone? = AdventureDatabase.getByName(locationName),
        preferences: Preferences? = null,
        questDatabase: QuestDatabase? = null,
    ): Boolean {
        if ((character?.adventuresLeft ?: 0) <= 0) return false
        return canAdventureAtZone(locationName, character, zone, preferences, questDatabase)
    }

    /**
     * Zone-only gates (drunk, preValidate, core/IoTM unlocks, stat, limit mode)
     * — ignores adventures remaining.
     */
    fun canAdventureAtZone(
        locationName: String,
        character: CharacterState?,
        zone: AdventureZone? = AdventureDatabase.getByName(locationName),
        preferences: Preferences? = null,
        questDatabase: QuestDatabase? = null,
    ): Boolean {
        val cs = character ?: return true
        val z = zone ?: return true
        val ctx = buildContext(cs, preferences, questDatabase)

        if (AdventureZoneGates.tooDrunkToAdventure(locationName, z, ctx)) return false

        if (z.isOverdrunk && cs.inebriety <= 0) return false

        if (cs.isInLimitMode) {
            val mode = cs.limitMode.lowercase()
            val inZone = z.urlParams.lowercase().contains(mode) ||
                z.zoneName.lowercase().contains(mode) ||
                z.locationName.lowercase().contains(mode) ||
                locationName.lowercase().contains(mode)
            if (!inZone && !mode.contains("astral") && !mode.contains("mole")) {
                // Astral/mole use dedicated zone gates below
                if (!z.zoneName.contains("Astral", ignoreCase = true) &&
                    !z.zoneName.contains("Mole", ignoreCase = true)
                ) {
                    return false
                }
            }
        }

        if (z.statRequirement > 0 && cs.buffedMainStat < z.statRequirement) return false

        if (z.zoneName.startsWith("PirateRealm", ignoreCase = true)) {
            if (!canAdventureAtPirateRealm(locationName, z.zoneName, preferences)) return false
        }

        return AdventureZoneGates.canAdventureZone(locationName, z, ctx)
    }

    fun canAdventureAtPirateRealm(
        locationName: String,
        zoneName: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return true
        if (!preferences.getBoolean("prAlways", false) &&
            !preferences.getBoolean("_prToday", false)
        ) {
            return false
        }
        if (locationName.equals("Sailing the PirateRealm Seas", ignoreCase = true) ||
            locationName.equals("PirateRealm Island", ignoreCase = true)
        ) {
            return true
        }
        if (zoneName.equals("PirateRealm Island", ignoreCase = true)) {
            val lastIsland = preferences.getString("_lastPirateRealmIsland", "")
            return lastIsland.equals(locationName, ignoreCase = true)
        }
        return true
    }

    fun woodsOpen(preferences: Preferences?, questDatabase: QuestDatabase? = null): Boolean =
        AdventureUnlockHelpers.woodsOpen(buildContext(null, preferences, questDatabase))

    fun cemeteryOpen(preferences: Preferences?, questDatabase: QuestDatabase? = null): Boolean =
        AdventureUnlockHelpers.cemeteryOpen(buildContext(null, preferences, questDatabase))

    fun tooDrunkToAdventure(
        locationName: String,
        character: CharacterState?,
        zone: AdventureZone? = AdventureDatabase.getByName(locationName),
        preferences: Preferences? = null,
    ): Boolean = AdventureZoneGates.tooDrunkToAdventure(
        locationName,
        zone,
        buildContext(character, preferences),
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
        questDatabase: QuestDatabase? = null,
    ): Boolean {
        if (!canAdventureAtZone(locationName, character, preferences = preferences, questDatabase = questDatabase)) {
            return false
        }

        val zone = AdventureDatabase.getByName(locationName)
        val ctx = buildContext(character, preferences, questDatabase)

        val familiarName = preferences?.getString("zoneFamiliar_$locationName", "")?.takeIf { it.isNotBlank() }
        if (familiarName != null) {
            val fm = familiarManager ?: return false
            if (fm.setFamiliar(familiarName).isFailure) return false
        }

        val outfitName = preferences?.getString("zoneOutfit_$locationName", "")?.takeIf { it.isNotBlank() }
            ?: AdventurePrepareActions.resolveOutfit(locationName, zone)

        if (outfitName != null) {
            val manager = outfitManager ?: return false
            if (!manager.wearOutfit(outfitName)) return false
        }

        if (!AdventurePrepareActions.prepare(
                locationName,
                zone,
                ctx,
                AdventurePrepareActions.PrepareDeps(
                    outfitManager = null, // already worn above
                    retrieveItemService = retrieveItemService,
                    useItemRequest = useItemRequest,
                    gameDatabase = gameDatabase,
                    visitUrl = visitUrl,
                    equipItem = equipItem,
                    unequipSlot = unequipSlot,
                    hasEffect = hasEffect,
                    stenchResistanceLevels = stenchResistanceLevels,
                    preferFamiliar = preferFamiliar,
                ),
            )
        ) {
            return false
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
