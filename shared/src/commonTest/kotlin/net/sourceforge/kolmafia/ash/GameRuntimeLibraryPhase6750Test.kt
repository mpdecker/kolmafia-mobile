package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.AirportShopAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterData

/**
 * Focused XLVIII Track A coverage (phases 6731–6750).
 * REVISION stays phase6850 until parent wrap.
 */
class GameRuntimeLibraryPhase6750Test {

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
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
        assertEquals("7210", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun airportAccessibility_hotColdSpookyStench() {
        val locked = prefs()
        assertEquals(
            "You don't have access to That 70s Volcano",
            AirportShopAccessibility.hotInaccessible(locked),
        )
        assertEquals(
            "You don't have access to The Glaciest",
            AirportShopAccessibility.coldInaccessible(locked),
        )
        assertEquals(
            "You don't have access to Conspiracy Island",
            AirportShopAccessibility.spookyInaccessible(locked),
        )
        assertEquals(
            "You don't have access to Dinseylandfill",
            AirportShopAccessibility.stenchInaccessible(locked),
        )
        assertFalse(AirportShopAccessibility.anyAirport(locked))
        assertEquals(
            "You cannot access the Elemental Airport",
            AirportShopAccessibility.dutyFreeInaccessible(locked),
        )

        val open = prefs {
            putBoolean("hotAirportAlways", true)
            putBoolean("coldAirportAlways", true)
            putBoolean("spookyAirportAlways", true)
            putBoolean("stenchAirportAlways", true)
        }
        assertNull(AirportShopAccessibility.hotInaccessible(open))
        assertNull(AirportShopAccessibility.coldInaccessible(open))
        assertNull(AirportShopAccessibility.spookyInaccessible(open))
        assertNull(AirportShopAccessibility.stenchInaccessible(open))
        assertTrue(AirportShopAccessibility.anyAirport(open))
        assertNull(AirportShopAccessibility.dutyFreeInaccessible(open))
    }

    @Test
    fun coinmasterRuleFor_airportNicknames() {
        val locked = prefs()
        assertFalse(
            CoinmasterAccessibility.isAccessible(master("infernodisco"), CharacterState(), locked),
        )
        assertFalse(
            CoinmasterAccessibility.isAccessible(master("airport"), CharacterState(), locked),
        )
        assertFalse(
            CoinmasterAccessibility.isAccessible(master("si_shop3"), CharacterState(), locked),
        )

        val open = prefs {
            putBoolean("_hotAirportToday", true)
            putBoolean("spookyAirportAlways", true)
            putBoolean("armoryUnlocked", true)
            putBoolean("sleazeAirportAlways", true)
        }
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("discogiftco"), CharacterState(), open),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("airport"), CharacterState(), open),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("thearmory"), CharacterState(), open),
        )
        // Bare armory (Armory & Leggery) stays ungated by airport prefs.
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("armory"), CharacterState(), locked),
        )
    }
}
