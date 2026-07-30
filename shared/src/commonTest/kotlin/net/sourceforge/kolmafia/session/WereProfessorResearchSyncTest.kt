package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.WereProfessorDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class WereProfessorResearchSyncTest {

    private lateinit var preferences: Preferences

    @BeforeTest
    fun setUp() {
        WereProfessorDatabase.resetForTest()
        WereProfessorDatabase.injectForTest(
            WereProfessorDatabase.parseForTest(
                """
                1	mus1	10	none	Osteocalcin injection	Mus +20%
                2	mus2	20	mus1	Somatostatin catalyst	Mus +30%
                3	mus3	30	mus2	Endothelin suspension	Mus +50%
                4	rend1	20	mus3	Ultraprogesterone potion	Rend (Phys)
                5	rend2	30	rend1	Lactide blocker	Increase damage
                6	rend3	40	rend2	Haemostatic membrane treatment	Restores HP
                7	slaughter	100	rend3	Norepinephrine transfusion	Slaughter (Instant)
                """.trimIndent(),
            ),
        )
        preferences = Preferences(MapSettings())
    }

    @AfterTest
    fun tearDown() {
        WereProfessorDatabase.resetForTest()
    }

    @Test
    fun visitChoice_parsesResearchPointsAndAvailableFields() {
        val html = """
            <p>You have 42 research points (rp).
            <input type="hidden" name="r" value="wereprof_mus2" />
        """.trimIndent()
        WereProfessorResearchSync.visitChoice(html, preferences)
        assertEquals(42, preferences.getInt("wereProfessorResearchPoints", 0))
        val available = WereProfessorDatabase.loadResearch(
            preferences,
            WereProfessorDatabase.AVAILABLE_RESEARCH,
        )
        assertEquals(1, available.size)
        assertEquals("mus2", available.single().field)
    }

    @Test
    fun visitChoice_derivesKnownResearchAndTierPrefs() {
        val html = """
            <p>You have 10 research points (rp).
            <input type="hidden" name="r" value="wereprof_mus2" />
        """.trimIndent()
        WereProfessorResearchSync.visitChoice(html, preferences)
        val known = WereProfessorDatabase.loadResearch(
            preferences,
            WereProfessorDatabase.KNOWN_RESEARCH,
        )
        assertTrue(known.any { it.field == "mus1" })
        assertEquals(0, preferences.getInt("wereProfessorRend", -1))
    }

    @Test
    fun getResearch_resolvesWereprofPrefixFromUrl() {
        val research = WereProfessorResearchSync.getResearch(
            "choice.php?whichchoice=1523&option=1&r=wereprof_mus1",
        )
        assertNotNull(research)
        assertEquals("mus1", research.field)
    }

    @Test
    fun registerRequest_logsChoice1523ResearchLine() {
        val logger = SessionLogger(preferences, net.sourceforge.kolmafia.event.GameEventBus())
        val logged = WereProfessorResearchSync.registerRequest(
            "choice.php?whichchoice=1523&option=1&r=wereprof_mus1",
            logger,
        )
        assertTrue(logged)
        assertTrue(
            preferences.getString(SessionLogger.SESSION_LOG_KEY, "")
                .contains("Took choice 1523/1"),
        )
    }

    @Test
    fun postChoice2_successMessageAndRefreshWhenNoChoiceLink() {
        preferences.setString(WereProfessorDatabase.KNOWN_RESEARCH, "")
        val logger = SessionLogger(preferences, net.sourceforge.kolmafia.event.GameEventBus())
        val html = """
            You successfully research Somatostatin catalyst.
            <p>You have 22 research points (rp).
            <input type="hidden" name="r" value="wereprof_mus3" />
        """.trimIndent()
        WereProfessorResearchSync.postChoice2(
            "choice.php?whichchoice=1523&option=1&r=wereprof_mus2",
            html,
            preferences,
            logger,
        )
        assertEquals(22, preferences.getInt("wereProfessorResearchPoints", 0))
        assertTrue(
            preferences.getString(SessionLogger.SESSION_LOG_KEY, "")
                .contains("You spent 20 rp"),
        )
    }

    @Test
    fun isResearchBenchChoice_matches1523Only() {
        assertTrue(WereProfessorResearchSync.isResearchBenchChoice(1523))
        assertFalse(WereProfessorResearchSync.isResearchBenchChoice(1522))
    }
}
