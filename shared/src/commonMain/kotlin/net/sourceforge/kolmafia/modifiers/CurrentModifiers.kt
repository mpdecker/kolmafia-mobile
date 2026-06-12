package net.sourceforge.kolmafia.modifiers

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.effect.EffectData
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Aggregates all active modifier sources for a character snapshot and exposes
 * computed effective values following the desktop's predict() algorithm exactly.
 *
 * Sources aggregated (in order):
 *   1. Equipped items (via ModifierDatabase "Item")
 *   2. Active effects  (via ModifierDatabase "Effect")
 *   3. Passive skills  (via ModifierDatabase "Skill")
 *   4. Zodiac sign     (via ModifierDatabase "Sign")
 *   5. Challenge path  (via ModifierDatabase "Path")
 *   6. Current familiar(via ModifierDatabase "Familiar")
 *   7. Complete outfits(via ModifierDatabase "Outfit")
 *
 * All values are advisory — authoritative buffed stats come from the KoL API.
 * Use this for pre-equip estimates and item advisor display.
 */
class CurrentModifiers(
    private val state: CharacterState,
    private val activeEffects: List<EffectData> = emptyList(),
    private val passiveSkillNames: Set<String> = emptySet()
) {
    private val context: ExpressionContext by lazy {
        ExpressionContext.from(state, activeEffects, passiveSkillNames)
    }

    /** Accumulated modifiers from all sources. Evaluated lazily once. */
    val values: ModifierValues by lazy { accumulate() }

    /** All five derived stats. Evaluated lazily once. */
    val derived: Map<DerivedModifier, Int> by lazy { predict() }

    // ── Derived stat accessors (from cached derived map) ──────────────────────

    fun buffedMuscle():      Int = derived[DerivedModifier.BUFFED_MUS] ?: 0
    fun buffedMysticality(): Int = derived[DerivedModifier.BUFFED_MYS] ?: 0
    fun buffedMoxie():       Int = derived[DerivedModifier.BUFFED_MOX] ?: 0
    fun buffedHp():          Int = derived[DerivedModifier.BUFFED_HP]  ?: 0
    fun buffedMp():          Int = derived[DerivedModifier.BUFFED_MP]  ?: 0

    fun buffedMainStat(): Int = when (state.mainStat) {
        MainStat.MUSCLE      -> buffedMuscle()
        MainStat.MYSTICALITY -> buffedMysticality()
        MainStat.MOXIE       -> buffedMoxie()
    }

    // ── Convenience modifiers ─────────────────────────────────────────────────

    fun itemDropBonus(): Double =
        values.get(DoubleModifier.ITEMDROP) +
        values.get(DoubleModifier.GEARDROP) -
        values.get(DoubleModifier.ITEMDROP_PENALTY)

    fun meatDropBonus(): Double =
        values.get(DoubleModifier.MEATDROP) -
        values.get(DoubleModifier.MEATDROP_PENALTY)

    fun initiativeBonus(): Double =
        values.get(DoubleModifier.INITIATIVE) -
        maxOf(0.0, values.get(DoubleModifier.INITIATIVE_PENALTY))

    /** Combat rate with diminishing returns cap applied (±25 uncapped; beyond, 1% per 5%). */
    fun combatRateBonus(): Double = cappedCombatRate(values.get(DoubleModifier.COMBAT_RATE))
    fun rawCombatRate():   Double = values.get(DoubleModifier.COMBAT_RATE)

    /** Prismatic damage = minimum of all five elemental damage values. */
    fun prismaticDamage(): Double = minOf(
        values.get(DoubleModifier.COLD_DAMAGE),
        values.get(DoubleModifier.HOT_DAMAGE),
        values.get(DoubleModifier.SLEAZE_DAMAGE),
        values.get(DoubleModifier.SPOOKY_DAMAGE),
        values.get(DoubleModifier.STENCH_DAMAGE)
    )

    // ── predict() — exact port of desktop Modifiers.predict() ─────────────────

    private fun predict(): Map<DerivedModifier, Int> {
        val v = values

        // Step 1: Base stats
        var mus = state.baseMusc
        var mys = state.baseMyst
        var mox = state.baseMoxie

        // Step 2: Equalization (EQUALIZE sets all three to one stat)
        val equalize = v.get(StringModifier.EQUALIZE).orEmpty()
        when {
            equalize.startsWith("Mus")  -> { mys = mus; mox = mus }
            equalize.startsWith("Mys")  -> { mus = mys; mox = mys }
            equalize.startsWith("Mox")  -> { mus = mox; mys = mox }
            equalize.startsWith("High") -> { val h = maxOf(mus, mys, mox); mus = h; mys = h; mox = h }
        }
        // Per-stat equalization (overrides individual stats)
        val musEq = v.get(StringModifier.EQUALIZE_MUSCLE).orEmpty()
        if (musEq.startsWith("Mys")) mus = mys else if (musEq.startsWith("Mox")) mus = mox
        val mysEq = v.get(StringModifier.EQUALIZE_MYST).orEmpty()
        if (mysEq.startsWith("Mus")) mys = mus else if (mysEq.startsWith("Mox")) mys = mox
        val moxEq = v.get(StringModifier.EQUALIZE_MOXIE).orEmpty()
        if (moxEq.startsWith("Mus")) mox = mus else if (moxEq.startsWith("Mys")) mox = mys

        // Step 3: Stat limits (applied to base BEFORE percentage calculation)
        val musLimit = v.getInt(DoubleModifier.MUS_LIMIT)
        if (musLimit > 0) mus = min(mus, musLimit)
        val mysLimit = v.getInt(DoubleModifier.MYS_LIMIT)
        if (mysLimit > 0) mys = min(mys, mysLimit)
        val moxLimit = v.getInt(DoubleModifier.MOX_LIMIT)
        if (moxLimit > 0) mox = min(mox, moxLimit)

        // Step 4: Buffed stats = base + flat + ceil(pct * base / 100)
        var buffedMus = mus + v.getInt(DoubleModifier.MUS) +
            ceil(v.get(DoubleModifier.MUS_PCT) * mus / 100.0).toInt()
        var buffedMys = mys + v.getInt(DoubleModifier.MYS) +
            ceil(v.get(DoubleModifier.MYS_PCT) * mys / 100.0).toInt()
        var buffedMox = mox + v.getInt(DoubleModifier.MOX) +
            ceil(v.get(DoubleModifier.MOX_PCT) * mox / 100.0).toInt()

        // Step 5: Buffed-stat floor constraints (each can be floored to another buffed stat)
        val musFloor = v.get(StringModifier.FLOOR_BUFFED_MUSCLE).orEmpty()
        if (musFloor.startsWith("Mys") && buffedMys > buffedMus) buffedMus = buffedMys
        else if (musFloor.startsWith("Mox") && buffedMox > buffedMus) buffedMus = buffedMox

        val mysFloor = v.get(StringModifier.FLOOR_BUFFED_MYST).orEmpty()
        if (mysFloor.startsWith("Mus") && buffedMus > buffedMys) buffedMys = buffedMus
        else if (mysFloor.startsWith("Mox") && buffedMox > buffedMys) buffedMys = buffedMox

        val moxFloor = v.get(StringModifier.FLOOR_BUFFED_MOXIE).orEmpty()
        if (moxFloor.startsWith("Mus") && buffedMus > buffedMox) buffedMox = buffedMus
        else if (moxFloor.startsWith("Mys") && buffedMys > buffedMox) buffedMox = buffedMys

        // Step 6: HP — class/path-specific base and multiplier
        val buffedHp = computeHp(v, buffedMus, mus)

        // Step 7: MP — Moxie-controls-MP paths/effects handled here
        val buffedMp = computeMp(v, buffedMys, buffedMox, mys)

        return mapOf(
            DerivedModifier.BUFFED_MUS to max(0, buffedMus),
            DerivedModifier.BUFFED_MYS to max(0, buffedMys),
            DerivedModifier.BUFFED_MOX to max(0, buffedMox),
            DerivedModifier.BUFFED_HP  to max(0, buffedHp),
            DerivedModifier.BUFFED_MP  to max(0, buffedMp)
        )
    }

    private fun computeHp(v: ModifierValues, buffedMus: Int, baseMus: Int): Int {
        val flat = v.getInt(DoubleModifier.HP)
        val pct  = v.get(DoubleModifier.HP_PCT)

        return when (state.ascensionPath) {
            AscensionPath.VAMPYRE -> {
                // Vampyre: HP base = base muscle (not buffed)
                max(state.baseMusc + flat, baseMus)
            }
            AscensionPath.Z_IS_FOR_ZOOTOMIST -> {
                // Zootomist: hpbase = buffedMus + 3
                max(buffedMus + 3 + flat, baseMus)
            }
            AscensionPath.YOU_ROBOT -> {
                // Robocore: fixed HP base of 30
                30 + flat
            }
            AscensionPath.GREY_YOU -> {
                // Grey Goo: base = current baseMaxHp minus existing flat HP modifier
                val hpbase = state.baseMaxHp - v.getInt(DoubleModifier.HP)
                hpbase + flat
            }
            else -> {
                // Standard: hpbase = buffedMus + 3; C = 1.5 for muscle class
                val hpbase = buffedMus + 3
                val C = if (state.characterClassEnum.isMuscleBased) 1.5 else 1.0
                val hp = ceil(hpbase * (C + pct / 100.0)).toInt() + flat
                max(hp, baseMus)
            }
        }
    }

    private fun computeMp(
        v: ModifierValues, buffedMys: Int, buffedMox: Int, baseMys: Int
    ): Int {
        val flat = v.getInt(DoubleModifier.MP)
        val pct  = v.get(DoubleModifier.MP_PCT)

        if (state.ascensionPath == AscensionPath.GREY_YOU) {
            val mpbase = state.baseMaxMp - v.getInt(DoubleModifier.MP)
            return mpbase + flat
        }

        // mpbase = buffedMys unless Moxie controls MP
        val mpbase = if (v.get(BooleanModifier.MOXIE_CONTROLS_MP) ||
            (v.get(BooleanModifier.MOXIE_MAY_CONTROL_MP) && buffedMox > buffedMys)) {
            buffedMox
        } else {
            buffedMys
        }

        val C = if (state.characterClassEnum.isMysticality) 1.5 else 1.0
        val mp = ceil(mpbase * (C + pct / 100.0)).toInt() + flat
        return max(mp, baseMys)
    }

    // ── Accumulation ──────────────────────────────────────────────────────────

    private fun accumulate(): ModifierValues {
        var total = ModifierValues.EMPTY

        fun ctxWithAccumulated() = context.copy(currentModifiers = total)

        // 1. Equipped items
        for ((_, itemName) in state.equippedItems()) {
            val raw = ModifierDatabase.getItem(itemName)?.modifiers ?: continue
            total = total + ModifierParser.parse(raw, ctxWithAccumulated())
        }

        // 2. Active effects
        for (effect in activeEffects) {
            val raw = ModifierDatabase.getEffect(effect.name)?.modifiers ?: continue
            total = total + ModifierParser.parse(raw, ctxWithAccumulated())
        }

        // 3. Passive skills
        for (skillName in passiveSkillNames) {
            val raw = ModifierDatabase.getSkill(skillName)?.modifiers ?: continue
            total = total + ModifierParser.parse(raw, ctxWithAccumulated())
        }

        // 4. Zodiac sign
        if (state.zodiacSign.isNotBlank()) {
            val raw = ModifierDatabase.getSign(state.zodiacSign)?.modifiers
            if (raw != null) total = total + ModifierParser.parse(raw, ctxWithAccumulated())
        }

        // 5. Challenge path
        if (state.challengePath.isNotBlank() && state.challengePath != "None") {
            val raw = ModifierDatabase.getPath(state.challengePath)?.modifiers
            if (raw != null) total = total + ModifierParser.parse(raw, ctxWithAccumulated())
        }

        // 6. Current familiar
        if (state.familiarName.isNotBlank()) {
            val raw = ModifierDatabase.getFamiliar(state.familiarName)?.modifiers
            if (raw != null) total = total + ModifierParser.parse(raw, ctxWithAccumulated())
        }

        // 7. Complete outfits (slot-aware multiset)
        for (outfit in OutfitDatabase.allOutfits()) {
            if (outfit.equipment.isEmpty()) continue
            if (OutfitManager.isWearingPieces(outfit.equipment, state.equipment)) {
                val raw = ModifierDatabase.getOutfit(outfit.name)?.modifiers
                if (raw != null) total = total + ModifierParser.parse(raw, ctxWithAccumulated())
            }
        }

        // 8. Item synergies (all listed pieces equipped)
        val equippedLower = state.equippedItems().map { it.second.lowercase() }.toSet()
        for (synergy in ModifierDatabase.synergies()) {
            val parts = synergy.name.split('/').map { it.trim().lowercase() }
            if (parts.isNotEmpty() && parts.all { part -> part in equippedLower }) {
                total = total + ModifierParser.parse(synergy.modifiers, ctxWithAccumulated())
            }
        }

        return total
    }

    companion object {
        /** Combat rate diminishing returns: uncapped at ±25; beyond that, 1% per 5%. */
        fun cappedCombatRate(rate: Double): Double = when {
            rate > 75.0  -> 35.0
            rate > 25.0  -> 25.0 + floor((rate - 25.0) / 5.0)
            rate < -75.0 -> -35.0
            rate < -25.0 -> -25.0 + ceil((rate + 25.0) / 5.0)
            else         -> rate
        }
    }
}
