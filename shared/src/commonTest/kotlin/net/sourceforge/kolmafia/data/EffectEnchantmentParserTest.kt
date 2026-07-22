package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectEnchantmentParserTest {

    @Test
    fun parseEffectEnchantments_blueBoldBlock() {
        val html = """
            <font color=blue><b>
             Muscle +15<br>
             Combat Initiative +30%<br>
            </b></font>
        """.trimIndent()
        assertEquals(
            "Muscle: +15, Initiative: +30",
            EffectEnchantmentParser.parseEffectEnchantments(html),
        )
    }

    @Test
    fun parseEffectEnchantments_graftedFixture() {
        val html = """
            <font color=blue><b>Muscle +10<br>+60% Meat from Monsters<br>Regenerate 5-15 MP per Adventure</b></font>
        """.trimIndent()
        val mods = EffectEnchantmentParser.parseEffectEnchantments(html)
        assertTrue(mods.contains("Muscle: +10"))
        assertTrue(mods.contains("Meat Drop: +60"))
        assertTrue(mods.contains("MP Regen Min: 5"))
        assertTrue(mods.contains("MP Regen Max: 15"))
    }
}
