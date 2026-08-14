package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.ash.resolveCliConsumeItemId
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
    fun findPoolGame_matchesStanceStatEffectAndIndex() {
        assertEquals(1, ClanLoungeVipOptions.findPoolGame("1"))
        assertEquals(1, ClanLoungeVipOptions.findPoolGame("aggressive"))
        assertEquals(1, ClanLoungeVipOptions.findPoolGame("mus"))
        assertEquals(1, ClanLoungeVipOptions.findPoolGame("billiards"))
        assertEquals(2, ClanLoungeVipOptions.findPoolGame("strategic"))
        assertEquals(2, ClanLoungeVipOptions.findPoolGame("myst"))
        assertEquals(3, ClanLoungeVipOptions.findPoolGame("stylish"))
        assertEquals(3, ClanLoungeVipOptions.findPoolGame("mox"))
        assertEquals(0, ClanLoungeVipOptions.findPoolGame("snooker"))
        assertEquals(0, ClanLoungeVipOptions.findPoolGame("4"))
    }

    @Test
    fun findSong_matchesModifierEffectAndIndex() {
        assertEquals(1, ClanRumpusRequest.findSong("1"))
        assertEquals(1, ClanRumpusRequest.findSong("meat"))
        assertEquals(1, ClanRumpusRequest.findSong("Material Witness"))
        assertEquals(2, ClanRumpusRequest.findSong("stats"))
        assertEquals(2, ClanRumpusRequest.findSong("No Worries"))
        assertEquals(3, ClanRumpusRequest.findSong("item"))
        assertEquals(4, ClanRumpusRequest.findSong("initiative"))
        assertEquals(4, ClanRumpusRequest.findSong("Metal Speed"))
        assertEquals(0, ClanRumpusRequest.findSong("jazz"))
        assertEquals(0, ClanRumpusRequest.findSong("5"))
    }

    @Test
    fun vipSync_setsPoolGames() {
        val p = prefs()
        ClanLoungeVipSync.syncPoolGameFromResponse(
            "You skillfully defeat Bob and take control of the table. Go you!",
            p,
        )
        assertEquals(1, p.getInt(ClanLoungeVipSync.POOL_GAMES_PREF, 0))
        ClanLoungeVipSync.syncPoolGameFromResponse(
            "You're kind of pooled out for today. Maybe you'll be in the mood to play again tomorrow.",
            p,
        )
        assertEquals(3, p.getInt(ClanLoungeVipSync.POOL_GAMES_PREF, 0))
    }

    @Test
    fun vipSync_setsJukebox() {
        val p = prefs()
        ClanLoungeVipSync.syncJukeboxFromResponse(p)
        assertTrue(p.getBoolean(ClanLoungeVipSync.JUKEBOX_PREF, false))
    }

    @Test
    fun clanLoungeRequest_delegatesFinders() {
        assertEquals(4, ClanLoungeRequest.findShowerOption("warm"))
        assertEquals(2, ClanLoungeRequest.findSwimmingOption("ml"))
        assertEquals(1, ClanLoungeRequest.findPoolGame("aggressive"))
        assertNotNull(ClanLoungeVipOptions.swimmingSubaction(1))
    }

    @Test
    fun parseConsumeQtyName_qtyAndBare() {
        assertEquals(1 to "savage macho dog", net.sourceforge.kolmafia.ash.parseConsumeQtyName("1 savage macho dog"))
        assertEquals(2 to "Lucky Lindy", net.sourceforge.kolmafia.ash.parseConsumeQtyName("2 Lucky Lindy"))
        assertEquals(1 to "salmon", net.sourceforge.kolmafia.ash.parseConsumeQtyName("salmon"))
        assertNull(net.sourceforge.kolmafia.ash.parseConsumeQtyName(""))
        assertNull(net.sourceforge.kolmafia.ash.parseConsumeQtyName("   "))
    }

    @Test
    fun parseConsumeItemList_splitsCommaQtyNames() {
        val list = net.sourceforge.kolmafia.ash.parseConsumeItemList(
            "1 Trivial Avocations Card: What?, 1 Trivial Avocations Card: When?, " +
                "1 Trivial Avocations Card: Who?, 1 Trivial Avocations Card: Where?",
        )
        assertEquals(4, list.size)
        assertEquals(1 to "Trivial Avocations Card: What?", list[0])
        assertEquals(1 to "Trivial Avocations Card: When?", list[1])
        assertEquals(1 to "Trivial Avocations Card: Who?", list[2])
        assertEquals(1 to "Trivial Avocations Card: Where?", list[3])
        assertEquals(
            listOf(2 to "salmon", 1 to "seal tooth"),
            net.sourceforge.kolmafia.ash.parseConsumeItemList("2 salmon, seal tooth"),
        )
        assertTrue(net.sourceforge.kolmafia.ash.parseConsumeItemList("").isEmpty())
    }

    @Test
    fun stripEitherConsumePrefix_stripsKeyword() {
        assertEquals(
            true to "1 serum of sarcasm, 1 evil serum of sarcasm",
            net.sourceforge.kolmafia.ash.stripEitherConsumePrefix(
                "either 1 serum of sarcasm, 1 evil serum of sarcasm",
            ),
        )
        assertEquals(
            true to "1 serum of sarcasm",
            net.sourceforge.kolmafia.ash.stripEitherConsumePrefix("Either 1 serum of sarcasm"),
        )
        assertEquals(
            false to "1 salmon, seal tooth",
            net.sourceforge.kolmafia.ash.stripEitherConsumePrefix("1 salmon, seal tooth"),
        )
    }

    @Test
    fun parseConsumeItemList_afterEitherStrip() {
        val (either, rest) = net.sourceforge.kolmafia.ash.stripEitherConsumePrefix(
            "either 1 serum of sarcasm, 1 evil serum of sarcasm",
        )
        assertTrue(either)
        assertEquals(
            listOf(1 to "serum of sarcasm", 1 to "evil serum of sarcasm"),
            net.sourceforge.kolmafia.ash.parseConsumeItemList(rest),
        )
    }

    @Test
    fun hotDogAndSpeakeasy_registryDetectsLoungeConsumables() {
        assertTrue(net.sourceforge.kolmafia.data.HotDogDatabase.isHotDog("savage macho dog"))
        assertTrue(net.sourceforge.kolmafia.data.HotDogDatabase.isHotDog("basic hot dog"))
        assertFalse(net.sourceforge.kolmafia.data.HotDogDatabase.isHotDog("salmon"))
        assertTrue(net.sourceforge.kolmafia.data.SpeakeasyDatabase.isSpeakeasyDrink("Lucky Lindy"))
        assertFalse(net.sourceforge.kolmafia.data.SpeakeasyDatabase.isSpeakeasyDrink("booze"))
    }

    @Test
    fun cafePurchaseMenu_registryDetectsCafeConsumables() {
        assertTrue(net.sourceforge.kolmafia.ash.isCafePurchaseMenuItem("Peche a la Frog"))
        assertTrue(net.sourceforge.kolmafia.ash.isCafePurchaseMenuItem("Petite Porter"))
        assertTrue(net.sourceforge.kolmafia.ash.isCafePurchaseMenuItem("Imp Ale"))
        assertFalse(net.sourceforge.kolmafia.ash.isCafePurchaseMenuItem("salmon"))
    }

    @Test
    fun resolveCliConsumeItemId_bangAndDb() {
        val p = prefs {
            putString("lastBangPotion819", "explosiveness")
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) =
                if (name.equals("seal tooth", ignoreCase = true)) {
                    net.sourceforge.kolmafia.data.ItemData(
                        id = 42, name = "seal tooth", descId = "", image = "",
                        primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                        secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null,
                    )
                } else {
                    null
                }
        }
        val lib = net.sourceforge.kolmafia.ash.GameRuntimeLibrary(
            gameDatabase = db,
            preferences = p,
        )
        val prints = mutableListOf<String>()
        assertEquals(42, lib.resolveCliConsumeItemId("seal tooth", prints::add))
        assertEquals(819, lib.resolveCliConsumeItemId("potion of explosiveness", prints::add))
        assertNull(lib.resolveCliConsumeItemId("potion of mystery", prints::add))
        assertTrue(prints.any { it.contains("You have not yet identified the potion of mystery") })
    }
}
