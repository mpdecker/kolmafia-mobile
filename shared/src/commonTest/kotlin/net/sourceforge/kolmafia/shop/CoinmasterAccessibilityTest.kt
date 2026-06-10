package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoinmasterAccessibilityTest {

    private val dimemaster = CoinmasterData(
        masterName = "Dimemaster",
        nickname = "dimemaster",
        token = "dime",
        shopId = null,
        buyItems = emptyList(),
        sellItems = emptyList(),
        buyUrl = "bigisland.php",
    )

    private val shore = CoinmasterData(
        masterName = "The Shore, Inc. Gift Shop",
        nickname = "shore",
        token = "scrip",
        shopId = "shore",
        buyItems = emptyList(),
        sellItems = emptyList(),
    )

    @Test
    fun dimemaster_blockedBeforeKingFreed() {
        val char = CharacterState(kingLiberated = false, level = 15)
        assertFalse(CoinmasterAccessibility.isAccessible(dimemaster, char))
    }

    @Test
    fun dimemaster_accessibleAfterKingFreed() {
        val char = CharacterState(kingLiberated = true, level = 15)
        assertTrue(CoinmasterAccessibility.isAccessible(dimemaster, char))
    }

    @Test
    fun shore_blockedBelowLevel4() {
        assertFalse(CoinmasterAccessibility.isAccessible(shore, CharacterState(level = 3)))
    }

    @Test
    fun shore_accessibleAtLevel4() {
        assertTrue(CoinmasterAccessibility.isAccessible(shore, CharacterState(level = 4)))
    }
}
