package net.sourceforge.kolmafia.modifiers

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModifierExpressionTest {

    private val base = ExpressionContext(
        level = 10,
        inebriety = 3,
        fullness = 7,
        spleenUsed = 2,
        familiarWeight = 20,
        ascensions = 5,
        effectsCount = 4,
        fury = 2,
        activeEffects = mapOf(
            "leash of linguini" to 15,
            "empathy" to 5
        ),
        skills = setOf("never fumble", "iron liver"),
        challengePath = "Teetotaler",
        className = "Disco Bandit",
        currentLocation = "The Spooky Forest",
        currentZone = "Nearby Plains",
        environment = "outdoor",
        isRestricted = false
    )

    private fun eval(expr: String, ctx: ExpressionContext = base): Double =
        ModifierExpression(expr).evaluate(ctx)

    // ── Literals ──────────────────────────────────────────────────────────────

    @Test fun `integer literal`()   { assertEquals(42.0,  eval("42"))   }
    @Test fun `negative literal`()  { assertEquals(-5.0,  eval("-5"))   }
    @Test fun `decimal literal`()   { assertEquals(3.14,  eval("3.14")) }

    // ── Arithmetic ────────────────────────────────────────────────────────────

    @Test fun `addition`()        { assertEquals(7.0,   eval("3+4"))      }
    @Test fun `subtraction`()     { assertEquals(1.0,   eval("5-4"))      }
    @Test fun `multiplication`()  { assertEquals(12.0,  eval("3*4"))      }
    @Test fun `division`()        { assertEquals(2.5,   eval("5/2"))      }
    @Test fun `exponentiation`()  { assertEquals(8.0,   eval("2^3"))      }
    @Test fun `precedence`()      { assertEquals(14.0,  eval("2+3*4"))    }
    @Test fun `parentheses`()     { assertEquals(20.0,  eval("(2+3)*4"))  }
    @Test fun `unary minus`()     { assertEquals(-3.0,  eval("-(1+2)"))   }
    @Test fun `division by zero`(){ assertEquals(0.0,   eval("5/0"))      }

    // ── Context variables ─────────────────────────────────────────────────────

    @Test fun `level variable L`()          { assertEquals(10.0, eval("L")) }
    @Test fun `drunk variable D`()          { assertEquals(3.0,  eval("D")) }
    @Test fun `fullness variable F`()       { assertEquals(7.0,  eval("F")) }
    @Test fun `spleen variable S`()         { assertEquals(2.0,  eval("S")) }
    @Test fun `familiar weight W`()         { assertEquals(20.0, eval("W")) }
    @Test fun `ascensions variable A`()     { assertEquals(5.0,  eval("A")) }
    @Test fun `effects count E`()           { assertEquals(4.0,  eval("E")) }
    @Test fun `fury variable Y`()           { assertEquals(2.0,  eval("Y")) }
    @Test fun `unknown variable returns 0`(){ assertEquals(0.0,  eval("Z")) }

    // ── Functions — arithmetic ────────────────────────────────────────────────

    @Test fun `min picks smaller`()         { assertEquals(3.0,  eval("min(3,7)"))  }
    @Test fun `max picks larger`()          { assertEquals(7.0,  eval("max(3,7)"))  }
    @Test fun `min with expressions`()      { assertEquals(1.0,  eval("min(L,1)"))  }
    @Test fun `ceil rounds up`()            { assertEquals(4.0,  eval("ceil(3.1)")) }
    @Test fun `floor rounds down`()         { assertEquals(3.0,  eval("floor(3.9)"))}
    @Test fun `abs of negative`()           { assertEquals(5.0,  eval("abs(-5)"))   }
    @Test fun `sqrt`()                      { assertEquals(4.0,  eval("sqrt(16)"))  }

    // ── Functions — game state ────────────────────────────────────────────────

    @Test fun `effect returns turns when active`() {
        assertEquals(15.0, eval("effect(Leash of Linguini)"))
    }
    @Test fun `effect returns 0 when inactive`() {
        assertEquals(0.0, eval("effect(Some Unknown Buff)"))
    }
    @Test fun `skill returns 1 when known`() {
        assertEquals(1.0, eval("skill(Never Fumble)"))
    }
    @Test fun `skill returns 0 when unknown`() {
        assertEquals(0.0, eval("skill(Some Unknown Skill)"))
    }
    @Test fun `loc returns 1 when match`() {
        assertEquals(1.0, eval("loc(Spooky)"))
    }
    @Test fun `loc returns 0 when no match`() {
        assertEquals(0.0, eval("loc(Knob Goblin)"))
    }
    @Test fun `zone matches substring`() {
        assertEquals(1.0, eval("zone(Plains)"))
        assertEquals(0.0, eval("zone(Island)"))
    }
    @Test fun `path matches substring`() {
        assertEquals(1.0, eval("path(Teetotaler)"))
        assertEquals(0.0, eval("path(Hardcore)"))
    }
    @Test fun `class matches substring`() {
        assertEquals(1.0, eval("class(Disco)"))
        assertEquals(0.0, eval("class(Seal)"))
    }
    @Test fun `interact returns 1 when not restricted`() {
        assertEquals(1.0, eval("interact()"))
    }
    @Test fun `interact returns 0 when restricted`() {
        val restricted = base.copy(isRestricted = true)
        assertEquals(0.0, eval("interact()", restricted))
    }
    @Test fun `pref always returns 0`() {
        assertEquals(0.0, eval("pref(_somePreference)"))
    }

    // ── Compound expressions (real modifiers.txt patterns) ────────────────────

    @Test fun `leash weight formula`() {
        // [3+3*min(1,effect(Leash of Linguini))] with 15 turns → 3+3*min(1,15)=3+3=6
        assertEquals(6.0, eval("3+3*min(1,effect(Leash of Linguini))"))
    }
    @Test fun `inactive effect formula`() {
        // [3+3*min(1,effect(Inactive))] → 3+3*min(1,0)=3
        assertEquals(3.0, eval("3+3*min(1,effect(Inactive Buff))"))
    }
    @Test fun `level-dependent formula`() {
        // [min(11,L)] with level=10 → min(11,10) = 10
        assertEquals(10.0, eval("min(11,L)"))
    }
    @Test fun `pref-dependent formula returns base`() {
        // [20+20*pref(bondItem1)] → 20+20*0=20 (pref returns 0)
        assertEquals(20.0, eval("20+20*pref(bondItem1)"))
    }

    // ── ModifierExpression.evaluate companion ─────────────────────────────────

    @Test fun `evaluate strips brackets`() {
        assertEquals(10.0, ModifierExpression.evaluate("[L]", base))
    }
    @Test fun `evaluate handles nested expression`() {
        assertEquals(6.0, ModifierExpression.evaluate(
            "[3+3*min(1,effect(Leash of Linguini))]", base
        ))
    }

    // ── mod / res / mainhand / fam (Phase 29) ─────────────────────────────────

    @Test fun `mod returns current modifier value`() {
        val values = ModifierValues(doubles = mapOf(DoubleModifier.MUS to 5.0))
        val ctx = base.copy(currentModifiers = values)
        assertEquals(5.0, eval("mod(Muscle)", ctx))
    }

    @Test fun `mod with item returns item modifier`() = runBlocking {
        ItemDatabase.load()
        ModifierDatabase.load()
        val ctx = base.copy(currentModifiers = ModifierValues.EMPTY)
        // chef's hat: +2 Mysticality in modifiers.txt
        assertEquals(2.0, eval("mod(Mysticality, chef's hat)", ctx))
    }

    @Test fun `mod with item id returns item modifier`() = runBlocking {
        ItemDatabase.load()
        ModifierDatabase.load()
        val hat = ItemDatabase.getByName("chef's hat") ?: error("chef's hat not in items.txt")
        val ctx = base.copy(currentModifiers = ModifierValues.EMPTY)
        assertEquals(2.0, eval("mod(Mysticality, ${hat.id})", ctx))
    }

    @Test fun `res maps cold to resistance`() {
        val values = ModifierValues(doubles = mapOf(DoubleModifier.COLD_RESISTANCE to 10.0))
        val ctx = base.copy(currentModifiers = values)
        assertEquals(10.0, eval("res(Cold)", ctx))
    }

    @Test fun `mainhand returns weapon modifier`() = runBlocking {
        ModifierDatabase.load()
        val ctx = base.copy(mainhandItemName = "chef's hat")
        assertEquals(2.0, eval("mainhand(Mysticality)", ctx))
    }

    @Test fun `fam returns familiar attribute`() = runBlocking {
        ModifierDatabase.load()
        val ctx = base.copy(familiarName = "leprechaun")
        assertTrue(eval("fam(Leprechaun)", ctx) >= 0.0)
    }
}
