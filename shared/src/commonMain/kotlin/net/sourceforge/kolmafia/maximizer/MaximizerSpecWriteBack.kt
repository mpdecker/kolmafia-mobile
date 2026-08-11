package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.modifiers.ModifierValuesFormatter
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.maximizer.Maximizer] / SpeculateCommand `_spec` write-back. */
object MaximizerSpecWriteBack {

    fun writeFromPlan(
        plan: MaximizerEmitSlot.Plan,
        charState: CharacterState,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
        carryFamiliars: List<String> = emptyList(),
        gameDatabase: GameDatabase? = null,
        preferences: Preferences? = null,
    ) {
        val values = MaximizerSpeculation.modifierValuesForPostEquipmentPlan(
            plan = plan,
            charState = charState,
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
            carryFamiliars = carryFamiliars,
            gameDatabase = gameDatabase,
            preferences = preferences,
        )
        ModifierDatabase.overrideGenerated("_spec", ModifierValuesFormatter.format(values))
    }

    /** Live post-equip `_spec` after maximize rescore (Phase 406). */
    fun writeFromLiveState(
        charState: CharacterState,
        activeEffects: List<EffectData> = emptyList(),
        passiveSkillNames: Set<String> = emptySet(),
        gameDatabase: GameDatabase? = null,
        preferences: Preferences? = null,
    ) {
        val values = MaximizerSpeculation.modifierValuesForPostEquipmentLive(
            charState = charState,
            activeEffects = activeEffects,
            passiveSkillNames = passiveSkillNames,
            gameDatabase = gameDatabase,
            preferences = preferences,
        )
        ModifierDatabase.overrideGenerated("_spec", ModifierValuesFormatter.format(values))
    }
}
