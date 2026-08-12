package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.clan.ClanLoungeVipSync
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VipBoostCliSupportTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun findShowerOption_matchesTempAndEffect() {
        assertEquals(1, ClanLoungeVipOptions.findShowerOption("cold"))
        assertEquals(1, ClanLoungeVipOptions.findShowerOption("ice"))
        assertEquals(2, ClanLoungeVipOptions.findShowerOption("cool"))
        assertEquals(2, ClanLoungeVipOptions.findShowerOption("mox"))
        assertEquals(5, ClanLoungeVipOptions.findShowerOption("hot"))
        assertEquals(5, ClanLoungeVipOptions.findShowerOption("mp"))
        assertEquals(0, ClanLoungeVipOptions.findShowerOption("boiling"))
    }

    @Test
    fun findSwimmingOption_matchesActionAndEffect() {
        assertEquals(1, ClanLoungeVipOptions.findSwimmingOption("cannon"))
        assertEquals(1, ClanLoungeVipOptions.findSwimmingOption("item"))
        assertEquals(2, ClanLoungeVipOptions.findSwimmingOption("laps"))
        assertEquals(2, ClanLoungeVipOptions.findSwimmingOption("ml"))
        assertEquals(3, ClanLoungeVipOptions.findSwimmingOption("sprint"))
        assertEquals(3, ClanLoungeVipOptions.findSwimmingOption("noncombat"))
        assertEquals(0, ClanLoungeVipOptions.findSwimmingOption("dive"))
    }

    @Test
    fun pillkeeper_resolve_days() {
        assertEquals(1, PillKeeperRequest.resolve("explode")?.option)
        assertEquals(2, PillKeeperRequest.resolve("extend")?.option)
        assertEquals(3, PillKeeperRequest.resolve("noncombat")?.option)
        assertEquals(7, PillKeeperRequest.resolve("lucky")?.option)
        assertEquals(8, PillKeeperRequest.resolve("random")?.option)
        assertTrue(PillKeeperRequest.resolve("free explode")?.wantFree == true)
        assertNull(PillKeeperRequest.resolve(""))
        assertNull(PillKeeperRequest.resolve("xyz"))
    }

    @Test
    fun pillkeeper_preflight_requiresItem() {
        val err = PillKeeperRequest.preflightError(
            PillKeeperRequest.resolve("explode")!!,
            state = null,
            preferences = prefs(),
            hasPillKeeper = false,
        )
        assertEquals("You need an Eight Days a Week Pill Keeper", err)
    }

    @Test
    fun pillkeeper_preflight_spleenWhenFreeUsed() {
        val p = prefs { putBoolean("_freePillKeeperUsed", true) }
        val state = CharacterState(spleenUsed = 14, spleenLimit = 15)
        val err = PillKeeperRequest.preflightError(
            PillKeeperRequest.resolve("explode")!!,
            state = state,
            preferences = p,
            hasPillKeeper = true,
        )
        assertEquals("Your spleen has been abused enough today", err)
    }

    @Test
    fun photobooth_effect_options() {
        assertEquals(1, PhotoBoothRequest.findEffectOption("wild"))
        assertEquals(2, PhotoBoothRequest.findEffectOption("tower"))
        assertEquals(3, PhotoBoothRequest.findEffectOption("space"))
        assertNull(PhotoBoothRequest.findEffectOption("selfie"))
    }

    @Test
    fun photobooth_preflight_needsBooth() {
        val p = prefs()
        assertEquals(
            "Your clan needs a photo booth.",
            PhotoBoothRequest.preflightError(p),
        )
        p.setBoolean(ClanLoungeSync.CLAN_HAS_PHOTO_BOOTH_PREF, true)
        assertNull(PhotoBoothRequest.preflightError(p))
        p.setInt(PhotoBoothRequest.EFFECTS_PREF, 3)
        assertEquals("You cannot get any more effects.", PhotoBoothRequest.preflightError(p))
    }

    @Test
    fun vipSync_setsAprilShower() {
        val p = prefs()
        ClanLoungeVipSync.syncShowerFromResponse(
            "this is way too hot for comfort",
            "takeshower",
            p,
        )
        assertTrue(p.getBoolean(ClanLoungeVipSync.APRIL_SHOWER_PREF, false))
    }

    @Test
    fun vipSync_setsOlympicSwim() {
        val p = prefs()
        ClanLoungeVipSync.syncSwimFromResponse(
            "You swam 42 laps in the pool.",
            "goswimming",
            p,
        )
        assertTrue(p.getBoolean(ClanLoungeVipSync.OLYMPIC_SWIMMING_POOL_PREF, false))
    }

    @Test
    fun vipSync_setsBallpit() {
        val p = prefs()
        ClanLoungeVipSync.syncBallpitFromResponse("You play in the ball pit. Wheeeeeee!", p)
        assertTrue(p.getBoolean(ClanLoungeVipSync.BALLPIT_PREF, false))
        val p2 = prefs()
        ClanLoungeVipSync.syncBallpitFromResponse("Something else", p2)
        assertFalse(p2.getBoolean(ClanLoungeVipSync.BALLPIT_PREF, false))
    }

    @Test
    fun clanLoungeRequest_delegatesFinders() {
        assertEquals(4, ClanLoungeRequest.findShowerOption("warm"))
        assertEquals(2, ClanLoungeRequest.findSwimmingOption("ml"))
        assertNotNull(ClanLoungeVipOptions.swimmingSubaction(1))
    }
}
