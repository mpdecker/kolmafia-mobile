package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.SwaggerShopSync

class GameRuntimeLibraryAshP723Test {

    @BeforeTest
    fun reset() {
        CoinmasterVisitInventory.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
    }

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_setsAvailableSwaggerFromYouHave() {
        val prefs = Preferences(MapSettings())
        val html = """
            You have 250 swagger
            You've earned 600 swagger during a pirate season, yarrr.
            <input type="hidden" name="whichitem" value="7732" />
            <input type="submit" value="Buy (1000 swagger)" />
        """.trimIndent()
        SwaggerShopSync.applyVisitShop(
            html = html,
            url = "peevpee.php?place=shop",
            prefs = prefs,
            sessionLogger = null,
            state = null,
        )
        assertEquals(250, prefs.getInt("availableSwagger", 0))
        assertEquals("pirate", prefs.getString("currentPVPSeason", ""))
        assertEquals(600, prefs.getInt("pirateSwagger", 0))
    }

    @Test
    fun visit_noneSeasonDoesNotOverwriteToken() {
        val prefs = Preferences(MapSettings())
        val html = """
            You have 80 swagger
            You've earned 12 swagger during a none season
        """.trimIndent()
        SwaggerShopSync.applyVisitShop(
            html = html,
            url = "peevpee.php?place=shop",
            prefs = prefs,
            sessionLogger = null,
            state = null,
        )
        assertEquals(80, prefs.getInt("availableSwagger", 0))
        assertEquals("none", prefs.getString("currentPVPSeason", ""))
    }

    @Test
    fun visit_unknownSeasonLogs() {
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        SwaggerShopSync.applyVisitShop(
            html = "You've earned 3 swagger during a foobar season",
            url = "peevpee.php?place=shop",
            prefs = prefs,
            sessionLogger = logger,
            state = null,
        )
        assertEquals("foobar", prefs.getString("currentPVPSeason", ""))
        assertTrue(prefs.getString(SessionLogger.SESSION_LOG_KEY, "").contains("*** Unknown PVP season: foobar"))
    }
}
