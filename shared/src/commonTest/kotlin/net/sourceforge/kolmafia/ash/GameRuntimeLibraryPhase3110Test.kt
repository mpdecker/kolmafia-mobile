package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryPhase3110Test {
    @Test
    fun spelunkyAshGettersReadCanonicalStatus() {
        val preferences = Preferences(MapSettings()).apply {
            setString(
                "spelunkyStatus",
                "Turns: 11, Gold: 222, Bombs: 3, Ropes: 2, Keys: 1, Buddy: Skeleton, Unlocks: Jungle",
            )
        }
        val lib = GameRuntimeLibrary(preferences = preferences)
        assertEquals("222", outputLib(lib, "print(spelunky_gold());"))
        assertEquals("Skeleton", outputLib(lib, "print(spelunky_buddy());"))
    }

    @Test
    fun bastilleCliReportsAdviceAndTestSummary() {
        val preferences = Preferences(MapSettings()).apply {
            setString("_bastilleStats", "MA=8,MD=1,CA=7,CD=1,PA=6,PD=1")
            setString("_bastilleEnemyCastle", "masterofnone")
            setInt("_bastilleGameTurn", 3)
        }
        val lib = GameRuntimeLibrary(preferences = preferences)
        val advice = outputLib(lib, """cli_execute("bastille advise");""")
        assertTrue(advice.contains("option 1"))
        val summary = outputLib(lib, """cli_execute("test bastille");""")
        assertTrue(summary.contains("12 scaling cheese formulae"))
    }

    @Test
    fun revisionIsPhase3110() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }
}
