package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ItemEnchantmentParserTest {

    @Test
    fun parseItemEnchantments_blueBlock() {
        val html = """
            <font color=blue>
             Muscle +15<br>
             Combat Initiative +30%<br>
            </font>
        """.trimIndent()
        assertEquals(
            "Muscle: +15, Initiative: +30",
            ItemEnchantmentParser.parseItemEnchantments(html),
        )
    }

    @Test
    fun parseItemEnchantments_pantogramFixture() {
        val html = """
            <center><b><font color="blue"><p></p><center><b><font color="blue">Muscle +10<br>So-So Spooky Resistance (+2)<br>+60% Meat from Monsters<br>Regenerate 5-15 MP per Adventure</font></b></center><br>Occasional Hilarity</font></b></center>
            <!-- Last Available Date: 2017-11 -->
        """.trimIndent()
        val mods = ItemEnchantmentParser.parseItemEnchantments(html)
        assertTrue(mods.contains("Muscle: +10"))
        assertTrue(mods.contains("Spooky Resistance: +2"))
        assertTrue(mods.contains("Meat Drop: +60"))
        assertTrue(mods.contains("MP Regen Min: 5"))
        assertTrue(mods.contains("MP Regen Max: 15"))
        assertTrue(mods.contains("Drops Items"))
    }
}
