package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP572Test {

    private fun libWithDb(configure: Preferences.() -> Unit = {}): Pair<GameRuntimeLibrary, Preferences> {
        val prefs = Preferences(MapSettings()).also(configure)
        return GameRuntimeLibrary(preferences = prefs, questDatabase = QuestDatabase(prefs)) to prefs
    }

    @Test
    fun revision_phase605() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun apartmentUnlock_setsProgress1() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html><a href="adventure.php?snarfblat=341">Apartment</a></html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=hiddencity",
        )
        assertEquals(1, prefs.getInt("hiddenApartmentProgress", 0))
    }

    @Test
    fun hospitalOfficeBowling_unlock() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """
            <html>
            snarfblat=342 snarfblat=343 snarfblat=344
            </html>
            """.trimIndent(),
            "https://www.kingdomofloathing.com/place.php?whichplace=hiddencity",
        )
        assertEquals(1, prefs.getInt("hiddenHospitalProgress", 0))
        assertEquals(1, prefs.getInt("hiddenOfficeProgress", 0))
        assertEquals(1, prefs.getInt("hiddenBowlingAlleyProgress", 0))
    }

    @Test
    fun hiddenTavern_setsUnlockAscension() {
        val (lib, prefs) = libWithDb()
        lib.processVisitResponseHooks(
            """<html>whichshop=hiddentavern</html>""",
            "https://www.kingdomofloathing.com/place.php?whichplace=hiddencity",
        )
        assertEquals(0, prefs.getInt("hiddenTavernUnlock", -1))
    }
}
