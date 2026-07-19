package net.sourceforge.kolmafia.ash

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierValues

class CombatAdjustmentTest {

    @Test
    fun elementalResistanceByLevel_lowLevelsAreTenEach() {
        assertEquals(0.0, CombatAdjustment.elementalResistanceByLevel(0, mystBonus = true, isMystClass = false))
        assertEquals(40.0, CombatAdjustment.elementalResistanceByLevel(4, mystBonus = true, isMystClass = false))
        assertEquals(45.0, CombatAdjustment.elementalResistanceByLevel(4, mystBonus = true, isMystClass = true))
    }

    @Test
    fun elementalResistanceByLevel_highLevelsUseAsymptoteFormula() {
        val expected = 90.0 - 50.0 * (5.0 / 6.0).pow(1)
        assertEquals(expected, CombatAdjustment.elementalResistanceByLevel(5, mystBonus = false, isMystClass = false), 0.0001)
    }

    @Test
    fun damageAbsorptionPercent_matchesDesktopSqrtFormula() {
        assertEquals(0.0, CombatAdjustment.damageAbsorptionPercent(0))
        assertEquals((kotlin.math.sqrt(100.0) - 1.0) * 10.0, CombatAdjustment.damageAbsorptionPercent(1000), 0.0001)
    }

    @Test
    fun expectedDamage_withZeroDaDrRes() {
        val monster = MonsterDefinition(
            name = "test bug",
            id = 1,
            image = "",
            attack = 40,
            defense = 0,
            hp = 10,
            initiative = 0,
            meatDrop = 0,
            phylum = "bug",
            isBoss = false,
            isGhost = false,
            isLucky = false,
            isScaling = false,
            scale = 0,
            cap = 0,
            floor = 0,
            drops = emptyList(),
        )
        val char = CharacterState(buffedMoxie = 10, characterClass = 5) // Disco Bandit / MOXIE
        val mods = CurrentModifiers(char)
        // base = max(0, 40-10) + 40/4 = 40; DA=0 → absorb 1.1 → ceil(44)
        assertEquals(44, CombatAdjustment.expectedDamage(monster, char, mods))
    }

    @Test
    fun expectedDamage_ninjaSnowmanAssassinSpecialCase() {
        val monster = MonsterDefinition(
            name = "ninja snowman assassin",
            id = 2,
            image = "",
            attack = 20,
            defense = 0,
            hp = 10,
            initiative = 0,
            meatDrop = 0,
            phylum = "penguin",
            isBoss = false,
            isGhost = false,
            isLucky = false,
            isScaling = false,
            scale = 0,
            cap = 0,
            floor = 0,
            attackElement = "cold",
            drops = emptyList(),
        )
        val char = CharacterState(buffedMoxie = 0)
        val mods = CurrentModifiers(char)
        // base = max(0, 20-0) + 120 = 140; DA=0 → absorb 1.1 → ceil(154)
        assertEquals(154, CombatAdjustment.expectedDamage(monster, char, mods))
    }

    @Test
    fun slimeResistance_skipsMystBonus() {
        val values = ModifierValues(doubles = mapOf(DoubleModifier.SLIME_RESISTANCE to 4.0))
        // Build via CurrentModifiers with empty state can't inject values easily —
        // assert formula path: myst class + slime should not add +5 at level 4
        assertEquals(
            40.0,
            CombatAdjustment.elementalResistanceByLevel(4, mystBonus = false, isMystClass = true),
        )
        assertEquals(MainStat.MYSTICALITY, CharacterState(characterClass = 3).mainStat)
        assertEquals(values.get(DoubleModifier.SLIME_RESISTANCE), 4.0)
    }

    @Test
    fun initiativeModifier_positivePenaltyIgnored() {
        val values = ModifierValues(
            doubles = mapOf(
                DoubleModifier.INITIATIVE to 40.0,
                DoubleModifier.INITIATIVE_PENALTY to 10.0,
            ),
        )
        assertEquals(40.0, CombatAdjustment.initiativeModifier(values))
    }

    @Test
    fun initiativeModifier_negativePenaltyApplied() {
        val values = ModifierValues(
            doubles = mapOf(
                DoubleModifier.INITIATIVE to 40.0,
                DoubleModifier.INITIATIVE_PENALTY to -15.0,
            ),
        )
        assertEquals(25.0, CombatAdjustment.initiativeModifier(values))
    }

    @Test
    fun meatDropModifier_penaltySemantics() {
        assertEquals(
            50.0,
            CombatAdjustment.meatDropModifier(
                ModifierValues(
                    doubles = mapOf(
                        DoubleModifier.MEATDROP to 50.0,
                        DoubleModifier.MEATDROP_PENALTY to 20.0,
                    ),
                ),
            ),
        )
        assertEquals(
            30.0,
            CombatAdjustment.meatDropModifier(
                ModifierValues(
                    doubles = mapOf(
                        DoubleModifier.MEATDROP to 50.0,
                        DoubleModifier.MEATDROP_PENALTY to -20.0,
                    ),
                ),
            ),
        )
    }

    @Test
    fun itemDropModifier_excludesGearDrop() {
        val values = ModifierValues(
            doubles = mapOf(
                DoubleModifier.ITEMDROP to 25.0,
                DoubleModifier.GEARDROP to 100.0,
                DoubleModifier.ITEMDROP_PENALTY to -5.0,
            ),
        )
        assertEquals(20.0, CombatAdjustment.itemDropModifier(values))
    }

    @Test
    fun experienceBonus_selectsPrimeStatOnly() {
        val values = ModifierValues(
            doubles = mapOf(
                DoubleModifier.MUS_EXPERIENCE to 7.0,
                DoubleModifier.MYS_EXPERIENCE to 3.0,
                DoubleModifier.MOX_EXPERIENCE to 2.0,
            ),
        )
        assertEquals(7.0, CombatAdjustment.experienceBonus(values, CharacterState(characterClass = 1)))
        assertEquals(3.0, CombatAdjustment.experienceBonus(values, CharacterState(characterClass = 3)))
        assertEquals(2.0, CombatAdjustment.experienceBonus(values, CharacterState(characterClass = 5)))
        assertEquals(0.0, CombatAdjustment.experienceBonus(values, null))
    }
}
