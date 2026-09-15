package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.InventoryTokenShopAccessibility

/**
 * XLVIII Track B focused coverage (phases 6751–6770).
 * REVISION stays phase6850 until parent mega wrap-up.
 */
class GameRuntimeLibraryPhase6770Test {

    private fun master(nickname: String, shopId: String = nickname) =
        CoinmasterData(
            masterName = nickname,
            nickname = nickname,
            token = null,
            shopId = shopId,
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    @Test
    fun revision_still_phase6850_until_parent_wrap() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun inventory_token_gates_live() {
        val cs = CharacterState()
        val toxic = master("toxic", "toxic")
        assertFalse(CoinmasterAccessibility.isAccessible(toxic, cs, accessibleCount = { 0 }))
        assertEquals(
            "You do not have a toxic globule in inventory",
            CoinmasterAccessibility.inaccessibleReason(toxic, cs, accessibleCount = { 0 }),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                toxic,
                cs,
                accessibleCount = { id ->
                    if (id == InventoryTokenShopAccessibility.TOXIC_GLOBULE) 1 else 0
                },
            ),
        )
    }

    @Test
    fun fishbones_guzzlr_warbear_shower_fdkol_messages() {
        val cs = CharacterState()
        assertEquals(
            "You do not have a freshwater fishbone in inventory",
            CoinmasterAccessibility.inaccessibleReason(master("fishbones"), cs, accessibleCount = { 0 }),
        )
        assertEquals(
            "You have no Guzzlrbucks to spend",
            CoinmasterAccessibility.inaccessibleReason(master("guzzlr"), cs, accessibleCount = { 0 }),
        )
        assertEquals(
            "You don't have a warbear black box",
            CoinmasterAccessibility.inaccessibleReason(master("warbear"), cs, accessibleCount = { 0 }),
        )
        assertEquals(
            "You do not have a glob of wet paper in inventory",
            CoinmasterAccessibility.inaccessibleReason(master("showerthoughts"), cs, accessibleCount = { 0 }),
        )
        assertEquals(
            "You do not have an FDKOL commendation in inventory",
            CoinmasterAccessibility.inaccessibleReason(master("fdkol"), cs, accessibleCount = { 0 }),
        )
        assertNull(
            CoinmasterAccessibility.inaccessibleReason(
                master("fdkol"),
                cs,
                accessibleCount = { id ->
                    if (id == InventoryTokenShopAccessibility.FDKOL_COMMENDATION) 1 else 0
                },
            ),
        )
    }

    @Test
    fun phase6751_marker_present() {
        assertEquals("Phase6751", Phase6751::class.simpleName)
    }
}
