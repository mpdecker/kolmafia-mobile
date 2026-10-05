package net.sourceforge.kolmafia.request

import kotlin.math.ceil
import net.sourceforge.kolmafia.data.EffectDatabase

/**
 * Desktop [UseItemRequest.elementalHelper] — preflight before Scratch's Fork / Frosty's Mug.
 * Removes conflicting Hotform/Coldform and recovers HP needed to survive the helper damage.
 */
object ElementalHelper {
    const val SCRATCHS_FORK = 3323
    const val FROSTYS_MUG = 3324
    const val HELPER_DAMAGE = 1000

    /** Active-effect presence by name (case-insensitive). */
    var hasEffect: (String) -> Boolean = { false }

    /** Uneffect by effect id; return true when the effect is gone (or was never present). */
    var uneffect: (suspend (Int) -> Boolean)? = null

    var currentHp: () -> Int = { 0 }

    /** Elemental resistance percent for "hot" / "cold" (0–100+). */
    var elementalResistancePercent: (String) -> Double = { 0.0 }

    /** Recover HP to at least [targetHp]; return true when current HP meets the target. */
    var recoverHpTo: (suspend (Int) -> Boolean)? = null

    /**
     * @return empty string on success, otherwise a desktop-shaped abort reason.
     */
    suspend fun prepare(removeEffectName: String, resistElement: String, amount: Int = HELPER_DAMAGE): String {
        val effect = EffectDatabase.getByName(removeEffectName)
            ?: EffectDatabase.getByName(removeEffectName.replace("form", " Form", ignoreCase = true))
        val stillHas: () -> Boolean = {
            hasEffect(removeEffectName) ||
                (effect != null && hasEffect(effect.name))
        }
        if (stillHas()) {
            val effectId = effect?.id ?: -1
            uneffect?.invoke(effectId)
            if (stillHas()) {
                return "Unable to remove $removeEffectName, which makes this helper unusable."
            }
        }

        val resist = elementalResistancePercent(resistElement).coerceAtLeast(0.0)
        val healthNeeded = ceil(amount * (100.0 - resist) / 100.0).toInt()
        if (currentHp() <= healthNeeded) {
            recoverHpTo?.invoke(healthNeeded + 1)
        }
        if (currentHp() <= healthNeeded) {
            return "Unable to gain enough HP to survive the use of this helper."
        }
        return ""
    }

    suspend fun prepareForUtensil(utensilId: Int): String = when (utensilId) {
        SCRATCHS_FORK -> prepare("Hotform", "hot")
        FROSTYS_MUG -> prepare("Coldform", "cold")
        else -> ""
    }

    fun resetForTest() {
        hasEffect = { false }
        uneffect = null
        currentHp = { 0 }
        elementalResistancePercent = { 0.0 }
        recoverHpTo = null
    }
}
