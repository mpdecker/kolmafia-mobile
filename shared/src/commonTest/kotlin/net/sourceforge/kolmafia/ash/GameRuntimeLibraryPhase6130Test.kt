package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.sourceforge.kolmafia.character.ApiStatusSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AccountSync
import net.sourceforge.kolmafia.request.ApiRequest
import net.sourceforge.kolmafia.request.GrandpaRequest
import net.sourceforge.kolmafia.request.InterestingCoinRequestHub
import net.sourceforge.kolmafia.request.MomRequest

class GameRuntimeLibraryPhase6130Test {

    @Test
    fun revision_phase6310() {
        // Superseded by XXXVIII; current runtime revision is phase6310.
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun apiRequest_routesInventoryClosetStorage() {
        assertEquals("inventory", ApiRequest.whatFromUrl("api.php?what=inventory&for=KoLmafia"))
        assertEquals("closet", ApiRequest.whatFromUrl("api.php?what=closet"))
        assertEquals("storage", ApiRequest.whatFromUrl("api.php?what=storage"))
        assertEquals(
            mapOf(100 to 3, 200 to 1),
            ApiRequest.parseIdQtyMap("""{"100":"3","200":1,"300":0}"""),
        )
    }

    @Test
    fun accountSync_parseStatus_flagConfigAndEudora() {
        val char = KoLCharacter()
        val prefs = Preferences(MapSettings())
        val root = Json.parseToJsonElement(
            """
            {
              "sign": "Blender",
              "path": "0",
              "hardcore": "1",
              "casual": "0",
              "freedralph": "0",
              "recalledskills": "0",
              "pwd": "abcdef1234567890",
              "flag_config": {
                "ignorezonewarnings": "1",
                "autoattack": "12",
                "whichpenpal": "3",
                "wowbar": "1"
              }
            }
            """.trimIndent(),
        ).jsonObject
        AccountSync.parseStatus(root, char, prefs)
        assertTrue(char.state.value.ignoreZoneWarnings)
        assertEquals(12, char.state.value.autoAttackAction)
        assertTrue(char.state.value.isHardcore)
        assertEquals("Xi Receiver Unit", prefs.getString("currentEudora", ""))
        assertEquals("abcdef1234567890", prefs.getString("pwdHash", ""))
        assertTrue(prefs.getBoolean("serverAddsCustomCombat", false))
    }

    @Test
    fun apiStatusSync_interestingCoinAndZootSlots() {
        val char = KoLCharacter()
        val prefs = Preferences(MapSettings())
        ApiStatusSync.parseStatus(
            """{"coolitems":"interesting","grafts":{"6":"9","10":"11","11":"12"}}""",
            char,
            prefs,
        )
        assertTrue(prefs.getBoolean("hasInterestingCoin", false))
        assertEquals(9, prefs.getInt("zootGraftedNippleRightFamiliar", 0))
        assertEquals(11, prefs.getInt("zootGraftedFootLeftFamiliar", 0))
        assertEquals(12, prefs.getInt("zootGraftedFootRightFamiliar", 0))
    }

    @Test
    fun apiCloset_writesCollectionCache() {
        val prefs = Preferences(MapSettings())
        assertTrue(ApiRequest.parseCloset("""{"55":"2","66":"4"}""", prefs))
        assertEquals(
            mapOf(55 to 2, 66 to 4),
            CollectionCache.load(prefs, Preferences.CACHED_CLOSET),
        )
    }

    @Test
    fun momAccessible_andParseResponse() {
        assertEquals(
            "You haven't rescued Mom yet.",
            MomRequest.accessible(questFinished = false),
        )
        assertNull(
            MomRequest.accessible(
                questFinished = true,
                inventoryCount = { id ->
                    when (id) {
                        MomRequest.SCUBA_GEAR, MomRequest.BATHYSPHERE -> 1
                        else -> 0
                    }
                },
            ),
        )
        val prefs = Preferences(MapSettings())
        MomRequest.parseResponse("You begin to sweat", prefs, null)
        assertTrue(prefs.getBoolean(MomRequest.FOOD_RECEIVED_PREF, false))
    }

    @Test
    fun grandpa_skipsWhenNotEquipped() {
        val prefs = Preferences(MapSettings())
        GrandpaRequest.parseResponse(
            "eel",
            "You can't visit the Sea Monkees without some way of breathing underwater.",
            prefs,
            null,
        )
        assertFalse(prefs.getBoolean("grandpaUnlockedEelSauce", false))
    }

    @Test
    fun interestingCoin_parseResponse() {
        val prefs = Preferences(MapSettings())
        InterestingCoinRequestHub.parseResponse(
            "shop.php?whichshop=interesting",
            "You have 42 Interesting Coins.",
            prefs,
        )
        assertEquals(42, prefs.getInt("availableInterestingCoins", 0))
        assertTrue(prefs.getBoolean("hasInterestingCoin", false))
    }
}
