package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterClass

class JourneymanDatabaseTest {

    @BeforeTest
    fun setUp() {
        JourneymanDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        JourneymanDatabase.resetForTest()
    }

    @Test
    fun parseForTest_readsZoneAndSkillRows() {
        val fixture = """
            Seal Clubber	Barrrney's Barrr	1	[6032]Accordion Bash
            Seal Clubber	Barrrney's Barrr	2	[6037]Bawdy Refrain
        """.trimIndent()
        val snapshot = JourneymanDatabase.parseForTest(fixture, validateReferences = false)
        assertEquals(2, snapshot.entryCount)
        assertEquals(1, snapshot.zoneNames.size)
        val skills = snapshot.zoneSkills["Barrrney's Barrr"]?.get(CharacterClass.SEAL_CLUBBER)
        assertNotNull(skills)
        assertEquals("Accordion Bash", skills[0])
        assertEquals("Bawdy Refrain", skills[1])
    }

    @Test
    fun load_fullFile_hasExpectedCounts() = runBlocking {
        AdventureDatabase.load()
        SkillDefinitionDatabase.load()
        JourneymanDatabase.load()
        assertEquals(1080, JourneymanDatabase.loadedEntryCount)
        assertEquals(30, JourneymanDatabase.zoneNames.size)
        assertEquals(180, JourneymanDatabase.skillCount)
    }

    @Test
    fun load_fullFile_barrrneysBarrrSealClubberSkill1() = runBlocking {
        AdventureDatabase.load()
        SkillDefinitionDatabase.load()
        JourneymanDatabase.load()
        val skills = JourneymanDatabase.skillsForZone("Barrrney's Barrr", CharacterClass.SEAL_CLUBBER)
        assertNotNull(skills)
        assertEquals("Accordion Bash", skills[0])
        val encoded = JourneymanDatabase.encodedLocation("Accordion Bash", CharacterClass.SEAL_CLUBBER)
        assertNotNull(encoded)
        assertEquals("Barrrney's Barrr", JourneymanDatabase.zoneNameForEncoded(encoded))
        assertEquals(4, JourneymanDatabase.turnsForEncoded(encoded))
    }

    @Test
    fun injectForTest_exposesLoadedState() {
        val snapshot = JourneymanDatabase.parseForTest(
            "Seal Clubber	Test Zone	3	Test Skill",
            validateReferences = false,
        )
        JourneymanDatabase.injectForTest(snapshot)
        assertTrue(JourneymanDatabase.isLoaded)
        assertEquals("Test Zone", JourneymanDatabase.zoneNames.single())
    }
}
