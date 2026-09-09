package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Group C — RequestLogger transfer item-name depth (Phases 5856–5865).
 *
 * Verifies that storage pull, closet take/add, display case take/put,
 * and clan stash take/add include parsed item names in session-log lines.
 */
class RequestLoggerXxxiiiTest {

    private lateinit var prefs: Preferences
    private lateinit var logger: SessionLogger

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        logger = SessionLogger(prefs, GameEventBus())
        RequestLogger.currentRound = { 0 }
        // Force unresolved labels so assertions stay stable without depending on ItemDatabase.
        RequestLogger.itemNameById = { null }
        ChoiceCombatAshState.reset()
    }

    // ── parseTransferItems ──────────────────────────────────────────────────

    @Test
    fun parseTransferItems_multiItem() {
        val url = "storage.php?action=pull&whichitem1=123&howmany1=2&whichitem2=456&howmany2=1"
        val items = RequestLogger.parseTransferItems(url)
        assertEquals(2, items.size)
        assertEquals(123 to 2, items[0])
        assertEquals(456 to 1, items[1])
    }

    @Test
    fun parseTransferItems_singleItem() {
        val url = "closet.php?action=closetpull&whichitem=789&howmany=5"
        val items = RequestLogger.parseTransferItems(url)
        assertEquals(1, items.size)
        assertEquals(789 to 5, items[0])
    }

    @Test
    fun parseTransferItems_singleItem_qtyFallback() {
        val url = "closet.php?action=closetpush&whichitem=42&quantity=3"
        val items = RequestLogger.parseTransferItems(url)
        assertEquals(1, items.size)
        assertEquals(42 to 3, items[0])
    }

    @Test
    fun parseTransferItems_noItems_returnsEmpty() {
        val url = "storage.php?action=pullall"
        val items = RequestLogger.parseTransferItems(url)
        assertTrue(items.isEmpty())
    }

    @Test
    fun parseTransferItems_multiItem_qtyFallback() {
        val url = "clan_stash.php?action=addgoodies&whichitem1=10&qty1=7&whichitem2=20&quantity2=4"
        val items = RequestLogger.parseTransferItems(url)
        assertEquals(2, items.size)
        assertEquals(10 to 7, items[0])
        assertEquals(20 to 4, items[1])
    }

    // ── formatTransferLog ───────────────────────────────────────────────────

    @Test
    fun formatTransferLog_noItems() {
        val msg = RequestLogger.formatTransferLog("pull", emptyList())
        assertEquals("pull", msg)
    }

    @Test
    fun formatTransferLog_withItems() {
        // Item IDs won't resolve names in test, so they become "item #N"
        val msg = RequestLogger.formatTransferLog("pull", listOf(123 to 1, 456 to 3))
        assertEquals("pull: item #123, 3 item #456", msg)
    }

    @Test
    fun formatTransferLog_withMeat() {
        val msg = RequestLogger.formatTransferLog("pull", listOf(10 to 1), meat = 500)
        assertEquals("pull: item #10, 500 Meat", msg)
    }

    @Test
    fun formatTransferLog_meatOnly() {
        val msg = RequestLogger.formatTransferLog("pull", emptyList(), meat = 1000)
        assertEquals("pull: 1000 Meat", msg)
    }

    // ── registerStorage pull with items ─────────────────────────────────────

    @Test
    fun registerStorage_pull_multiItem() {
        assertTrue(
            RequestLogger.registerRequest(
                "storage.php?action=pull&whichitem1=123&howmany1=2&whichitem2=456&howmany2=1",
                logger,
                prefs,
            ),
        )
        val lines = logger.recentLines()
        assertTrue(lines.any { it.startsWith("pull: ") })
        assertTrue(lines.any { it.contains("item #123") })
        assertTrue(lines.any { it.contains("item #456") })
    }

    @Test
    fun registerStorage_pull_noItems() {
        assertTrue(
            RequestLogger.registerRequest(
                "storage.php?action=pull",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it == "pull" })
    }

    @Test
    fun registerStorage_pullall_unchanged() {
        assertTrue(
            RequestLogger.registerRequest(
                "storage.php?action=pullall",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it == "Emptying storage" })
    }

    @Test
    fun registerStorage_takemeat_unchanged() {
        assertTrue(
            RequestLogger.registerRequest(
                "storage.php?action=takemeat&amt=5000",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it == "pull: 5000 Meat" })
    }

    // ── registerCloset take/add with items ──────────────────────────────────

    @Test
    fun registerCloset_take_withItems() {
        assertTrue(
            RequestLogger.registerRequest(
                "closet.php?action=closetpull&whichitem=789&howmany=5",
                logger,
                prefs,
            ),
        )
        val lines = logger.recentLines()
        assertTrue(lines.any { it.startsWith("take from closet: ") })
        assertTrue(lines.any { it.contains("item #789") })
    }

    @Test
    fun registerCloset_add_withItems() {
        assertTrue(
            RequestLogger.registerRequest(
                "inventory.php?action=closetpush&whichitem1=10&howmany1=3&whichitem2=20&howmany2=1",
                logger,
                prefs,
            ),
        )
        val lines = logger.recentLines()
        assertTrue(lines.any { it.startsWith("add to closet: ") })
        assertTrue(lines.any { it.contains("item #10") })
        assertTrue(lines.any { it.contains("item #20") })
    }

    @Test
    fun registerCloset_meat_unchanged() {
        assertTrue(
            RequestLogger.registerRequest(
                "closet.php?action=addtakeclosetmeat&addtake=add&quantity=1000",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it == "add to closet: 1000 Meat" })
    }

    // ── registerDisplayCase take/put with items ─────────────────────────────

    @Test
    fun registerDisplayCase_take_withItems() {
        assertTrue(
            RequestLogger.registerRequest(
                "managecollection.php?action=take&whichitem1=100&howmany1=2",
                logger,
                prefs,
            ),
        )
        val lines = logger.recentLines()
        assertTrue(lines.any { it.startsWith("remove from display case: ") })
        assertTrue(lines.any { it.contains("item #100") })
    }

    @Test
    fun registerDisplayCase_put_withItems() {
        assertTrue(
            RequestLogger.registerRequest(
                "managecollection.php?action=put&whichitem=55&howmany=10",
                logger,
                prefs,
            ),
        )
        val lines = logger.recentLines()
        assertTrue(lines.any { it.startsWith("put in display case: ") })
        assertTrue(lines.any { it.contains("item #55") })
    }

    @Test
    fun registerDisplayCase_visit_unchanged() {
        assertTrue(
            RequestLogger.registerRequest(
                "managecollection.php?action=list",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it == "Visiting Display Case" })
    }

    // ── registerClanStash take/add with items ───────────────────────────────

    @Test
    fun registerClanStash_take_withItems() {
        assertTrue(
            RequestLogger.registerRequest(
                "clan_stash.php?action=takegoodies&whichitem1=200&howmany1=1&whichitem2=300&howmany2=5",
                logger,
                prefs,
            ),
        )
        val lines = logger.recentLines()
        assertTrue(lines.any { it.startsWith("remove from stash: ") })
        assertTrue(lines.any { it.contains("item #200") })
        assertTrue(lines.any { it.contains("item #300") })
    }

    @Test
    fun registerClanStash_add_withItems() {
        assertTrue(
            RequestLogger.registerRequest(
                "clan_stash.php?action=addgoodies&whichitem=42&howmany=3",
                logger,
                prefs,
            ),
        )
        val lines = logger.recentLines()
        assertTrue(lines.any { it.startsWith("add to stash: ") })
        assertTrue(lines.any { it.contains("item #42") })
    }

    @Test
    fun registerClanStash_contribute_meat_unchanged() {
        assertTrue(
            RequestLogger.registerRequest(
                "clan_stash.php?action=contribute&howmuch=5000",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it == "add to stash: 5000 Meat" })
    }

    @Test
    fun registerClanStash_visit_unchanged() {
        // Bare URL without `?` is skipped by doRegister's no-query gate (desktop visit
        // lines require a query or specialized early claim). With a query, log the visit.
        assertFalse(
            RequestLogger.registerRequest(
                "clan_stash.php",
                logger,
                prefs,
            ),
        )
        assertTrue(
            RequestLogger.registerRequest(
                "clan_stash.php?pwd=x",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it == "Visiting Clan Stash" })
    }
}
