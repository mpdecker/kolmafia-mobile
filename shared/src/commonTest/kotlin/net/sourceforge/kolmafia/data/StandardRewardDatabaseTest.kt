package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import net.sourceforge.kolmafia.character.CharacterClass

class StandardRewardDatabaseTest {

    @AfterTest
    fun cleanup() {
        StandardRewardDatabase.resetForTest()
    }

    @Test
    fun loadRewardsAndPulverized() {
        StandardRewardDatabase.loadFromText(SAMPLE_REWARDS, SAMPLE_PULVERIZED)

        val mossMace = StandardRewardDatabase.findStandardReward(MOSS_MACE)
        assertNotNull(mossMace)
        assertEquals(2024, mossMace.year)
        assertEquals(false, mossMace.hardcore)
        assertEquals(CharacterClass.SEAL_CLUBBER, mossMace.characterClass)
        assertEquals("ROW1454", mossMace.row)

        val pulverized = StandardRewardDatabase.findStandardPulverized(CREPE_BITS)
        assertNotNull(pulverized)
        assertEquals(2025, pulverized.year)
    }

    @Test
    fun findPulverizationMapsYearAndPathType() {
        StandardRewardDatabase.loadFromText(SAMPLE_REWARDS, SAMPLE_PULVERIZED)

        val mossMace = StandardRewardDatabase.findStandardReward(MOSS_MACE)!!
        assertEquals(CREPE_BITS, StandardRewardDatabase.findPulverization(mossMace))

        val adobe = StandardRewardDatabase.findStandardReward(ADOBE_ARSECOVER)!!
        assertEquals(PETRIFIED_WOOD, StandardRewardDatabase.findPulverization(adobe))
    }

    @Test
    fun unknownRowExcludedFromParseRowNumber() {
        StandardRewardDatabase.loadFromText(
            """
                12068	2026	norm	SC	UNKNOWN	angelbone kilt
            """.trimIndent(),
            SAMPLE_PULVERIZED,
        )
        assertNull(StandardRewardDatabase.parseRowNumber("UNKNOWN"))
    }

    companion object {
        private const val MOSS_MACE = 11504
        private const val ADOBE_ARSECOVER = 11512
        private const val CREPE_BITS = 11526
        private const val PETRIFIED_WOOD = 11534

        private val SAMPLE_REWARDS = """
            11504	2024	norm	SC	ROW1454	moss mace
            11512	2024	hard	SC	ROW1460	adobe arsecover
            11520	2025	norm	SC	ROW1461	crepe paper phrygian cap
        """.trimIndent()

        private val SAMPLE_PULVERIZED = """
            11526	2025	norm	crepe paper pared cuttings
            11534	2025	hard	petrified wood waste parts
            12074	2026	norm	angelbone fragments
        """.trimIndent()
    }
}
