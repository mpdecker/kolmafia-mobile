package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.effect.EffectData as ActiveEffectData
import net.sourceforge.kolmafia.modifiers.BitmapModifier
import net.sourceforge.kolmafia.modifiers.ModifierValues
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaximizerMutexViolationsTest {
    @BeforeTest
    fun setUp() {
        ModifierDatabase.resetForTest()
        EffectDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        EffectDatabase.resetForTest()
    }

    @Test
    fun introducesNewViolations_detectsNewViolationBits() {
        val baseline = ModifierValues(
            bitmaps = mapOf(BitmapModifier.MUTEX_VIOLATIONS to 0b0010),
        )
        val candidate = ModifierValues(
            bitmaps = mapOf(BitmapModifier.MUTEX_VIOLATIONS to 0b0011),
        )
        assertTrue(MaximizerMutexViolations.introducesNewViolations(baseline, candidate))
    }

    @Test
    fun introducesNewViolations_allowsSubsetViolations() {
        val baseline = ModifierValues(
            bitmaps = mapOf(BitmapModifier.MUTEX_VIOLATIONS to 0b0011),
        )
        val candidate = ModifierValues(
            bitmaps = mapOf(BitmapModifier.MUTEX_VIOLATIONS to 0b0010),
        )
        assertFalse(MaximizerMutexViolations.introducesNewViolations(baseline, candidate))
    }

    @Test
    fun scoreLoadout_conflictingMutexAccessories_marksFailed() {
        setupMutexItems("mutex watch a", "mutex watch b")
        val evaluator = Evaluator("meat")
        val baseState = CharacterState(
            level = 15,
            equipment = mapOf(EquipmentSlot.ACC1 to "mutex watch a"),
        )
        val assignment = mapOf(
            EquipmentSlot.ACC1 to ("mutex watch a" to 1.0),
            EquipmentSlot.ACC2 to ("mutex watch b" to 1.0),
        )
        MaximizerSpeculation.scoreLoadout(
            baseState = baseState,
            assignment = assignment,
            evaluator = evaluator,
        )
        assertTrue(evaluator.failed)
    }

    @Test
    fun buildEffectBoosts_skipsGainWhenMutexWouldViolate() {
        setupMutexEffects("mutex boost a", "mutex boost b")
        val ctx = nonEquipmentContext(
            activeEffects = listOf(
                ActiveEffectData(id = 8800, name = "mutex boost a", duration = 5),
            ),
        )
        val boosts = MaximizerNonEquipmentBoosts.build(ctx)
        assertFalse(boosts.any { it.text.contains("mutex boost b", ignoreCase = true) })
    }

    @Test
    fun priorityBoost_sortsBeforeEqualDeltaNonPriority() {
        val low = MaximizerBoost(cmd = "a", text = "a", delta = 5.0, isEquipment = false)
        val high = MaximizerBoost(
            cmd = "b",
            text = "b",
            delta = 5.0,
            isEquipment = false,
            priority = true,
        )
        val sorted = listOf(low, high).sorted()
        assertTrue(sorted.first().priority)
    }

    private fun setupMutexItems(vararg names: String) {
        names.forEach { name ->
            ModifierDatabase.injectForTest("Item", name, "Meat Drop: +1")
        }
        ModifierDatabase.injectForTest("MutexI", names.joinToString("/"), "none")
        ModifierDatabase.rebuildMutexBitsForTest()
    }

    private fun setupMutexEffects(vararg names: String) {
        names.forEachIndexed { index, name ->
            ModifierDatabase.injectForTest("Effect", name, "Meat Drop: +5")
            EffectDatabase.registerForTest(
                EffectData(
                    id = 8800 + index,
                    name = name,
                    image = "mutex.gif",
                    descId = "mutex$index",
                    quality = EffectQuality.NEUTRAL,
                    attributes = emptySet(),
                    actions = "cast 1 $name",
                ),
            )
        }
        ModifierDatabase.injectForTest("MutexE", names.joinToString("/"), "none")
        ModifierDatabase.rebuildMutexBitsForTest()
    }

    private fun nonEquipmentContext(
        activeEffects: List<ActiveEffectData> = emptyList(),
    ): MaximizerNonEquipmentBoosts.Context {
        val spec = MaximizeGoal.parseSpec("meat")!!
        val plan = MaximizerEmitSlot.Plan(
            goal = "meat",
            spec = spec,
            scoreBefore = 0.0,
            scoreAfter = 10.0,
            bestPerSlot = emptyMap(),
        )
        return MaximizerNonEquipmentBoosts.Context(
            plan = plan,
            charState = CharacterState(level = 15),
            activeEffects = activeEffects,
            inventory = MaximizerEmitSlot.InventorySnapshot(),
            inventoryCount = { 0 },
            gameDatabase = GameDatabase(),
            preferences = null,
            mallPriceManager = null,
            priceLevel = MaximizerPriceLevel.DONT_CHECK,
        )
    }
}
