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
import net.sourceforge.kolmafia.shop.LegacyCoinmasterAccessibility
import net.sourceforge.kolmafia.shop.SpecialtyShopAccessibility

/**
 * Focused HTTP Residual L Track A–B coverage (phases 6851–6890).
 * Parent wrap bumps REVISION to phase6910.
 */
class GameRuntimeLibraryPhase6870Test {

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
    fun revision_isPhase6910() {
        assertEquals("phase6910", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun specialtyShops_gateOnPrefsPathAndInventory() {
        val locked = prefs()
        assertEquals(
            "You need to be at The Shadow Forge to make that.",
            SpecialtyShopAccessibility.shadowForgeInaccessible(CharacterState(currentRun = 10), locked),
        )
        val atForge = prefs { putInt("lastShadowForgeUnlockAdventure", 10) }
        assertNull(
            SpecialtyShopAccessibility.shadowForgeInaccessible(CharacterState(currentRun = 10), atForge),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("shadowforge"),
                CharacterState(currentRun = 10),
                atForge,
            ),
        )

        assertEquals(
            "Need access to Fantasy Realm",
            SpecialtyShopAccessibility.fantasyRealmInaccessible(locked),
        )
        val fr = prefs { putBoolean("frAlways", true) }
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("fantasyrealm"), CharacterState(), fr),
        )

        assertEquals(
            "You can't access the server room.",
            SpecialtyShopAccessibility.dedigitizerInaccessible(locked),
        )
        val cyber = prefs { putBoolean("_crToday", true) }
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("cyber_dedigitizer"), CharacterState(), cyber),
        )

        assertEquals(
            "You can't buy with sand pennies outside 11,037 Leagues Under the Sea",
            SpecialtyShopAccessibility.sandPennyInaccessible(CharacterState()),
        )
        val sea = CharacterState(challengePath = AscensionPath.UNDER_THE_SEA.apiName)
        assertTrue(CoinmasterAccessibility.isAccessible(master("sandpenny"), sea))

        assertEquals(
            "You do not have a topiary nugglet in inventory",
            SpecialtyShopAccessibility.topiaryInaccessible { 0 },
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("topiary"),
                CharacterState(),
                accessibleCount = { id ->
                    if (id == SpecialtyShopAccessibility.TOPIARY_NUGGLET) 1 else 0
                },
            ),
        )
    }

    @Test
    fun legacyCoinmasters_gateOnCatalogTokenAndMode() {
        assertEquals(
            "You need a 2002 Mr. Store Catalog in order to shop here.",
            LegacyCoinmasterAccessibility.mrStore2002Inaccessible(CharacterState()) { 0 },
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("mrstore2002"),
                CharacterState(),
                accessibleCount = { id ->
                    if (id == LegacyCoinmasterAccessibility.MR_STORE_2002_CATALOG) 1 else 0
                },
            ),
        )
        val lol = CharacterState(challengePath = AscensionPath.LEGACY_OF_LOATHING.apiName)
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("mrstore2002"),
                lol,
                accessibleCount = { id ->
                    if (id == LegacyCoinmasterAccessibility.REPLICA_MR_STORE_2002_CATALOG) 1 else 0
                },
            ),
        )
        assertFalse(
            CoinmasterAccessibility.isAccessible(
                master("mrstore2002"),
                CharacterState(),
                accessibleCount = { id ->
                    if (id == LegacyCoinmasterAccessibility.REPLICA_MR_STORE_2002_CATALOG) 1 else 0
                },
            ),
        )

        assertEquals(
            "You don't have any BURTs",
            LegacyCoinmasterAccessibility.burtInaccessible { 0 },
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("burt"),
                CharacterState(),
                accessibleCount = { id -> if (id == LegacyCoinmasterAccessibility.BURT) 1 else 0 },
            ),
        )

        assertEquals(
            "You don't have a wand of fudge control",
            LegacyCoinmasterAccessibility.fudgeWandInaccessible { 0 },
        )
        assertEquals(
            "You don't have any fudgecules",
            LegacyCoinmasterAccessibility.fudgeWandInaccessible { id ->
                if (id == LegacyCoinmasterAccessibility.FUDGE_WAND) 1 else 0
            },
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("fudge"),
                CharacterState(),
                accessibleCount = { id ->
                    when (id) {
                        LegacyCoinmasterAccessibility.FUDGE_WAND,
                        LegacyCoinmasterAccessibility.FUDGECULE,
                        -> 1
                        else -> 0
                    }
                },
            ),
        )

        val koe = CharacterState(challengePath = AscensionPath.KINGDOM_OF_EXPLOATHING.apiName)
        assertEquals(
            "The Hermitage exploded",
            LegacyCoinmasterAccessibility.hermitInaccessible(koe),
        )
        assertTrue(CoinmasterAccessibility.isAccessible(master("hermit"), CharacterState()))

        assertEquals(
            "Characters in Hardcore or Ronin cannot redeem Game Shoppe credit.",
            LegacyCoinmasterAccessibility.gameShoppeInaccessible(CharacterState(isHardcore = true)),
        )
        assertTrue(CoinmasterAccessibility.isAccessible(master("gameshoppe"), CharacterState()))
    }
}
