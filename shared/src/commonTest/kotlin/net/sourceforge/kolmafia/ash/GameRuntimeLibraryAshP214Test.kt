package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.ItemStack
import net.sourceforge.kolmafia.shop.ShopInventorySync
import net.sourceforge.kolmafia.shop.ShopRow
import net.sourceforge.kolmafia.shop.ShopRowDatabase

class GameRuntimeLibraryAshP214Test {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        ShopRowDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        ItemDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun revision_phase222() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun isCoinmasterSkill_validateFalseUntilVisitHook() {
        registerSkillShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = inventoryWithTokens(),
        )
        assertEquals("false", outputLib(lib, """print(is_coinmaster_skill($SKILL_ID, true));"""))
        lib.processVisitResponseHooks(skillVisitHtml(), "shop.php?whichshop=$SHOP_ID")
        assertEquals("true", outputLib(lib, """print(is_coinmaster_skill($SKILL_ID, true));"""))
        CoinmasterVisitInventory.replaceBuyRows(SHOP_ID, emptyList())
        assertEquals("false", outputLib(lib, """print(is_coinmaster_skill($SKILL_ID, true));"""))
    }

    @Test
    fun isCoinmasterSkill_validateFalseWhenAlreadyKnown() {
        registerSkillShop()
        syncSkillVisit()
        assertFalse(
            CoinmasterDatabase.containsBuySkill(
                skillId = SKILL_ID,
                validate = true,
                prefs = Preferences(MapSettings()),
                hasSkill = { it == SKILL_ID },
                accessibleCount = { if (it == TOKEN_ITEM) 10 else 0 },
            ),
        )
    }

    @Test
    fun learnedShopRowsRestore_enablesGetShopRowAfterReset() {
        registerVisitItems()
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        val prefs = Preferences(MapSettings())
        val visitRow = ShopRow(
            rowId = 1500,
            item = ItemStack(itemId = VISIT_ITEM, count = 1),
            costs = listOf(ItemStack(itemId = TOKEN_ITEM, count = 75)),
        )
        ShopRowDatabase.registerVisitRow(1500, "fdkol", visitRow)
        ShopRowDatabase.persistLearnedRow(prefs, 1500, "fdkol", visitRow)

        ShopRowDatabase.resetForTest()
        ItemDatabase.resetForTest()
        registerVisitItems()
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "fdkol\tFDKOL Requisitions Tent\n",
        )
        ShopRowDatabase.restoreLearnedRows(prefs)
        assertNotNull(ShopRowDatabase.getShopRow(1500))
    }

    private fun syncSkillVisit() {
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "skillshop\tCorpus Skill Shop\n",
        )
        ShopInventorySync.parseAndLearn(
            html = skillVisitHtml(),
            url = "shop.php?whichshop=$SHOP_ID",
            sessionLogger = null,
        )
    }

    private fun skillVisitHtml() = """
        <tr rel="99999">
        <td></td>
        <td><img src="itemimages/skillbook.gif" onclick="javascript:poop('desc_skill.php?whichskill=$SKILL_ID&amp;self=true','skill',350,300)"></td>
        <td><b>Corpus Skill</b></td>
        <td><img src="itemimages/token.gif" onclick="javascript:descitem($TOKEN_ITEM)"></td>
        <td><b>5</b></td>
        <td><a href="shop.php?action=buyitem&whichshop=$SHOP_ID&whichrow=2100">Buy</a></td>
        </tr>
    """.trimIndent()

    private fun inventoryWithTokens(): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        TOKEN_ITEM to InventoryItem(
                            TOKEN_ITEM,
                            "shop token",
                            10,
                            ItemType.OTHER,
                        ),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
        }

    private fun registerSkillShop() {
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
        CoinmasterDatabase.loadFromText(
            shopsText = "skillshop\tCorpus Skill Shop\tCOIN\n",
            coinText = """
                Corpus Skill Shop	buy	1	Other Skill	ROW2099
            """.trimIndent(),
        )
    }

    private fun registerVisitItems() {
        ItemDatabase.registerForTest(
            ItemData(
                id = VISIT_ITEM,
                name = "visit-learned item",
                descId = VISIT_ITEM.toString(),
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
                id = TOKEN_ITEM,
                name = "FDKOL commendation",
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

    companion object {
        private const val SHOP_ID = "skillshop"
        private const val SKILL_ID = 6027
        private const val TOKEN_ITEM = 99301
        private const val VISIT_ITEM = 99001
    }
}
