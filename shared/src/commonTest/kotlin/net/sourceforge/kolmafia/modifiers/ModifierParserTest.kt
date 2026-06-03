package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModifierParserTest {

    // ── Basic numeric parsing ─────────────────────────────────────────────────

    @Test
    fun `parses simple flat muscle bonus`() {
        val v = ModifierParser.parse("Muscle: +5")
        assertEquals(5.0, v.get(DoubleModifier.MUS))
    }

    @Test
    fun `parses negative value`() {
        val v = ModifierParser.parse("Mana Cost: -3")
        assertEquals(-3.0, v.get(DoubleModifier.MANA_COST))
    }

    @Test
    fun `parses multiple numeric modifiers`() {
        val v = ModifierParser.parse("Muscle Percent: +15, Initiative: +30, Spooky Damage: +5")
        assertEquals(15.0, v.get(DoubleModifier.MUS_PCT))
        assertEquals(30.0, v.get(DoubleModifier.INITIATIVE))
        assertEquals(5.0,  v.get(DoubleModifier.SPOOKY_DAMAGE))
    }

    @Test
    fun `parses parenthetical tags correctly`() {
        // tag = "Experience (Muscle)", not "Muscle Experience"
        val v = ModifierParser.parse("Experience (Muscle): +2")
        assertEquals(2.0, v.get(DoubleModifier.MUS_EXPERIENCE))
        assertEquals(0.0, v.get(DoubleModifier.MYS_EXPERIENCE))
    }

    @Test
    fun `parses mana cost stackable tag`() {
        val v = ModifierParser.parse("Mana Cost (stackable): -1")
        assertEquals(-1.0, v.get(DoubleModifier.STACKABLE_MANA_COST))
    }

    @Test
    fun `parses familiar weight`() {
        val v = ModifierParser.parse("Familiar Weight: +5")
        assertEquals(5.0, v.get(DoubleModifier.FAMILIAR_WEIGHT))
    }

    // ── Expression handling ───────────────────────────────────────────────────

    @Test
    fun `evaluates bracket expressions with empty context`() {
        // effect(X) with no active effects → 0 turns → 3 + 3*min(1,0) = 3.0
        val v = ModifierParser.parse("Experience (Mysticality): [3+3*min(1,effect(X))]")
        assertEquals(3.0, v.get(DoubleModifier.MYS_EXPERIENCE))
    }

    @Test
    fun `evaluates bracket expressions with live context`() {
        // With effect active (10 turns), min(1,10)=1 → 3+3*1 = 6.0
        val ctx = ExpressionContext(activeEffects = mapOf("x" to 10))
        val v = ModifierParser.parse("Experience (Mysticality): [3+3*min(1,effect(X))]", ctx)
        assertEquals(6.0, v.get(DoubleModifier.MYS_EXPERIENCE))
    }

    @Test
    fun `parses mixed expression and literal`() {
        // effect(X) with empty context → 0 → 20 + 20*min(1,0) = 20.0
        val v = ModifierParser.parse("Meat Drop: [20+20*min(1,effect(X))], Muscle: +3")
        assertEquals(20.0, v.get(DoubleModifier.MEATDROP))
        assertEquals(3.0,  v.get(DoubleModifier.MUS))
    }

    // ── Boolean modifiers ─────────────────────────────────────────────────────

    @Test
    fun `parses bare boolean flag`() {
        val v = ModifierParser.parse("Lasts Until Rollover")
        assertTrue(v.get(BooleanModifier.LASTS_ONE_DAY))
    }

    @Test
    fun `parses boolean among other modifiers`() {
        val v = ModifierParser.parse("Moxie: +5, Lasts Until Rollover, Initiative: +10")
        assertEquals(5.0,  v.get(DoubleModifier.MOX))
        assertTrue(v.get(BooleanModifier.LASTS_ONE_DAY))
        assertEquals(10.0, v.get(DoubleModifier.INITIATIVE))
    }

    @Test
    fun `unrecognised boolean does not throw`() {
        val v = ModifierParser.parse("Some Unknown Flag")
        assertTrue(v.booleans.isEmpty())
        assertTrue(v.doubles.isEmpty())
    }

    // ── String modifiers ──────────────────────────────────────────────────────

    @Test
    fun `parses string modifier with quoted value`() {
        val v = ModifierParser.parse("Last Available: \"2024-12\"")
        assertEquals("2024-12", v.get(StringModifier.LAST_AVAILABLE_DATE))
    }

    @Test
    fun `parses familiar effect string`() {
        val v = ModifierParser.parse("Familiar Effect: \"atk, 1xLep, cap 12\"")
        // Comma inside quotes must NOT split the token
        assertEquals("atk, 1xLep, cap 12", v.get(StringModifier.FAMILIAR_EFFECT))
    }

    @Test
    fun `multiple string values accumulate into list`() {
        val v = ModifierParser.parse("Effect: \"Buff A\", Effect: \"Buff B\"")
        val effects = v.getAll(StringModifier.EFFECT)
        assertEquals(2, effects.size)
        assertTrue("Buff A" in effects)
        assertTrue("Buff B" in effects)
    }

    // ── Accumulation (+ operator) ─────────────────────────────────────────────

    @Test
    fun `plus operator sums double modifiers`() {
        val a = ModifierParser.parse("Muscle: +5, Initiative: +10")
        val b = ModifierParser.parse("Muscle: +3, Moxie: +7")
        val c = a + b
        assertEquals(8.0,  c.get(DoubleModifier.MUS))
        assertEquals(10.0, c.get(DoubleModifier.INITIATIVE))
        assertEquals(7.0,  c.get(DoubleModifier.MOX))
    }

    @Test
    fun `plus operator ORs boolean modifiers`() {
        val a = ModifierParser.parse("Never Fumble")
        val b = ModifierParser.parse("Lasts Until Rollover")
        val c = a + b
        assertTrue(c.get(BooleanModifier.NEVER_FUMBLE))
        assertTrue(c.get(BooleanModifier.LASTS_ONE_DAY))
    }

    @Test
    fun `plus with EMPTY is identity`() {
        val a = ModifierParser.parse("Muscle: +5")
        assertEquals(a, a + ModifierValues.EMPTY)
        assertEquals(a, ModifierValues.EMPTY + a)
    }

    // ── Tag lookup ────────────────────────────────────────────────────────────

    @Test
    fun `byTag is case-insensitive`() {
        assertEquals(DoubleModifier.MUS,     DoubleModifier.byTag("muscle"))
        assertEquals(DoubleModifier.MUS,     DoubleModifier.byTag("Muscle"))
        assertEquals(DoubleModifier.MUS,     DoubleModifier.byTag("MUSCLE"))
        assertEquals(DoubleModifier.MOX_PCT, DoubleModifier.byTag("moxie percent"))
    }

    @Test
    fun `byTag returns null for unknown tag`() {
        assertNull(DoubleModifier.byTag("nonexistent modifier xyz"))
    }

    @Test
    fun `BooleanModifier byTag works`() {
        assertEquals(BooleanModifier.FOUR_SONGS, BooleanModifier.byTag("Four Songs"))
        assertEquals(BooleanModifier.SOFTCORE,   BooleanModifier.byTag("softcore only"))
    }

    @Test
    fun `StringModifier byTag works`() {
        assertEquals(StringModifier.LAST_AVAILABLE_DATE, StringModifier.byTag("last available"))
        assertEquals(StringModifier.FAMILIAR_EFFECT,     StringModifier.byTag("Familiar Effect"))
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    fun `empty string returns EMPTY`() {
        assertEquals(ModifierValues.EMPTY, ModifierParser.parse(""))
        assertEquals(ModifierValues.EMPTY, ModifierParser.parse("none"))
        assertEquals(ModifierValues.EMPTY, ModifierParser.parse("   "))
    }

    @Test
    fun `real item entry from modifiers txt`() {
        // "8-billed baseball cap" from modifiers.txt
        val raw = "Muscle Percent: +15, Initiative: +30, Spooky Damage: +5, " +
                  "Stench Damage: +5, Hot Damage: +5, Cold Damage: +5, " +
                  "Sleaze Damage: +5, Last Available: \"2014-05\", " +
                  "Familiar Effect: \"1xVolley, 1xBarrr, cap 12\""
        val v = ModifierParser.parse(raw)
        assertEquals(15.0, v.get(DoubleModifier.MUS_PCT))
        assertEquals(30.0, v.get(DoubleModifier.INITIATIVE))
        assertEquals(5.0,  v.get(DoubleModifier.SPOOKY_DAMAGE))
        assertEquals(5.0,  v.get(DoubleModifier.COLD_DAMAGE))
        assertEquals("2014-05", v.get(StringModifier.LAST_AVAILABLE_DATE))
        assertEquals("1xVolley, 1xBarrr, cap 12", v.get(StringModifier.FAMILIAR_EFFECT))
    }
}
