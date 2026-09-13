package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.TravelingTraderRequest
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.SpaaaceShopAccessibility
import net.sourceforge.kolmafia.shop.TimeTowerAccessibility

/**
 * Focused HTTP Residual LII Track A–B coverage (phases 6971–7010).
 * Parent wrap bumps REVISION to phase7150.
 */
class GameRuntimeLibraryPhase6990Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun master(nickname: String, shopId: String? = nickname) =
        CoinmasterData(
            masterName = nickname,
            nickname = nickname,
            token = null,
            shopId = shopId,
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    @Test
    fun revision_isPhase7030() {
        assertEquals("phase7150", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun spaaaceShops_gateOnGeneratorAndTransponder() {
        assertEquals(
            "You need to repair the Elves' Shield Generator to go there.",
            SpaaaceShopAccessibility.inaccessible(
                generatorFinished = false,
                accessibleCount = { 0 },
                hasEffect = { false },
            ),
        )
        assertEquals(
            "You need a transporter transponder to go there.",
            SpaaaceShopAccessibility.inaccessible(
                generatorFinished = true,
                accessibleCount = { 0 },
                hasEffect = { false },
            ),
        )
        assertNull(
            SpaaaceShopAccessibility.inaccessible(
                generatorFinished = true,
                accessibleCount = { id ->
                    if (id == SpaaaceShopAccessibility.TRANSPONDER) 1 else 0
                },
                hasEffect = { false },
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("lunarlunch"),
                CharacterState(),
                accessibleCount = { id ->
                    if (id == SpaaaceShopAccessibility.TRANSPONDER) 1 else 0
                },
                generatorQuestFinished = true,
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("elvishp1"),
                CharacterState(),
                hasEffect = { id -> id == SpaaaceShopAccessibility.TRANSPONDENT_EFFECT },
                generatorQuestFinished = true,
            ),
        )
    }

    @Test
    fun twitchJousting_usesRenaissanceGiftShopMessage() {
        val locked = prefs()
        assertEquals(
            "You can't get to the Renaissance Gift Shop",
            TimeTowerAccessibility.inaccessibleReason("twitch_jousting", locked),
        )
        val open = prefs { putBoolean("timeTowerAvailable", true) }
        assertNull(TimeTowerAccessibility.inaccessibleReason("twitch_jousting", open))
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("twitch_jousting"),
                CharacterState(),
                open,
            ),
        )
    }

    @Test
    fun travelingTrader_parsesDynamicBuyRows() {
        CoinmasterVisitInventory.replaceBuyRows(TravelingTraderRequest.SHOP_KEY, emptyList())
        val html = """
            The traveling trader is looking to acquire:<br>
            <img class='hand item' onclick='descitem(503220568);'
            src='http://images.kingdomofloathing.com/itemimages/scwad.gif'>
            <b>twinkly wads</b><br>
            (You have <b>12</b> on you.)
            <tr><td><input type=radio name=whichitem value=4411
            checked="checked"></td><td><a class=nounder
            href='javascript:descitem(629749615);'> <img class='hand item'
            src='http://images.kingdomofloathing.com/itemimages/music.gif'>
            <b>Inigo's Incantation of Inspiration</b></a></td><td>100 twinkly
            wads</td></tr>
        """.trimIndent()
        val p = prefs()
        TravelingTraderRequest.parseResponse("traveler.php", html, p)
        assertEquals("twinkly wads", p.getString("travelingTraderToken", ""))
        assertEquals(12, p.getInt("travelingTraderHave", -1))
        assertEquals(12, p.getInt("availableTwinklyWads", -1))
        val row = CoinmasterVisitInventory.findBuyRow(TravelingTraderRequest.SHOP_KEY, 4411)
        assertEquals(4411, row?.item?.itemId)
        assertEquals(100, row?.price)
    }

    @Test
    fun wereprofessor_requiresMildManneredEffectViaHasEffect() {
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                master("wereprofessor_tinker"),
                CharacterState(),
                hasEffect = { false },
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("wereprofessor_tinker"),
                CharacterState(),
                hasEffect = { id -> id == 2897 },
            ),
        )
    }
}
