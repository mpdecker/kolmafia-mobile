package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.FamiliarSlotModifiers
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.ModifierValues
import net.sourceforge.kolmafia.modifiers.StringModifier
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Scores equipment carried in the familiar slot.
 * Hand/Left-Hand use stripped item modifiers; Hatrack/Scarecrow use familiar-effect only.
 */
object FamiliarCarriedScoring {

    private val multiplierPattern = Regex("""([\d.]+)\s*x\s*(Volley|Somb|Lep|Fairy)""")
    private val capPattern = Regex("""cap ([\d.]+)""")

    fun score(
        race: String,
        itemName: String,
        modifier: DoubleModifier,
        gameDatabase: GameDatabase,
        familiarWeight: Int = 10,
    ): Double {
        val entry = gameDatabase.itemModifier(itemName) ?: return 0.0
        return when (race) {
            FamiliarCarryRules.HAND_RACE, FamiliarCarryRules.LEFT_HAND_RACE ->
                FamiliarSlotModifiers.forHandSlot(ModifierParser.parse(entry.modifiers)).get(modifier)
            FamiliarCarryRules.HATRACK_RACE, FamiliarCarryRules.SCARECROW_RACE ->
                scoreFamiliarEquip(race, entry.modifiers, modifier, familiarWeight)
            else -> ModifierParser.parse(entry.modifiers).get(modifier)
        }
    }

    fun translateFamiliarEffect(effect: String): String {
        var translated = multiplierPattern.replace(effect) { match ->
            val amount = match.groupValues[1]
            val type = when (match.groupValues[2]) {
                "Volley" -> "Volleyball"
                "Somb" -> "Sombrero"
                "Lep" -> "Leprechaun"
                else -> "Fairy"
            }
            "$type: $amount"
        }
        translated = capPattern.replace(translated) { "Familiar Weight Cap: ${it.groupValues[1]}" }
        return translated
    }

    private fun familiarEffectValues(itemModifiers: String): ModifierValues {
        val parsed = ModifierParser.parse(itemModifiers)
        val effect = parsed.get(StringModifier.FAMILIAR_EFFECT) ?: return ModifierValues.EMPTY
        return ModifierParser.parse(translateFamiliarEffect(effect))
    }

    private fun scoreFamiliarEquip(
        race: String,
        itemModifiers: String,
        modifier: DoubleModifier,
        familiarWeight: Int,
    ): Double {
        val effectValues = familiarEffectValues(itemModifiers)
        val weight = familiarWeight.coerceAtLeast(1)
        val cap = effectValues.get(DoubleModifier.FAMILIAR_WEIGHT_CAP).toInt()
        val capped = if (cap > 0) min(weight, cap) else weight

        var total = ModifierDatabase.getFamiliar(race)?.let {
            ModifierParser.parse(it.modifiers).get(modifier)
        } ?: 0.0

        total += when (modifier) {
            DoubleModifier.EXPERIENCE, DoubleModifier.FAMILIAR_EXP ->
                volleyballExperience(effectValues, capped) + sombreroExperience(effectValues, capped)
            DoubleModifier.MEATDROP -> leprechaunMeat(effectValues, capped)
            DoubleModifier.ITEMDROP -> fairyDrop(effectValues, capped)
            else -> effectValues.get(modifier)
        }
        return total
    }

    private fun volleyballExperience(values: ModifierValues, weight: Int): Double {
        val multiplier = values.get(DoubleModifier.VOLLEYBALL_WEIGHT)
        if (multiplier == 0.0) return 0.0
        val effective = weight * multiplier
        val factor = values.get(DoubleModifier.VOLLEYBALL_EFFECTIVENESS).let { if (it == 0.0) 1.0 else it }
        return factor * (2 + effective / 5.0)
    }

    private fun sombreroExperience(values: ModifierValues, weight: Int): Double {
        val multiplier = values.get(DoubleModifier.SOMBRERO_WEIGHT)
        if (multiplier == 0.0) return 0.0
        val effective = weight * multiplier + values.get(DoubleModifier.SOMBRERO_BONUS)
        if (effective == 0.0) return 0.0
        val factor = values.get(DoubleModifier.SOMBRERO_EFFECTIVENESS).let { if (it == 0.0) 1.0 else it }
        return min(maxOf(factor * 4.0 * (0.1 + 0.005 * effective), 1.0), 230.0)
    }

    private fun leprechaunMeat(values: ModifierValues, weight: Int): Double {
        val multiplier = values.get(DoubleModifier.LEPRECHAUN_WEIGHT)
        if (multiplier == 0.0) return 0.0
        val effective = weight * multiplier
        val factor = values.get(DoubleModifier.LEPRECHAUN_EFFECTIVENESS).let { if (it == 0.0) 1.0 else it }
        return factor * (sqrt(220.0 * effective) + 2 * effective - 6)
    }

    private fun fairyDrop(values: ModifierValues, weight: Int): Double {
        val multiplier = values.get(DoubleModifier.FAIRY_WEIGHT)
        if (multiplier == 0.0) return 0.0
        val effective = weight * multiplier
        val factor = values.get(DoubleModifier.FAIRY_EFFECTIVENESS).let { if (it == 0.0) 1.0 else it }
        return factor * (sqrt(55.0 * effective) + effective - 3)
    }
}
