package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

class AirportShopAccessibilityTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

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
    fun hot_requiresAirportPref() {
        val p = prefs()
        assertEquals(
            "You don't have access to That 70s Volcano",
            AirportShopAccessibility.hotInaccessible(p),
        )
        p.setBoolean("_hotAirportToday", true)
        assertNull(AirportShopAccessibility.hotInaccessible(p))
    }

    @Test
    fun cold_and_stench_messages() {
        val p = prefs()
        assertEquals(
            "You don't have access to The Glaciest",
            AirportShopAccessibility.coldInaccessible(p),
        )
        assertEquals(
            "You don't have access to Dinseylandfill",
            AirportShopAccessibility.stenchInaccessible(p),
        )
        p.setBoolean("coldAirportAlways", true)
        p.setBoolean("stenchAirportAlways", true)
        assertNull(AirportShopAccessibility.coldInaccessible(p))
        assertNull(AirportShopAccessibility.stenchInaccessible(p))
    }

    @Test
    fun spooky_and_si_unlock_gates() {
        val p = prefs()
        assertEquals(
            "You don't have access to Conspiracy Island",
            AirportShopAccessibility.spookyInaccessible(p),
        )
        p.setBoolean("spookyAirportAlways", true)
        assertEquals(
            "SHAWARMA Initiative is locked",
            AirportShopAccessibility.shawarmaInaccessible(p),
        )
        assertEquals(
            "The Canteen is locked",
            AirportShopAccessibility.canteenInaccessible(p),
        )
        assertEquals(
            "The Armory is locked",
            AirportShopAccessibility.spacegateArmoryInaccessible(p),
        )
        p.setBoolean("SHAWARMAInitiativeUnlocked", true)
        p.setBoolean("canteenUnlocked", true)
        p.setBoolean("armoryUnlocked", true)
        assertNull(AirportShopAccessibility.shawarmaInaccessible(p))
        assertNull(AirportShopAccessibility.canteenInaccessible(p))
        assertNull(AirportShopAccessibility.spacegateArmoryInaccessible(p))
    }

    @Test
    fun anyAirport_and_dutyFree() {
        val p = prefs()
        assertFalse(AirportShopAccessibility.anyAirport(p))
        assertEquals(
            "You cannot access the Elemental Airport",
            AirportShopAccessibility.dutyFreeInaccessible(p),
        )
        p.setBoolean("_sleazeAirportToday", true)
        assertTrue(AirportShopAccessibility.anyAirport(p))
        assertNull(AirportShopAccessibility.dutyFreeInaccessible(p))
    }

    @Test
    fun limitZone_blocksWhenBatman() {
        val p = prefs().also { it.setBoolean("hotAirportAlways", true) }
        assertNull(AirportShopAccessibility.hotInaccessible(p, limitMode = ""))
        assertEquals(
            "You cannot currently access That 70s Volcano",
            AirportShopAccessibility.hotInaccessible(p, limitMode = "batman"),
        )
    }

    @Test
    fun coinmasterWiring_infernodiscoAndAliases() {
        val locked = prefs()
        assertFalse(
            CoinmasterAccessibility.isAccessible(master("infernodisco"), CharacterState(), locked),
        )
        assertFalse(
            CoinmasterAccessibility.isAccessible(master("discogiftco"), CharacterState(), locked),
        )
        val open = prefs { putBoolean("hotAirportAlways", true) }
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("infernodisco"), CharacterState(), open),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("glaciest"), CharacterState(), prefs {
                putBoolean("coldAirportAlways", true)
            }),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(master("walmart"), CharacterState(), prefs {
                putBoolean("_coldAirportToday", true)
            }),
        )
    }

    @Test
    fun coinmasterWiring_siShopsAndThearmory_notBareArmory() {
        val p = prefs {
            putBoolean("spookyAirportAlways", true)
            putBoolean("SHAWARMAInitiativeUnlocked", true)
            putBoolean("canteenUnlocked", true)
            putBoolean("armoryUnlocked", true)
        }
        assertTrue(CoinmasterAccessibility.isAccessible(master("si_shop1"), CharacterState(), p))
        assertTrue(CoinmasterAccessibility.isAccessible(master("shawarma"), CharacterState(), p))
        assertTrue(CoinmasterAccessibility.isAccessible(master("si_shop2"), CharacterState(), p))
        assertTrue(CoinmasterAccessibility.isAccessible(master("canteen"), CharacterState(), p))
        assertTrue(CoinmasterAccessibility.isAccessible(master("si_shop3"), CharacterState(), p))
        assertTrue(CoinmasterAccessibility.isAccessible(master("thearmory"), CharacterState(), p))
        // Armory & Leggery shopId/nickname "armory" must NOT get SI airport gates.
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("armory", shopId = "armory"),
                CharacterState(),
                prefs(),
            ),
        )
    }

    @Test
    fun coinmasterWiring_airportDutyFree() {
        assertFalse(
            CoinmasterAccessibility.isAccessible(master("airport"), CharacterState(), prefs()),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("airport"),
                CharacterState(),
                prefs { putBoolean("stenchAirportAlways", true) },
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("landfillstore"),
                CharacterState(),
                prefs { putBoolean("_stenchAirportToday", true) },
            ),
        )
        assertTrue(
            CoinmasterAccessibility.isAccessible(
                master("dinseystore"),
                CharacterState(),
                prefs { putBoolean("stenchAirportAlways", true) },
            ),
        )
    }
}
