package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.WeaponType
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.request.FoldItemPlanner

/**
 * Desktop Evaluator.enumerateEquipment weapon / chefstaff / path gates (Phases 1446–1460).
 */
object MaximizerWeaponGates {

    const val SPECIAL_SAUCE_GLOVE = FoldItemPlanner.SPECIAL_SAUCE_GLOVE
    const val SPIRIT_OF_RIGATONI = FoldItemPlanner.SPIRIT_OF_RIGATONI
    const val FOURTH_SABER = 10251
    const val REPLICA_FOURTH_SABER = 11240

    /** Whether chefstaves may be considered in the weapon slot. */
    fun canUseChefstaff(
        charState: CharacterState?,
        hasSkill: (Int) -> Boolean,
        gloveAvailable: Boolean,
    ): Boolean {
        if (charState == null) return true
        if (hasSkill(SPIRIT_OF_RIGATONI)) return true
        if (charState.ascensionPath == AscensionPath.AVATAR_OF_JARLSBERG) return true
        if (charState.characterClassEnum == CharacterClass.SAUCEROR && gloveAvailable) return true
        return false
    }

    fun gloveAvailable(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(SPECIAL_SAUCE_GLOVE) > 0

    /**
     * Returns false when [itemId] should be skipped for weapon/offhand enumeration
     * under the evaluator's weapon-type constraints.
     */
    fun passesWeaponConstraints(
        itemId: Int,
        primaryUse: ItemPrimaryUse,
        evaluator: Evaluator,
        charState: CharacterState?,
        canChefstaff: Boolean,
        ironPalms: Boolean = false,
        hasSkill: (Int) -> Boolean = { false },
    ): Boolean {
        val isWeapon = primaryUse == ItemPrimaryUse.WEAPON || primaryUse == ItemPrimaryUse.SIXGUN
        val isOffhand = primaryUse == ItemPrimaryUse.OFFHAND
        if (!isWeapon && !isOffhand) return true

        if (isWeapon && EquipmentDatabase.isChefStaff(itemId) && !canChefstaff) {
            return false
        }

        if (evaluator.requireClub() && isWeapon &&
            !EquipmentDatabase.isClub(itemId, ironPalms)
        ) {
            return false
        }
        if (evaluator.requireSword() && isWeapon && !EquipmentDatabase.isSword(itemId)) {
            return false
        }
        if (evaluator.requireKnife() && isWeapon && !EquipmentDatabase.isKnife(itemId)) {
            return false
        }
        if (evaluator.requireUtensil() && isWeapon && !EquipmentDatabase.isUtensil(itemId)) {
            return false
        }
        if (evaluator.requireAccordion() && isWeapon && !EquipmentDatabase.isAccordion(itemId)) {
            return false
        }
        if (evaluator.requireShield() && isOffhand && !EquipmentDatabase.isShield(itemId)) {
            // Umbrella forward-facing is treated as shield via modeable elsewhere;
            // still allow non-shield offhands only when not requiring shield exclusivity —
            // desktop requireShield filters OFFHAND to shields.
            return false
        }

        val typeFilter = evaluator.weaponTypeFilter()
        if (!typeFilter.isNullOrBlank() && isWeapon) {
            val itemType = EquipmentDatabase.getItemType(itemId)
            if (!itemType.equals(typeFilter, ignoreCase = true) &&
                !itemType.contains(typeFilter, ignoreCase = true)
            ) {
                return false
            }
        }

        val handsWanted = evaluator.handsConstraint()
        if (handsWanted > 0 && isWeapon) {
            val hands = EquipmentDatabase.getHands(itemId)
            if (handsWanted >= 2 && hands < 2) return false
            if (handsWanted == 1 && hands != 1 && !EquipmentDatabase.isChefStaff(itemId)) {
                return false
            }
        }

        val melee = evaluator.meleeConstraint()
        if (melee != 0 && isWeapon) {
            val wt = EquipmentDatabase.getWeaponType(itemId)
            when {
                melee >= 2 && wt != WeaponType.MELEE -> return false
                melee <= -2 && wt != WeaponType.RANGED -> return false
                melee == 1 && wt == WeaponType.RANGED -> return false
                melee == -1 && wt == WeaponType.MELEE -> return false
            }
        }

        if (evaluator.effectiveWeapons() && isWeapon &&
            !isEffectiveWeapon(itemId, charState, hasSkill)
        ) {
            return false
        }

        // Hardcore path equipment gates (high-traffic subset)
        if (charState != null && !passesPathGate(itemId, charState)) return false

        return true
    }

    private fun isEffectiveWeapon(
        itemId: Int,
        state: CharacterState?,
        hasSkill: (Int) -> Boolean,
    ): Boolean {
        if (itemId == FOURTH_SABER || itemId == REPLICA_FOURTH_SABER) return true
        val name = net.sourceforge.kolmafia.data.ItemDatabase.getItemName(itemId)
        if (ModifierDatabase.hasBooleanModifier(name, BooleanModifier.ATTACKS_CANT_MISS)) return true
        val weaponType = EquipmentDatabase.getWeaponType(itemId)
        if (state == null) return true
        return if (state.buffedMoxie >= state.buffedMusc) {
            weaponType == WeaponType.RANGED ||
                (EquipmentDatabase.isKnife(itemId) && hasSkill(TRICKY_KNIFEWORK))
        } else {
            weaponType == WeaponType.MELEE
        }
    }

    // SkillPool.TRICKY_KNIFEWORK without importing the desktop object pool.
    private const val TRICKY_KNIFEWORK = 5029

    private fun passesPathGate(itemId: Int, state: CharacterState): Boolean {
        // Boris: only Boris-specific weapons; keep soft — skip nothing unless known restricted.
        // Folder holder already gated via MaximizerSubSlotItems.
        return when (state.ascensionPath) {
            AscensionPath.AVATAR_OF_BORIS -> true
            AscensionPath.AVATAR_OF_JARLSBERG -> true
            AscensionPath.AVATAR_OF_SNEAKY_PETE -> true
            AscensionPath.ZOMBIE_SLAYER -> true
            else -> true
        }
    }
}
