package net.sourceforge.kolmafia.familiar

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class FamiliarSyncTest {

    private fun manager(prefs: Preferences = Preferences(MapSettings())): FamiliarManager {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        return FamiliarManager(client, GameEventBus(), prefs)
    }

    @Test
    fun newfam_setsActiveFamiliar() {
        val prefs = Preferences(MapSettings())
        val mgr = manager(prefs)
        mgr.testSetState(
            FamiliarState(
                ownedFamiliars = listOf(
                    FamiliarData(1, "Elvis", "Leprechaun", 20, 100, 0),
                    FamiliarData(2, "Sucky", "Baby Gravy Fairy", 5, 10, 0),
                ),
            ),
        )
        val ok = FamiliarSync.parseResponse(
            url = "familiar.php?action=newfam&whichfam=2",
            html = "You put Elvis back in the Terrarium. You take Sucky with you.",
            familiarManager = mgr,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(2, mgr.state.value.activeFamiliar?.id)
        assertEquals(2, prefs.getInt("activeFamiliarId", 0))
        assertEquals("Baby Gravy Fairy", prefs.getString("activeFamiliarRace", ""))
    }

    @Test
    fun putback_clearsActiveFamiliar() {
        val prefs = Preferences(MapSettings())
        val mgr = manager(prefs)
        mgr.testSetState(
            FamiliarState(
                activeFamiliar = FamiliarData(2, "Sucky", "Baby Gravy Fairy", 5, 10, 0),
                ownedFamiliars = listOf(
                    FamiliarData(2, "Sucky", "Baby Gravy Fairy", 5, 10, 0),
                ),
            ),
        )
        val ok = FamiliarSync.parseResponse(
            url = "familiar.php?action=putback",
            html = "You put Sucky back in the Terrarium.",
            familiarManager = mgr,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(null, mgr.state.value.activeFamiliar)
        assertEquals(0, prefs.getInt("activeFamiliarId", -1))
    }

    @Test
    fun equip_setsFamiliarItem() {
        val prefs = Preferences(MapSettings())
        val mgr = manager(prefs)
        mgr.testSetState(
            FamiliarState(
                activeFamiliar = FamiliarData(2, "Sucky", "Baby Gravy Fairy", 5, 10, 0),
                ownedFamiliars = listOf(
                    FamiliarData(2, "Sucky", "Baby Gravy Fairy", 5, 10, 0),
                ),
            ),
        )
        val ok = FamiliarSync.parseResponse(
            url = "familiar.php?action=equip&whichfam=2&whichitem=865",
            html = "You equip Sucky with the time sword.",
            familiarManager = mgr,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(865, mgr.state.value.activeFamiliar?.equipment?.itemId)
        assertEquals(865, prefs.getInt("familiarItemId", 0))
    }

    @Test
    fun lockequip_togglesPref() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("familiarEquipmentLocked", false)
        assertTrue(
            FamiliarSync.parseResponse(
                url = "familiar.php?action=lockequip",
                html = "Familiar equipment locked.",
                familiarManager = null,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("familiarEquipmentLocked", false))
    }

    @Test
    fun weightXp_updatesPrefs() {
        val prefs = Preferences(MapSettings())
        FamiliarSync.parseWeightXp(
            html = "Your familiar weighs 22 lbs and has 440 xp.",
            familiarManager = null,
            preferences = prefs,
        )
        assertEquals(22, prefs.getInt("familiarWeight", 0))
        assertEquals(440, prefs.getInt("familiarExperience", 0))
    }
}
