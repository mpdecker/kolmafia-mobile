package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WereProfessorDatabaseTest {

    @BeforeTest
    fun setUp() {
        WereProfessorDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        WereProfessorDatabase.resetForTest()
    }

    @Test
    fun parseForTest_readsResearchRowsAndSkipsComments() {
        val fixture = """
            # Muscle Skill Tree
            1	mus1	10	none	Osteocalcin injection	Mus +20%
            2	mus2	20	mus1	Somatostatin catalyst	Mus +30%

        """.trimIndent()
        val snapshot = WereProfessorDatabase.parseForTest(fixture)
        assertEquals(2, snapshot.allResearch.size)
        val mus1 = snapshot.fieldToResearch["mus1"]
        assertNotNull(mus1)
        assertEquals("mus1", mus1.field)
        assertEquals("none", mus1.parent)
        val mus2 = snapshot.fieldToResearch["mus2"]
        assertNotNull(mus2)
        assertEquals("mus1", mus2.parent)
    }

    @Test
    fun findResearch_resolvesBareFieldAndWereprofPrefix() {
        val fixture = "1	mus1	10	none	Osteocalcin injection	Mus +20%"
        val snapshot = WereProfessorDatabase.parseForTest(fixture)
        WereProfessorDatabase.injectForTest(snapshot)

        val bare = WereProfessorDatabase.findResearch("mus1")
        val prefixed = WereProfessorDatabase.findResearch("wereprof_mus1")
        assertNotNull(bare)
        assertNotNull(prefixed)
        assertEquals(bare, prefixed)
        assertEquals("Osteocalcin injection", bare.name)
    }

    @Test
    fun findResearch_unknownFieldReturnsNull() {
        val fixture = "1	mus1	10	none	Osteocalcin injection	Mus +20%"
        WereProfessorDatabase.injectForTest(WereProfessorDatabase.parseForTest(fixture))
        assertNull(WereProfessorDatabase.findResearch("unknown"))
    }

    @Test
    fun load_fullFile_hasExpectedCounts() = runBlocking {
        WereProfessorDatabase.load()
        assertTrue(WereProfessorDatabase.isLoaded)
        assertEquals(54, WereProfessorDatabase.loadedResearchCount)
        assertEquals(9, WereProfessorDatabase.terminalResearch().size)
    }

    @Test
    fun load_fullFile_terminalSlaughterEntry() = runBlocking {
        WereProfessorDatabase.load()
        val slaughter = WereProfessorDatabase.findResearch("slaughter")
        assertNotNull(slaughter)
        assertEquals(100, slaughter.cost)
        assertEquals("rend3", slaughter.parent)
        assertEquals("Norepinephrine transfusion", slaughter.name)
        assertTrue(WereProfessorDatabase.terminalResearch().contains(slaughter))
    }

    @Test
    fun injectForTest_exposesLoadedState() {
        val snapshot = WereProfessorDatabase.parseForTest(
            "7	slaughter	100	rend3	Norepinephrine transfusion	Slaughter (Instant)",
        )
        WereProfessorDatabase.injectForTest(snapshot)
        assertTrue(WereProfessorDatabase.isLoaded)
        assertEquals(1, WereProfessorDatabase.loadedResearchCount)
        assertEquals(1, WereProfessorDatabase.terminalResearch().size)
    }

    @Test
    fun deriveKnownResearch_marksParentsOfAvailableNode() {
        val snapshot = WereProfessorDatabase.parseForTest(
            """
            1	mus1	10	none	Osteocalcin injection	Mus +20%
            2	mus2	20	mus1	Somatostatin catalyst	Mus +30%
            3	mus3	30	mus2	Endothelin suspension	Mus +50%
            4	rend1	20	mus3	Ultraprogesterone potion	Rend (Phys)
            5	rend2	30	rend1	Lactide blocker	Increase damage
            6	rend3	40	rend2	Haemostatic membrane treatment	Restores HP
            7	slaughter	100	rend3	Norepinephrine transfusion	Slaughter (Instant)
            """.trimIndent(),
        )
        WereProfessorDatabase.injectForTest(snapshot)
        val mus2 = WereProfessorDatabase.findResearch("mus2")!!
        val known = WereProfessorDatabase.deriveKnownResearch(setOf(mus2))
        assertTrue(known.any { it.field == "mus1" })
        assertFalse(known.contains(mus2))
    }

    @Test
    fun loadAndSaveResearch_roundTripsCommaSeparatedFields() {
        val snapshot = WereProfessorDatabase.parseForTest(
            """
            1	mus1	10	none	Osteocalcin injection	Mus +20%
            2	mus2	20	mus1	Somatostatin catalyst	Mus +30%
            """.trimIndent(),
        )
        WereProfessorDatabase.injectForTest(snapshot)
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        val mus1 = WereProfessorDatabase.findResearch("mus1")!!
        WereProfessorDatabase.saveResearch(preferences, WereProfessorDatabase.KNOWN_RESEARCH, setOf(mus1))
        val loaded = WereProfessorDatabase.loadResearch(preferences, WereProfessorDatabase.KNOWN_RESEARCH)
        assertEquals(setOf(mus1), loaded)
    }
}
