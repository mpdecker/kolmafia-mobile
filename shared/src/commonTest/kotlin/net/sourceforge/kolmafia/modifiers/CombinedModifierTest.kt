package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CombinedModifierTest {

    @Test
    fun parse_allAttributes_writesCombinedAndSubsumedMembers() {
        val v = ModifierParser.parse("All Attributes: +5")
        assertEquals(5.0, v.get(DoubleModifier.ALL_ATTRIBUTES))
        assertEquals(5.0, v.get(DoubleModifier.MUS))
        assertEquals(5.0, v.get(DoubleModifier.MYS))
        assertEquals(5.0, v.get(DoubleModifier.MOX))
    }

    @Test
    fun parse_maximumHpMp_writesCombinedAndSubsumedMembers() {
        val v = ModifierParser.parse("Maximum HP / MP: +25")
        assertEquals(25.0, v.get(DoubleModifier.MAXIMUM_HP_MP))
        assertEquals(25.0, v.get(DoubleModifier.HP))
        assertEquals(25.0, v.get(DoubleModifier.MP))
    }

    @Test
    fun parse_allAttributesPercent_writesThreePercentMembers() {
        val v = ModifierParser.parse("All Attributes Percent: +10")
        assertEquals(10.0, v.get(DoubleModifier.ALL_ATTRIBUTES_PCT))
        assertEquals(10.0, v.get(DoubleModifier.MUS_PCT))
        assertEquals(10.0, v.get(DoubleModifier.MYS_PCT))
        assertEquals(10.0, v.get(DoubleModifier.MOX_PCT))
    }

    @Test
    fun parse_hatPantsDrop_writesCombinedAndSubsumedMembers() {
        val v = ModifierParser.parse("Hat / Pants Drop: +30")
        assertEquals(30.0, v.get(DoubleModifier.HAT_PANTS_DROP))
        assertEquals(30.0, v.get(DoubleModifier.HATDROP))
        assertEquals(30.0, v.get(DoubleModifier.PANTSDROP))
    }

    @Test
    fun get_allAttributes_straddlingZero_returnsZero() {
        val v = ModifierValues(
            doubles = mapOf(
                DoubleModifier.MUS to 5.0,
                DoubleModifier.MYS to 5.0,
                DoubleModifier.MOX to -3.0,
            ),
        )
        assertEquals(0.0, v.get(DoubleModifier.ALL_ATTRIBUTES))
    }

    @Test
    fun get_allAttributes_allPositive_returnsMinMember() {
        val v = ModifierValues(
            doubles = mapOf(
                DoubleModifier.MUS to 5.0,
                DoubleModifier.MYS to 5.0,
                DoubleModifier.MOX to 3.0,
            ),
        )
        assertEquals(3.0, v.get(DoubleModifier.ALL_ATTRIBUTES))
    }

    @Test
    fun byTag_resolvesDesktopCombinedTagsCaseInsensitively() {
        assertEquals(DoubleModifier.ALL_ATTRIBUTES, DoubleModifier.byTag("All Attributes"))
        assertEquals(DoubleModifier.ALL_ATTRIBUTES_PCT, DoubleModifier.byTag("all attributes percent"))
        assertEquals(DoubleModifier.MAXIMUM_HP_MP, DoubleModifier.byTag("Maximum HP / MP"))
        assertEquals(DoubleModifier.HAT_PANTS_DROP, DoubleModifier.byTag("hat / pants drop"))
    }

    @Test
    fun combinedTags_subsumeExactDesktopMembers() {
        assertContentEquals(
            arrayOf(DoubleModifier.MUS, DoubleModifier.MYS, DoubleModifier.MOX),
            DoubleModifier.ALL_ATTRIBUTES.subsumed,
        )
        assertContentEquals(
            arrayOf(DoubleModifier.MUS_PCT, DoubleModifier.MYS_PCT, DoubleModifier.MOX_PCT),
            DoubleModifier.ALL_ATTRIBUTES_PCT.subsumed,
        )
        assertContentEquals(
            arrayOf(DoubleModifier.HP, DoubleModifier.MP),
            DoubleModifier.MAXIMUM_HP_MP.subsumed,
        )
        assertContentEquals(
            arrayOf(DoubleModifier.HATDROP, DoubleModifier.PANTSDROP),
            DoubleModifier.HAT_PANTS_DROP.subsumed,
        )
    }

    @Test
    fun parseModifier_allAttributesDesc_usesCombinedTag() {
        assertEquals(
            "All Attributes: +10",
            ModifierEnchantmentParser.parseModifier("All Attributes +10"),
        )
    }

    @Test
    fun parseModifier_maximumHpMpDesc_usesCombinedTag() {
        assertEquals(
            "Maximum HP / MP: +25",
            ModifierEnchantmentParser.parseModifier("Maximum HP/MP +25"),
        )
    }
}
