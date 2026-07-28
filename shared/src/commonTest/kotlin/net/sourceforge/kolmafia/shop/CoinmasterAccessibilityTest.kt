package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class CoinmasterAccessibilityTest {

    private fun prefs(unlock: Int = -1): Preferences {
        val p = Preferences(MapSettings())
        p.setInt("lastDesertUnlock", unlock)
        return p
    }

    private fun jarlsberg(): CoinmasterData =
        CoinmasterData(
            masterName = "Jarlsberg's Cosmic Kitchen",
            nickname = "jarl",
            token = null,
            shopId = "jarl",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun swagger(): CoinmasterData =
        CoinmasterData(
            masterName = "The Swagger Shop",
            nickname = "swagger",
            token = "swagger",
            shopId = "swagger",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun replica(): CoinmasterData =
        CoinmasterData(
            masterName = "Replica Mr. Store",
            nickname = "mrreplica",
            token = "replica Mr. Accessory",
            shopId = "mrreplica",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun shore(): CoinmasterData =
        CoinmasterData(
            masterName = "The Shore, Inc. Gift Shop",
            nickname = "shore",
            token = "Shore Inc. Ship Trip Scrip",
            shopId = "shore",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun mystic(): CoinmasterData =
        CoinmasterData(
            masterName = "The Crackpot Mystic's Shed",
            nickname = "mystic",
            token = null,
            shopId = "mystic",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    @Test
    fun replica_requiresLegacyOfLoathing() {
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                replica(),
                CharacterState(challengePath = AscensionPath.STANDARD.apiName),
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                replica(),
                CharacterState(challengePath = AscensionPath.LEGACY_OF_LOATHING.apiName),
            ),
        )
    }

    @Test
    fun mystic_blockedDuringKingdomOfExploathing() {
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                mystic(),
                CharacterState(
                    level = 10,
                    challengePath = AscensionPath.KINGDOM_OF_EXPLOATHING.apiName,
                ),
            ),
        )
    }

    @Test
    fun shore_requiresDesertBeachUnlock() {
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                shore(),
                CharacterState(level = 5, ascensionNumber = 3),
                prefs(unlock = -1),
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                shore(),
                CharacterState(level = 5, ascensionNumber = 3),
                prefs(unlock = 3),
            ),
        )
    }

    @Test
    fun jarlsberg_requiresAvatarOfJarlsbergPath() {
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                jarlsberg(),
                CharacterState(challengePath = AscensionPath.STANDARD.apiName),
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                jarlsberg(),
                CharacterState(challengePath = AscensionPath.AVATAR_OF_JARLSBERG.apiName),
            ),
        )
    }

    @Test
    fun swagger_blockedDuringHardcoreOrRonin() {
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                swagger(),
                CharacterState(isHardcore = true),
            ),
        )
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                swagger(),
                CharacterState(roninLeft = 1),
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                swagger(),
                CharacterState(),
            ),
        )
    }

    @Test
    fun fiveDPrinter_requiresOwnedPrinter() {
        val master = CoinmasterData(
            masterName = "Xiblaxian 5D printer",
            nickname = "5dprinter",
            token = null,
            shopId = "5dprinter",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                master,
                CharacterState(),
                accessibleCount = { 0 },
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master,
                CharacterState(),
                accessibleCount = { if (it == 7750) 1 else 0 },
            ),
        )
    }
}
