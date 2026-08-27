package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AfterAvatarChoiceSync
import net.sourceforge.kolmafia.quest.DecorateTentChoiceSync
import net.sourceforge.kolmafia.quest.EternityCodpieceChoiceSync
import net.sourceforge.kolmafia.quest.GovernmentShipmentChoiceSync
import net.sourceforge.kolmafia.quest.GreyYouLabChoiceSync
import net.sourceforge.kolmafia.quest.LoathingIdolChoiceSync
import net.sourceforge.kolmafia.quest.MimicEggDifferentiateChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SiteAlphaLabChoiceSync
import net.sourceforge.kolmafia.quest.StillSuitChoiceSync
import net.sourceforge.kolmafia.session.GreyYouManager

class GameRuntimeLibraryAshP849To876Test {
    @AfterTest fun resetGreyYou() = GreyYouManager.resetAbsorptions()

    @Test fun revision_atLeastPhase876() {
        assertTrue(GameRuntimeLibrary.REVISION.removePrefix("phase").toInt() >= 876)
    }

    @Test
    fun decorateTentConsumesStickAndStoresMode() {
        val prefs = Preferences(MapSettings())
        var consumed = 0
        assertTrue(DecorateTentChoiceSync.apply(1392, 2, "magical symbols", prefs) { consumed = it })
        assertEquals(ItemPool.BURNT_STICK, consumed)
        assertEquals(2, prefs.getInt("campAwayDecoration"))
    }

    @Test
    fun governmentShipmentConsumesCorrectPackage() {
        var consumed = 0
        assertTrue(GovernmentShipmentChoiceSync.apply(1443, "You fill out all the appropriate forms") {
            consumed = it
        })
        assertEquals(ItemPool.GOVERNMENT_BOOZE_SHIPMENT, consumed)
    }

    @Test
    fun greyYouLabsConsumeRecipeInputsAndUpdateAlpha() {
        val costs = mutableMapOf<Int, Int>()
        assertTrue(GreyYouLabChoiceSync.apply(1460, 7, "You acquire an item") { id, qty -> costs[id] = qty })
        assertEquals(10, costs[ItemPool.GOOIFIED_ANIMAL_MATTER])
        val prefs = Preferences(MapSettings())
        prefs.setInt("primaryLabGooIntensity", 10)
        assertTrue(SiteAlphaLabChoiceSync.apply(1461, 1, prefs) {})
        assertEquals(11, prefs.getInt("primaryLabGooIntensity"))
    }

    @Test
    fun afterAvatarResolvesClassRoad() {
        var road = ""
        assertTrue(AfterAvatarChoiceSync.apply(1524, 4) { road = it })
        assertEquals("Sauceror", road)
    }

    @Test
    fun greyYouParsesKnownShapeAndUnknownAbsorption() {
        val logs = mutableListOf<String>()
        GreyYouManager.parseAbsorptions(
            "Absorptions: Absorbed a peculiar bonus from mystery beast.<!-- 999999 -->",
            inGreyYou = true,
            unknownLog = logs::add,
        )
        assertEquals("a peculiar bonus", GreyYouManager.unknownDescription(999999))
        assertTrue(logs.single().contains("Unknown Grey You absorption"))
    }

    @Test
    fun greyYouPathGateClearsState() {
        GreyYouManager.unknownAbsorptions[7] = "test"
        GreyYouManager.parseAbsorptions("ordinary character sheet", inGreyYou = false)
        assertTrue(GreyYouManager.unknownAbsorptions.isEmpty())
    }

    @Test
    fun rufusCallAcceptsAndCallbackConsumesItems() {
        val prefs = Preferences(MapSettings())
        val quests = QuestDatabase(prefs)
        val manager = RufusManager(prefs)
        manager.parseCall("Right now, 3 shadow bricks would be valuable")
        manager.parseCallResponse("", 3, quests, { 1 }, { 3 })
        assertEquals("step1", quests.getProgress(Quest.RUFUS))
        var consumed = 0
        manager.parseCallBackResponse("Rufus's shadow lodestone", 1, quests, { 1 }) { _, qty -> consumed = qty }
        assertEquals(3, consumed)
        assertEquals(QuestDatabase.UNSTARTED, quests.getProgress(Quest.RUFUS))
    }

    @Test
    fun mimicDifferentiateConsumesEggAndDecrementsMonsterCount() {
        val prefs = Preferences(MapSettings())
        prefs.setString("mimicEggMonsters", "12:2,13:1")
        var consumed = 0
        assertTrue(MimicEggDifferentiateChoiceSync.apply(1516, "choice.php?mid=12", prefs) { consumed = it })
        assertEquals(ItemPool.MIMIC_EGG, consumed)
        assertEquals("12:1,13:1", prefs.getString("mimicEggMonsters"))
    }

    @Test
    fun microphoneDegradesAndStillsuitDrinkClearsSweat() {
        var consumed = 0
        var gained = 0
        assertTrue(LoathingIdolChoiceSync.apply(
            1505, "You sing:", ItemPool.LOATHING_IDOL_MICROPHONE,
            { consumed = it }, { gained = it },
        ))
        assertEquals(ItemPool.LOATHING_IDOL_MICROPHONE, consumed)
        assertEquals(ItemPool.LOATHING_IDOL_MICROPHONE_75, gained)
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", 40)
        prefs.setString("nextDistillateMods", "Meat Drop: +10")
        assertTrue(StillSuitChoiceSync.apply(1476, 1, "You put your lips to the nozzle", prefs))
        assertEquals(0, prefs.getInt("familiarSweat"))
        assertEquals("", prefs.getString("nextDistillateMods"))
    }

    @Test
    fun codpieceParsesInventorySwapAndRefreshes() {
        val consumed = mutableListOf<Int>()
        val gained = mutableListOf<Int>()
        var refreshed = false
        assertTrue(EternityCodpieceChoiceSync.apply(
            1588,
            "You lose an item: <b>new gem</b> You acquire an item: <b>old gem</b>",
            { if (it == "new gem") 11 else 12 },
            consumed::add,
            gained::add,
            { refreshed = true },
        ))
        assertEquals(listOf(11), consumed)
        assertEquals(listOf(12), gained)
        assertTrue(refreshed)
    }
}
