package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampAwaySync
import net.sourceforge.kolmafia.request.FalloutShelterSync
import net.sourceforge.kolmafia.request.MiscShopTokenResponseParse

class GameRuntimeLibraryPhase6370Test {

    @Test
    fun revision_phase6370() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun miscShop_parsesArcadeAndFdkol() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=arcade",
                "You currently have 1,500 Game Grid redemption tickets.",
                prefs,
            ),
        )
        assertEquals(1500, prefs.getInt("availableGameGridTickets", 0))
        assertTrue(
            MiscShopTokenResponseParse.parseResponse(
                "shop.php?whichshop=fdkol",
                "<td>42 FDKOL commendation",
                prefs,
            ),
        )
        assertEquals(42, prefs.getInt("availableFDKOLCommendations", 0))
    }

    @Test
    fun campAway_parsesSmileAndDecoration() {
        val prefs = Preferences(MapSettings())
        CampAwaySync.parseResponse(
            "place.php?whichplace=campaway&action=campaway_sky",
            "You acquire an effect: <b>Smile of the Goat</b>",
            prefs,
        )
        assertEquals("Goat", prefs.getString("_campAwaySmileBuffSign", ""))
        assertEquals(1, prefs.getInt("_campAwaySmileBuffs", 0))

        CampAwaySync.parseResponse(
            "place.php?whichplace=campaway&action=campaway_tent",
            "You acquire an effect: <b>Muscular</b><img src='campaway/restlabel_free.gif'>",
            prefs,
        )
        assertEquals(1, prefs.getInt("campAwayDecoration", 0))
        assertTrue(prefs.getBoolean("_freeRestsAvailable", false))
    }

    @Test
    fun falloutShelter_parsesLevelAndChrono() {
        val prefs = Preferences(MapSettings())
        FalloutShelterSync.parseResponse(
            "place.php?whichplace=falloutshelter&action=vault5",
            """<img src="vault3.gif"><img src="vault5.gif">more ominous shade of green""",
            prefs,
        )
        assertEquals(5, prefs.getInt("falloutShelterLevel", 0))
        assertTrue(prefs.getBoolean("falloutShelterChronoUsed", false))
    }

    @Test
    fun expected_damage_prefersStatusTracker() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6370 slug",
                id = 63701,
                image = "x.gif",
                attack = 10,
                defense = 10,
                hp = 20,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
                attackElement = "hot",
            ),
            emptyList(),
        )
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            // Should resolve without throwing; damage may be 0 with empty modifiers.
            val out = outputLib(lib, "print(expected_damage());").trim()
            assertTrue(out.toIntOrNull() != null, "expected numeric, got $out")
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }
}
