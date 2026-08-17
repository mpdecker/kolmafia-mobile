package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.HiddenCityChoiceSync
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP603Test {

    @Test
    fun apartment_decision4_setsCandyCane() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityChoiceSync.applyPostChoice(
                choiceId = 780,
                html = "",
                decision = 4,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("candyCaneSwordApartmentBuilding", false))
    }

    @Test
    fun office_decision4_setsOvergrownShrine() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityChoiceSync.applyPostChoice(
                choiceId = 785,
                html = "",
                decision = 4,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_candyCaneSwordOvergrownShrine", false))
    }

    @Test
    fun bowling_decision2_setsCandyCane() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("hiddenBowlingAlleyProgress", 6)
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityChoiceSync.applyPostChoice(
                choiceId = 788,
                html = "",
                decision = 2,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("candyCaneSwordBowlingAlley", false))
    }
}
