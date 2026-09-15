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
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.session.BatManager
import net.sourceforge.kolmafia.shop.BatCoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.FunALogUnlockPrefs
import net.sourceforge.kolmafia.shop.PathShopAccessibility
import net.sourceforge.kolmafia.shop.QuestShopAccessibility

/**
 * Focused XLIX Track A–B coverage (phases 6791–6830).
 * Parent wrap bumps REVISION to phase6850.
 */
class GameRuntimeLibraryPhase6810Test {

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
    fun revision_isPhase6850() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pathShops_gateOnPrefsAndPath() {
        val locked = prefs()
        assertEquals(
            "You don't own a speakeasy",
            PathShopAccessibility.fancyDanInaccessible(locked),
        )
        assertFalse(CoinmasterAccessibility.isAccessible(master("olivers"), CharacterState(), locked))

        val open = prefs { putBoolean("ownsSpeakeasy", true) }
        assertNull(PathShopAccessibility.fancyDanInaccessible(open))
        assertTrue(CoinmasterAccessibility.isAccessible(master("olivers"), CharacterState(), open))

        val exploathing = CharacterState(challengePath = AscensionPath.KINGDOM_OF_EXPLOATHING.apiName)
        assertNull(PathShopAccessibility.cosmicRaysInaccessible(exploathing))
        assertTrue(CoinmasterAccessibility.isAccessible(master("exploathing"), exploathing))

        val poke = CharacterState(challengePath = AscensionPath.POKEFAM.apiName)
        assertTrue(CoinmasterAccessibility.isAccessible(master("pokefam"), poke))

        val nuclear = CharacterState(challengePath = AscensionPath.NUCLEAR_AUTUMN.apiName)
        assertTrue(CoinmasterAccessibility.isAccessible(master("mutate"), nuclear))

        val plumber = CharacterState(challengePath = AscensionPath.PLUMBER.apiName)
        assertTrue(CoinmasterAccessibility.isAccessible(master("mariogear"), plumber))
        assertEquals(
            "You are not a plumber.",
            PathShopAccessibility.plumberInaccessible(CharacterState()),
        )

        val ed = CharacterState(
            challengePath = AscensionPath.ACTUALLY_ED_THE_UNDYING.apiName,
            limitMode = "ed",
        )
        assertNull(PathShopAccessibility.edShopInaccessible(ed))
        assertTrue(CoinmasterAccessibility.isAccessible(master("edunder_shopshop"), ed))
        assertEquals(
            "Only Ed can come here.",
            PathShopAccessibility.edShopInaccessible(CharacterState()),
        )

        val detective = prefs { putBoolean("hasDetectiveSchool", true) }
        assertTrue(CoinmasterAccessibility.isAccessible(master("detective"), CharacterState(), detective))

        val rumple = prefs { putString("grimstoneMaskPath", "gnome") }
        assertTrue(CoinmasterAccessibility.isAccessible(master("rumple"), CharacterState(), rumple))

        val space = prefs { putBoolean("spacegateAlways", true) }
        assertTrue(CoinmasterAccessibility.isAccessible(master("spacegate"), CharacterState(), space))

        val camp = prefs { putBoolean("getawayCampsiteUnlocked", true) }
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("campfire"), CharacterState(), camp),
        )

        assertEquals(
            "You don't have an odd silver coin.",
            PathShopAccessibility.boutiqueInaccessible { 0 },
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("cindy"),
                CharacterState(),
                accessibleCount = { id -> if (id == PathShopAccessibility.ODD_SILVER_COIN) 1 else 0 },
            ),
        )
    }

    @Test
    fun questShops_andBatFabricator_gateCorrectly() {
        val locked = prefs()
        assertEquals(
            "You must rescue Grandma first.",
            QuestShopAccessibility.grandmaInaccessible(locked),
        )
        val rescued = prefs { putString(Quest.SEA_MONKEES.prefKey, "step9") }
        assertNull(QuestShopAccessibility.grandmaInaccessible(rescued))
        assertTrue(CoinmasterAccessibility.isAccessible(master("grandma"), CharacterState(), rescued))

        assertEquals(
            "The Black Market is not currently available",
            QuestShopAccessibility.blackMarketInaccessible(CharacterState(), locked),
        )
        val mac = prefs { putString(Quest.MACGUFFIN.prefKey, "step1") }
        assertNull(QuestShopAccessibility.blackMarketInaccessible(CharacterState(), mac))
        assertTrue(CoinmasterAccessibility.isAccessible(master("blackmarket"), CharacterState(), mac))

        assertEquals(
            "Need PirateRealm fun-a-log",
            QuestShopAccessibility.funALogInaccessible { 0 },
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("piraterealm"),
                CharacterState(),
                accessibleCount = { id -> if (id == FunALogUnlockPrefs.PIRATE_REALM_FUN_LOG) 1 else 0 },
            ),
        )

        val bat = CharacterState(limitMode = "batman")
        BatManager.setBatZone(BatManager.DOWNTOWN, null)
        assertEquals(
            "Batfellow can only use the Bat-Fabricator in the BatCavern.",
            BatCoinmasterAccessibility.fabricatorInaccessibleReason("batman"),
        )
        assertFalse(CoinmasterAccessibility.isAccessible(master("batman_cave"), bat))

        BatManager.setBatZone(BatManager.BAT_CAVERN, null)
        assertNull(BatCoinmasterAccessibility.fabricatorInaccessibleReason("batman"))
        assertTrue(CoinmasterAccessibility.isAccessible(master("batman_cave"), bat))
        assertTrue(BatCoinmasterAccessibility.fabricatorAccessible("batman"))
    }
}
