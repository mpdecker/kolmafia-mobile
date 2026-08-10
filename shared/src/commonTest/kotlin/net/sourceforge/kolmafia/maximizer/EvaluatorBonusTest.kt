package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvaluatorBonusTest {

    @Test
    fun letterBonus_countsNameLength() {
        assertEquals(10.0, MaximizerLetterBonus.letterBonus("short name"))
        assertEquals(0.0, MaximizerLetterBonus.letterBonus(null))
    }

    @Test
    fun letterBonus_countsMatchingLetters() {
        assertEquals(5.0, MaximizerLetterBonus.letterBonus("beekeeper", "e"))
        assertEquals(0.0, MaximizerLetterBonus.letterBonus("beekeeper", "z"))
    }

    @Test
    fun numberBonus_countsDigits() {
        assertEquals(3.0, MaximizerLetterBonus.numberBonus("item 42 v2"))
    }

    @Test
    fun equipmentBonus_sumsLetterWeightAcrossLoadout() {
        val evaluator = Evaluator("letter")
        val bonus = evaluator.equipmentBonus(listOf("ab", "cde"))
        assertEquals(5.0, bonus)
    }

    @Test
    fun itemBonus_appliesFlatBonusWeight() {
        val evaluator = Evaluator("3 bonus magic wand")
        assertEquals(3.0, evaluator.itemBonus("magic wand"))
        assertEquals(0.0, evaluator.itemBonus("other wand"))
    }

    @Test
    fun parse_negEquipFromStatExpression() {
        val evaluator = Evaluator("mus, -equip banned hat")
        assertTrue(evaluator.isNegEquip("banned hat"))
    }

    @Test
    fun maximizeGoal_negEquipConstraint() {
        val spec = MaximizeGoal.parseSpec("mus, -equip banned shirt")!!
        assertTrue(spec.evaluator.isNegEquip("banned shirt"))
    }

    @Test
    fun resolvePlumberTools_failsOffPath() {
        val evaluator = Evaluator("plumber, mus")
        val ok = evaluator.resolvePlumberTools(
            CharacterState(challengePath = "None"),
            accessibleCount = { 1 },
            gameDatabase = stubDb(9001 to "heavy hammer"),
        )
        assertFalse(ok)
    }

    @Test
    fun resolvePlumberTools_addsPrimeStatToolOnPath() {
        val evaluator = Evaluator("plumber, mus")
        val db = stubDb(9001 to "heavy hammer")
        val ok = evaluator.resolvePlumberTools(
            CharacterState(
                challengePath = "Plumber",
                characterClass = 1,
            ),
            accessibleCount = { id -> if (id == 9001) 1 else 0 },
            gameDatabase = db,
        )
        assertTrue(ok)
        assertTrue(
            evaluator.posEquip.any { it.equals("heavy hammer", ignoreCase = true) },
            evaluator.posEquip.toString(),
        )
    }

    @Test
    fun resolvePlumberTools_coldPlumberRequiresFlower() {
        val evaluator = Evaluator("cold plumber, mus")
        val db = stubDb(9002 to "bonfire flower", 9003 to "frosty button")
        val ok = evaluator.resolvePlumberTools(
            CharacterState(
                challengePath = "Plumber",
                characterClass = 3,
            ),
            accessibleCount = { id -> if (id == 9002) 1 else 0 },
            gameDatabase = db,
        )
        assertTrue(ok)
        assertTrue(evaluator.posEquip.any { it.contains("flower", ignoreCase = true) })
        assertTrue(evaluator.posEquip.any { it.equals("frosty button", ignoreCase = true) })
    }

    @Test
    fun scoreLoadout_includesEquipmentBonus() {
        val evaluator = Evaluator("letter")
        val state = CharacterState(
            equipment = mapOf(EquipmentSlot.HAT to "abc"),
        )
        val score = MaximizerSpeculation.scoreLoadout(
            state,
            mapOf(EquipmentSlot.HAT to ("abc" to 0.0)),
            evaluator,
        )
        assertEquals(3.0, score)
    }

    private fun stubDb(vararg items: Pair<Int, String>): GameDatabase = object : GameDatabase() {
        private val byId = items.associate { (id, name) ->
            id to ItemData(id, name, "", "", ItemPrimaryUse.WEAPON, emptySet(), setOf('t'), 0, null)
        }

        override fun item(id: Int): ItemData? = byId[id]

        override fun item(name: String): ItemData? =
            byId.values.find { it.name.equals(name, ignoreCase = true) }
    }
}
