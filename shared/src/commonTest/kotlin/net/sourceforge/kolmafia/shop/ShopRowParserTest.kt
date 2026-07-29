package net.sourceforge.kolmafia.shop

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase

class ShopRowParserTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun parseShop_singleCostCoinRow() {
        registerItems()
        val html = """
            <tr rel="$VISIT_ITEM">
            <a onClick='javascript:descitem($VISIT_ITEM)'><b>visit-learned item</b></a>
            <span title="FDKOL commendation"><b>75</b></span>
            <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1500">
            </tr>
        """.trimIndent()

        val rows = ShopRowParser.parseShop(html)
        assertEquals(1, rows.size)
        assertEquals(1500, rows[0].rowId)
        assertEquals(VISIT_ITEM, rows[0].item.itemId)
        assertEquals(FDKOL_COMMENDATION, rows[0].costs.single().itemId)
        assertEquals(75, rows[0].costs.single().count)
    }

    @Test
    fun parseShop_meatRow() {
        registerItems()
        val html = """
            <tr rel="$MEAT_ITEM">
            <a onClick='javascript:descitem($MEAT_ITEM)'><b>meat snack</b></a>
            <span title="Meat"><b>1,000</b></span>
            <form action="shop.php?action=buy&whichshop=fdkol&whichrow=1501">
            </tr>
        """.trimIndent()

        val rows = ShopRowParser.parseShop(html)
        assertEquals(1, rows.size)
        assertTrue(rows[0].isMeatPurchase)
        assertEquals(1000, rows[0].costs.single().count)
    }

    @Test
    fun parseShop_skipsMalformedRows() {
        val rows = ShopRowParser.parseShop("<table><tr><td>no shop row</td></tr></table>")
        assertTrue(rows.isEmpty())
    }

    @Test
    fun parseShop_multiCostSkillRow() {
        registerSkillShopItems()
        val html = """
            <tr rel="99999">
            <td></td>
            <td><img src="itemimages/skillbook.gif" onclick="javascript:poop('desc_skill.php?whichskill=$SKILL_ID&amp;self=true','skill',350,300)"></td>
            <td><b>Corpus Skill</b></td>
            <td><img src="itemimages/token.gif" onclick="javascript:descitem($TOKEN_ITEM)"></td>
            <td><b>5</b></td>
            <td><a href="shop.php?action=buyitem&whichshop=skillshop&whichrow=2100">Buy</a></td>
            </tr>
        """.trimIndent()

        val rows = ShopRowParser.parseShop(html)
        assertEquals(1, rows.size)
        assertEquals(2100, rows[0].rowId)
        assertTrue(rows[0].isSkillPurchase)
        assertEquals(SKILL_ID, rows[0].item.itemId)
        assertTrue(rows[0].item.isSkill)
        assertEquals(TOKEN_ITEM, rows[0].costs.single().itemId)
        assertEquals(5, rows[0].costs.single().count)
    }

    @Test
    fun parseShop_multiCostSkillRow_registersUnknownSkillFromHtml() {
        registerSkillShopTokenOnly()
        val html = """
            <tr rel="99999">
            <td></td>
            <td><img src="itemimages/skillbook.gif" onclick="javascript:poop('desc_skill.php?whichskill=$SKILL_ID&amp;self=true','skill',350,300)"></td>
            <td><b>Visit Learned Skill</b></td>
            <td><img src="itemimages/token.gif" onclick="javascript:descitem($TOKEN_ITEM)"></td>
            <td><b>5</b></td>
            <td><a href="shop.php?action=buyitem&whichshop=skillshop&whichrow=2100">Buy</a></td>
            </tr>
        """.trimIndent()

        val rows = ShopRowParser.parseShop(html)
        assertEquals(1, rows.size)
        assertEquals("Visit Learned Skill", SkillDefinitionDatabase.getById(SKILL_ID)?.name)
    }

    private fun registerSkillShopTokenOnly() {
        ItemDatabase.registerForTest(
            ItemData(
                id = TOKEN_ITEM,
                name = "shop token",
                descId = TOKEN_ITEM.toString(),
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun registerSkillShopItems() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = SKILL_ID,
                name = "Corpus Skill",
                image = "skillbook",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = TOKEN_ITEM,
                name = "shop token",
                descId = TOKEN_ITEM.toString(),
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun registerItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = VISIT_ITEM,
                name = "visit-learned item",
                descId = "d$VISIT_ITEM",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = FDKOL_COMMENDATION,
                name = "FDKOL commendation",
                descId = "d$FDKOL_COMMENDATION",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = MEAT_ITEM,
                name = "meat snack",
                descId = "d$MEAT_ITEM",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val VISIT_ITEM = 99101
        private const val FDKOL_COMMENDATION = 99102
        private const val MEAT_ITEM = 99103
        private const val SKILL_ID = 6027
        private const val TOKEN_ITEM = 99104
    }
}
