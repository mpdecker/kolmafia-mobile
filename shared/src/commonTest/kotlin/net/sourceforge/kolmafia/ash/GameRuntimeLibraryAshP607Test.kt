package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SeaVisitSync

class GameRuntimeLibraryAshP607Test {

    @Test
    fun revision_phase607() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun seafloor_outpost_setsStep6AndMaps() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromVisit(
                url = "seafloor.php",
                html = "outpost mine trench divebar reef skatepark currents corral",
                questDatabase = db,
                preferences = prefs,
                isMuscleClass = false,
                isMysticalityClass = false,
                isMoxieClass = false,
            ),
        )
        assertEquals("step6", db.getProgress(Quest.SEA_MONKEES))
        assertTrue(prefs.getBoolean("mapToAnemoneMinePurchased", false))
        assertTrue(prefs.getBoolean("mapToTheMarinaraTrenchPurchased", false))
        assertTrue(prefs.getBoolean("mapToTheDiveBarPurchased", false))
        assertTrue(prefs.getBoolean("mapToMadnessReefPurchased", false))
        assertTrue(prefs.getBoolean("mapToTheSkateParkPurchased", false))
        assertTrue(prefs.getBoolean("intenseCurrents", false))
        assertTrue(prefs.getBoolean("corralUnlocked", false))
    }

    @Test
    fun seafloor_muscleClassAfterStep4_skipsMineMap() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.SEA_MONKEES, "step4")
        SeaVisitSync.applyFromVisit(
            url = "seafloor.php",
            html = "mine",
            questDatabase = db,
            preferences = prefs,
            isMuscleClass = true,
        )
        assertFalse(prefs.getBoolean("mapToAnemoneMinePurchased", false))
    }

    @Test
    fun monkeycastle_bigBrother_setsStep2() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromVisit(
                url = "monkeycastle.php",
                html = """<a href="monkeycastle.php?who=2">Big Brother</a>""",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("step2", db.getProgress(Quest.SEA_MONKEES))
        assertTrue(prefs.getBoolean("bigBrotherRescued", false))
    }

    @Test
    fun seaMerkin_parsesSeahorseName() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            SeaVisitSync.applyFromVisit(
                url = "sea_merkin.php",
                html = "atop your trusty seahorse <b>Shimmerswim</b>, you crest",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("Shimmerswim", prefs.getString("seahorseName", ""))
    }
}
