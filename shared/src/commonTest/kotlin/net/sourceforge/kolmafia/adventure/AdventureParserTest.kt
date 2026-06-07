package net.sourceforge.kolmafia.adventure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AdventureParserTest {

    @Test
    fun parsesNonCombat_whenFinalUrlIsAdventurePage() {
        val html = """
            <html><body>
            <b>A Spooky Treehouse</b>
            <p>You find something interesting.</p>
            <p>You gain 42 Meat.</p>
            <p>You acquire an item: <b>spooky stick</b></p>
            </body></html>
        """.trimIndent()
        val result = AdventureParser.parseAdventureResponse(html, "https://www.kingdomofloathing.com/adventure.php")
        assertIs<AdventureResult.NonCombat>(result)
        assertEquals(42, result.meatGained)
        assertTrue(result.itemsGained.contains("spooky stick"))
    }

    @Test
    fun parsesCombat_whenFinalUrlIsFightPage() {
        val html = """
            <html><body>
            <span id='monname'>fluffy bunny</span>
            <p>You're fighting a fluffy bunny!</p>
            </body></html>
        """.trimIndent()
        val result = AdventureParser.parseAdventureResponse(html, "https://www.kingdomofloathing.com/fight.php")
        assertIs<AdventureResult.Combat>(result)
        assertEquals("fluffy bunny", result.monster)
    }

    @Test
    fun parsesChoice_whenFinalUrlIsChoicePage() {
        val html = """
            <html><body>
            <form method="POST" action="choice.php">
            <input type="hidden" name="whichchoice" value="105">
            <a href="choice.php?pwd=xxx&option=1">Take the sword</a>
            <a href="choice.php?pwd=xxx&option=2">Ignore it</a>
            </form>
            </body></html>
        """.trimIndent()
        val result = AdventureParser.parseAdventureResponse(html, "https://www.kingdomofloathing.com/choice.php")
        assertIs<AdventureResult.Choice>(result)
        assertEquals(105, result.choiceId)
        assertEquals(2, result.options.size)
    }

    @Test
    fun parseFightResult_extractsWin() {
        val html = """
            <html><body>
            <span id='monname'>fluffy bunny</span>
            <p>You win the fight!</p>
            <p>You gain 15 Meat.</p>
            <p>You acquire an item: <b>bunny liver</b></p>
            <p>You gain 12 Beefiness (12 exp)</p>
            </body></html>
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.won)
        assertEquals("fluffy bunny", result.monster)
        assertEquals(15, result.meatGained)
        assertTrue(result.itemsGained.contains("bunny liver"))
        assertEquals(mapOf("Beefiness" to 12), result.statsGained)
    }

    @Test
    fun parseFightResult_extractsLoss() {
        val html = "<html><body><p>You lose the fight.</p></body></html>"
        val result = AdventureParser.parseFightResult(html)
        assertTrue(!result.won)
    }

    @Test
    fun parsesMultipleItems() {
        val html = """
            <p>You acquire an item: <b>seal tooth</b></p>
            <p>You acquire an item: <b>seal-clubbing club</b></p>
        """.trimIndent()
        val result = AdventureParser.parseAdventureResponse(html, "https://www.kingdomofloathing.com/adventure.php")
        assertIs<AdventureResult.NonCombat>(result)
        assertEquals(2, result.itemsGained.size)
    }

    @Test fun parseFightResult_winWithoutBanish_banishedFalse() {
        val html = """
            <span id='monname'>Ninja Snowman</span>
            You win the fight!
            You gain 100 Meat
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.won)
        assertFalse(result.banished)
    }

    @Test fun parseFightResult_fleeInTerror_banishedTrue() {
        val html = """
            <span id='monname'>Ninja Snowman</span>
            The monster flees in terror!
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals("Ninja Snowman", result.monster)
    }

    @Test fun parseFightResult_banishedFromAdventures_banishedTrue() {
        val html = """
            <span id='monname'>Ninja Snowman</span>
            The Ninja Snowman is banished from your adventures for the rest of today.
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
    }

    @Test fun parseFightResult_goneSomewhere_banishedTrue() {
        val html = """
            <span id='monname'>Ninja Snowman</span>
            It has gone somewhere else to live.
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
    }

    @Test fun parseFightResult_fleeField_banishedTrue() {
        val html = """
            <span id='monname'>Spooky Vampire</span>
            It flees the field.
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
    }

    @Test fun parseFightResult_fledTheArea_banishedTrue() {
        val html = """
            <span id='monname'>Ninja Snowman</span>
            It fled the area.
        """.trimIndent()
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
    }
}
