package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RumpleManagerTest {

    private lateinit var prefs: Preferences
    private lateinit var sessionLogger: SessionLogger

    @BeforeTest
    fun setUp() {
        RumpleManager.resetForTest()
        prefs = Preferences(MapSettings())
        sessionLogger = SessionLogger(prefs, GameEventBus())
    }

    @AfterTest
    fun tearDown() {
        RumpleManager.resetForTest()
    }

    @Test
    fun spyOnParents_detectsSins() {
        val html = """
            <span class='guts'>You peer through the portal into a house full of activity.  Children are
            everywhere!  The portal lets you watch them and their parents without fear of being noticed.
            You see the father tearing down the blinds to peep out of the window. You watch some of the
            many children play for awhile, and then you see the mother reclined in an overstuffed chair
            eating a bag of bacon-flavored onion rings. You're distracted by yet more kids romping around,
            and when you look back you see the father trying to squeeze into a girdle. Then the portal
            shimmers and you see no more.</span>
        """.trimIndent()
        RumpleManager.spyOnParents(html, sessionLogger)
        val sins = RumpleManager.detectedSins()
        assertEquals(RumpleManager.FATHER, sins[0]?.get(0))
        assertEquals(RumpleManager.LUSTFULNESS, sins[0]?.get(1))
        assertEquals(RumpleManager.VIOLENCE, sins[0]?.get(2))
        assertTrue(sessionLogger.recentLines().any { it.contains("father") })
    }

    @Test
    fun pickParentSin_and_trade() {
        RumpleManager.pickParent(1, prefs)
        RumpleManager.pickSin(1)
        assertEquals(RumpleManager.FATHER, RumpleManager.currentParent())
        assertEquals(RumpleManager.GREED, RumpleManager.currentSin())
        RumpleManager.recordTrade("You rescue three kids from their parents.", prefs, sessionLogger)
        assertEquals(3, prefs.getInt("rumpelstiltskinKidsRescued", 0))
        assertTrue(sessionLogger.recentLines().any { it.contains("inherent greed") })
    }

    @Test
    fun masteryAndAdvisor() {
        RumpleManager.updateMastery(
            "You're pretty sure you'll figure it out after roughly, say, 4 more tries, though!",
            1,
            prefs,
        )
        assertEquals(4, prefs.getInt("craftingStraw", -1))
        RumpleManager.updateMastery("That's it! You've figured it out!", 1, prefs)
        assertEquals(0, prefs.getInt("craftingStraw", -1))
        assertTrue(RumpleManager.advisorLines(prefs).isNotEmpty())
        assertTrue(RumpleManager.BRIBES.any { it.first() == RumpleManager.GREED })
    }

    @Test
    fun visitChoice_masteryPref() {
        RumpleManager.visitChoice(
            849,
            "You are already a master leather craftsman and no longer need to practice with it.",
            prefs,
        )
        assertEquals(0, prefs.getInt("craftingLeather", -1))
    }
}
