package net.sourceforge.kolmafia.shop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState

class InventoryTokenShopAccessibilityTest {

    private fun master(nickname: String) =
        CoinmasterData(
            masterName = nickname,
            nickname = nickname,
            token = null,
            shopId = nickname,
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun countOf(tokenId: Int, n: Int = 1): (Int) -> Int =
        { id -> if (id == tokenId) n else 0 }

    @Test
    fun toxic_requiresGlobule() {
        assertEquals(
            "You do not have a toxic globule in inventory",
            InventoryTokenShopAccessibility.inaccessibleReason("toxic") { 0 },
        )
        assertNull(
            InventoryTokenShopAccessibility.inaccessibleReason(
                "toxic",
                countOf(InventoryTokenShopAccessibility.TOXIC_GLOBULE),
            ),
        )
        assertNull(
            InventoryTokenShopAccessibility.inaccessibleReason(
                "ToxicChemistry",
                countOf(InventoryTokenShopAccessibility.TOXIC_GLOBULE),
            ),
        )
    }

    @Test
    fun fishbones_requiresFishbone() {
        assertEquals(
            "You do not have a freshwater fishbone in inventory",
            InventoryTokenShopAccessibility.inaccessibleReason("fishbones") { 0 },
        )
        assertNull(
            InventoryTokenShopAccessibility.inaccessibleReason(
                "fishbonery",
                countOf(InventoryTokenShopAccessibility.FRESHWATER_FISHBONE),
            ),
        )
    }

    @Test
    fun guzzlr_requiresGuzzlrbuck() {
        assertEquals(
            "You have no Guzzlrbucks to spend",
            InventoryTokenShopAccessibility.inaccessibleReason("guzzlr") { 0 },
        )
        assertNull(
            InventoryTokenShopAccessibility.inaccessibleReason(
                "guzzlr",
                countOf(InventoryTokenShopAccessibility.GUZZLRBUCK),
            ),
        )
    }

    @Test
    fun warbear_requiresBlackBox() {
        assertEquals(
            "You don't have a warbear black box",
            InventoryTokenShopAccessibility.inaccessibleReason("warbear") { 0 },
        )
        assertNull(
            InventoryTokenShopAccessibility.inaccessibleReason(
                "warbearbox",
                countOf(InventoryTokenShopAccessibility.WARBEAR_BLACK_BOX),
            ),
        )
    }

    @Test
    fun showerthoughts_requiresWetPaper() {
        assertEquals(
            "You do not have a glob of wet paper in inventory",
            InventoryTokenShopAccessibility.inaccessibleReason("showerthoughts") { 0 },
        )
        assertNull(
            InventoryTokenShopAccessibility.inaccessibleReason(
                "showerthoughts",
                countOf(InventoryTokenShopAccessibility.GLOB_OF_WET_PAPER),
            ),
        )
    }

    @Test
    fun fdkol_requiresCommendation() {
        assertEquals(
            "You do not have an FDKOL commendation in inventory",
            InventoryTokenShopAccessibility.inaccessibleReason("fdkol") { 0 },
        )
        assertNull(
            InventoryTokenShopAccessibility.inaccessibleReason(
                "FDKOL",
                countOf(InventoryTokenShopAccessibility.FDKOL_COMMENDATION),
            ),
        )
    }

    @Test
    fun coinmasterAccessibility_wiresAllTokenShops() {
        val cs = CharacterState()
        val nicknames = listOf(
            "toxic",
            "fishbones",
            "guzzlr",
            "warbear",
            "showerthoughts",
            "fdkol",
        )
        for (nick in nicknames) {
            assertFalse(
                CoinmasterAccessibility.isAccessible(master(nick), cs, accessibleCount = { 0 }),
                "expected inaccessible without token: $nick",
            )
        }
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("toxic"),
                cs,
                accessibleCount = countOf(InventoryTokenShopAccessibility.TOXIC_GLOBULE, 2),
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("fishbones"),
                cs,
                accessibleCount = countOf(InventoryTokenShopAccessibility.FRESHWATER_FISHBONE),
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("guzzlr"),
                cs,
                accessibleCount = countOf(InventoryTokenShopAccessibility.GUZZLRBUCK),
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("warbear"),
                cs,
                accessibleCount = countOf(InventoryTokenShopAccessibility.WARBEAR_BLACK_BOX),
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("showerthoughts"),
                cs,
                accessibleCount = countOf(InventoryTokenShopAccessibility.GLOB_OF_WET_PAPER),
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("fdkol"),
                cs,
                accessibleCount = countOf(InventoryTokenShopAccessibility.FDKOL_COMMENDATION),
            ),
        )
    }
}
