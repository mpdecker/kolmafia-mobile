package net.sourceforge.kolmafia.inventory

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class ItemFlagsParserTest {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun parse_readsAllSectionsAndSkipsUnknownNames() {
        registerItem(100, "meat stack")
        registerItem(101, "batgut")
        registerItem(102, "bugbear beanie")
        registerItem(103, "tiny plastic Crimbo wreath")
        registerItem(104, "seal tooth")

        val text = """
             > junk
            meat stack
            missing junk item
            batgut

             > singleton
            bugbear beanie

             > mementos
            tiny plastic Crimbo wreath

             > profitable
            3 seal tooth
        """.trimIndent()

        val sections = ItemFlagsParser.parse(text) { name ->
            ItemDatabase.getByName(name) != null
        }

        assertEquals(listOf("meat stack", "batgut", "bugbear beanie"), sections.junk)
        assertEquals(listOf("bugbear beanie"), sections.singleton)
        assertEquals(listOf("tiny plastic Crimbo wreath"), sections.memento)
        assertEquals(listOf("seal tooth"), sections.profitable)
    }

    @Test
    fun parseItemName_stripsProfitableCountPrefix() {
        assertEquals("seal tooth", ItemFlagsParser.parseItemName("3 seal tooth"))
        assertEquals("meat stack", ItemFlagsParser.parseItemName("meat stack"))
    }

    @Test
    fun export_roundTripsThroughParse() {
        registerItem(100, "meat stack")
        registerItem(101, "batgut")
        registerItem(102, "bugbear beanie")
        registerItem(103, "tiny plastic Crimbo wreath")
        registerItem(104, "seal tooth")

        val exported = ItemFlagsParser.export(
            junk = listOf("meat stack", "batgut", "bugbear beanie"),
            singleton = setOf("bugbear beanie"),
            memento = listOf("tiny plastic Crimbo wreath"),
            profitable = listOf("seal tooth" to 2),
        )

        val parsed = ItemFlagsParser.parse(exported) { name ->
            ItemDatabase.getByName(name) != null
        }

        assertEquals(listOf("meat stack", "batgut", "bugbear beanie"), parsed.junk)
        assertEquals(listOf("bugbear beanie"), parsed.singleton)
        assertEquals(listOf("tiny plastic Crimbo wreath"), parsed.memento)
        assertEquals(listOf("seal tooth"), parsed.profitable)
        val junkSection = exported.substringAfter(" > junk").substringBefore(" > singleton")
        assertFalse(junkSection.contains("bugbear beanie"))
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
