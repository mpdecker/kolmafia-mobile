package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class FightFamiliarMessageSyncTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
    }

    @Test
    fun commerceGhostQuestSetsItem() {
        assertTrue(
            FightFamiliarMessageSync.applyGhostOfCommerce(
                "Better get an antique suitcase while there's still some left!",
                prefs,
            ),
        )
        assertEquals("antique suitcase", prefs.getString("commerceGhostItem", ""))
        assertEquals(10, prefs.getInt("commerceGhostCombats", 0))
    }

    @Test
    fun commerceGhostCompleteClears() {
        prefs.setString("commerceGhostItem", "widget")
        prefs.setInt("commerceGhostCombats", 10)
        FightFamiliarMessageSync.applyGhostOfCommerce(
            "Nice, you bought a widget!",
            prefs,
        )
        assertEquals("", prefs.getString("commerceGhostItem", "x"))
        assertEquals(0, prefs.getInt("commerceGhostCombats", -1))
    }

    @Test
    fun noteFightStartIncrementsCommerceCombats() {
        FightFamiliarMessageSync.noteFightStart(prefs, FightFamiliarMessageSync.FAMILIAR_GHOST_COMMERCE)
        assertEquals(1, prefs.getInt("commerceGhostCombats", 0))
    }

    @Test
    fun patrioticEagleDecrementsScreech() {
        prefs.setInt("screechCombats", 5)
        FightFamiliarMessageSync.applyPatrioticEagle(
            "throat is still too raw to screech",
            prefs,
        )
        assertEquals(4, prefs.getInt("screechCombats", 0))
    }

    @Test
    fun cookbookbatQuestSetsPrefs() {
        FightFamiliarMessageSync.applyCookbookbatQuest(
            """"As I recall, olive oil was common in The Shore, back in my day. Perhaps if you kill a crab, you'll find one."""",
            prefs,
        )
        assertEquals("olive oil", prefs.getString("_cookbookbatQuestIngredient", ""))
        assertEquals("The Shore", prefs.getString("_cookbookbatQuestLastLocation", ""))
        assertEquals("crab", prefs.getString("_cookbookbatQuestMonster", ""))
        assertEquals(6, prefs.getInt("_cookbookbatCombatsUntilNewQuest", 0))
    }

    @Test
    fun bellydancerPickpocket() {
        FightFamiliarMessageSync.applyBellydancer(
            "dances lithely around your opponent, distracting them",
            prefs,
        )
        assertEquals(1, prefs.getInt("_bellydancerPickpockets", 0))
    }
}
