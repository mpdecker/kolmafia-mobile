package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ArmoryRequest
import net.sourceforge.kolmafia.request.BoutiqueRequest
import net.sourceforge.kolmafia.request.CRIMBCOGiftShopRequest
import net.sourceforge.kolmafia.request.CoinMasterRequest
import net.sourceforge.kolmafia.request.CoinMasterShopRequest
import net.sourceforge.kolmafia.request.LTTRequest
import net.sourceforge.kolmafia.request.MemeShopRequest
import net.sourceforge.kolmafia.request.NinjaStoreRequest
import net.sourceforge.kolmafia.request.SHAWARMARequest
import net.sourceforge.kolmafia.request.SushiRequest
import net.sourceforge.kolmafia.request.TakerSpaceRequest
import net.sourceforge.kolmafia.request.VYKEARequest
import net.sourceforge.kolmafia.request.VykeaChoiceMapper
import net.sourceforge.kolmafia.session.BatManager
import net.sourceforge.kolmafia.shop.BatCoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.KolhsShopAccessibility

/**
 * Focused XLVIII Track C coverage (phases 6771–6790).
 * REVISION stays phase6850 until parent wrap.
 */
class GameRuntimeLibraryphase6850Test {

    @BeforeTest
    fun setUp() {
        BatManager.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        BatManager.resetForTest()
    }

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun master(nickname: String) =
        CoinmasterData(
            masterName = nickname,
            nickname = nickname,
            token = null,
            shopId = nickname,
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    @Test
    fun revision_staysPhase6730() {
        assertEquals("phase6910", GameRuntimeLibrary.REVISION)
        assertEquals("6910", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun aliasHubs_delegateToSiblingShops() {
        assertTrue(SHAWARMARequest.registerRequest("shop.php?whichshop=si_shop1"))
        assertFalse(SHAWARMARequest.registerRequest("shop.php?whichshop=si_shop3"))
        assertTrue(ArmoryRequest.registerRequest("shop.php?whichshop=si_shop3"))
        assertFalse(ArmoryRequest.registerRequest("shop.php?whichshop=armory"))
        assertTrue(LTTRequest.registerRequest("shop.php?whichshop=ltt"))
        assertTrue(MemeShopRequest.registerRequest("shop.php?whichshop=bacon"))
        assertTrue(NinjaStoreRequest.registerRequest("shop.php?whichshop=nina"))
        assertTrue(BoutiqueRequest.registerRequest("shop.php?whichshop=cindy"))
        assertTrue(CRIMBCOGiftShopRequest.registerRequest("shop.php?whichshop=crimbco"))
    }

    @Test
    fun sushiTakerSpaceVykea_registerRequest() {
        assertTrue(SushiRequest.registerRequest("sushi.php"))
        assertTrue(SushiRequest.registerRequest("sushi.php?whichsushi=1"))
        assertFalse(SushiRequest.registerRequest("shop.php?whichshop=bacon"))

        assertTrue(TakerSpaceRequest.registerRequest("choice.php?whichchoice=1537&option=1"))
        assertFalse(TakerSpaceRequest.registerRequest("shop.php?whichshop=guzzlr"))

        assertTrue(
            VYKEARequest.registerRequest(
                "inv_use.php?whichitem=${VykeaChoiceMapper.INSTRUCTIONS_ID}",
            ),
        )
        assertTrue(VYKEARequest.registerRequest("choice.php?whichchoice=1120&option=1"))
        assertFalse(VYKEARequest.registerRequest("shop.php?whichshop=armory"))
    }

    @Test
    fun memeShop_parseResponse_syncsBaconPrefs() {
        val p = prefs()
        assertTrue(
            MemeShopRequest.parseResponse(
                "shop.php?whichshop=bacon",
                "<html>store empty of meme toys</html>",
                p,
            ),
        )
        assertTrue(p.getBoolean("_internetViralVideoBought", false))
        assertFalse(MemeShopRequest.parseResponse("shop.php?whichshop=nina", "<html/>", p))
    }

    @Test
    fun coinMasterStubs_documentConsolidation() {
        assertFalse(CoinMasterRequest.registerRequest("shop.php?whichshop=bacon"))
        assertFalse(CoinMasterShopRequest.registerRequest("shop.php?whichshop=bacon"))
    }

    @Test
    fun kolhsAccessibility_requiresPathAndUnlockPref() {
        val locked = prefs()
        val notKolhs = CharacterState(currentRun = 40)
        assertEquals(
            "You need to be in Shop Class to make that.",
            KolhsShopAccessibility.inaccessibleReason(
                notKolhs,
                locked,
                "lastKOLHSShopClassUnlockAdventure",
                "Shop Class",
            ),
        )
        assertFalse(
            CoinmasterAccessibility.isAccessible(master("kolhs_shop"), notKolhs, locked),
        )

        val open = prefs { putInt("lastKOLHSShopClassUnlockAdventure", 40) }
        val inSchool = CharacterState(
            challengePath = AscensionPath.KOLHS.apiName,
            currentRun = 40,
        )
        assertNull(
            KolhsShopAccessibility.inaccessibleReason(
                inSchool,
                open,
                "lastKOLHSShopClassUnlockAdventure",
                "Shop Class",
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("kolhs_shop"), inSchool, open),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("kolhs_art"),
                inSchool,
                prefs { putInt("lastKOLHSArtClassUnlockAdventure", 40) },
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("kolhs_chem"),
                inSchool,
                prefs { putInt("lastKOLHSChemClassUnlockAdventure", 40) },
            ),
        )
    }

    @Test
    fun batfellowAccessibility_requiresBatmanDowntown() {
        val cs = CharacterState(limitMode = "batman")
        BatManager.setBatZone(BatManager.BAT_CAVERN, null)
        assertEquals(
            "Batfellow can only visit ChemiCorp while Downtown.",
            BatCoinmasterAccessibility.downtownInaccessibleReason(
                "batman",
                BatCoinmasterAccessibility.CHEMICORP,
            ),
        )
        assertFalse(
            CoinmasterAccessibility.isAccessible(master("batman_chemicorp"), cs),
        )

        BatManager.setBatZone(BatManager.DOWNTOWN, null)
        assertNull(
            BatCoinmasterAccessibility.downtownInaccessibleReason(
                "batman",
                BatCoinmasterAccessibility.CHEMICORP,
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("batman_chemicorp"), cs),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("batman_orphanage"), cs),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("batman_pd"), cs),
        )

        assertEquals(
            "Only Batfellow can go to ChemiCorp.",
            BatCoinmasterAccessibility.downtownInaccessibleReason(
                "",
                BatCoinmasterAccessibility.CHEMICORP,
            ),
        )
    }
}
