package net.sourceforge.kolmafia.banish

import kotlin.test.Test
import kotlin.test.assertEquals

class BanisherExpansionTest {

    @Test fun balefulHowl_roundTrips() =
        assertEquals(Banisher.BALEFUL_HOWL, Banisher.fromName("baleful howl"))

    @Test fun baseballDiamond_roundTrips() =
        assertEquals(Banisher.BASEBALL_DIAMOND, Banisher.fromName("Baseball Diamond"))

    @Test fun batterUp_roundTrips() =
        assertEquals(Banisher.BATTER_UP, Banisher.fromName("batter up!"))

    @Test fun beAMindMaster_roundTrips() =
        assertEquals(Banisher.BE_A_MIND_MASTER, Banisher.fromName("Be a Mind Master"))

    @Test fun blartSprayWide_roundTrips() =
        assertEquals(Banisher.BLART_SPRAY_WIDE, Banisher.fromName("B. L. A. R. T. Spray (wide)"))

    @Test fun bundleOfFragrantHerbs_roundTrips() =
        assertEquals(Banisher.BUNDLE_OF_FRAGRANT_HERBS, Banisher.fromName("bundle of &quot;fragrant&quot; herbs"))

    @Test fun classyMonkey_roundTrips() =
        assertEquals(Banisher.CLASSY_MONKEY, Banisher.fromName("classy monkey"))

    @Test fun cocktailNapkin_roundTrips() =
        assertEquals(Banisher.COCKTAIL_NAPKIN, Banisher.fromName("cocktail napkin"))

    @Test fun crimbuccaneerRiggingLasso_roundTrips() =
        assertEquals(Banisher.CRIMBUCCANEER_RIGGING_LASSO, Banisher.fromName("Crimbuccaneer rigging lasso"))

    @Test fun crystalSkull_roundTrips() =
        assertEquals(Banisher.CRYSTAL_SKULL, Banisher.fromName("crystal skull"))

    @Test fun curseOfVacation_roundTrips() =
        assertEquals(Banisher.CURSE_OF_VACATION, Banisher.fromName("curse of vacation"))

    @Test fun deathchucks_roundTrips() =
        assertEquals(Banisher.DEATHCHUCKS, Banisher.fromName("deathchucks"))

    @Test fun dirtyStinkbomb_roundTrips() =
        assertEquals(Banisher.DIRTY_STINKBOMB, Banisher.fromName("dirty stinkbomb"))

    @Test fun gingerbreadRestrainingOrder_roundTrips() =
        assertEquals(Banisher.GINGERBREAD_RESTRAINING_ORDER, Banisher.fromName("gingerbread restraining order"))

    @Test fun glitchedMalware_roundTrips() =
        assertEquals(Banisher.GLITCHED_MALWARE, Banisher.fromName("Deploy Glitched Malware"))

    @Test fun haroldsBell_roundTrips() =
        assertEquals(Banisher.HAROLDS_BELL, Banisher.fromName("harold's bell"))

    @Test fun heartstoneBanish_roundTrips() =
        assertEquals(Banisher.HEARTSTONE_BANISH, Banisher.fromName("Heartstone %banish"))

    @Test fun howlOfTheAlpha_roundTrips() =
        assertEquals(Banisher.HOWL_OF_THE_ALPHA, Banisher.fromName("howl of the alpha"))

    @Test fun humanMusk_roundTrips() =
        assertEquals(Banisher.HUMAN_MUSK, Banisher.fromName("human musk"))

    @Test fun iceHotelBell_roundTrips() =
        assertEquals(Banisher.ICE_HOTEL_BELL, Banisher.fromName("ice hotel bell"))

    @Test fun leftZootKick_roundTrips() =
        assertEquals(Banisher.LEFT_ZOOT_KICK, Banisher.fromName("Left %n Kick"))

    @Test fun licoriceRope_roundTrips() =
        assertEquals(Banisher.LICORICE_ROPE, Banisher.fromName("licorice rope"))

    @Test fun markYourTerritory_roundTrips() =
        assertEquals(Banisher.MARK_YOUR_TERRITORY, Banisher.fromName("Mark Your Territory"))

    @Test fun monkeySlap_roundTrips() =
        assertEquals(Banisher.MONKEY_SLAP, Banisher.fromName("Monkey Slap"))

    @Test fun nanorhino_roundTrips() =
        assertEquals(Banisher.NANORHINO, Banisher.fromName("nanorhino"))

    @Test fun peelOut_roundTrips() =
        assertEquals(Banisher.PEEL_OUT, Banisher.fromName("peel out"))

    @Test fun peppermintBomb_roundTrips() =
        assertEquals(Banisher.PEPPERMINT_BOMB, Banisher.fromName("peppermint bomb"))

