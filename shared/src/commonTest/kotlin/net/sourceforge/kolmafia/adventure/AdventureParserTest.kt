package net.sourceforge.kolmafia.adventure

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.banish.Banisher

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

    @Test fun parseFightResult_snokebomb_detectsBanisher() {
        val html = "<span id='monname'>Goblin</span>\nYou throw the smokebomb at your feet and your foe flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.SNOKEBOMB, result.banisher)
    }

    @Test fun parseFightResult_kgbDart_detectsBanisher() {
        val html = "<span id='monname'>Pirate</span>\nYou press the secret switch and your foe flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.KGB_TRANQUILIZER_DART, result.banisher)
    }

    @Test fun parseFightResult_mafiaMFR_detectsBanisher() {
        val html = "<span id='monname'>Ninja</span>\n\"Well, I never,\" the monster exclaims. It flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertEquals(Banisher.MAFIA_MIDDLEFINGER_RING, result.banisher)
    }

    @Test fun parseFightResult_latte_detectsBanisher() {
        val html = "<span id='monname'>Goblin</span>\nThey run off, covered in delicious latte. It flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertEquals(Banisher.THROW_LATTE_ON_OPPONENT, result.banisher)
    }

    @Test fun parseFightResult_banishedNoPattern_returnsUnknown() {
        val html = "<span id='monname'>Goblin</span>\nIt flees in terror from some mysterious force."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.UNKNOWN, result.banisher)
    }

    @Test fun parseFightResult_notBanished_returnsUnknown() {
        val html = "<span id='monname'>Goblin</span>\nYou win the fight!"
        val result = AdventureParser.parseFightResult(html)
        assertFalse(result.banished)
        assertEquals(Banisher.UNKNOWN, result.banisher)
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

    // New banisher detection patterns — Phase 12

    @Test fun parseFightResult_stinkyCheeseEye_detected() {
        val html = "The monster flees in terror from stinky cheese eye goo on it."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.STINKY_CHEESE_EYE, result.banisher)
    }

    @Test fun parseFightResult_smokeGrenade_detected() {
        val html = "You throw a smoke grenade and your foe flees in terror while the smoke clears."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.SMOKE_GRENADE, result.banisher)
    }

    @Test fun parseFightResult_walkAwayFromExplosion_detected() {
        val html = "You walk away from the explosion in slow motion as your foe flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.WALK_AWAY_FROM_EXPLOSION, result.banisher)
    }

    @Test fun parseFightResult_splitPeaSoup_detected() {
        val html = "You hurl split pea soup and the monster flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.SPLIT_PEA_SOUP, result.banisher)
    }

    @Test fun parseFightResult_tennisBall_detected() {
        val html = "You chuck a tennis ball and the monster flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.TENNIS_BALL, result.banisher)
    }

    @Test fun parseFightResult_cocktailNapkin_detected() {
        val html = "You flourish your cocktail napkin and the monster flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.COCKTAIL_NAPKIN, result.banisher)
    }

    @Test fun parseFightResult_vivala_detected() {
        val html = "You make a v for vivala mask gesture and the monster flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.V_FOR_VIVALA_MASK, result.banisher)
    }

    @Test fun parseFightResult_indigoTaffy_detected() {
        val html = "You throw indigo taffy and the monster flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.PULLED_INDIGO_TAFFY, result.banisher)
    }

    @Test fun parseFightResult_thunderClap_detected() {
        val html = "You thunder clap the monster and it flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.THUNDER_CLAP, result.banisher)
    }

    @Test fun parseFightResult_dirtyStinkbomb_detected() {
        val html = "You throw a dirty stinkbomb and the monster flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.DIRTY_STINKBOMB, result.banisher)
    }

    @Test fun parseFightResult_peppermintBomb_detected() {
        val html = "You lob a peppermint bomb at the monster, causing it to flee in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.PEPPERMINT_BOMB, result.banisher)
    }

    @Test fun parseFightResult_haroldsBell_detected() {
        val html = "You throw the bell away, and the monster flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.HAROLDS_BELL, result.banisher)
    }

    @Test fun parseFightResult_crystalSkull_detected() {
        val html = "Your skull explodes into a million pieces! The monster flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.CRYSTAL_SKULL, result.banisher)
    }

    @Test fun parseFightResult_classyMonkey_detected() {
        val html = "EEEEEEEEEEEEEEEEEEEEEEEK! The monster flees in terror from the monkey."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.CLASSY_MONKEY, result.banisher)
    }

    @Test fun parseFightResult_beAMindMaster_detected() {
        val html = "You push away your opponent, who flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.BE_A_MIND_MASTER, result.banisher)
    }

    @Test fun parseFightResult_throwinEmber_detected() {
        val html = "You burned that foe so hard, you won't see them again. They flee in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.THROWIN_EMBER, result.banisher)
    }

    @Test fun parseFightResult_punchOutYourFoe_detected() {
        val html = "You deliver an epic punch and the monster flees in terror."
        val result = AdventureParser.parseFightResult(html)
        assertTrue(result.banished)
        assertEquals(Banisher.PUNCH_OUT_YOUR_FOE, result.banisher)
    }
}
