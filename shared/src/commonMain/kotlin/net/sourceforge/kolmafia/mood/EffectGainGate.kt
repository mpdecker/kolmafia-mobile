package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.TurtleBlessing
import net.sourceforge.kolmafia.character.TurtleBlessingLevel
import net.sourceforge.kolmafia.effect.EffectGainEffectIds
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.maximizer.Evaluator.cannotGainEffect]. */
object EffectGainGate {

    fun cannotGainEffect(
        effectId: Int,
        charState: CharacterState,
        effectState: EffectState,
        prefs: Preferences,
    ): Boolean {
        val blessingType = TurtleBlessing.fromActiveEffects(effectState)
        val blessingLevel = TurtleBlessingLevel.fromActiveEffects(effectState)
        val muffler = prefs.getString(Preferences.PETE_MOTORBIKE_MUFFLER, "")

        return when (effectId) {
            EffectGainEffectIds.NEARLY_SILENT_HUNTING -> charState.isSealClubber
            EffectGainEffectIds.SILENT_HUNTING,
            EffectGainEffectIds.BARREL_CHESTED,
            -> !charState.isSealClubber
            EffectGainEffectIds.BOON_OF_SHE_WHO_WAS ->
                blessingType != TurtleBlessing.SHE_WHO_WAS ||
                    blessingLevel == TurtleBlessingLevel.AVATAR
            EffectGainEffectIds.BOON_OF_THE_STORM_TORTOISE ->
                blessingType != TurtleBlessing.STORM ||
                    blessingLevel == TurtleBlessingLevel.AVATAR
            EffectGainEffectIds.BOON_OF_THE_WAR_SNAPPER ->
                blessingType != TurtleBlessing.WAR ||
                    blessingLevel == TurtleBlessingLevel.AVATAR
            EffectGainEffectIds.AVATAR_OF_SHE_WHO_WAS ->
                blessingType != TurtleBlessing.SHE_WHO_WAS ||
                    blessingLevel != TurtleBlessingLevel.GLORIOUS_BLESSING
            EffectGainEffectIds.AVATAR_OF_THE_STORM_TORTOISE ->
                blessingType != TurtleBlessing.STORM ||
                    blessingLevel != TurtleBlessingLevel.GLORIOUS_BLESSING
            EffectGainEffectIds.AVATAR_OF_THE_WAR_SNAPPER ->
                blessingType != TurtleBlessing.WAR ||
                    blessingLevel != TurtleBlessingLevel.GLORIOUS_BLESSING
            EffectGainEffectIds.BLESSING_OF_SHE_WHO_WAS ->
                !charState.isTurtleTamer ||
                    blessingType == TurtleBlessing.SHE_WHO_WAS ||
                    blessingLevel == TurtleBlessingLevel.PARIAH ||
                    blessingLevel == TurtleBlessingLevel.AVATAR
            EffectGainEffectIds.BLESSING_OF_THE_STORM_TORTOISE ->
                !charState.isTurtleTamer ||
                    blessingType == TurtleBlessing.STORM ||
                    blessingLevel == TurtleBlessingLevel.PARIAH ||
                    blessingLevel == TurtleBlessingLevel.AVATAR
            EffectGainEffectIds.BLESSING_OF_THE_WAR_SNAPPER ->
                !charState.isTurtleTamer ||
                    blessingType == TurtleBlessing.WAR ||
                    blessingLevel == TurtleBlessingLevel.PARIAH ||
                    blessingLevel == TurtleBlessingLevel.AVATAR
            EffectGainEffectIds.DISDAIN_OF_SHE_WHO_WAS,
            EffectGainEffectIds.DISDAIN_OF_THE_STORM_TORTOISE,
            EffectGainEffectIds.DISDAIN_OF_THE_WAR_SNAPPER,
            -> charState.isTurtleTamer
            EffectGainEffectIds.BARREL_OF_LAUGHS -> !charState.isTurtleTamer
            EffectGainEffectIds.FLIMSY_SHIELD_OF_THE_PASTALORD,
            EffectGainEffectIds.BLOODY_POTATO_BITS,
            EffectGainEffectIds.SLINKING_NOODLE_GLOB,
            EffectGainEffectIds.WHISPERING_STRANDS,
            EffectGainEffectIds.MACARONI_COATING,
            EffectGainEffectIds.PENNE_FEDORA,
            EffectGainEffectIds.PASTA_EYEBALL,
            EffectGainEffectIds.SPICE_HAZE,
            EffectGainEffectIds.LEGENDARY_BLOODY_POTATO_BITS,
            EffectGainEffectIds.LEGENDARY_SLINKING_NOODLE_GLOB,
            EffectGainEffectIds.LEGENDARY_WHISPERING_STRANDS,
            EffectGainEffectIds.LEGENDARY_MACARONI_COATING,
            EffectGainEffectIds.LEGENDARY_PENNE_FEDORA,
            EffectGainEffectIds.LEGENDARY_PASTA_EYEBALL,
            EffectGainEffectIds.LEGENDARY_SPICE_HAZE,
            -> charState.isPastamancer
            EffectGainEffectIds.SHIELD_OF_THE_PASTALORD,
            EffectGainEffectIds.PORK_BARREL,
            -> !charState.isPastamancer
            EffectGainEffectIds.BLOOD_SUGAR_SAUCE_MAGIC,
            EffectGainEffectIds.SOULERSKATES,
            EffectGainEffectIds.WARLOCK_WARSTOCK_WARBARREL,
            -> !charState.isSauceror
            EffectGainEffectIds.BLOOD_SUGAR_SAUCE_MAGIC_LITE -> charState.isSauceror
            EffectGainEffectIds.DOUBLE_BARRELED -> !charState.isDiscoBandit
            EffectGainEffectIds.BEER_BARREL_POLKA -> !charState.isAccordionThief
            EffectGainEffectIds.UNMUFFLED -> muffler != "Extra-Loud Muffler"
            EffectGainEffectIds.MUFFLED -> muffler != "Extra-Quiet Muffler"
            else -> false
        }
    }
}
