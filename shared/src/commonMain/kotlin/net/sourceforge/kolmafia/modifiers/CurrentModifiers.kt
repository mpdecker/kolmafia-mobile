package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.effect.EffectData
import kotlin.math.floor
import kotlin.math.max

// Aggregates all active modifier sources for a character snapshot and exposes
// computed effective values. Instantiate per-request — it's stateless and cheap.
class CurrentModifiers(
    private val state: CharacterState,
    private val activeEffects: List<EffectData> = emptyList()
) {
    // Accumulated modifier total from equipment + effects
    val values: ModifierValues by lazy { accumulate() }

    // ── Derived stat computations (mirrors desktop Modifiers.predict()) ────────

    fun buffedMuscle(): Int {
        val flat  = values.get(DoubleModifier.MUS)
        val pct   = values.get(DoubleModifier.MUS_PCT)
        val limit = values.get(DoubleModifier.MUS_LIMIT)
        val base  = state.baseMusc
        var result = floor(base * (1 + pct / 100.0)).toInt() + flat.toInt()
        if (limit > 0) result = result.coerceAtMost(limit.toInt())
        return max(0, result)
    }

    fun buffedMysticality(): Int {
        val flat  = values.get(DoubleModifier.MYS)
        val pct   = values.get(DoubleModifier.MYS_PCT)
        val limit = values.get(DoubleModifier.MYS_LIMIT)
        val base  = state.baseMyst
        var result = floor(base * (1 + pct / 100.0)).toInt() + flat.toInt()
        if (limit > 0) result = result.coerceAtMost(limit.toInt())
        return max(0, result)
    }

    fun buffedMoxie(): Int {
        val flat  = values.get(DoubleModifier.MOX)
        val pct   = values.get(DoubleModifier.MOX_PCT)
        val limit = values.get(DoubleModifier.MOX_LIMIT)
        val base  = state.baseMoxie
        var result = floor(base * (1 + pct / 100.0)).toInt() + flat.toInt()
        if (limit > 0) result = result.coerceAtMost(limit.toInt())
        return max(0, result)
    }

    fun buffedMainStat(): Int = when (state.mainStat) {
        MainStat.MUSCLE      -> buffedMuscle()
        MainStat.MYSTICALITY -> buffedMysticality()
        MainStat.MOXIE       -> buffedMoxie()
    }

    fun buffedHp(): Int {
        val flat = values.get(DoubleModifier.HP)
        val pct  = values.get(DoubleModifier.HP_PCT)
        val base = state.baseMaxHp
        return max(0, floor(base * (1 + pct / 100.0)).toInt() + flat.toInt())
    }

    fun buffedMp(): Int {
        val flat = values.get(DoubleModifier.MP)
        val pct  = values.get(DoubleModifier.MP_PCT)
        val base = state.baseMaxMp
        return max(0, floor(base * (1 + pct / 100.0)).toInt() + flat.toInt())
    }

    // Convenience: all derived values as a map
    fun derived(): Map<DerivedModifier, Int> = mapOf(
        DerivedModifier.BUFFED_MUS  to buffedMuscle(),
        DerivedModifier.BUFFED_MYS  to buffedMysticality(),
        DerivedModifier.BUFFED_MOX  to buffedMoxie(),
        DerivedModifier.BUFFED_HP   to buffedHp(),
        DerivedModifier.BUFFED_MP   to buffedMp()
    )

    // ── Itemdrop / meatdrop helpers ───────────────────────────────────────────

    fun itemDropBonus(): Double = values.get(DoubleModifier.ITEMDROP) +
        values.get(DoubleModifier.GEARDROP) - values.get(DoubleModifier.ITEMDROP_PENALTY)

    fun meatDropBonus(): Double = values.get(DoubleModifier.MEATDROP) -
        values.get(DoubleModifier.MEATDROP_PENALTY)

    fun initiativeBonus(): Double = values.get(DoubleModifier.INITIATIVE) -
        maxOf(0.0, values.get(DoubleModifier.INITIATIVE_PENALTY))

    fun combatRateBonus(): Double = values.get(DoubleModifier.COMBAT_RATE)

    // ── Private accumulation ──────────────────────────────────────────────────

    private fun accumulate(): ModifierValues {
        var total = ModifierValues.EMPTY

        // Equipment modifiers
        for ((_, itemName) in state.equippedItems()) {
            val raw = ModifierDatabase.getItem(itemName)?.modifiers ?: continue
            total = total + ModifierParser.parse(raw)
        }

        // Effect modifiers
        for (effect in activeEffects) {
            val raw = ModifierDatabase.getEffect(effect.name)?.modifiers ?: continue
            total = total + ModifierParser.parse(raw)
        }

        return total
    }
}
