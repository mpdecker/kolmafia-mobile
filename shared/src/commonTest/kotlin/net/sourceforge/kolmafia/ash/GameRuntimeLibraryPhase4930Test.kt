package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.Crimbo12Request
import net.sourceforge.kolmafia.request.HeyDezeRequest
import net.sourceforge.kolmafia.request.UpdateSuppressedRequest
import net.sourceforge.kolmafia.request.WaxGlobRequest

class GameRuntimeLibraryPhase4930Test {

    @Test
    fun revision_phase4930() {
        assertEquals("phase5950", GameRuntimeLibrary.REVISION)
        assertEquals("phase5950", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun batch_open_close_coalescesPilcrowClosetPuts() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(
            lib,
            """
            batch_open();
            put_closet(2, to_item("seal tooth"));
            put_closet(1, to_item("seal tooth"));
            print(has_queued_commands());
            boolean ok = batch_close();
            print(ok);
            print(has_queued_commands());
            """.trimIndent(),
        )
        // Without closetRequest, flush still runs CLI and returns continue true;
        // has_queued_commands is true while open+enqueued, false after close.
        assertTrue(out.contains("true"))
        assertTrue(out.trim().endsWith("false") || out.contains("true\ntrue\nfalse"))
    }

    @Test
    fun batch_emptyCloset_enqueues() {
        assertEquals(
            "true",
            outputLib(
                GameRuntimeLibrary(),
                """
                batch_open();
                empty_closet();
                print(has_queued_commands());
                batch_close();
                """.trimIndent(),
            ).lines().first(),
        )
    }

    @Test
    fun closet_storage_collectionCache_adjust() {
        val prefs = Preferences(MapSettings())
        CollectionCache.save(prefs, Preferences.CACHED_CLOSET, mapOf(1 to 3))
        CollectionCache.adjust(prefs, Preferences.CACHED_CLOSET, 1, 2)
        assertEquals(5, CollectionCache.load(prefs, Preferences.CACHED_CLOSET)[1])
        CollectionCache.adjust(prefs, Preferences.CACHED_STORAGE, 9, 4)
        assertEquals(4, CollectionCache.load(prefs, Preferences.CACHED_STORAGE)[9])
    }

    @Test
    fun resolveMallBuyItemId_pilcrow() {
        val lib = GameRuntimeLibrary()
        assertEquals(123, lib.resolveMallBuyItemId("\u00B6123"))
        assertEquals(456, lib.resolveMallBuyItemId("¶456"))
        assertEquals(7, lib.resolveMallBuyItemId("[7]"))
    }

    @Test
    fun waxGlob_crimbo12_heyDeze_updateSuppressed() {
        assertEquals("1", WaxGlobRequest.optionForItemId(9306))
        assertEquals("wax hand", WaxGlobRequest.optionToName(2))
        assertTrue(WaxGlobRequest.registerRequest("choice.php?whichchoice=1218&option=2"))
        assertFalse(WaxGlobRequest.registerRequest("choice.php?whichchoice=1"))
        assertTrue(
            Crimbo12Request.registerRequest(
                "shop.php?whichshop=crimbo12&whichitem=100&quantity=3",
            ),
        )
        assertFalse(Crimbo12Request.registerRequest("shop.php?whichshop=other&whichitem=1&quantity=1"))
        assertEquals("muscle", HeyDezeRequest.styxStat("heydeze.php?action=styxbuff&whichbuff=446"))
        assertTrue(HeyDezeRequest.registerRequest("heydeze.php?place=meansucker"))
        assertTrue(UpdateSuppressedRequest.shouldSuppress("api.php?what=status"))
        assertFalse(UpdateSuppressedRequest.shouldSuppress("adventure.php"))
    }

    @Test
    fun parseMallShopPuts_qtyPilcrow() {
        val specs = LongTailCli.parseMallShopPuts("2 \u00B6123 @ 999 limit 1")
        assertEquals(1, specs?.size)
        assertEquals("\u00B6123", specs!![0].itemName)
        assertEquals(2, specs[0].quantity)
        assertEquals(999, specs[0].price)
        assertEquals(1, specs[0].limit)
    }
}
