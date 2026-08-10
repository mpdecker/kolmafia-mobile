package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.modifiers.BitmapModifier
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.ModifierValues

/**
 * Desktop Evaluator.enumerateEquipment automatic-bucket context (Phase 368).
 * Computes which synergy/outfit/category items must stay in candidate lists.
 */
class MaximizerAutoContext private constructor(
    private val evaluator: Evaluator,
    val nullScore: Double,
    private val catUseful: Map<String, Boolean>,
    private val usefulSynergyMask: Int,
    val usefulSynergyItemNames: Set<String>,
    val usefulOutfitPieceNames: Set<String>,
) {
    fun isCatUseful(catName: String): Boolean = catUseful[catName] == true

    fun shouldPinAutomatic(itemName: String, itemMods: ModifierValues): Boolean {
        when (evaluator.checkConstraints(itemMods)) {
            Evaluator.Constraint.MEETS -> return true
            Evaluator.Constraint.VIOLATES -> return false
            Evaluator.Constraint.IRRELEVANT -> Unit
        }

        if (isCatUseful("_hoboPower") &&
            (itemName.startsWith("Hodgman's", ignoreCase = true) ||
                itemMods.get(DoubleModifier.HOBO_POWER) > 0.0)
        ) {
            return true
        }
        if (isCatUseful("_brimstone") && itemMods.get(BitmapModifier.BRIMSTONE) != 0) {
            return true
        }
        if (isCatUseful("_cloathing") && itemMods.get(BitmapModifier.CLOATHING) != 0) {
            return true
        }
        if (isCatUseful("_smithsness") && itemMods.get(DoubleModifier.SMITHSNESS) > 0.0) {
            return true
        }
        if (isCatUseful("_slimeHate") && itemMods.get(DoubleModifier.SLIME_HATES_IT) > 0.0) {
            return true
        }
        if (isCatUseful("_mcHugeLarge") && itemMods.get(BitmapModifier.MCHUGELARGE) != 0) {
            return true
        }
        val synergetic = itemMods.get(BitmapModifier.SYNERGETIC)
        if (synergetic != 0 && (synergetic and usefulSynergyMask) != 0) {
            return true
        }
        if (itemName.lowercase() in usefulSynergyItemNames) {
            return true
        }
        if (itemName.lowercase() in usefulOutfitPieceNames) {
            return true
        }
        return false
    }

    companion object {
        private val MAX_CAT_NAMES = listOf(
            "_hoboPower",
            "_brimstone",
            "_cloathing",
            "_smithsness",
            "_slimeHate",
            "_mcHugeLarge",
        )

        fun from(evaluator: Evaluator): MaximizerAutoContext {
            val nullScore = evaluator.getScore(CurrentModifiers(CharacterState()))
            val catUseful = MAX_CAT_NAMES.associateWith { catName ->
                isCatUseful(evaluator, catName)
            }

            var usefulSynergyMask = 0
            val usefulSynergyItemNames = mutableSetOf<String>()
            for (entry in ModifierDatabase.synergies()) {
                val synergyMods = ModifierParser.parse(entry.modifiers)
                if (evaluator.getItemContribution(synergyMods) <= 0.0 &&
                    evaluator.checkConstraints(synergyMods) != Evaluator.Constraint.MEETS
                ) {
                    continue
                }
                ModifierDatabase.synergyMaskByName()[entry.name]?.let { usefulSynergyMask = usefulSynergyMask or it }
                for (piece in entry.name.split('/')) {
                    val pieceName = piece.trim()
                    if (pieceName.isNotEmpty()) {
                        usefulSynergyItemNames += pieceName.lowercase()
                    }
                }
            }

            val usefulOutfitPieceNames = mutableSetOf<String>()
            for (outfit in OutfitDatabase.all()) {
                val outfitEntry = ModifierDatabase.getOutfit(outfit.name) ?: continue
                val outfitMods = ModifierParser.parse(outfitEntry.modifiers)
                val useful = when (evaluator.checkConstraints(outfitMods)) {
                    Evaluator.Constraint.MEETS -> true
                    Evaluator.Constraint.VIOLATES -> false
                    Evaluator.Constraint.IRRELEVANT ->
                        evaluator.getItemContribution(outfitMods) > 0.0
                }
                if (useful) {
                    for (piece in outfit.equipment) {
                        usefulOutfitPieceNames += piece.lowercase()
                    }
                }
            }

            return MaximizerAutoContext(
                evaluator = evaluator,
                nullScore = nullScore,
                catUseful = catUseful,
                usefulSynergyMask = usefulSynergyMask,
                usefulSynergyItemNames = usefulSynergyItemNames,
                usefulOutfitPieceNames = usefulOutfitPieceNames,
            )
        }

        private fun isCatUseful(evaluator: Evaluator, catName: String): Boolean {
            val entry = ModifierDatabase.getMaxCat(catName) ?: return false
            val mods = ModifierParser.parse(entry.modifiers)
            return evaluator.getItemContribution(mods) > 0.0
        }
    }
}
