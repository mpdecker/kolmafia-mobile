package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.quest.KrampusFacilityChoiceSync

class GameRuntimeLibraryAshP880Test {
    @Test
    fun krampusChargesDirectAndUpgradeCosts() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(KrampusFacilityChoiceSync.apply(810, 2, "", "", { id, qty -> consumed += id to qty }))
        val html = "<td><a href='choice.php?whichchoice=810&option=4&slot=7'><img alt='Toybot (Level 3)'></a></td>You upgrade the robot!"
        assertTrue(KrampusFacilityChoiceSync.apply(810, 4, "choice.php?whichchoice=810&option=4&slot=7", html, { id, qty -> consumed += id to qty }))
        assertEquals(listOf(6913 to 100, 6913 to 500), consumed)
    }
}
