package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class SkillDefinitionDatabaseTest {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun registerForTest_allowsLookupById() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 48,
                name = "Slimy Shoulders",
                image = "sebashield.gif",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
            ),
        )
        assertEquals("Slimy Shoulders", SkillDefinitionDatabase.getById(48)?.name)
        assertNull(SkillDefinitionDatabase.getById(999))
    }

    @Test
    fun load_indexesBundledSkills() = runTest {
        SkillDefinitionDatabase.load()
        assertEquals("Banishing Shout", SkillDefinitionDatabase.getById(11020)?.name)
    }
}

class SkillDescriptionConsequenceDatabaseTest {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
        SkillDescriptionConsequenceDatabase.resetForTest()
    }

    private fun registerSkill(id: Int, name: String) {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = id,
                name = name,
                image = "test.gif",
                tags = setOf("passive"),
                mpCost = 0,
                duration = 0,
                isPassive = true,
                isCombat = false,
                isNonCombat = false,
                isSong = false,
            ),
        )
    }

    @Test
    fun parseForTest_loadsAllDescSkillRowsWhenSkillsRegistered() {
        registerSkill(48, "Slimy Shoulders")
        registerSkill(46, "Slimy Sinews")
        registerSkill(47, "Slimy Synapses")
        registerSkill(107, "Summon Annoyance")
        registerSkill(134, "Bear Essence")
        registerSkill(190, "Visit your Favorite Bird")
        registerSkill(227, "Chitinous Soul")
        registerSkill(11020, "Banishing Shout")

        val parsed = SkillDescriptionConsequenceDatabase.parseForTest(
            """
            DESC_SKILL	Banishing Shout	currently banished:<br>([^<]+)<br>(?:([^<]+)<br>(?:([^<]+)<br>|</blockquote>)|</blockquote>)	banishingShoutMonsters=$1|$2|$3
            DESC_SKILL	Slimy Shoulders	giving you \+(\d+)	skillLevel48=[$1/2]
            DESC_SKILL	Slimy Sinews	giving you \+(\d+)	skillLevel46=[$1/2]
            DESC_SKILL	Slimy Synapses	giving you \+(\d+)	skillLevel47=$1
            DESC_SKILL	Summon Annoyance	Cost</b>: (\d+)	summonAnnoyanceCost=$1
            DESC_SKILL	Bear Essence	\+(\d+)% Picnic Basket	skillLevel134=[$1/20]
            DESC_SKILL	Chitinous Soul	Maximum HP \+(\d+)	skillLevel227=$1
            DESC_SKILL	Visit your Favorite Bird	your favorite bird, the (.*?), and get	yourFavoriteBird=$1
            """.trimIndent(),
        )
        assertEquals(8, parsed.size)
        assertEquals(1, parsed[11020]?.size)
        assertEquals(1, parsed[48]?.size)
    }

    @Test
    fun parseForTest_skipsUnknownSkill() {
        val parsed = SkillDescriptionConsequenceDatabase.parseForTest(
            """
            DESC_SKILL	Unknown Skill	foo=bar	foo=bar
            """.trimIndent(),
        )
        assertEquals(0, parsed.size)
    }
}
