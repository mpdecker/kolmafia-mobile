package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ApiRequest
import net.sourceforge.kolmafia.request.ClipArtRequestHub
import net.sourceforge.kolmafia.request.MeteoroidRequest
import net.sourceforge.kolmafia.request.SugarSheetRequestHub
import net.sourceforge.kolmafia.request.WaxGlobRequest
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

class GameRuntimeLibraryPhase6190Test {

    @Test
    fun revision_phase6310() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun apiRequest_routesItemAndRegisters() {
        assertEquals("item", ApiRequest.whatFromUrl("api.php?what=item&id=123"))
        assertEquals(123, ApiRequest.idFromUrl("api.php?what=item&id=123"))
        assertTrue(
            ApiRequest.parseItem(
                """{"name":"phase6190 widget","descid":"d6190","plural":"phase6190 widgets"}""",
                61901,
            ),
        )
        assertEquals("phase6190 widget", ItemDatabase.getItemName(61901))
        assertEquals("phase6190 widgets", ItemDatabase.getById(61901)?.plural)
    }

    @Test
    fun sugarSheet_parseResponse() {
        val prefs = Preferences(MapSettings())
        SugarSheetRequestHub.parseResponse(
            "shop.php?whichshop=sugarsheets",
            "You have 7 sugar sheets.",
            prefs,
        )
        assertEquals(7, prefs.getInt("availableSugarSheets", 0))
    }

    @Test
    fun waxGlob_andMeteoroid_parseResponse() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            WaxGlobRequest.parseResponse(
                "choice.php?whichchoice=1218&option=2",
                "You acquire an item: <b>wax hand</b>",
                prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_waxGlobCrafted", false))
        assertTrue(
            MeteoroidRequest.parseResponse(
                "choice.php?whichchoice=1264&option=1",
                "You acquire an item: <b>meteoroid gear</b>",
                prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_meteoroidCrafted", false))
    }

    @Test
    fun clipArt_parseResponse_increments() {
        val prefs = Preferences(MapSettings())
        ClipArtRequestHub.parseResponse(
            "campground.php?action=bookshelf&preaction=summonclipart",
            "You acquire an item from clip art",
            prefs,
        )
        assertEquals(1, prefs.getInt("_clipartSummons", 0))
    }

    @Test
    fun form_fields_decodesQuery() {
        ChoiceCombatAshState.reset()
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        lib.lastVisitPath = "choice.php?whichchoice=1&pwd=ab%2Bcd"
        assertEquals(
            "ab+cd",
            outputLib(
                lib,
                """
                string [string] f = form_fields();
                print(f["pwd"]);
                """.trimIndent(),
            ).trim(),
        )
    }

    @Test
    fun to_plural_int_overload() {
        ItemDatabase.registerItem(61902, "phase6190 doodad", "d61902", "phase6190 doodads")
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
        assertEquals(
            "phase6190 doodads",
            outputLib(lib, "print(to_plural(61902));").trim(),
        )
    }
}
