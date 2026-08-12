package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.chat.PlayerIdRegistry
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BreakfastItemIds
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WishEquipCliSupportTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @AfterTest
    fun clearRegistry() {
        PlayerIdRegistry.clearForTest()
    }

    @Test
    fun genie_resolveWish_aliases() {
        assertEquals("I was rich", GenieRequest.resolveWish("meat").getOrNull())
        assertEquals("I was a little bit taller", GenieRequest.resolveWish("stat mus").getOrNull())
        assertEquals("I was a baller", GenieRequest.resolveWish("stat mox").getOrNull())
        assertEquals("I was big", GenieRequest.resolveWish("stat all").getOrNull())
        assertEquals("for a pony", GenieRequest.resolveWish("item pony").getOrNull())
        assertEquals("for more wishes", GenieRequest.resolveWish("item pocket").getOrNull())
        assertEquals("custom text", GenieRequest.resolveWish("wish custom text").getOrNull())
        assertTrue(GenieRequest.resolveWish("freedom").isFailure)
        assertTrue(GenieRequest.resolveWish("monster foo").isFailure)
    }

    @Test
    fun genie_selectWishItem_prefersBottleThenPocket() {
        val p = prefs { putInt(GenieRequest.WISHES_USED_PREF, 0) }
        assertEquals(
            BreakfastItemIds.GENIE_BOTTLE_ID,
            GenieRequest.selectWishItem(p, { id -> if (id == BreakfastItemIds.GENIE_BOTTLE_ID) 1 else 0 }, false),
        )
        val usedUp = prefs { putInt(GenieRequest.WISHES_USED_PREF, 3) }
        assertEquals(
            GenieRequest.POCKET_WISH_ID,
            GenieRequest.selectWishItem(
                usedUp,
                { id ->
                    when (id) {
                        BreakfastItemIds.GENIE_BOTTLE_ID -> 1
                        GenieRequest.POCKET_WISH_ID -> 2
                        else -> 0
                    }
                },
                false,
            ),
        )
        assertNull(
            GenieRequest.selectWishItem(p, { 0 }, false),
        )
    }

    @Test
    fun genie_visitChoice_and_postChoice() {
        val p = prefs()
        GenieRequest.visitChoice("You have 2 wishes left today.", p)
        assertEquals(1, p.getInt(GenieRequest.WISHES_USED_PREF, 0))

        val p2 = prefs { putInt(GenieRequest.WISHES_USED_PREF, 1) }
        GenieRequest.postChoice("You acquire an effect: Foo", "to be Foo", p2, usedPocketWish = false)
        assertEquals(2, p2.getInt(GenieRequest.WISHES_USED_PREF, 0))
    }

    @Test
    fun genie_pocketMoreWishes_guard() {
        val p = prefs { putInt(GenieRequest.WISHES_USED_PREF, 3) }
        assertNotNull(
            GenieRequest.preflightPocketMoreWishes(
                "for more wishes",
                p,
                { if (it == BreakfastItemIds.GENIE_BOTTLE_ID) 1 else 0 },
                false,
            ),
        )
    }

    @Test
    fun monkeypaw_substring_helpers() {
        assertEquals("Plain Effect", MonkeyPawRequest.getValidEffectSubstring("Plain Effect"))
        assertTrue(MonkeyPawRequest.resolveWish("wish something").getOrNull() == "something")
        assertTrue(MonkeyPawRequest.resolveWish("").isFailure)
    }

    @Test
    fun monkeypaw_preflight_and_prefs() {
        val err = MonkeyPawRequest.preflightError(prefs(), null) { 0 }
        assertEquals("You do not have a cursed monkey paw.", err)

        val used = prefs { putInt(MonkeyPawRequest.WISHES_USED_PREF, 5) }
        assertEquals(
            "You have been cursed enough today.",
            MonkeyPawRequest.preflightError(used, null) { 1 },
        )

        val p = prefs()
        MonkeyPawRequest.visitChoice("It has 3 fingers held up expectantly.", p)
        assertEquals(2, p.getInt(MonkeyPawRequest.WISHES_USED_PREF, 0))

        val p2 = prefs { putInt(MonkeyPawRequest.WISHES_USED_PREF, 1) }
        MonkeyPawRequest.postChoice("Wish granted.", p2)
        assertEquals(2, p2.getInt(MonkeyPawRequest.WISHES_USED_PREF, 0))
    }

    @Test
    fun monorail_parse_setsFavored() {
        val p = prefs()
        MonorailRequest.parseResponse("ok", p)
        assertTrue(p.getBoolean(MonorailRequest.FAVORED_PREF, false))
    }

    @Test
    fun toggle_preflight_requiresInterest() {
        assertEquals(
            "You don't have an effect to toggle.",
            ToggleInterestRequest.preflightError(EffectState()),
        )
        val ok = EffectState(
            effects = listOf(
                EffectData(id = ToggleInterestRequest.INTENSELY_INTERESTED, name = "Intensely Interested", duration = 10),
            ),
        )
        assertNull(ToggleInterestRequest.preflightError(ok))
    }

    @Test
    fun crossstreams_preflight_and_parse() {
        assertEquals(
            "Do not have a Proton Accelerator Pack",
            CrossStreamsRequest.preflightError(prefs(), null) { 0 },
        )
        val crossed = prefs { putBoolean(CrossStreamsRequest.STREAMS_CROSSED_PREF, true) }
        assertEquals(
            "Have already crossed streams today",
            CrossStreamsRequest.preflightError(crossed, null) { 1 },
        )

        val p = prefs()
        CrossStreamsRequest.parseResponse(
            "creating an intense but localized nuclear reaction",
            p,
        )
        assertTrue(p.getBoolean(CrossStreamsRequest.STREAMS_CROSSED_PREF, false))
    }

    @Test
    fun crossstreams_resolveTargetIdCached() {
        PlayerIdRegistry.register("Bob", "12345")
        assertEquals("12345", CrossStreamsRequest.resolveTargetIdCached("Bob"))
        assertEquals("999", CrossStreamsRequest.resolveTargetIdCached("999"))
        assertNull(CrossStreamsRequest.resolveTargetIdCached("Unknown Player"))
        assertFalse(GenieRequest.isCombatWish("to be strong"))
        assertTrue(GenieRequest.isCombatWish("you were free"))
    }
}