    @Test fun pulledIndigoTaffy_roundTrips() =
        assertEquals(Banisher.PULLED_INDIGO_TAFFY, Banisher.fromName("pulled indigo taffy"))

    @Test fun punchOutYourFoe_roundTrips() =
        assertEquals(Banisher.PUNCH_OUT_YOUR_FOE, Banisher.fromName("Punch Out your Foe"))

    @Test fun puntAosol_roundTrips() =
        assertEquals(Banisher.PUNT_AOSOL, Banisher.fromName("[28021]Punt"))

    @Test fun puntWereprof_roundTrips() =
        assertEquals(Banisher.PUNT_WEREPROF, Banisher.fromName("[7510]Punt"))

    @Test fun rightZootKick_roundTrips() =
        assertEquals(Banisher.RIGHT_ZOOT_KICK, Banisher.fromName("Right %n Kick"))

    @Test fun roarLikeALion_roundTrips() =
        assertEquals(Banisher.ROAR_LIKE_A_LION, Banisher.fromName("Roar like a Lion"))

    @Test fun seadentLightning_roundTrips() =
        assertEquals(Banisher.SEADENT_LIGHTNING, Banisher.fromName("Sea *dent"))

    @Test fun showBoringPictures_roundTrips() =
        assertEquals(Banisher.SHOW_YOUR_BORING_FAMILIAR_PICTURES, Banisher.fromName("Show your boring familiar pictures"))

    @Test fun smokeGrenade_roundTrips() =
        assertEquals(Banisher.SMOKE_GRENADE, Banisher.fromName("smoke grenade"))

    @Test fun spookyMusicBox_roundTrips() =
        assertEquals(Banisher.SPOOKY_MUSIC_BOX_MECHANISM, Banisher.fromName("spooky music box mechanism"))

    @Test fun splitPeaSoup_roundTrips() =
        assertEquals(Banisher.SPLIT_PEA_SOUP, Banisher.fromName("handful of split pea soup"))

    @Test fun springKick_roundTrips() =
        assertEquals(Banisher.SPRING_KICK, Banisher.fromName("Spring Kick"))

    @Test fun staffOfStandaloneCheese_roundTrips() =
        assertEquals(Banisher.STAFF_OF_THE_STANDALONE_CHEESE, Banisher.fromName("staff of the standalone cheese"))

    @Test fun stinkyCheeseEye_roundTrips() =
        assertEquals(Banisher.STINKY_CHEESE_EYE, Banisher.fromName("stinky cheese eye"))

    @Test fun tennisBall_roundTrips() =
        assertEquals(Banisher.TENNIS_BALL, Banisher.fromName("tennis ball"))

    @Test fun throwinEmber_roundTrips() =
        assertEquals(Banisher.THROWIN_EMBER, Banisher.fromName("throwin' ember"))

    @Test fun thunderClap_roundTrips() =
        assertEquals(Banisher.THUNDER_CLAP, Banisher.fromName("thunder clap"))

    @Test fun tryptophanDart_roundTrips() =
        assertEquals(Banisher.TRYPTOPHAN_DART, Banisher.fromName("tryptophan dart"))

    @Test fun ultraHammer_roundTrips() =
        assertEquals(Banisher.ULTRA_HAMMER, Banisher.fromName("Ultra Hammer"))

    @Test fun vForVivalaMask_roundTrips() =
        assertEquals(Banisher.V_FOR_VIVALA_MASK, Banisher.fromName("v for vivala mask"))

    @Test fun walkAwayFromExplosion_roundTrips() =
        assertEquals(Banisher.WALK_AWAY_FROM_EXPLOSION, Banisher.fromName("walk away from explosion"))

    @Test fun patrioticScreech_roundTrips() =
        assertEquals(Banisher.PATRIOTIC_SCREECH, Banisher.fromName("Patriotic Screech"))

    // Reset type spot-checks
    @Test fun balefulHowl_isRollover() =
        assertEquals(ResetType.ROLLOVER, Banisher.BALEFUL_HOWL.resetType)

    @Test fun beAMindMaster_isTurns() =
        assertEquals(ResetType.TURNS, Banisher.BE_A_MIND_MASTER.resetType)

    @Test fun tennisBall_isTurnRollover() =
        assertEquals(ResetType.TURN_ROLLOVER, Banisher.TENNIS_BALL.resetType)

    @Test fun howlOfAlpha_isAvatar() =
        assertEquals(ResetType.AVATAR, Banisher.HOWL_OF_THE_ALPHA.resetType)

    @Test fun iceHouse_isNever() =
        assertEquals(ResetType.NEVER, Banisher.ICE_HOUSE.resetType)

    // Total count: 20 original + 49 new = 69 named + UNKNOWN = 70 entries
    @Test fun totalBanisherCount_is70() =
        assertEquals(70, Banisher.entries.size)
}
