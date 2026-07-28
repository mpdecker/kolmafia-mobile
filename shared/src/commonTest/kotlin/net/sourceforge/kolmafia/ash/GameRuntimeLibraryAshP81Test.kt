package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.CultShortsDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CargoPocketSync
import net.sourceforge.kolmafia.session.YegDemonNameSync

class GameRuntimeLibraryAshP81Test {

    @Test
    fun revision_phase141() {
        assertEquals("phase190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun picked_pockets_returnsEmptiedSet() {
        val prefs = Preferences(MapSettings())
        val pocketSync = CargoPocketSync(prefs, YegDemonNameSync(prefs))
        pocketSync.parsePocketPick(7, "ok")
        pocketSync.parsePocketPick(373, "ok")
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            cargoPocketSync = pocketSync,
            yegDemonNameSync = YegDemonNameSync(prefs),
        )
        assertEquals("2", outputLib(lib, """print(count(picked_pockets()));"""))
    }

    @Test
    fun picked_scraps_returnsKnownScrapKeys() {
        CultShortsDatabase.resetForTest()
        val prefs = Preferences(MapSettings())
        val yeg = YegDemonNameSync(prefs)
        yeg.saveScrapPockets(mapOf(373 to "Ga", 7 to "Go"))
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            yegDemonNameSync = yeg,
        )
        assertEquals("2", outputLib(lib, """print(count(picked_scraps()));"""))
    }
}
