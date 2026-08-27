package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class FightLovebugSyncTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
    }

    @Test
    fun unlocksOnLovebugPresence() {
        assertTrue(
            FightLovebugSync.apply(
                """<img src="lb_cricket.gif"> a jaunty tune""",
                prefs,
            ),
        )
        assertTrue(prefs.getBoolean("lovebugsUnlocked", false))
        assertEquals(1, prefs.getInt("lovebugsItemDrop", 0))
    }

    @Test
    fun antIncrementsOrcChasm() {
        FightLovebugSync.apply("""src="images/itemimages/lb_ant.gif"""", prefs)
        assertEquals(1, prefs.getInt("lovebugsOrcChasm", 0))
    }

    @Test
    fun oilBeetleIncrementsOilPeak() {
        FightLovebugSync.apply(
            """<img src="lb_beetle.gif"> An oil beetle skitters.""",
            prefs,
        )
        assertEquals(1, prefs.getInt("lovebugsOilPeak", 0))
    }

    @Test
    fun deferredMuscleGainFromStagBeetle() {
        FightLovebugSync.apply(
            """<img src="lb_beetle.gif"> A stag beetle. You gain 5 Fortitude.""",
            prefs,
        )
        assertEquals(5, prefs.getInt("lovebugsMuscle", 0))
    }

    @Test
    fun fireflyCyrptWithEvilometer() {
        prefs.setInt("cyrptAlcoveEvilness", 50)
        prefs.setInt("cyrptTotalEvilness", 200)
        FightLovebugSync.apply(
            html = """
                <img src="lb_firefly.gif"> seem slightly brighter.
                Your Evilometer beeps once.
                The Defiled Alcove
            """.trimIndent(),
            preferences = prefs,
            adventureId = "261",
        )
        assertEquals(1, prefs.getInt("lovebugsCyrpt", 0))
        assertEquals(49, prefs.getInt("cyrptAlcoveEvilness", 0))
    }

    @Test
    fun cricketMeatDropAndFlyBooze() {
        FightLovebugSync.apply("""src="lb_fly.gif"""", prefs)
        assertEquals(1, prefs.getInt("lovebugsBooze", 0))
        FightLovebugSync.apply(
            """src="lb_grub.gif"> love grub shyly offers extra Meat""",
            prefs,
        )
        assertEquals(1, prefs.getInt("lovebugsMeatDrop", 0))
    }

    @Test
    fun tickWalmartAndWormBeach() {
        FightLovebugSync.apply(
            """src="lb_tick.gif"> love snow flea""",
            prefs,
        )
        assertEquals(1, prefs.getInt("lovebugsWalmart", 0))
        FightLovebugSync.apply("""src="lb_worm.gif"""", prefs)
        assertEquals(1, prefs.getInt("lovebugsBeachBuck", 0))
    }
}
