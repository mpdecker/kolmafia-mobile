package net.sourceforge.kolmafia.quest

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ConsequenceAction
import net.sourceforge.kolmafia.data.ConsequenceActionParser
import net.sourceforge.kolmafia.data.ConsequenceRule
import net.sourceforge.kolmafia.data.MonsterConsequenceDatabase

class MonsterConsequenceSyncTest {

    @AfterTest
    fun tearDown() {
        MonsterConsequenceDatabase.resetForTest()
    }

    private fun injectRules(monsterName: String, vararg ruleLines: String) {
        val rules = ruleLines.map { line ->
            val parts = line.split('\t')
            ConsequenceRule(
                spec = parts[0],
                pattern = Regex(parts[1]),
                actions = listOfNotNull(ConsequenceActionParser.parseAction(parts[2])),
            )
        }
        MonsterConsequenceDatabase.injectForTest(mapOf(monsterName to rules))
    }

    @Test
    fun disambiguateMonster_edGifCapturesFormNumber() {
        injectRules(
            "Ed the Undying",
            "Ed the Undying\t/ed(\\d)\\.gif\t\"Ed the Undying (\$1)\"",
            "Ed the Undying\t.\t\"Ed the Undying (1)\"",
        )
        val html = """<html><img src="/ed4.gif"><!-- MONSTERID: 473 --></html>"""
        assertEquals(
            "Ed the Undying (4)",
            MonsterConsequenceSync.disambiguateMonster("Ed the Undying", html),
        )
    }

    @Test
    fun disambiguateMonster_edFallbackWhenNoGifMatch() {
        injectRules(
            "Ed the Undying",
            "Ed the Undying\t/ed(\\d)\\.gif\t\"Ed the Undying (\$1)\"",
            "Ed the Undying\t.\t\"Ed the Undying (1)\"",
        )
        val html = """<html><span id='monname'>Ed the Undying</span></html>"""
        assertEquals(
            "Ed the Undying (1)",
            MonsterConsequenceSync.disambiguateMonster("Ed the Undying", html),
        )
    }

    @Test
    fun disambiguateMonster_hardModeGif() {
        injectRules(
            "Count Drunkula",
            "Count Drunkula\tdrunkula_hm\\.gif\t\"Count Drunkula (Hard Mode)\"",
        )
        val html = """<html><img src="drunkula_hm.gif"></html>"""
        assertEquals(
            "Count Drunkula (Hard Mode)",
            MonsterConsequenceSync.disambiguateMonster("Count Drunkula", html),
        )
    }

    @Test
    fun disambiguateMonster_returnsOriginalWhenNoRules() {
        assertEquals(
            "Knob Goblin",
            MonsterConsequenceSync.disambiguateMonster("Knob Goblin", "<html></html>"),
        )
    }

    @Test
    fun disambiguateMonster_returnsOriginalWhenRulesDoNotMatch() {
        injectRules(
            "Count Drunkula",
            "Count Drunkula\tdrunkula_hm\\.gif\t\"Count Drunkula (Hard Mode)\"",
        )
        assertEquals(
            "Count Drunkula",
            MonsterConsequenceSync.disambiguateMonster("Count Drunkula", "<html>normal fight</html>"),
        )
    }
}
