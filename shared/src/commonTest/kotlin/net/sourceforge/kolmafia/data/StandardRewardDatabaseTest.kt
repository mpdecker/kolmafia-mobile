package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterClass

class StandardRewardDatabaseTest {

    @Test
    fun toData_rewardRoundTripFormat() {
        val reward = StandardRewardDatabase.StandardReward(
            itemId = 11528,
            year = 2025,
            hardcore = true,
            characterClass = CharacterClass.SEAL_CLUBBER,
            row = "1700",
            itemName = "petrified wood war pike",
        )
        assertEquals(
            "11528\t2025\thard\tSC\t1700\tpetrified wood war pike",
            StandardRewardDatabase.toData(reward),
        )
    }

    @Test
    fun toData_pulverizedRoundTripFormat() {
        val pulverized = StandardRewardDatabase.StandardPulverized(
            itemId = 11534,
            year = 2026,
            hardcore = true,
            itemName = "petrified wood waste parts",
        )
        assertEquals(
            "11534\t2026\thard\tpetrified wood waste parts",
            StandardRewardDatabase.toData(pulverized),
        )
    }
}
