package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP631Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cocktail_incrementsFromSauceBottle() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("tacoDanCocktailSauce", 2)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "Sloppy Seconds Cocktail",
                "You smash the cocktail sauce bottle",
                db,
                prefs,
            ),
        )
        assertEquals(3, prefs.getInt("tacoDanCocktailSauce"))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.TACO_DAN_COCKTAIL))
    }

    @Test
    fun cocktail_finishesFromDefeatedBottle() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("tacoDanCocktailSauce", 14)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "Sloppy Seconds Cocktail",
                "You defeated foe with your bottle",
                db,
                prefs,
            ),
        )
        assertEquals(15, prefs.getInt("tacoDanCocktailSauce"))
        assertEquals("step1", db.getProgress(Quest.TACO_DAN_COCKTAIL))
    }

    @Test
    fun cocktail_withoutBottleIsNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            AirportCombatSync.apply("Sloppy Seconds Cocktail", "You win the fight", db, prefs),
        )
        assertEquals(0, prefs.getInt("tacoDanCocktailSauce", 0))
    }

    @Test
    fun sundae_incrementsSprinkles() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("brodenSprinkles", 5)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "Sloppy Seconds Sundae",
                "You knock the sprinkles off",
                db,
                prefs,
            ),
        )
        assertEquals(6, prefs.getInt("brodenSprinkles"))
    }

    @Test
    fun sundae_finishesAtFifteen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("brodenSprinkles", 14)
        val db = QuestDatabase(prefs)
        assertTrue(
            AirportCombatSync.apply(
                "Sloppy Seconds Sundae",
                "You knock the sprinkles off",
                db,
                prefs,
            ),
        )
        assertEquals(15, prefs.getInt("brodenSprinkles"))
        assertEquals("step1", db.getProgress(Quest.BRODEN_SPRINKLES))
    }
}
