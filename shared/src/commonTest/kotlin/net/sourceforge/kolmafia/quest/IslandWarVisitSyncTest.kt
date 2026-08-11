package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandWarVisitSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun sessionLogger(prefs: Preferences): SessionLogger =
        SessionLogger(prefs, GameEventBus())

    private fun sessionLog(prefs: Preferences): String =
        prefs.getString(SessionLogger.SESSION_LOG_KEY, "")

    private fun context(
        hasItem: Set<Int> = emptySet(),
        consumed: MutableList<Pair<Int, Int>> = mutableListOf(),
        isWearingWarHippyOutfit: Boolean = false,
        ascensionNumber: Int = 0,
    ): IslandWarVisitSync.IslandVisitContext =
        IslandWarVisitSync.IslandVisitContext(
            hasItemId = { id -> id in hasItem },
            consumeItem = { id, qty -> consumed.add(id to qty) },
            isWearingWarHippyOutfit = { isWearingWarHippyOutfit },
            ascensionNumber = ascensionNumber,
        )

    @Test
    fun parseQuest_concert_returnsArena() {
        assertEquals(IslandWarVisitSync.IslandSidequest.ARENA, IslandWarVisitSync.parseQuest("bigisland.php?place=concert"))
        assertEquals(IslandWarVisitSync.IslandSidequest.ARENA, IslandWarVisitSync.parseQuest("bigisland.php?action=concert"))
    }

    @Test
    fun parseQuest_sidequestUrls() {
        assertEquals(IslandWarVisitSync.IslandSidequest.JUNKYARD, IslandWarVisitSync.parseQuest("bigisland.php?action=junkman"))
        assertEquals(IslandWarVisitSync.IslandSidequest.ORCHARD, IslandWarVisitSync.parseQuest("bigisland.php?action=stand"))
        assertEquals(IslandWarVisitSync.IslandSidequest.FARM, IslandWarVisitSync.parseQuest("bigisland.php?action=farmer"))
        assertEquals(IslandWarVisitSync.IslandSidequest.NUNS, IslandWarVisitSync.parseQuest("bigisland.php?place=nunnery"))
        assertEquals(IslandWarVisitSync.IslandSidequest.LIGHTHOUSE, IslandWarVisitSync.parseQuest("bigisland.php?action=pyro"))
        assertEquals(IslandWarVisitSync.IslandSidequest.NONE, IslandWarVisitSync.parseQuest("bigisland.php"))
    }

    @Test
    fun applyFromBigIslandVisit_setsWarProgressStarted() {
        val prefs = prefs()
        prefs.setString("warProgress", "unstarted")
        assertTrue(IslandWarVisitSync.applyFromBigIslandVisit(html = "<html></html>", preferences = prefs))
        assertEquals("started", prefs.getString("warProgress", ""))
    }

    @Test
    fun applyFromBigIslandVisit_noMapPattern_leavesCountersUnchanged() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 42)
        prefs.setInt("hippiesDefeated", 57)
        IslandWarVisitSync.applyFromBigIslandVisit(html = "<html>no battlefield map</html>", preferences = prefs)
        assertEquals(42, prefs.getInt("fratboysDefeated", 0))
        assertEquals(57, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_clampsFratboysUpToImageMin() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 10)
        IslandWarVisitSync.applyFromBigIslandVisit(html = "<img src=bfleft4><img src=bfright0>", preferences = prefs)
        assertEquals(28, prefs.getInt("fratboysDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_clampsHippiesDownToImageMax() {
        val prefs = prefs()
        prefs.setInt("hippiesDefeated", 200)
        IslandWarVisitSync.applyFromBigIslandVisit(html = "<img src=bfleft0><img src=bfright10>", preferences = prefs)
        assertEquals(131, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_inRangeCounterUnchanged() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 35)
        prefs.setInt("hippiesDefeated", 120)
        IslandWarVisitSync.applyFromBigIslandVisit(html = "<img src=bfleft4><img src=bfright10>", preferences = prefs)
        assertEquals(35, prefs.getInt("fratboysDefeated", 0))
        assertEquals(120, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_image32ClampsBothTo1000() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 500)
        prefs.setInt("hippiesDefeated", 800)
        IslandWarVisitSync.applyFromBigIslandVisit(html = "<img src=bfleft32><img src=bfright32>", preferences = prefs)
        assertEquals(1000, prefs.getInt("fratboysDefeated", 0))
        assertEquals(1000, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun applyFromBigIslandVisit_bothSidesClampedIndependently() {
        val prefs = prefs()
        prefs.setInt("fratboysDefeated", 5)
        prefs.setInt("hippiesDefeated", 250)
        IslandWarVisitSync.applyFromBigIslandVisit(html = "<img src=bfleft5><img src=bfright11>", preferences = prefs)
        assertEquals(40, prefs.getInt("fratboysDefeated", 0))
        assertEquals(151, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun imageRange_image4_returns28To39() {
        assertEquals(28 to 39, IslandWarVisitSync.imageRange(4))
    }

    @Test
    fun imageRange_outOfRange_returnsNull() {
        assertEquals(null, IslandWarVisitSync.imageRange(-1))
        assertEquals(null, IslandWarVisitSync.imageRange(33))
    }

    @Test
    fun applyFromBigIslandVisit_nullPreferences_returnsFalse() {
        assertFalse(IslandWarVisitSync.applyFromBigIslandVisit(html = "<html></html>", preferences = null))
    }

    @Test
    fun parseArena_hippySong_setsSidequest() {
        val prefs = prefs()
        assertTrue(
            IslandWarVisitSync.parseArena(
                "well into the first song",
                prefs,
                context(),
            ),
        )
        assertEquals("hippy", prefs.getString("sidequestArenaCompleted", ""))
    }

    @Test
    fun parseArena_hippyFlyers_consumesJamBandFlyers() {
        val prefs = prefs()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            IslandWarVisitSync.parseArena(
                "I'll take 'em",
                prefs,
                context(hasItem = setOf(2404), consumed = consumed),
            ),
        )
        assertEquals("hippy", prefs.getString("sidequestArenaCompleted", ""))
        assertEquals(listOf(2404 to 1), consumed)
    }

    @Test
    fun parseArena_fratboyStage_setsSidequest() {
        val prefs = prefs()
        assertTrue(
            IslandWarVisitSync.parseArena(
                "has already taken the stage",
                prefs,
                context(),
            ),
        )
        assertEquals("fratboy", prefs.getString("sidequestArenaCompleted", ""))
    }

    @Test
    fun parseArena_fratboyFlyers_consumesRockBandFlyers() {
        val prefs = prefs()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            IslandWarVisitSync.parseArena(
                "I'll take them",
                prefs,
                context(hasItem = setOf(2405), consumed = consumed),
            ),
        )
        assertEquals("fratboy", prefs.getString("sidequestArenaCompleted", ""))
        assertEquals(listOf(2405 to 1), consumed)
    }

    @Test
    fun parseArena_emptyStage_setsNone() {
        val prefs = prefs()
        prefs.setString("sidequestArenaCompleted", "hippy")
        assertTrue(
            IslandWarVisitSync.parseArena(
                "The stage at the Mysterious Island Arena is empty",
                prefs,
                context(),
            ),
        )
        assertEquals("none", prefs.getString("sidequestArenaCompleted", ""))
    }

    @Test
    fun parseFarm_soybeans_setsHippy() {
        val prefs = prefs()
        assertTrue(IslandWarVisitSync.parseFarm("growing soybeans", prefs))
        assertEquals("hippy", prefs.getString("sidequestFarmCompleted", ""))
    }

    @Test
    fun parseFarm_hops_setsFratboy() {
        val prefs = prefs()
        assertTrue(IslandWarVisitSync.parseFarm("growing hops", prefs))
        assertEquals("fratboy", prefs.getString("sidequestFarmCompleted", ""))
    }

    @Test
    fun parseNunnery_wounds_setsHippy() {
        val prefs = prefs()
        assertTrue(IslandWarVisitSync.parseNunnery("tend to your wounds", prefs))
        assertEquals("hippy", prefs.getString("sidequestNunsCompleted", ""))
    }

    @Test
    fun parseNunnery_massage_setsFratboy() {
        val prefs = prefs()
        assertTrue(IslandWarVisitSync.parseNunnery("refreshing massage", prefs))
        assertEquals("fratboy", prefs.getString("sidequestNunsCompleted", ""))
    }

    @Test
    fun parseNunnery_traveler_setsNone() {
        val prefs = prefs()
        prefs.setString("sidequestNunsCompleted", "hippy")
        assertTrue(IslandWarVisitSync.parseNunnery("world-weary traveler", prefs))
        assertEquals("none", prefs.getString("sidequestNunsCompleted", ""))
    }

    @Test
    fun parseNunnery_visit_incrementsNunsVisits() {
        val prefs = prefs()
        prefs.setInt("nunsVisits", 2)
        assertTrue(IslandWarVisitSync.parseNunnery("The Sisters tend to your wounds", prefs))
        assertEquals(3, prefs.getInt("nunsVisits", 0))
    }

    @Test
    fun parseNunnery_busy_setsNunsVisits99() {
        val prefs = prefs()
        assertTrue(IslandWarVisitSync.parseNunnery("all of the Sisters are busy right now", prefs))
        assertEquals(99, prefs.getInt("nunsVisits", 0))
    }

    @Test
    fun applyFromBigIslandVisit_nunneryUrl_routesToNunneryParser() {
        val prefs = prefs()
        IslandWarVisitSync.applyFromBigIslandVisit(
            url = "bigisland.php?place=nunnery",
            html = "refreshing massage",
            preferences = prefs,
        )
        assertEquals("fratboy", prefs.getString("sidequestNunsCompleted", ""))
        assertEquals("started", prefs.getString("warProgress", ""))
    }

    @Test
    fun parseOrchard_noPhrase_returnsFalse() {
        val prefs = prefs()
        assertFalse(IslandWarVisitSync.parseOrchard("nothing here", prefs, context()))
    }

    @Test
    fun parseOrchard_hippyOutfit_setsHippyPrefsAndFilthClearance() {
        val prefs = prefs()
        assertTrue(
            IslandWarVisitSync.parseOrchard(
                "tyranny of nature",
                prefs,
                context(isWearingWarHippyOutfit = true, ascensionNumber = 7),
            ),
        )
        assertEquals("hippy", prefs.getString("sidequestOrchardCompleted", ""))
        assertEquals(7, prefs.getInt("lastFilthClearance", -1))
        assertEquals("hippy", prefs.getString("currentHippyStore", ""))
    }

    @Test
    fun parseOrchard_fratboyOutfit_setsFratboyPrefs() {
        val prefs = prefs()
        assertTrue(
            IslandWarVisitSync.parseOrchard(
                "tyranny of nature",
                prefs,
                context(ascensionNumber = 3),
            ),
        )
        assertEquals("fratboy", prefs.getString("sidequestOrchardCompleted", ""))
        assertEquals(3, prefs.getInt("lastFilthClearance", -1))
        assertEquals("fratboy", prefs.getString("currentHippyStore", ""))
    }

    @Test
    fun parseOrchard_consumesQueenHeartWhenPresent() {
        val prefs = prefs()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            IslandWarVisitSync.parseOrchard(
                "tyranny of nature",
                prefs,
                context(hasItem = setOf(2347), consumed = consumed),
            ),
        )
        assertEquals(listOf(2347 to 1), consumed)
    }

    @Test
    fun applyFromBigIslandVisit_orchardUrl_routesToOrchardParser() {
        val prefs = prefs()
        IslandWarVisitSync.applyFromBigIslandVisit(
            url = "bigisland.php?action=stand",
            html = "tyranny of nature",
            preferences = prefs,
            context = context(isWearingWarHippyOutfit = true, ascensionNumber = 5),
        )
        assertEquals("hippy", prefs.getString("sidequestOrchardCompleted", ""))
        assertEquals(5, prefs.getInt("lastFilthClearance", -1))
    }

    @Test
    fun parseLighthouse_noPhrase_returnsFalse() {
        val prefs = prefs()
        assertFalse(IslandWarVisitSync.parseLighthouse("nothing here", prefs, context()))
    }

    @Test
    fun parseLighthouse_hippyOutfit_setsHippySidequest() {
        val prefs = prefs()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            IslandWarVisitSync.parseLighthouse(
                "My bombs for you, bumpty-bumpty-bump!",
                prefs,
                context(isWearingWarHippyOutfit = true, consumed = consumed),
            ),
        )
        assertEquals("hippy", prefs.getString("sidequestLighthouseCompleted", ""))
        assertEquals(listOf(2403 to 5), consumed)
    }

    @Test
    fun parseLighthouse_fratboyOutfit_setsFratboySidequest() {
        val prefs = prefs()
        assertTrue(
            IslandWarVisitSync.parseLighthouse(
                "My bombs for you",
                prefs,
                context(),
            ),
        )
        assertEquals("fratboy", prefs.getString("sidequestLighthouseCompleted", ""))
    }

    @Test
    fun applyFromBigIslandVisit_lighthouseUrl_routesToLighthouseParser() {
        val prefs = prefs()
        val consumed = mutableListOf<Pair<Int, Int>>()
        IslandWarVisitSync.applyFromBigIslandVisit(
            url = "bigisland.php?action=pyro",
            html = "My bombs for you",
            preferences = prefs,
            context = context(consumed = consumed),
        )
        assertEquals("fratboy", prefs.getString("sidequestLighthouseCompleted", ""))
        assertEquals(listOf(2403 to 5), consumed)
    }

    @Test
    fun parseJunkyard_hintRegex_setsToolAndCanonicalLocation() {
        val prefs = prefs()
        val changed = IslandWarVisitSync.parseJunkyard(
            html = "The last time I saw my hammer, it was a barrel.",
            preferences = prefs,
            context = context(),
        )
        assertTrue(changed)
        assertEquals("molybdenum hammer", prefs.getString("currentJunkyardTool", ""))
        assertEquals(
            "next to that barrel with something burning in it",
            prefs.getString("currentJunkyardLocation", ""),
        )
    }

    @Test
    fun parseJunkyard_wrenchAlias_usesCrescentWrench() {
        val prefs = prefs()
        IslandWarVisitSync.parseJunkyard(
            html = "muttering something about a wrench, it was a car.",
            preferences = prefs,
            context = context(),
        )
        assertEquals("molybdenum crescent wrench", prefs.getString("currentJunkyardTool", ""))
        assertEquals("out by that rusted-out car", prefs.getString("currentJunkyardLocation", ""))
    }

    @Test
    fun parseJunkyard_sameLocationSecondVisit_returnsFalse() {
        val prefs = prefs()
        val location = "next to that barrel with something burning in it"
        prefs.setString("currentJunkyardTool", "molybdenum hammer")
        prefs.setString("currentJunkyardLocation", location)
        val changed = IslandWarVisitSync.parseJunkyard(
            html = "The last time I saw my hammer, it was a barrel.",
            preferences = prefs,
            context = context(),
        )
        assertFalse(changed)
        assertEquals("molybdenum hammer", prefs.getString("currentJunkyardTool", ""))
        assertEquals(location, prefs.getString("currentJunkyardLocation", ""))
    }

    @Test
    fun parseJunkyard_completionHippy_consumesToolsAndClearsPrefs() {
        val prefs = prefs()
        prefs.setString("currentJunkyardTool", "molybdenum hammer")
        prefs.setString("currentJunkyardLocation", "out by that rusted-out car")
        val consumed = mutableListOf<Pair<Int, Int>>()
        val changed = IslandWarVisitSync.parseJunkyard(
            html = "I made this while you were off getting my tools. Here is your spark plug earring.",
            preferences = prefs,
            context = context(consumed = consumed),
        )
        assertTrue(changed)
        assertEquals("", prefs.getString("currentJunkyardTool", "x"))
        assertEquals("", prefs.getString("currentJunkyardLocation", "x"))
        assertEquals("hippy", prefs.getString("sidequestJunkyardCompleted", ""))
        assertEquals(
            listOf(2497 to 1, 2498 to 1, 2499 to 1, 2500 to 1, 2501 to 1),
            consumed,
        )
    }

    @Test
    fun parseJunkyard_completionFratboy_setsSidequestPref() {
        val prefs = prefs()
        val consumed = mutableListOf<Pair<Int, Int>>()
        IslandWarVisitSync.parseJunkyard(
            html = "I made this while you were off getting my tools. Take this sawblade shield.",
            preferences = prefs,
            context = context(consumed = consumed),
        )
        assertEquals("fratboy", prefs.getString("sidequestJunkyardCompleted", ""))
        assertEquals(5, consumed.size)
    }

    @Test
    fun applyFromBigIslandVisit_junkyardUrl_routesToJunkyardParser() {
        val prefs = prefs()
        IslandWarVisitSync.applyFromBigIslandVisit(
            url = "bigisland.php?action=junkman",
            html = "The last time I saw my hammer, it was a barrel.",
            preferences = prefs,
            context = context(),
        )
        assertEquals("molybdenum hammer", prefs.getString("currentJunkyardTool", ""))
        assertEquals(
            "next to that barrel with something burning in it",
            prefs.getString("currentJunkyardLocation", ""),
        )
    }

    @Test
    fun parseQuest_camp_returnsCamp() {
        assertEquals(
            IslandWarVisitSync.IslandSidequest.CAMP,
            IslandWarVisitSync.parseQuest("bigisland.php?place=camp&whichcamp=1"),
        )
    }

    @Test
    fun findCampMaster_resolvesDimemasterAndQuartersmaster() = kotlinx.coroutines.runBlocking {
        net.sourceforge.kolmafia.data.GameDatabase().load()
        val dimemaster = IslandWarVisitSync.findCampMaster("bigisland.php?place=camp&whichcamp=1")
        val quartersmaster = IslandWarVisitSync.findCampMaster("bigisland.php?place=camp&whichcamp=2")
        assertEquals("dimemaster", dimemaster?.nickname)
        assertEquals("quartersmaster", quartersmaster?.nickname)
        assertEquals(null, IslandWarVisitSync.findCampMaster("bigisland.php?place=camp&whichcamp=9"))
    }

    private fun dimemasterCoinmaster() = net.sourceforge.kolmafia.shop.CoinmasterData(
        masterName = "Dimemaster",
        nickname = "dimemaster",
        token = "dime",
        property = "availableDimes",
        shopId = null,
        buyItems = emptyList(),
        sellItems = emptyList(),
    )

    private fun quartersmasterCoinmaster() = net.sourceforge.kolmafia.shop.CoinmasterData(
        masterName = "Quartersmaster",
        nickname = "quartersmaster",
        token = "quarter",
        property = "availableQuarters",
        shopId = null,
        buyItems = emptyList(),
        sellItems = emptyList(),
    )

    @Test
    fun parseCampTokenBalance_dimemasterZeroTokens_setsPref() {
        val prefs = prefs()
        prefs.setInt("availableDimes", 5)
        val changed = IslandWarVisitSync.parseCampTokenBalance(
            coinmaster = dimemasterCoinmaster(),
            html = "You don't have any dimes to spend.",
            preferences = prefs,
        )
        assertTrue(changed)
        assertEquals(0, prefs.getInt("availableDimes", -1))
    }

    @Test
    fun parseCampTokenBalance_dimemasterBalance_parsesCommaCount() {
        val prefs = prefs()
        IslandWarVisitSync.parseCampTokenBalance(
            coinmaster = dimemasterCoinmaster(),
            html = "You've currently got 1,234 dimes in your pocket.",
            preferences = prefs,
        )
        assertEquals(1234, prefs.getInt("availableDimes", 0))
    }

    @Test
    fun parseCampTokenBalance_quartersmasterBalance_setsPref() {
        val prefs = prefs()
        IslandWarVisitSync.parseCampTokenBalance(
            coinmaster = quartersmasterCoinmaster(),
            html = "You've currently got 7 quarters in your pocket.",
            preferences = prefs,
        )
        assertEquals(7, prefs.getInt("availableQuarters", 0))
    }

    @Test
    fun parseCampTokenBalance_sameBalanceSecondVisit_returnsFalse() {
        val prefs = prefs()
        val coinmaster = dimemasterCoinmaster()
        val html = "You've currently got 42 dimes in your pocket."
        assertTrue(IslandWarVisitSync.parseCampTokenBalance(coinmaster, html, prefs))
        assertFalse(IslandWarVisitSync.parseCampTokenBalance(coinmaster, html, prefs))
        assertEquals(42, prefs.getInt("availableDimes", 0))
    }

    @Test
    fun applyFromBigIslandVisit_campUrl_routesToCampParser() = kotlinx.coroutines.runBlocking {
        net.sourceforge.kolmafia.data.GameDatabase().load()
        val prefs = prefs()
        IslandWarVisitSync.applyFromBigIslandVisit(
            url = "bigisland.php?place=camp&whichcamp=1",
            html = "You've currently got 15 dimes in your pocket.",
            preferences = prefs,
            context = context(),
        )
        assertEquals(15, prefs.getInt("availableDimes", 0))
    }

    @Test
    fun parseCamp_getgear_decrementsDimesAndLogs() = kotlinx.coroutines.runBlocking {
        net.sourceforge.kolmafia.data.GameDatabase().load()
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        prefs.setInt("availableDimes", 20)
        val waterPipeBombId = 2348
        IslandWarVisitSync.parseCamp(
            url = "bigisland.php?place=camp&whichcamp=1&action=getgear&whichitem=$waterPipeBombId&quantity=1",
            html = "You've currently got 19 dimes in your pocket.",
            preferences = prefs,
            sessionLogger = logger,
        )
        assertEquals(19, prefs.getInt("availableDimes", 0))
        assertTrue(sessionLog(prefs).contains("trading 1 dime for 1 water pipe bomb"))
    }

    @Test
    fun parseCamp_turnin_incrementsQuartersConsumesItemAndLogs() = kotlinx.coroutines.runBlocking {
        net.sourceforge.kolmafia.data.GameDatabase().load()
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        val hippyButtonId = 2029
        IslandWarVisitSync.parseCamp(
            url = "bigisland.php?place=camp&whichcamp=2&action=turnin&whichitem=$hippyButtonId&quantity=1",
            html = "You've currently got 1 quarter in your pocket.",
            preferences = prefs,
            context = context(consumed = consumed),
            sessionLogger = logger,
        )
        assertEquals(1, prefs.getInt("availableQuarters", 0))
        assertEquals(listOf(hippyButtonId to 1), consumed)
        val log = sessionLog(prefs)
        assertTrue(log.contains("trading 1 hippy protest button for 1 quarter"))
        assertTrue(log.contains("You acquire 1 quarter"))
    }

    @Test
    fun parseCamp_getgear_insufficient_noPrefChange() = kotlinx.coroutines.runBlocking {
        net.sourceforge.kolmafia.data.GameDatabase().load()
        val prefs = prefs()
        prefs.setInt("availableDimes", 20)
        IslandWarVisitSync.parseCamp(
            url = "bigisland.php?place=camp&whichcamp=1&action=getgear&whichitem=2348&quantity=1",
            html = "You don't have enough dimes for that.",
            preferences = prefs,
        )
        assertEquals(20, prefs.getInt("availableDimes", 0))
    }

    @Test
    fun parseCamp_tokenBalanceReconcilesAfterTransaction() = kotlinx.coroutines.runBlocking {
        net.sourceforge.kolmafia.data.GameDatabase().load()
        val prefs = prefs()
        prefs.setInt("availableDimes", 20)
        IslandWarVisitSync.parseCamp(
            url = "bigisland.php?place=camp&whichcamp=1&action=getgear&whichitem=2348&quantity=1",
            html = "You've currently got 18 dimes in your pocket.",
            preferences = prefs,
        )
        assertEquals(18, prefs.getInt("availableDimes", 0))
    }

    @Test
    fun deduceWinner_fratboysLost_setsSideDefeatedAndFinished() {
        val prefs = prefs()
        assertTrue(
            IslandWarVisitSync.deduceWinner(
                html = """<img src="snarfblat=150">""",
                preferences = prefs,
            ),
        )
        assertEquals("fratboys", prefs.getString("sideDefeated", ""))
        assertEquals("finished", prefs.getString("warProgress", ""))
    }

    @Test
    fun deduceWinner_hippiesLost_setsSideDefeatedHippies() {
        val prefs = prefs()
        IslandWarVisitSync.deduceWinner(
            html = """<img src="snarfblat=149">""",
            preferences = prefs,
        )
        assertEquals("hippies", prefs.getString("sideDefeated", ""))
        assertEquals("finished", prefs.getString("warProgress", ""))
    }

    @Test
    fun deduceWinner_bothLost_setsSideDefeatedBoth() {
        val prefs = prefs()
        IslandWarVisitSync.deduceWinner(
            html = """<img src="snarfblat=149"><img src="snarfblat=150">""",
            preferences = prefs,
        )
        assertEquals("both", prefs.getString("sideDefeated", ""))
    }

    @Test
    fun deduceWinner_noSnarfblat_defaultsToFratboysLost() {
        val prefs = prefs()
        IslandWarVisitSync.deduceWinner(html = "<html></html>", preferences = prefs)
        assertEquals("fratboys", prefs.getString("sideDefeated", ""))
    }

    @Test
    fun deduceWinner_samePrefs_returnsFalse() {
        val prefs = prefs()
        prefs.setString("sideDefeated", "fratboys")
        prefs.setString("warProgress", "finished")
        assertFalse(
            IslandWarVisitSync.deduceWinner(
                html = """<img src="snarfblat=150">""",
                preferences = prefs,
            ),
        )
    }

    @Test
    fun applyFromPostwarIslandVisit_arenaUrl_routesToArenaParser() {
        val prefs = prefs()
        IslandWarVisitSync.applyFromPostwarIslandVisit(
            url = "postwarisland.php?place=concert",
            html = "well into the first song",
            preferences = prefs,
            context = context(),
        )
        assertEquals("hippy", prefs.getString("sidequestArenaCompleted", ""))
        assertEquals("finished", prefs.getString("warProgress", ""))
    }

    @Test
    fun applyFromPostwarIslandVisit_nunsUrl_routesToNunneryParser() {
        val prefs = prefs()
        IslandWarVisitSync.applyFromPostwarIslandVisit(
            url = "postwarisland.php?place=nunnery",
            html = "tend to your wounds",
            preferences = prefs,
            context = context(),
        )
        assertEquals("hippy", prefs.getString("sidequestNunsCompleted", ""))
    }

    @Test
    fun applyFromPostwarIslandVisit_skipsWarStartedBootstrap() {
        val prefs = prefs()
        prefs.setString("warProgress", "unstarted")
        IslandWarVisitSync.applyFromPostwarIslandVisit(
            url = "postwarisland.php",
            html = "<html></html>",
            preferences = prefs,
            context = context(),
        )
        assertEquals("finished", prefs.getString("warProgress", ""))
        assertFalse(prefs.getString("warProgress", "") == "started")
    }
}
