package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.IslandWarShopAccessibility
import net.sourceforge.kolmafia.shop.ResidualLegacyShopAccessibility

/**
 * Focused HTTP Residual LI Track A–B coverage (phases 6911–6950).
 * Parent wrap bumps REVISION to phase7330.
 */
class GameRuntimeLibraryPhase6930Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun master(nickname: String, shopId: String? = nickname, buyUrl: String? = null) =
        CoinmasterData(
            masterName = nickname,
            nickname = nickname,
            token = null,
            shopId = shopId,
            buyUrl = buyUrl ?: if (shopId == null) "legacy.php" else null,
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    private fun warHippyCount(id: Int): Int =
        when (id) {
            IslandWarShopAccessibility.WAR_HIPPY_HEADBAND,
            IslandWarShopAccessibility.WAR_HIPPY_CORDS,
            IslandWarShopAccessibility.WAR_HIPPY_GLASSES,
            -> 1
            else -> 0
        }

    private fun warFratCount(id: Int): Int =
        when (id) {
            IslandWarShopAccessibility.WAR_FRAT_HELMET,
            IslandWarShopAccessibility.WAR_FRAT_PANTS,
            IslandWarShopAccessibility.WAR_FRAT_PIN,
            -> 1
            else -> 0
        }

    @Test
    fun revision_isPhase7030() {
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun islandWarShops_gateOnWarProgressAndOutfit() {
        val idle = prefs()
        assertEquals(
            "You're not at war.",
            IslandWarShopAccessibility.dimemasterInaccessible(idle) { 0 },
        )
        assertEquals(
            "You're not at war.",
            IslandWarShopAccessibility.quartersmasterInaccessible(idle) { 0 },
        )

        val atWar = prefs { putString("warProgress", "started") }
        assertEquals(
            "You don't have the War Hippy Fatigues",
            IslandWarShopAccessibility.dimemasterInaccessible(atWar) { 0 },
        )
        assertEquals(
            "You don't have the Frat Warrior Fatigues",
            IslandWarShopAccessibility.quartersmasterInaccessible(atWar) { 0 },
        )

        assertNull(IslandWarShopAccessibility.dimemasterInaccessible(atWar, ::warHippyCount))
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("dimemaster", shopId = null, buyUrl = "bigisland.php"),
                CharacterState(),
                atWar,
                accessibleCount = ::warHippyCount,
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("quartersmaster", shopId = null, buyUrl = "bigisland.php"),
                CharacterState(),
                atWar,
                accessibleCount = ::warFratCount,
            ),
        )
    }

    @Test
    fun residualLegacyShops_gateOnTokenPathFamiliarAndAltar() {
        assertEquals(
            "You don't have any A. W. O. L. commendations",
            ResidualLegacyShopAccessibility.awolInaccessible { 0 },
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("awol", shopId = null),
                CharacterState(),
                accessibleCount = { id ->
                    if (id == ResidualLegacyShopAccessibility.AWOL_COMMENDATION) 1 else 0
                },
            ),
        )

        assertEquals(
            "You haven't rescued Big Brother yet.",
            ResidualLegacyShopAccessibility.bigBrotherInaccessible(prefs(), accessibleCount = { 0 }),
        )
        val rescued = prefs { putBoolean("bigBrotherRescued", true) }
        assertEquals(
            "You don't have the right equipment to adventure underwater.",
            ResidualLegacyShopAccessibility.bigBrotherInaccessible(rescued, accessibleCount = { 0 }),
        )
        assertNull(
            ResidualLegacyShopAccessibility.bigBrotherInaccessible(
                rescued,
                accessibleCount = { id -> if (id == 734 || id == 3470) 1 else 0 },
            ),
        )

        assertEquals(
            "Dino World is not available",
            ResidualLegacyShopAccessibility.dinostaurInaccessible(CharacterState()),
        )
        val dino = CharacterState(challengePath = AscensionPath.DINOSAURS.apiName)
        assertTrue(CoinmasterAccessibility.isAccessible(master("dino"), dino))

        assertEquals(
            "You need some crystalline cheer.",
            ResidualLegacyShopAccessibility.crimbo17Inaccessible { 0 },
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("crimbo17"),
                CharacterState(),
                accessibleCount = { id ->
                    if (id == ResidualLegacyShopAccessibility.CRYSTALLINE_CHEER) 1 else 0
                },
            ),
        )

        assertEquals(
            "You do not have a Skeleton of Crimbo Past",
            ResidualLegacyShopAccessibility.skeletonOfCrimboPastInaccessible { false },
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("socp", shopId = null),
                CharacterState(),
                ownsFamiliar = { id ->
                    id == ResidualLegacyShopAccessibility.SKELETON_OF_CRIMBO_PAST
                },
            ),
        )

        assertEquals(
            "The Altar of Bones is not available",
            ResidualLegacyShopAccessibility.altarOfBonesInaccessible(),
        )
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                master("bonealtar", shopId = null),
                CharacterState(),
            ),
        )
    }
}
