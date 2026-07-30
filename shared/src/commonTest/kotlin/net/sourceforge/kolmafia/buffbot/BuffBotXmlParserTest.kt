package net.sourceforge.kolmafia.buffbot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuffBotXmlParserTest {

    private val sampleXml = """
        <buffbot>
        <buffdata>
          <name>Empathy of the Newt</name>
          <price>100</price>
          <turns>10</turns>
          <philanthropic>false</philanthropic>
        </buffdata>
        <buffdata>
          <name>Empathy of the Newt</name>
          <price>50</price>
          <turns>10</turns>
          <philanthropic>true</philanthropic>
        </buffdata>
        </buffbot>
    """.trimIndent()

    @Test
    fun parse_splitsPhilanthropicAndStandardOfferings() {
        val (philanthropic, standard) = BuffBotXmlParser.parse(sampleXml, "OakBot")

        assertEquals(1, philanthropic.size)
        assertEquals(50, philanthropic[0].price)
        assertTrue(philanthropic[0].philanthropic)
        assertEquals("Empathy of the Newt", philanthropic[0].buffs.single())

        assertEquals(1, standard.size)
        assertEquals(100, standard[0].price)
        assertEquals(false, standard[0].philanthropic)
    }

    @Test
    fun parse_jalaPrefixMapsToJalapenoSaucesphere() {
        val xml = """
            <buffdata>
              <name>Jala something</name>
              <price>75</price>
              <turns>5</turns>
              <philanthropic>false</philanthropic>
            </buffdata>
        """.trimIndent()

        val (_, standard) = BuffBotXmlParser.parse(xml, "OakBot")

        assertEquals("Jalape\u00f1o Saucesphere", standard.single().buffs.single())
    }
}
