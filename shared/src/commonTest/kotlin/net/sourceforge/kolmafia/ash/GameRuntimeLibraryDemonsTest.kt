package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.DemonInCombatNameSync
import net.sourceforge.kolmafia.session.DemonNamesManager

class GameRuntimeLibraryDemonsTest {

    private fun lib(prefs: Preferences = Preferences(MapSettings())): GameRuntimeLibrary {
        val segmentSync = DemonInCombatNameSync(prefs)
        return GameRuntimeLibrary(
            preferences = prefs,
            demonInCombatNameSync = segmentSync,
            demonNamesManager = DemonNamesManager(prefs, segmentSync),
        )
    }

    @Test
    fun demons_listsKnownNames() {
        val prefs = Preferences(MapSettings())
        prefs.setString("demonName7", "Ak'gyxoth")
        val out = outputLib(lib(prefs), """cli_execute("demons");""")
        assertTrue(out.contains("7: Ak'gyxoth"))
        assertTrue(out.contains(" => Gives pile of smoking rags"))
    }

    @Test
    fun demons_solve14_withoutSegments() {
        val out = outputLib(lib(), """cli_execute("demons solve14");""")
        assertTrue(out.contains("Allied Radio Backpack"))
    }

    @Test
    fun demons_solve14_findsSolution() {
        val prefs = Preferences(MapSettings())
        prefs.setString(
            Preferences.DEMON_NAME_14_SEGMENTS,
            "Mor,rNi,Nix,xAr,Arg,gPh,Pha,aDa,Dar,arH,Hut,utR,Rog,ogB,gBa,alK,Kru",
        )
        val out = outputLib(lib(prefs), """cli_execute("demons solve14");""")
        assertTrue(out.contains("MorNixArgPhaDarHutRogBalKru"))
    }
}
