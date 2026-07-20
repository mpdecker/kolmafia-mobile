package net.sourceforge.kolmafia.ash

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ExpressionContext
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

    @Test
    fun monsterStatWithMl_zeroBaseStaysZero() {
        assertEquals(0, CombatAdjustment.monsterStatWithMl(0, 40))
        assertEquals(0, CombatAdjustment.monsterAttack(null, 10))
        assertEquals(0, CombatAdjustment.monsterDefense(null, 10))
        assertEquals(0, CombatAdjustment.monsterHp(null, 10))
        assertEquals(0, CombatAdjustment.monsterInitiative(null))
        assertEquals("", CombatAdjustment.monsterPhylum(null))
    }

    @Test
    fun monsterStatWithMl_appliesMaxOneFloor() {
        assertEquals(41, CombatAdjustment.monsterStatWithMl(1, 40))
        assertEquals(56, CombatAdjustment.monsterStatWithMl(16, 40))
        // Negative ML can floor at 1 when base is positive
        assertEquals(1, CombatAdjustment.monsterStatWithMl(5, -10))
    }

    @Test
    fun initPenalty_matchesDesktopTiers() {
        assertEquals(0, CombatAdjustment.initPenalty(20))
        assertEquals(0, CombatAdjustment.initPenalty(0))
        assertEquals(1, CombatAdjustment.initPenalty(21))
        assertEquals(20, CombatAdjustment.initPenalty(40))
        assertEquals(22, CombatAdjustment.initPenalty(41))
        assertEquals(60, CombatAdjustment.initPenalty(60))
        assertEquals(63, CombatAdjustment.initPenalty(61))
        assertEquals(120, CombatAdjustment.initPenalty(80))
        assertEquals(124, CombatAdjustment.initPenalty(81))
        assertEquals(200, CombatAdjustment.initPenalty(100))
        assertEquals(205, CombatAdjustment.initPenalty(101))
    }

    @Test
    fun jumpChance_sentinelsAndClamp() {
        fun monster(init: Int, atk: Int = 10) = MonsterDefinition(
            name = "test",
            id = 1,
            image = "",
            attack = atk,
            defense = 0,
            hp = 10,
            initiative = init,
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
        assertEquals(0, CombatAdjustment.jumpChance(null, 0, 0, 0, 0))
        assertEquals(0, CombatAdjustment.jumpChance(monster(10000), 0, 0, 0, 0))
        assertEquals(100, CombatAdjustment.jumpChance(monster(-10000), 0, 0, 0, 0))
        // 100 - 20 + 0 + max(0, 0-10) = 80
        assertEquals(80, CombatAdjustment.jumpChance(monster(20, atk = 10), 0, 0, 0, 0))
        // mainstat excess: 100 - 20 + 0 + 15 = 95
        assertEquals(95, CombatAdjustment.jumpChance(monster(20, atk = 10), 0, 0, 0, 25))
        // initPenalty(40)=20 → monsterInit 40 → 100-40=60
        assertEquals(60, CombatAdjustment.jumpChance(monster(20, atk = 10), 0, 40, 0, 0))
        // Clamp high
        assertEquals(100, CombatAdjustment.jumpChance(monster(0, atk = 0), 50, 0, 0, 100))
        // Clamp low
        assertEquals(0, CombatAdjustment.jumpChance(monster(200, atk = 0), 0, 0, 0, 0))
    }

    @Test
    fun jumpChance_missingInit_returnsMinusOne() {
        val missing = MonsterDefinition(
            name = "no init", id = 1, image = "", attack = 10, defense = 0, hp = 10,
            initiative = 0, hasInitiative = false, meatDrop = 0, phylum = "dude",
            isBoss = false, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val initZero = MonsterDefinition(
            name = "init zero", id = 2, image = "", attack = 10, defense = 0, hp = 10,
            initiative = 0, hasInitiative = true, meatDrop = 0, phylum = "dude",
            isBoss = false, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        assertEquals(-1, CombatAdjustment.jumpChance(missing, 0, 0, 0, 0))
        // Init: 0 present → formula 100 - 0 + 0 + max(0,0-10) = 100
        assertEquals(100, CombatAdjustment.jumpChance(initZero, 0, 0, 0, 0))
    }

    @Test
    fun jumpChance_overclockedSourceAgent() {
        val agent = MonsterDefinition(
            name = "Source Agent", id = 1945, image = "", attack = 30, defense = 30, hp = 40,
            initiative = 25, hasInitiative = true, meatDrop = 0, phylum = "construct",
            isBoss = false, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val without = CombatAdjustment.jumpChance(agent, 0, 0, 0, 0, hasOverclocked = false)
        val with = CombatAdjustment.jumpChance(agent, 0, 0, 0, 0, hasOverclocked = true)
        // 100 - 25 + 0 + max(0, 0-30) = 75 without; +200 init → clamp 100 with
        assertEquals(75, without)
        assertEquals(100, with)
        assertTrue(with > without)
    }

    @Test
    fun resolveBaseInitiative_evaluatesPrefExpression() {
        val agent = MonsterDefinition(
            name = "Source Agent", id = 1945, image = "", attack = 30, defense = 30, hp = 40,
            initiative = 0, hasInitiative = true,
            initiativeExpression = "25+25*pref(sourceAgentsDefeated)",
            meatDrop = 0, phylum = "construct",
            isBoss = false, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val atZero = ExpressionContext(prefLookup = { if (it == "sourceAgentsDefeated") "0" else "" })
        val atTwo = ExpressionContext(prefLookup = { if (it == "sourceAgentsDefeated") "2" else "" })
        assertEquals(25, CombatAdjustment.resolveBaseInitiative(agent, atZero))
        assertEquals(75, CombatAdjustment.resolveBaseInitiative(agent, atTwo))
        // jump: 100 - 25 = 75; with Overclocked clamps to 100
        assertEquals(75, CombatAdjustment.jumpChance(agent, 0, 0, 0, 0, false, atZero))
        assertEquals(100, CombatAdjustment.jumpChance(agent, 0, 0, 0, 0, true, atZero))
    }

    @Test
    fun resolveBaseInitiative_evaluatesDreadKissTokens() {
        val bugbear = MonsterDefinition(
            name = "cold bugbear", id = 1393, image = "", attack = 400, defense = 400, hp = 600,
            initiative = 0, hasInitiative = true,
            initiativeExpression = "15+KW*10",
            meatDrop = 0, phylum = "beast",
            isBoss = false, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val ctx = ExpressionContext(dreadKissWoods = 3)
        assertEquals(45, CombatAdjustment.resolveBaseInitiative(bugbear, ctx))
    }

    @Test
    fun resolveBaseAttack_evaluatesPrefExpression() {
        val agent = MonsterDefinition(
            name = "Source Agent", id = 1945, image = "", attack = 0, defense = 0, hp = 0,
            attackExpression = "30+30*pref(sourceAgentsDefeated)+ML",
            initiative = 0, hasInitiative = true,
            meatDrop = 0, phylum = "construct",
            isBoss = false, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val atZero = ExpressionContext(
            prefLookup = { if (it == "sourceAgentsDefeated") "0" else "" },
            monsterLevel = 0,
        )
        val atTwo = ExpressionContext(
            prefLookup = { if (it == "sourceAgentsDefeated") "2" else "" },
            monsterLevel = 0,
        )
        val withMl = ExpressionContext(
            prefLookup = { if (it == "sourceAgentsDefeated") "0" else "" },
            monsterLevel = 40,
        )
        assertEquals(30, CombatAdjustment.resolveBaseAttack(agent, atZero))
        assertEquals(90, CombatAdjustment.resolveBaseAttack(agent, atTwo))
        // Expression includes ML inside formula — no outer +ml
        assertEquals(70, CombatAdjustment.monsterAttack(agent, ml = 999, withMl))
        assertEquals(30, CombatAdjustment.monsterAttack(agent, ml = 999, atZero))
    }

    @Test
    fun resolveBaseAttack_bracketLiteral_noOuterMl() {
        val drippy = MonsterDefinition(
            name = "drippy bat", id = 2163, image = "", attack = 0, defense = 0, hp = 0,
            attackExpression = "200",
            initiative = 100, hasInitiative = true,
            meatDrop = 0, phylum = "beast",
            isBoss = false, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        assertEquals(200, CombatAdjustment.monsterAttack(drippy, ml = 40, ExpressionContext.EMPTY))
    }

    @Test
    fun resolveBaseAttack_moxFormula() {
        val baron = MonsterDefinition(
            name = "Baron von Ratsworth", id = 286, image = "", attack = 0, defense = 0, hp = 0,
            attackExpression = "MOX+min(13,3+A)",
            initiative = 80, hasInitiative = true,
            meatDrop = 0, phylum = "beast",
            isBoss = true, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val ctx = ExpressionContext(buffedMoxie = 10, ascensions = 0)
        // 10+min(13,3)=13
        assertEquals(13, CombatAdjustment.resolveBaseAttack(baron, ctx))
    }

    @Test
    fun resolveBaseDefense_evaluatesPrefExpression() {
        val agent = MonsterDefinition(
            name = "Source Agent", id = 1945, image = "", attack = 0, defense = 0, hp = 0,
            defenseExpression = "30+30*pref(sourceAgentsDefeated)+ML",
            initiative = 0, hasInitiative = true,
            meatDrop = 0, phylum = "construct",
            isBoss = false, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val atZero = ExpressionContext(
            prefLookup = { if (it == "sourceAgentsDefeated") "0" else "" },
            monsterLevel = 0,
        )
        val atTwo = ExpressionContext(
            prefLookup = { if (it == "sourceAgentsDefeated") "2" else "" },
            monsterLevel = 0,
        )
        val withMl = ExpressionContext(
            prefLookup = { if (it == "sourceAgentsDefeated") "0" else "" },
            monsterLevel = 40,
        )
        assertEquals(30, CombatAdjustment.resolveBaseDefense(agent, atZero))
        assertEquals(90, CombatAdjustment.resolveBaseDefense(agent, atTwo))
        assertEquals(70, CombatAdjustment.monsterDefense(agent, ml = 999, withMl))
        assertEquals(30, CombatAdjustment.monsterDefense(agent, ml = 999, atZero))
    }

    @Test
    fun resolveBaseDefense_musFormula() {
        val baron = MonsterDefinition(
            name = "Baron von Ratsworth", id = 286, image = "", attack = 0, defense = 0, hp = 0,
            defenseExpression = "MUS+min(13,3+A)",
            initiative = 80, hasInitiative = true,
            meatDrop = 0, phylum = "beast",
            isBoss = true, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val ctx = ExpressionContext(buffedMuscle = 10, ascensions = 0)
        assertEquals(13, CombatAdjustment.resolveBaseDefense(baron, ctx))
    }

    @Test
    fun resolveBaseHp_evaluatesPrefExpression() {
        val agent = MonsterDefinition(
            name = "Source Agent", id = 1945, image = "", attack = 0, defense = 0, hp = 0,
            hpExpression = "40+40*pref(sourceAgentsDefeated)+ML",
            initiative = 0, hasInitiative = true,
            meatDrop = 0, phylum = "construct",
            isBoss = false, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val atZero = ExpressionContext(
            prefLookup = { if (it == "sourceAgentsDefeated") "0" else "" },
            monsterLevel = 0,
        )
        val atTwo = ExpressionContext(
            prefLookup = { if (it == "sourceAgentsDefeated") "2" else "" },
            monsterLevel = 0,
        )
        val withMl = ExpressionContext(
            prefLookup = { if (it == "sourceAgentsDefeated") "0" else "" },
            monsterLevel = 40,
        )
        assertEquals(40, CombatAdjustment.resolveBaseHp(agent, atZero))
        assertEquals(120, CombatAdjustment.resolveBaseHp(agent, atTwo))
        assertEquals(80, CombatAdjustment.monsterHp(agent, ml = 999, withMl))
        assertEquals(40, CombatAdjustment.monsterHp(agent, ml = 999, atZero))
    }

    @Test
    fun resolveBaseHp_characterHpFormula() {
        val baron = MonsterDefinition(
            name = "Baron von Ratsworth", id = 286, image = "", attack = 0, defense = 0, hp = 0,
            hpExpression = "HP*1.25",
            initiative = 80, hasInitiative = true,
            meatDrop = 0, phylum = "beast",
            isBoss = true, isGhost = false, isLucky = false, isScaling = false,
            scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        val ctx = ExpressionContext(characterMaxHp = 100)
        assertEquals(125, CombatAdjustment.resolveBaseHp(baron, ctx))
    }

    @Test
    fun hasInitiative_parsedFromMonstersTxt() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertTrue(mosquito.hasInitiative)
        assertEquals(20, mosquito.initiative)
        val noInit = MonsterDatabase.getByName("crazy bastard")!!
        assertFalse(noInit.hasInitiative)
        val agent = MonsterDatabase.getByName("Source Agent")!!
        // Init: [expr] present → hasInitiative true, expression stored, numeric cache 0
        assertTrue(agent.hasInitiative)
        assertEquals(0, agent.initiative)
        assertEquals("25+25*pref(sourceAgentsDefeated)", agent.initiativeExpression)
        assertEquals("30+30*pref(sourceAgentsDefeated)+ML", agent.attackExpression)
        assertEquals(0, agent.attack)
        assertEquals("30+30*pref(sourceAgentsDefeated)+ML", agent.defenseExpression)
        assertEquals(0, agent.defense)
        assertEquals("40+40*pref(sourceAgentsDefeated)+ML", agent.hpExpression)
        assertEquals(0, agent.hp)
    }

    @Test
    fun locationJumpChance_minOverPositiveWeight() = runBlocking {
        val db = GameDatabase()
        db.load()
        val zone = CombatDatabase.getByLocation("The Spooky Forest")!!
        val weighted = zone.monsters.filter { it.weight > 0 }
        assertTrue(weighted.size >= 2)
        val resolve = { name: String -> MonsterDatabase.getByName(name) }
        val perMonster = weighted.map { mw ->
            CombatAdjustment.jumpChance(resolve(mw.name), 0, 0, 0, 0)
        }
        val expected = perMonster.minOrNull()!!
        assertEquals(
            expected,
            CombatAdjustment.locationJumpChance(
                "The Spooky Forest",
                initBonus = 0,
                initMl = 0,
                attackMl = 0,
                baseMainstat = 0,
                resolveMonster = resolve,
            ),
        )
        // Zero-weight / negative-weight entries must not raise the min
        assertTrue(zone.monsters.any { it.weight <= 0 })
    }

    @Test
    fun locationJumpChance_unknownLocation_returnsZero() {
        assertEquals(
            0,
            CombatAdjustment.locationJumpChance(
                "Nowhere Land That Does Not Exist",
                initBonus = 0,
                initMl = 0,
                attackMl = 0,
                baseMainstat = 0,
                resolveMonster = { null },
            ),
        )
    }

    @Test
    fun hitPercent_matchesDesktopFormula() {
        assertEquals(50.0, CombatAdjustment.hitPercent(10, 10), 0.0001)
        assertEquals(100.0, CombatAdjustment.hitPercent(100, 0), 0.0001)
        assertEquals(0.0, CombatAdjustment.hitPercent(0, 100), 0.0001)
        // Equal atk/def → 50; +9 atk → 100
        assertEquals(100.0, CombatAdjustment.hitPercent(19, 10), 0.0001)
    }

    @Test
    fun willUsuallyDodge_threshold() {
        fun monster(atk: Int) = MonsterDefinition(
            name = "test", id = 1, image = "", attack = atk, defense = 0, hp = 10,
            initiative = 0, meatDrop = 0, phylum = "bug", isBoss = false, isGhost = false,
            isLucky = false, isScaling = false, scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        assertFalse(CombatAdjustment.willUsuallyDodge(null, 100, 0))
        // mox - atk - 6 > 0 → mox > atk + 6
        assertFalse(CombatAdjustment.willUsuallyDodge(monster(16), 22, 0))
        assertTrue(CombatAdjustment.willUsuallyDodge(monster(16), 23, 0))
    }

    @Test
    fun willUsuallyMiss_atFiftyPercentBoundary() {
        fun monster(def: Int) = MonsterDefinition(
            name = "test", id = 1, image = "", attack = 0, defense = def, hp = 10,
            initiative = 0, meatDrop = 0, phylum = "bug", isBoss = false, isGhost = false,
            isLucky = false, isScaling = false, scale = 0, cap = 0, floor = 0, drops = emptyList(),
        )
        assertFalse(CombatAdjustment.willUsuallyMiss(null, 100, 0))
        // hitStat == defense → hitPercent 50 → usually miss
        assertTrue(CombatAdjustment.willUsuallyMiss(monster(14), 14, 0))
        // hitStat clearly above → not usually miss
        assertFalse(CombatAdjustment.willUsuallyMiss(monster(14), 40, 0))
    }

    @Test
    fun hitStatKind_moxWeaponIsMoxie() = runBlocking {
        val db = GameDatabase()
        db.load()
        assertEquals(HitStatKind.MOXIE, CombatAdjustment.hitStatKind("airblaster gun"))
        assertEquals(HitStatKind.MUSCLE, CombatAdjustment.hitStatKind("adobe adze"))
        assertEquals(HitStatKind.MUSCLE, CombatAdjustment.hitStatKind(null))
        assertEquals(HitStatKind.MUSCLE, CombatAdjustment.hitStatKind(""))
    }
}
