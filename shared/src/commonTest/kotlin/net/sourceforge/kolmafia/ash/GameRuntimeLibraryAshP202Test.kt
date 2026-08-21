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
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.ShopInventorySync
import net.sourceforge.kolmafia.shop.ShopRowDatabase
import net.sourceforge.kolmafia.shop.ShopRowFormatting

class GameRuntimeLibraryAshP202Test {

    @Test
    fun revision_phase207() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        ShopRowDatabase.resetForTest()
        CoinmasterVisitInventory.resetForTest()
        ItemDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun dynamicSkillShopVisit_registersSkillNameAndValidatePasses() {
        registerSkillShopItemsOnly()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = inventoryWithTokens(),
        )
        lib.processVisitResponseHooks(skillVisitHtml(), "shop.php?whichshop=$SHOP_ID")
        assertEquals(DYNAMIC_SKILL_NAME, SkillDefinitionDatabase.getById(SKILL_ID)?.name)
        val charState = CharacterState(meat = 100_000)
        assertTrue(
            CoinmasterDatabase.containsBuySkill(
                skillId = SKILL_ID,
                validate = true,
                state = charState,
                prefs = prefs,
                hasSkill = { false },
                accessibleCount = { if (it == TOKEN_ITEM) 10 else 0 },
            ),
        )
    }

    @Test
    fun dynamicSkillShopVisit_sessionLogUsesHtmlSkillName() {
        registerSkillShopItemsOnly()
        val prefs = Preferences(MapSettings())
        val sessionLogger = SessionLogger(prefs, GameEventBus())
        ShopInventorySync.parseAndLearn(
            html = skillVisitHtml(),
            url = "shop.php?whichshop=$SHOP_ID",
            sessionLogger = sessionLogger,
        )
        val log = prefs.getString(SessionLogger.SESSION_LOG_KEY, "")
        assertTrue(log.contains(DYNAMIC_SKILL_NAME))
        assertFalse(log.contains("skill $SKILL_ID"))
        assertEquals(
            DYNAMIC_SKILL_NAME,
            ShopRowFormatting.formatStack(
                net.sourceforge.kolmafia.shop.ItemStack(itemId = SKILL_ID, count = 1, isSkill = true),
            ),
        )
    }

    @Test
    fun dynamicSkillShopVisit_prefetchesDescSkillOnceForNewRegistration() {
        registerSkillShopItemsOnly()
        var descSkillRequests = 0
        val client = HttpClient(
            MockEngine { request ->
                if (request.url.encodedPath.contains("desc_skill.php")) {
                    descSkillRequests++
                    respond(
                        """<center><b>$DYNAMIC_SKILL_NAME</b><br><img src=itemimages/skillbook.gif>""",
                        HttpStatusCode.OK,
                    )
                } else {
                    respond("ok", HttpStatusCode.OK)
                }
            },
        )
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            httpClient = client,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(meat = "100000"))
            },
            inventoryManager = inventoryWithTokens(client),
        )
        lib.processVisitResponseHooks(skillVisitHtml(), "shop.php?whichshop=$SHOP_ID")
        assertEquals(1, descSkillRequests)
        lib.processVisitResponseHooks(skillVisitHtml(), "shop.php?whichshop=$SHOP_ID")
        assertEquals(1, descSkillRequests)
    }

    private fun skillVisitHtml() = """
        <tr rel="99999">
        <td></td>
        <td><img src="itemimages/skillbook.gif" onclick="javascript:poop('desc_skill.php?whichskill=$SKILL_ID&amp;self=true','skill',350,300)"></td>
        <td><b>$DYNAMIC_SKILL_NAME</b></td>
        <td><img src="itemimages/token.gif" onclick="javascript:descitem($TOKEN_ITEM)"></td>
        <td><b>5</b></td>
        <td><a href="shop.php?action=buyitem&whichshop=$SHOP_ID&whichrow=2100">Buy</a></td>
        </tr>
    """.trimIndent()

    private fun inventoryWithTokens(client: HttpClient = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) =
        object : InventoryManager(client, GameEventBus()) {
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

    private fun registerSkillShopItemsOnly() {
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
        ShopRowDatabase.loadFromText(
            shopRowsText = "",
            shopsText = "skillshop\tCorpus Skill Shop\n",
        )
    }

    companion object {
        private const val SHOP_ID = "skillshop"
        private const val SKILL_ID = 6099
        private const val TOKEN_ITEM = 99301
        private const val DYNAMIC_SKILL_NAME = "Visit Learned Skill"
    }
}
