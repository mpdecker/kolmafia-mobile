package net.sourceforge.kolmafia.quest

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.familiar.FamiliarData

class CrownBjornDescSyncTest {

    private val sealLarva = FamiliarData(
        id = 1,
        name = "Adorable Seal Larva",
        race = "Seal Larva",
        weight = 5,
        experience = 0,
        kills = 0,
    )

    private val angryGoat = FamiliarData(
        id = 7,
        name = "Biscuit",
        race = "Angry Goat",
        weight = 12,
        experience = 0,
        kills = 0,
    )

    private fun occupantHtml(race: String): String =
        """<center><b>Crown of Thrones</b><br>Current Occupant: <b>Adorable $race the $race</b></center>"""

    @Test
    fun parseOccupant_matchesOwnedRace() {
        val occupant = CrownBjornDescSync.parseOccupant(
            occupantHtml("Seal Larva"),
            listOf(sealLarva, angryGoat),
        )
        assertEquals(1, occupant.id)
        assertEquals("Seal Larva", occupant.race)
    }

    @Test
    fun parseOccupant_clearsWhenRaceNotOwned() {
        val occupant = CrownBjornDescSync.parseOccupant(
            occupantHtml("Purse Rat"),
            listOf(sealLarva),
        )
        assertEquals(CrownBjornDescSync.CLEARED, occupant)
    }

    @Test
    fun parseOccupant_clearsWhenNoOccupantInHtml() {
        val occupant = CrownBjornDescSync.parseOccupant(
            "<center><b>Crown of Thrones</b><br>No familiar is sitting in it.</center>",
            listOf(sealLarva),
        )
        assertEquals(CrownBjornDescSync.CLEARED, occupant)
    }
}
