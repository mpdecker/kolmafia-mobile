package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP560Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun asdonmartin_status_printsFuel() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(CampgroundItemSync.ASDON_MARTIN_FUEL_PREF, 100)
        val out = outputLib(
            GameRuntimeLibrary(preferences = prefs),
            """cli_execute("asdonmartin status");""",
        )
        assertTrue(out.contains("100"))
        assertTrue(out.contains("fuel", ignoreCase = true))
        assertTrue(out.contains("Drive style", ignoreCase = true))
    }

    @Test
    fun asdonmartin_clear_whenNotDriving() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """cli_execute("asdonmartin clear");""",
        )
        assertTrue(out.contains("not currently driving", ignoreCase = true))
    }

    @Test
    fun asdonmartin_fuel_postsConvertor() {
        var seenAction: String? = null
        var seenIid: String? = null
        var seenQty: String? = null
        val client = HttpClient(MockEngine { request ->
            val form = request.body as FormDataContent
            seenAction = form.formData["action"]
            seenIid = form.formData["iid"]
            seenQty = form.formData["qty"]
            respond("ok")
        })
        val db = object : GameDatabase() {
            override fun item(name: String) = ItemData(
                id = 100,
                name = name,
                descId = "",
                image = "",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            )
        }
        outputLib(
            GameRuntimeLibrary(httpClient = client, gameDatabase = db),
            """cli_execute("asdonmartin fuel 2 toast");""",
        )
        assertEquals("fuelconvertor", seenAction)
        assertEquals("100", seenIid)
        assertEquals("2", seenQty)
    }
}
