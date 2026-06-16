package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase

// Stateless helpers that derive combat-relevant values from CharacterState
// and the bundled data layer. Call these after GameDatabase.load() has completed.
object CharacterStats {

    // Buffed value of the character's primary combat stat.
    fun mainStatBuffed(state: CharacterState): Int = when (state.characterClassEnum.mainStat) {
        MainStat.MUSCLE        -> state.buffedMusc
        MainStat.MYSTICALITY   -> state.buffedMyst
        MainStat.MOXIE         -> state.buffedMoxie
    }

    // Base (unbuffed) value of the character's primary combat stat.
    fun mainStatBase(state: CharacterState): Int = when (state.characterClassEnum.mainStat) {
        MainStat.MUSCLE        -> state.baseMusc
        MainStat.MYSTICALITY   -> state.baseMyst
        MainStat.MOXIE         -> state.baseMoxie
    }

    // Total equipment power across all currently equipped items.
    // Requires EquipmentDatabase to be loaded.
    fun totalEquipmentPower(state: CharacterState): Int =
        state.equipment.values.sumOf { name ->
            EquipmentDatabase.getByName(name)?.power ?: 0
        }

    // Returns the power of a single equipped slot, or 0 if not equipped / not in DB.
    fun slotPower(state: CharacterState, slot: EquipmentSlot): Int {
        val name = state.equipment[slot] ?: return 0
        return EquipmentDatabase.getByName(name)?.power ?: 0
    }

    // Returns true if the character meets the stat requirement for the named equipment piece.
    // Requirement format from equipment.txt: "Mus: 85", "Mys: 40", "Mox: 67"
    fun meetsRequirement(state: CharacterState, itemName: String): Boolean {
        val equip = EquipmentDatabase.getByName(itemName) ?: return true
        val req = equip.statRequirement ?: return true
        val (statStr, valueStr) = req.split(":").map { it.trim() }.let {
            if (it.size < 2) return true
            it[0] to it[1]
        }
        val required = valueStr.toIntOrNull() ?: return true
        return when (statStr.lowercase()) {
            "mus", "muscle"        -> state.buffedMusc >= required
            "mys", "mysticality"   -> state.buffedMyst >= required
            "mox", "moxie"         -> state.buffedMoxie >= required
            else                   -> true
        }
    }

    // Readable modifier string for an equipped item, straight from ModifierDatabase.
    fun itemModifiers(itemName: String): String =
        ModifierDatabase.getItem(itemName)?.modifiers ?: ""

    // Rough estimate of HP regen per turn (from equipment modifiers, simplified).
    // Only parses the "HP Regen" portion of modifier strings — good enough for UI hints.
    fun estimatedHpRegenPerTurn(state: CharacterState): IntRange {
        var min = 0; var max = 0
        for (itemName in state.equipment.values) {
            val mods = ModifierDatabase.getItem(itemName)?.modifiers ?: continue
            val match = Regex("""HP Regen[^:]*:\s*\+?(\d+)(?:-(\d+))?""").find(mods)
            if (match != null) {
                min += match.groupValues[1].toIntOrNull() ?: 0
                max += (match.groupValues[2].takeIf { it.isNotBlank() }
                    ?: match.groupValues[1]).toIntOrNull() ?: 0
            }
        }
        return min..max
    }

    // Rough estimate of MP regen per turn.
    fun estimatedMpRegenPerTurn(state: CharacterState): IntRange {
        var min = 0; var max = 0
        for (itemName in state.equipment.values) {
            val mods = ModifierDatabase.getItem(itemName)?.modifiers ?: continue
            val match = Regex("""MP Regen[^:]*:\s*\+?(\d+)(?:-(\d+))?""").find(mods)
            if (match != null) {
                min += match.groupValues[1].toIntOrNull() ?: 0
                max += (match.groupValues[2].takeIf { it.isNotBlank() }
                    ?: match.groupValues[1]).toIntOrNull() ?: 0
            }
        }
        return min..max
    }

    /** Mirrors desktop KoLCharacter.canPickpocket() — class/path/equipment gates. */
    fun canPickpocket(
        state: CharacterState,
        hasSkill: (String) -> Boolean = { false },
    ): Boolean {
        if (state.limitMode.equals("bird", ignoreCase = true)) return true
        when (state.characterClassEnum) {
            CharacterClass.DISCO_BANDIT,
            CharacterClass.ACCORDION_THIEF,
            CharacterClass.GELATINOUS_NOOB -> return true
            else -> Unit
        }
        if (state.ascensionPath == AscensionPath.AVATAR_OF_SNEAKY_PETE) return true
        val weapon = state.equippedItem(EquipmentSlot.WEAPON).orEmpty()
        if (weapon.contains("focused magnetron pistol", ignoreCase = true)) return true
        val offhand = state.equippedItem(EquipmentSlot.OFFHAND).orEmpty()
        if (offhand.contains("tiny black hole", ignoreCase = true)) return true
        if (state.equipment.values.any { it.contains("mime army infiltration glove", ignoreCase = true) }) {
            return true
        }
        if (hasSkill("Chicken Fingers")) return true
        return false
    }
}
