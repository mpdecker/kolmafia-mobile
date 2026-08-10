package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.Beeosity
import net.sourceforge.kolmafia.data.CafeDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop Maximizer consumption gates (~593–723). */
object MaximizerConsumptionGates {

    private const val VINTNER_WINE_EFFECT_PREF = "vintnerWineEffect"

    private val TRIVIA_MASTER_USE =
        "use 1 Trivial Avocations Card: What?, 1 Trivial Avocations Card: When?"

    fun isTriviaMasterUse(source: String): Boolean =
        source.contains(TRIVIA_MASTER_USE, ignoreCase = true)

    fun canMasterTrivia(inventoryCount: (Int) -> Int, canInteract: Boolean): Boolean {
        if (canInteract) return true
        return inventoryCount(ItemDatabase.WHAT_CARD) > 0 &&
            inventoryCount(ItemDatabase.WHEN_CARD) > 0 &&
            inventoryCount(ItemDatabase.WHERE_CARD) > 0 &&
            inventoryCount(ItemDatabase.WHO_CARD) > 0
    }

    fun excludedTCRSItem(itemId: Int): Boolean =
        itemId == ItemDatabase.DIETING_PILL

    fun isCafeMenuItem(itemName: String): Boolean =
        CafeDatabase.getFood(itemName) != null || CafeDatabase.getDrink(itemName) != null

    fun blockedInGLover(itemId: Int, itemName: String, inGLover: Boolean): Boolean {
        if (!inGLover) return false
        if (Beeosity.hasGs(itemName)) return false
        if (isCafeMenuItem(itemName)) return false
        return ItemDatabase.unusableInGLover(itemId)
    }

    fun vintnerWineAllowed(
        effectName: String,
        inventoryCount: (Int) -> Int,
        preferences: Preferences?,
    ): Boolean {
        if (inventoryCount(ItemDatabase.VAMPIRE_VINTNER_WINE) <= 0) return false
        val wineEffect = preferences?.getString(VINTNER_WINE_EFFECT_PREF, "") ?: return false
        return wineEffect.equals(effectName, ignoreCase = true)
    }

    fun itemEffectDuration(itemName: String, effectName: String): Int {
        val entry = ModifierDatabase.getItem(itemName) ?: return 0
        val parsed = ModifierParser.parse(entry.modifiers)
        val effects = parsed.getAll(StringModifier.EFFECT)
        if (effects.isEmpty()) return 0
        val index = effects.indexOfFirst { it.equals(effectName, ignoreCase = true) }
        if (index < 0) return 0
        if (effects.size == 1) {
            return parsed.getInt(DoubleModifier.EFFECT_DURATION)
        }
        return pairedEffectDurations(entry.modifiers).getOrNull(index) ?: 0
    }

    private fun pairedEffectDurations(modifierString: String): List<Int> {
        val durations = mutableListOf<Int>()
        for (token in modifierString.split(',')) {
            val trimmed = token.trim()
            when {
                trimmed.startsWith("${DoubleModifier.EFFECT_DURATION.tag}:", ignoreCase = true) -> {
                    val value = trimmed.substringAfter(':').trim().toIntOrNull() ?: 0
                    durations += value
                }
            }
        }
        return durations
    }
}
