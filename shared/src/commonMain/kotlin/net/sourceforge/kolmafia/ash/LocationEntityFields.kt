package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.data.BountyDatabase
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.EncounterDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ZoneParentDatabase
import net.sourceforge.kolmafia.modifiers.LocationNames
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.AdventureSpentTracker
import net.sourceforge.kolmafia.session.DreadKissesTracker
import net.sourceforge.kolmafia.session.WildfireCampManager
import net.sourceforge.kolmafia.character.CharacterState

/**
 * Resolves `$location[field]` bracket access. Mirrors desktop [LocationProxy].
 */
internal object LocationEntityFields {

    fun resolve(
        locationName: String,
        fieldName: String,
        gameDatabase: GameDatabase?,
        preferences: Preferences?,
        adventureSpentTracker: AdventureSpentTracker? = null,
        dreadKissesTracker: DreadKissesTracker? = null,
        wildfireCampManager: WildfireCampManager? = null,
        characterState: CharacterState? = null,
    ): AshValue {
        val canonical = LocationNames.resolve(locationName) ?: locationName
        val zone = AdventureDatabase.getByName(canonical)
            ?: gameDatabase?.zone(canonical)
        return when (fieldName.lowercase()) {
            "id" -> AshValue.of((zone?.snarfblat?.toIntOrNull() ?: 0).toLong())
            "zone" -> AshValue.of(zone?.zoneName ?: "")
            "parent" -> AshValue.of(parentZone(zone))
            "parentdesc" -> AshValue.of(parentDescription(zone))
            "root" -> AshValue.of(rootZone(zone))
            "difficulty_level" -> AshValue.of(zone?.diffLevel ?: "")
            "environment" -> AshValue.of(zone?.environment ?: "")
            "recommended_stat" -> AshValue.of((zone?.statRequirement ?: 0).toLong())
            "force_noncombat" -> AshValue.of((zone?.forceNoncombat ?: 0).toLong())
            "wanderers" -> AshValue.of(!(zone?.noWander ?: false))
            "combat_percent" -> AshValue.of(combatPercent(zone).toDouble())
            "combat_queue" -> AshValue.of(combatQueue(zone))
            "noncombat_queue" -> AshValue.of(noncombatQueue(canonical))
            "bounty" -> AshValue(AshType.BOUNTY, bountyForLocation(canonical))
            "nocombats" -> AshValue.of(isNoCombats(zone))
            "pledge_allegiance" -> AshValue.of(pledgeAllegiance(zone))
            "turns_spent" -> AshValue.of(
                (adventureSpentTracker?.getTurns(canonical) ?: 0).toLong(),
            )
            "last_noncombat_turns_spent" -> AshValue.of(
                adventureSpentTracker?.lastNoncombatTurnsSpent(zone)
                    ?: if ((zone?.forceNoncombat ?: 0) > 0) -1L else -1L,
            )
            "kisses" -> AshValue.of(dreadKissesTracker?.kissesForLocation(canonical) ?: 0L)
            "poison" -> AshValue.of(CombatDatabase.poisonForLocation(canonical).toLong())
            "water_level" -> AshValue.of(
                if (characterState?.isRaincore == true) (zone?.waterLevel ?: 0).toLong() else 0L,
            )
            "fire_level" -> AshValue.of(
                if (characterState?.isFirecore == true) {
                    (wildfireCampManager?.getFireLevel(canonical)
                        ?: WildfireCampManager.DEFAULT_FIRE_LEVEL).toLong()
                } else {
                    0L
                },
            )
            else -> throw ScriptException("location has no field '$fieldName'")
        }
    }

    private fun parentZone(zone: AdventureZone?): String {
        if (zone == null) return ""
        return ZoneParentDatabase.getByName(zone.zoneName)?.parent ?: ""
    }

    private fun parentDescription(zone: AdventureZone?): String {
        val parent = parentZone(zone)
        if (parent.isBlank()) return ""
        return ZoneParentDatabase.getByName(parent)?.description ?: ""
    }

    private fun rootZone(zone: AdventureZone?): String {
        if (zone == null) return ""
        var current = zone.zoneName
        val visited = mutableSetOf<String>()
        while (true) {
            if (!visited.add(current.lowercase())) break
            val parent = ZoneParentDatabase.getByName(current) ?: break
            if (parent.isTopLevel) return parent.name
            current = parent.parent
        }
        return current
    }

    private fun combatPercent(zone: AdventureZone?): Int {
        if (zone == null) return 0
        return CombatDatabase.getByLocation(zone.locationName)?.combatPercent ?: 0
    }

    private fun combatQueue(zone: AdventureZone?): String {
        if (zone == null) return ""
        val monsters = CombatDatabase.getByLocation(zone.locationName)?.monsters.orEmpty()
        return monsters.joinToString("; ") { it.name }
    }

    private fun noncombatQueue(locationName: String): String {
        return EncounterDatabase.forLocation(locationName)
            .joinToString("; ") { it.title }
    }

    private fun bountyForLocation(locationName: String): String {
        val bounty = BountyDatabase.all().firstOrNull {
            it.bestLocation.equals(locationName, ignoreCase = true)
        }
        return bounty?.name ?: ""
    }

    private fun isNoCombats(zone: AdventureZone?): Boolean {
        if (zone == null) return false
        if (zone.snarfblat == null) return true
        val combat = CombatDatabase.getByLocation(zone.locationName) ?: return false
        return combat.monsters.isEmpty() && combat.combatPercent == 0
    }

    private fun pledgeAllegiance(zone: AdventureZone?): String {
        val id = zone?.snarfblat?.toIntOrNull() ?: return ""
        if (id < 0) return ""
        val mod = id % 10
        val strEffect = when (mod) {
            0 -> "Item Drop: 30, Spooky Damage: 10, Spooky Spell Damage: 10"
            1 -> "Item Drop: 15, Meat Drop: 25, Stench Damage: 10, Stench Spell Damage: 10"
            2 -> "Meat Drop: 50, Hot Damage: 10, Hot Spell Damage: 10"
            3 -> "Meat Drop: 25, ${allResistance(2)}, Cold Damage: 10, Cold Spell Damage: 10"
            4 -> "${allResistance(4)}, Sleaze Damage: 10, Sleaze Spell Damage: 10"
            5 -> "${allResistance(2)}, Spooky Damage: 10, Spooky Spell Damage: 10, MP Regen Min: 10, MP Regen Max: 15"
            6 -> "Stench Damage: 10, Stench Spell Damage: 10, MP Regen Min: 20, MP Regen Max: 30"
            7 -> "Initiative: 50, Hot Damage: 10, Hot Spell Damage: 10, MP Regen Min: 10, MP Regen Max: 15"
            8 -> "Initiative: 100, Cold Damage: 10, Cold Spell Damage: 10"
            9 -> "Item Drop: 15, Initiative: 50, Sleaze Damage: 10, Sleaze Spell Damage: 10"
            else -> ""
        }
        val statEffect = when (id % 9) {
            0, 7, 8 -> ""
            1 -> ", Mysticality: 10"
            2 -> ", Moxie: 10"
            3 -> ", Muscle Percent: 10"
            4 -> ", Mysticality Percent: 10"
            5 -> ", Moxie Percent: 10"
            6 -> ", Muscle: 10"
            else -> ""
        }
        return strEffect + statEffect
    }

    private fun allResistance(amount: Int): String =
        "Hot Resistance: $amount, Cold Resistance: $amount, Spooky Resistance: $amount, " +
            "Stench Resistance: $amount, Sleaze Resistance: $amount"
}
