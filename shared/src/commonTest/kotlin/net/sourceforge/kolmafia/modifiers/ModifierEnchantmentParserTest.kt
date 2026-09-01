package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModifierEnchantmentParserTest {

    @Test
    fun parseModifier_muscleLine() {
        assertEquals("Muscle: +15", ModifierEnchantmentParser.parseModifier("Muscle +15"))
    }

    @Test
    fun parseModifier_allAttributes() {
        assertEquals(
            "All Attributes: +5",
            ModifierEnchantmentParser.parseModifier("All Attributes +5"),
        )
    }

    @Test
    fun parseModifier_regeneration() {
        assertEquals(
            "HP Regen Min: 5, HP Regen Max: 10",
            ModifierEnchantmentParser.parseModifier("Regenerate 5-10 HP per adventure"),
        )
    }

    @Test
    fun parseModifier_slimeHatesIt() {
        assertEquals("Slime Hates It: +1", ModifierEnchantmentParser.parseModifier("Slime Hates It"))
        assertEquals("Slime Hates It: +2", ModifierEnchantmentParser.parseModifier("Slime Really Hates You"))
    }

    @Test
    fun parseStringModifier_rolloverEffect() {
        assertEquals(
            """Rollover Effect: "Buff"""",
            ModifierEnchantmentParser.parseStringModifier(
                "Adventures of <b><a href=foo>Buff</a></b> at Rollover",
            ),
        )
    }

    @Test
    fun parseEffect_usesNameWithoutDatabaseLookup() {
        assertEquals(
            """Effect: "Adventurer Boost"""",
            ModifierEnchantmentParser.parseEffect(
                """Effect: <b><a href="desc.php?id=1">Adventurer Boost</a></b>""",
            ),
        )
    }

    @Test
    fun parseModifierFromDesc_delegatesOnEnums() {
        assertEquals("Initiative: +30", DoubleModifier.parseModifierFromDesc("Combat Initiative +30%"))
        assertEquals("Never Fumble", BooleanModifier.parseModifierFromDesc("Never Fumble"))
        assertNull(BitmapModifier.parseModifierFromDesc("Brimstone"))
    }

    @Test
    fun parseModifier_spellDamagePercent() {
        assertEquals(
            "Spell Damage Percent: +50",
            ModifierEnchantmentParser.parseModifier("+50% Spell Damage"),
        )
    }

    @Test
    fun parseModifier_weakensMonster() {
        assertEquals("Weakens Monster", ModifierEnchantmentParser.parseModifier("Successful hit weakens opponent"))
    }

    @Test
    fun parseModifier_combatRateMore() {
        assertEquals(
            "Combat Rate: +10",
            ModifierEnchantmentParser.parseModifier("Monsters are much more attracted to you."),
        )
    }

    @Test
    fun parseModifier_resistanceAllElements() {
        val result = ModifierEnchantmentParser.parseModifier("Sublime Resistance to All Elements (+9)")
        assertTrue(result?.contains("Spooky Resistance: +9") == true)
        assertTrue(result?.contains("Cold Resistance: +9") == true)
    }
}
