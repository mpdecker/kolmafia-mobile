package net.sourceforge.kolmafia.session

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.choice.ItemPool

class HobopolisManagerTest {

    @AfterTest
    fun tearDown() {
        HobopolisManager.consumePendingStop()
    }

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

    @Test
    fun hobopolisBossName_andWait() {
        assertEquals("Hodgman", HobopolisManager.hobopolisBossName(200))
        assertEquals("Ol' Scratch", HobopolisManager.hobopolisBossName(201))
        assertEquals("Uncle Hobo", HobopolisManager.hobopolisBossName(518))
        assertEquals("Hodgman waits for you.", HobopolisManager.bossWaitMessage(200, 2))
        assertNull(HobopolisManager.bossWaitMessage(200, 1))
        assertEquals("Richard takes a hobo skin", HobopolisManager.richardTakesMessage("hobo skin"))
    }

    @Test
    fun tireKills_matchesDecoratorFormula() {
        assertEquals(0, HobopolisManager.tireKills(0))
        assertEquals(1, HobopolisManager.tireKills(1))
        assertEquals(6, HobopolisManager.tireKills(5))
        assertEquals(54, HobopolisManager.tireKills(20))
    }

    @Test
    fun checkDungeonSewers_codeAndItemAndGrate() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        val counts = mutableMapOf(
            ItemPool.DUMPLINGS to 2,
            ItemPool.SEWER_WAD to 2,
            ItemPool.OOZE_O to 2,
            ItemPool.OIL_OF_OILINESS to 6,
            ItemPool.GATORSKIN_UMBRELLA to 2,
        )
        val html = """
            You steel your nerves and descend into the darkened tunnel.
            'crewcut'
            some of your unfortunate dumplings
            the sight of your sewer wad
            somebody else opened this grate
        """.trimIndent()
        val result = HobopolisManager.checkDungeonSewers(
            html = html,
            accessibleCount = { counts[it] ?: 0 },
            consumeItem = { id, qty ->
                consumed += id to qty
                counts[id] = (counts[id] ?: 0) - qty
            },
        )
        assertEquals(8, result?.explorations)
        assertEquals("+8 Explorations", result?.message)
        assertFalse(result?.error == true)
        assertEquals(listOf(ItemPool.DUMPLINGS to 1, ItemPool.SEWER_WAD to 1), consumed)
    }

    @Test
    fun checkDungeonSewers_requireTestItemsErrors() {
        val html = "You steel your nerves and descend into the darkened tunnel. 'crewcut'"
        val result = HobopolisManager.checkDungeonSewers(
            html = html,
            accessibleCount = { 0 },
            consumeItem = { _, _ -> },
            requireSewerTestItems = true,
        )
        assertEquals(1, result?.explorations)
        assertTrue(result?.error == true)
        assertTrue(result?.message?.contains("NEED:") == true)
    }

    @Test
    fun sewerPrepError_whenRequiredAndMissing() {
        val err = HobopolisManager.sewerPrepError(
            requireSewerTestItems = true,
            hasEquippedUmbrella = false,
            hasEquippedBinder = true,
            hasSewerWad = true,
            hasOozeO = true,
            hasDumplings = true,
            hasOilOfOiliness = true,
        )
        assertTrue(err?.contains("gatorskin umbrella") == true)
        assertNull(
            HobopolisManager.sewerPrepError(
                requireSewerTestItems = false,
                hasEquippedUmbrella = false,
                hasEquippedBinder = false,
                hasSewerWad = false,
                hasOozeO = false,
                hasDumplings = false,
                hasOilOfOiliness = false,
            ),
        )
    }

    @Test
    fun postChoice_recordsBossWait() {
        assertTrue(HobopolisManager.postChoice(200, 2, "", { 0 }, { _, _ -> }))
        assertEquals("Hodgman waits for you.", HobopolisManager.consumePendingStop())
        assertNull(HobopolisManager.consumePendingStop())
    }
}
