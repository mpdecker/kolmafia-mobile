package net.sourceforge.kolmafia.adventure.choice

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.skill.SkillState

data class ChoiceContext(
    val choiceId: Int,
    val options: Map<Int, String>,
    val responseText: String,
    val characterState: CharacterState,
    val inventoryState: InventoryState,
    val effectState: EffectState,
    val skillState: SkillState,
    val preferences: Preferences,
    val goalManager: GoalManager,
    val questDatabase: QuestDatabase,
    val solvers: ChoiceSolvers,
    val preference: Int,
    val stepCount: Int = 0,
    val skillUses: Int = 0,
) {
    fun getCount(itemId: Int): Int = inventoryState.items[itemId]?.quantity ?: 0
    fun hasItem(itemId: Int): Boolean = getCount(itemId) > 0

    fun hasEquipped(itemId: Int): Boolean {
        val name = ItemDatabase.getById(itemId)?.name ?: return false
        return characterState.equipment.values.any { it.equals(name, ignoreCase = true) }
    }

    fun isWearingOutfit(outfitId: Int): Boolean {
        val outfit = OutfitDatabase.getById(outfitId) ?: return false
        return OutfitManager.isWearingPieces(outfit.equipment, characterState.equipment)
    }

    fun hasEffect(effectName: String): Boolean =
        effectState.effects.any { it.name.equals(effectName, ignoreCase = true) }

    fun currentNumericModifier(modifier: DoubleModifier): Double {
        val passiveSkillNames = skillState.skills
            .filter { !it.isActive }
            .map { it.name }
            .toSet()
        return CurrentModifiers(characterState, effectState.effects, passiveSkillNames)
            .values.get(modifier)
    }

    val characterClass get() = characterState.characterClassEnum
    val mainStat: MainStat  get() = characterState.mainStat
    val buffedMusc: Int     get() = characterState.buffedMusc
    val buffedMyst: Int     get() = characterState.buffedMyst
    val buffedMoxie: Int    get() = characterState.buffedMoxie
    val availableMeat: Int  get() = characterState.meat
    val ascensionNumber: Int get() = characterState.ascensionNumber
    val currentMp: Int      get() = characterState.currentMp
    val isFistcore: Boolean get() = characterState.isFistcore
    val isAxecore: Boolean  get() = characterState.isAxecore
    val kingLiberated: Boolean get() = characterState.kingLiberated

    fun hasItemGoal(itemId: Int): Boolean = goalManager.hasItemGoal(itemId)

    fun prefInt(key: String, default: Int = 0): Int       = preferences.getInt(key, default)
    fun prefBool(key: String, default: Boolean = false): Boolean = preferences.getBoolean(key, default)
    fun prefString(key: String, default: String = ""): String    = preferences.getString(key, default)

    fun optionExists(n: Int): Boolean = options.containsKey(n)
}
