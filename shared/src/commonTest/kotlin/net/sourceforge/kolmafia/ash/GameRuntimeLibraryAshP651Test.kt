package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.VoteMonsterManager

class GameRuntimeLibraryAshP651Test {

    @Test
    fun revision_phase647() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    private val boothHtml = """
        Today's Leader
        initiatives: </b><div style='margin-left: 1em; color: blue'>+25% Meat from Monsters<br>+10% Item Drops from Monsters</div>
        <b>Today's Leader: </b>Pork Elf Historical Preservation Party<br><blockquote>strict curtailing of unnatural modern technologies</blockquote>
    """.trimIndent()

    @Test
    fun parseBooth_setsModifierAndMonster() {
        val prefs = Preferences(MapSettings())
        assertTrue(VoteMonsterManager.parseBooth(boothHtml, prefs))
        assertTrue(prefs.getString("_voteModifier").contains("Meat Drop"))
        assertTrue(prefs.getString("_voteModifier").contains("Item Drop"))
        assertEquals("government bureaucrat", prefs.getString("_voteMonster"))
    }

    @Test
    fun parseBooth_doesNotOverwriteExistingMonster() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_voteMonster", "angry ghost")
        assertTrue(VoteMonsterManager.parseBooth(boothHtml, prefs))
        assertEquals("angry ghost", prefs.getString("_voteMonster"))
    }

    @Test
    fun parseBooth_clanVentriloMapsSlime() {
        val prefs = Preferences(MapSettings())
        val html = """
            Today's Leader
            <b>Today's Leader: </b>Clan Ventrilo<br><blockquote>bringing this blessing to the entire population</blockquote>
        """.trimIndent()
        assertTrue(VoteMonsterManager.parseBooth(html, prefs))
        assertEquals("slime blob", prefs.getString("_voteMonster"))
    }

    @Test
    fun applyFromVisit_requiresTownrightVoteAction() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            VoteMonsterManager.applyFromVisit(
                "place.php?whichplace=town_right",
                boothHtml,
                prefs,
            ),
        )
        assertTrue(
            VoteMonsterManager.applyFromVisit(
                "place.php?whichplace=town_right&action=townright_vote",
                boothHtml,
                prefs,
            ),
        )
        assertEquals("government bureaucrat", prefs.getString("_voteMonster"))
    }

    @Test
    fun parseBooth_withoutLeader_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(VoteMonsterManager.parseBooth("just a town page", prefs))
        assertEquals("", prefs.getString("_voteMonster"))
    }
}
