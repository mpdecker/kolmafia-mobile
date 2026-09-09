package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HobopolisManagerTest {

    @Test
    fun parseTownSquareWin_hoboSkin() {
        val html = "WINWINWIN you knocked him completely out of his skin, which is now lying in a wrinkly heap on the ground"
        assertEquals("hobo skin", HobopolisManager.parseTownSquareWin(html))
    }

    @Test
    fun parseTownSquareWin_charredHoboBoots() {
        val html = "WINWINWIN All that's left is a smoking pair of boots!"
        assertEquals("charred hobo boots", HobopolisManager.parseTownSquareWin(html))
    }

    @Test
    fun parseTownSquareWin_frozenHoboEyeballs() {
        val html = "WINWINWIN All that's left is a pair of frozen eyeballs."
        assertEquals("frozen hobo eyeballs", HobopolisManager.parseTownSquareWin(html))
    }

    @Test
    fun parseTownSquareWin_stinkingHoboGuts() {
        val html = "WINWINWIN All that's left of him is a pile of foul-smelling guts."
        assertEquals("stinking hobo guts", HobopolisManager.parseTownSquareWin(html))
    }

    @Test
    fun parseTownSquareWin_creepyHoboSkull() {
        val html = "WINWINWIN He ran off so fast that he left his skull behind!"
        assertEquals("creepy hobo skull", HobopolisManager.parseTownSquareWin(html))
    }

    @Test
    fun parseTownSquareWin_hoboCrotch() {
        val html = "WINWINWIN He ran off without his crotch! he ran off without his crotch"
        assertEquals("hobo crotch", HobopolisManager.parseTownSquareWin(html))
    }

    @Test
    fun parseTownSquareWin_noWin() {
        val html = "You're fighting Hodgman the Hoboverlord"
        assertNull(HobopolisManager.parseTownSquareWin(html))
    }

    @Test
    fun parseTownSquareWin_winButNoItem() {
        val html = "WINWINWIN but no matching flavour text"
        assertNull(HobopolisManager.parseTownSquareWin(html))
    }
}
