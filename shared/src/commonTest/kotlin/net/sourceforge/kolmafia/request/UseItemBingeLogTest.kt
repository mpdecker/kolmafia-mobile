package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UseItemBingeLogTest {

    private fun name(id: Int) = when (id) {
        12 -> "milk"
        88 -> "meat stack"
        127 -> "Gnollish autoplunger"
        else -> "item #$id"
    }

    @Test
    fun ghost_feedsByQuantity() {
        val line = UseItemBingeLog.line(
            "familiarbinger.php?whichitem=12&action=binge&qty=3",
            UseItemBingeLog.GHOST,
            "Gluttonous Green Ghost",
            ::name,
        )
        assertEquals("feed 3 milk to Gluttonous Green Ghost", line)
    }

    @Test
    fun otherFamiliar_isNotABinge() {
        assertNull(
            UseItemBingeLog.line(
                "familiarbinger.php?whichitem=12&qty=1",
                familiarId = 1,
                familiarRace = "Leprechaun",
                itemName = ::name,
            ),
        )
    }

    @Test
    fun slimeling_estimatesChargesFromItemPower() {
        val line = UseItemBingeLog.line(
            "inventory.php?action=slime&whichitem=12&qty=2",
            UseItemBingeLog.SLIMELING,
            "",
            ::name,
            itemPower = { 15 },
        )
        assertEquals("feed 2 milk to Slimeling (estimated 3.0 charges)", line)
    }

    @Test
    fun slimeling_countsSlimeStacksForTheAutoplunger() {
        val line = UseItemBingeLog.line(
            "familiarbinger.php?whichitem=127&qty=4",
            UseItemBingeLog.SLIMELING,
            "Slimeling",
            ::name,
        )
        assertEquals("feed 4 Gnollish autoplunger to Slimeling (4 more slime stack(s) due)", line)
    }

    @Test
    fun slimeling_countsSlimeStacksWhenTheItemUsesAMeatStack() {
        val line = UseItemBingeLog.line(
            "familiarbinger.php?whichitem=258&qty=1",
            UseItemBingeLog.SLIMELING,
            "Slimeling",
            ::name,
            usesMeatStack = { it == 258 },
        )
        assertEquals("feed 1 item #258 to Slimeling (1 more slime stack(s) due)", line)
    }

    @Test
    fun registerRequest_writesTheFeedLine() {
        RequestLogger.currentRound = { 0 }
        RequestLogger.familiarId = { UseItemBingeLog.STOCKING_MIMIC }
        RequestLogger.familiarRace = { "Stocking Mimic" }
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val claimed = RequestLogger.registerRequest(
            "familiarbinger.php?whichitem=999999&action=candy&qty=2",
            logger,
            prefs,
        )
        assertTrue(claimed)
        assertTrue(logger.recentLines().any { it.contains("feed 2 item #999999 to Stocking Mimic") })
    }
}
