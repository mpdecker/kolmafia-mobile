package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals

class DescriptionParserTest {

    @Test
    fun parseName_readsBoldHeading() {
        val html = """<center><b>spicy bouncing batwing</b><br/>Type: <b>food (good)</b></center>"""
        assertEquals("spicy bouncing batwing", DescriptionParser.parseName(html))
    }

    @Test
    fun parseConsumableSize_readsSizeLine() {
        val html = """Size: <b>3</b>"""
        assertEquals(3, DescriptionParser.parseConsumableSize(html))
    }

    @Test
    fun parseQuality_readsParentheticalQuality() {
        val html = """Type: <b>food <font color=green>(good)</font></b>"""
        assertEquals("good", DescriptionParser.parseQuality(html))
    }
}
