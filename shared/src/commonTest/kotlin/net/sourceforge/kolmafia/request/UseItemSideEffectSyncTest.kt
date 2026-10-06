package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.TurnCounter
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UseItemSideEffectSyncTest {

    @Test
    fun revision_isPhase10330() {
        assertEquals("phase10510", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun sombreroAndSoda_setKeepPrefs() {
        val prefs = Preferences(MapSettings())
        UseItemConsumptionSync.parseConsumption(
            responseText = "You tip your outrageous sombrero.",
            itemId = UseItemSideEffectSync.OUTRAGEOUS_SOMBRERO,
            count = 1,
            preferences = prefs,
            inventory = inventory(),
        )
        assertTrue(prefs.getBoolean("outrageousSombreroUsed", false))

        UseItemConsumptionSync.parseConsumption(
            responseText = "You drink the neverending soda.",
            itemId = UseItemSideEffectSync.NEVERENDING_SODA,
            count = 1,
            preferences = prefs,
            inventory = inventory(),
        )
        assertTrue(prefs.getBoolean("oscusSodaUsed", false))
    }

    @Test
    fun trapezoid_setsPortalEnergy() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(UseItemSideEffectSync.TRAPEZOID, 1)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "you put it on the ground at your campsite and it hums",
            itemId = UseItemSideEffectSync.TRAPEZOID,
            count = 1,
            preferences = prefs,
            inventory = inv,
        )
        assertTrue(ok)
        assertEquals(20, prefs.getInt("currentPortalEnergy", 0))
        assertEquals(0, inv.getCount(UseItemSideEffectSync.TRAPEZOID))
    }

    @Test
    fun refuseGates_keepMassagerAndEvBugs() {
        val inv = inventory()
        inv.gainItemLocally(UseItemSideEffectSync.PERSONAL_MASSAGER, 1)
        inv.gainItemLocally(UseItemSideEffectSync.GRUB, 1)
        assertTrue(
            UseItemConsumptionSync.parseConsumption(
                responseText = "You don't really need a massage right now",
                itemId = UseItemSideEffectSync.PERSONAL_MASSAGER,
                count = 1,
                inventory = inv,
            ),
        )
        assertEquals(1, inv.getCount(UseItemSideEffectSync.PERSONAL_MASSAGER))

        assertTrue(
            UseItemConsumptionSync.parseConsumption(
                responseText = "filled with revulsion at the prospect",
                itemId = UseItemSideEffectSync.GRUB,
                count = 1,
                inventory = inv,
            ),
        )
        assertEquals(1, inv.getCount(UseItemSideEffectSync.GRUB))
    }

    @Test
    fun honeypot_stopsBeeCounters() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 0, 15, "Bee window begin", "bee.gif")
        TurnCounter.startCounting(prefs, 0, 20, "Bee window end", "bee.gif")
        val inv = inventory()
        inv.gainItemLocally(UseItemSideEffectSync.HONEYPOT, 1)
        assertTrue(
            UseItemConsumptionSync.parseConsumption(
                responseText = "You smear yourself with honey.",
                itemId = UseItemSideEffectSync.HONEYPOT,
                count = 1,
                preferences = prefs,
                inventory = inv,
            ),
        )
        assertFalse(TurnCounter.isCounting(prefs, "Bee window begin", 0))
        assertFalse(TurnCounter.isCounting(prefs, "Bee window end", 0))
    }

    @Test
    fun tipi_setsCurrentDwelling() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(UseItemSideEffectSync.MINI_KIWI_TIPI, 1)
        assertTrue(
            UseItemConsumptionSync.parseConsumption(
                responseText = "You erect the mini-kiwi tipi at your campsite.",
                itemId = UseItemSideEffectSync.MINI_KIWI_TIPI,
                count = 1,
                preferences = prefs,
                inventory = inv,
            ),
        )
        assertEquals(
            UseItemSideEffectSync.MINI_KIWI_TIPI,
            prefs.getInt("_currentDwellingItemId", -1),
        )
        assertEquals(0, inv.getCount(UseItemSideEffectSync.MINI_KIWI_TIPI))
    }

    @Test
    fun ledClock_installsCampFurniture() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(UseItemSideEffectSync.LED_CLOCK, 1)
        assertTrue(
            UseItemConsumptionSync.parseConsumption(
                responseText = "You install the LED clock.",
                itemId = UseItemSideEffectSync.LED_CLOCK,
                count = 1,
                preferences = prefs,
                inventory = inv,
            ),
        )
        assertEquals(
            1,
            CampgroundInventorySync.load(prefs)[UseItemSideEffectSync.LED_CLOCK] ?: 0,
        )
    }

    @Test
    fun cyrptSculpture_marksEvilometerRefresh() = runBlocking {
        UseItemRequestState.clearFollowUps()
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(UseItemSideEffectSync.QUASIRELGIOUS_SCULPTURE, 1)
        assertTrue(
            UseItemConsumptionSync.parseConsumption(
                responseText = "The entire Cyrpt feels safer somehow.",
                itemId = UseItemSideEffectSync.QUASIRELGIOUS_SCULPTURE,
                count = 1,
                preferences = prefs,
                inventory = inv,
            ),
        )
        val engine = MockEngine {
            respond(
                """Total Evil: <b>100</b>""",
                HttpStatusCode.OK,
            )
        }
        UseItemRequestState.refreshFollowUps(HttpClient(engine), prefs)
        // Follow-up ran (no throw); sculpture itself was consumed.
        assertEquals(0, inv.getCount(UseItemSideEffectSync.QUASIRELGIOUS_SCULPTURE))
    }

    @Test
    fun cursedChest_consumesMatchingKey() {
        val inventory = inventory()
        inventory.gainItemLocally(UseItemSideEffectSync.ORNATE_CURSED_KEY, 1)
        inventory.gainItemLocally(UseItemSideEffectSync.ORNATE_CURSED_CHEST, 1)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You unlock the ornate cursed chest.",
            itemId = UseItemSideEffectSync.ORNATE_CURSED_CHEST,
            count = 1,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(0, inventory.getCount(UseItemSideEffectSync.ORNATE_CURSED_KEY))
        assertEquals(0, inventory.getCount(UseItemSideEffectSync.ORNATE_CURSED_CHEST))
    }

    @Test
    fun dustySkull_wipesBonesOnSuccess() {
        val inventory = inventory()
        inventory.gainItemLocally(UseItemSideEffectSync.DUSTY_ANIMAL_SKULL, 1)
        inventory.gainItemLocally(1802, 1)
        inventory.gainItemLocally(1850, 1)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = """The magic that had previously animated the animals kicks back
                in, and it stands up shakily and looks at you. "Graaangh?" """,
            itemId = UseItemSideEffectSync.DUSTY_ANIMAL_SKULL,
            count = 1,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(0, inventory.getCount(1802))
        assertEquals(0, inventory.getCount(1850))
        assertEquals(0, inventory.getCount(UseItemSideEffectSync.DUSTY_ANIMAL_SKULL))
    }

    @Test
    fun generalAssembly_consumesLaserComponents() {
        val inventory = inventory()
        inventory.gainItemLocally(UseItemSideEffectSync.GENERAL_ASSEMBLY_MODULE, 1)
        inventory.gainItemLocally(UseItemSideEffectSync.LASER_CANON, 1)
        inventory.gainItemLocally(UseItemSideEffectSync.LASER_TARGETING_CHIP, 1)
        inventory.gainItemLocally(UseItemSideEffectSync.UNOBTAINIUM_STRAPS, 1)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You breathe a heavy sigh of relief as the pseudopods emerge from your inventory, carrying the  laser cannon, laser targeting chip, and the set of Unobtainium straps",
            itemId = UseItemSideEffectSync.GENERAL_ASSEMBLY_MODULE,
            count = 1,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(0, inventory.getCount(UseItemSideEffectSync.LASER_CANON))
        assertEquals(0, inventory.getCount(UseItemSideEffectSync.LASER_TARGETING_CHIP))
        assertEquals(0, inventory.getCount(UseItemSideEffectSync.UNOBTAINIUM_STRAPS))
    }

    @Test
    fun bangPotion_setsIdentificationPref() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You drink the potion and feel like liquid fire.",
            itemId = 819,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals("inebriety", prefs.getString("lastBangPotion819", ""))
    }

    @Test
    fun workytimeTea_abortsWhenNotBored() {
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You're not quite bored enough to drink that much tea.",
            itemId = UseItemSideEffectSync.WORKYTIME_TEA,
            count = 1,
        )
        assertFalse(ok)
        assertEquals(
            "You're not bored enough to drink that much tea.",
            UseItemConsumptionSync.lastUpdate,
        )
    }

    @Test
    fun warmSubject_consumesOnlyOneOnIronicShirt() {
        val inventory = inventory()
        inventory.gainItemLocally(UseItemSideEffectSync.WARM_SUBJECT, 5)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You go to Warm Subject and browse the shirts for a while. You find one that you wouldn't mind wearing ironically. There seems to be only one in the store, though.",
            itemId = UseItemSideEffectSync.WARM_SUBJECT,
            count = 5,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(4, inventory.getCount(UseItemSideEffectSync.WARM_SUBJECT))
    }

    @Test
    fun legendaryBeat_setsPrefAndKeepsItem() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You bang the legendary beat.",
            itemId = UseItemSideEffectSync.LEGENDARY_BEAT,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("_legendaryBeat", false))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun gong_abortsWhenStillDrunk() {
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "the mallet keeps falling out of your hand. Maybe you should try it later, when you've sobered up a little.",
            itemId = UseItemSideEffectSync.GONG,
            count = 1,
            preferences = Preferences(MapSettings()),
        )
        assertFalse(ok)
        assertEquals(
            "Insufficient adventures or sobriety to use a gong.",
            UseItemConsumptionSync.lastUpdate,
        )
    }

    @Test
    fun fireworks_illegalDoesNotSetPref() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "these particular fireworks are illegal on any day other than the Fourth of Bor.",
            itemId = UseItemSideEffectSync.SPARKLER,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertFalse(prefs.getBoolean("_fireworkUsed", false))
    }

    @Test
    fun fireworks_successSetsPref() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You light the sparkler and celebrate.",
            itemId = UseItemSideEffectSync.SPARKLER,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("_fireworkUsed", false))
    }

    @Test
    fun enchantedBean_advancesGarbageQuest() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "it immediately grows into an enormous beanstalk",
            itemId = UseItemSideEffectSync.ENCHANTED_BEAN,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals("step1", QuestDatabase(prefs).getProgress(Quest.GARBAGE))
    }

    @Test
    fun enchantedBean_withoutStalkIsKept() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "There's already a beanstalk in the Nearby Plains.",
            itemId = UseItemSideEffectSync.ENCHANTED_BEAN,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(QuestDatabase.UNSTARTED, QuestDatabase(prefs).getProgress(Quest.GARBAGE))
    }

    @Test
    fun cobbsKnob_consumesKeyAndStartsGoblin() {
        val prefs = Preferences(MapSettings())
        val inventory = inventory()
        inventory.gainItemLocally(UseItemSideEffectSync.ENCRYPTION_KEY, 1)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You memorize the location of the door, then eat both the map and the encryption key.",
            itemId = UseItemSideEffectSync.COBBS_KNOB_MAP,
            count = 1,
            preferences = prefs,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals("step1", QuestDatabase(prefs).getProgress(Quest.GOBLIN))
        assertEquals(0, inventory.getCount(UseItemSideEffectSync.ENCRYPTION_KEY))
    }

    @Test
    fun spookyMap_requiresSapling() {
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You plant the sapling.",
            itemId = UseItemSideEffectSync.SPOOKY_MAP,
            count = 1,
            preferences = Preferences(MapSettings()),
            inventory = inventory(),
        )
        assertFalse(ok)
        assertEquals("You don't have everything you need.", UseItemConsumptionSync.lastUpdate)
    }

    @Test
    fun chateauKey_abortsWhenAlreadyOpen() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You already have a room.",
            itemId = UseItemSideEffectSync.CHATEAU_ROOM_KEY,
            count = 1,
            preferences = prefs,
        )
        assertFalse(ok)
        assertTrue(prefs.getBoolean("chateauAvailable", false))
    }

    @Test
    fun skillBook_abortsWithoutLearnText() {
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You can't make sense of the diagrams.",
            itemId = 1411,
            count = 1,
            preferences = Preferences(MapSettings()),
        )
        assertFalse(ok)
        assertEquals("You can't learn that skill.", UseItemConsumptionSync.lastUpdate)
    }

    @Test
    fun spiceMelange_reducesFullnessAndDrunk() {
        val prefs = Preferences(MapSettings())
        val character = KoLCharacter()
        character.updateConsumables(fullness = 5, inebriety = 4, spleenUsed = 0)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You pop the spice melange into your mouth and chew it up.",
            itemId = UseItemSideEffectSync.SPICE_MELANGE,
            count = 1,
            preferences = prefs,
            character = character,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("spiceMelangeUsed", false))
        assertEquals(2, character.state.value.fullness)
        assertEquals(1, character.state.value.inebriety)
    }

    @Test
    fun airportCharter_setsAlwaysOnRegistry() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "Your name gets added to the registry.",
            itemId = UseItemSideEffectSync.SPRING_BEACH_CHARTER,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("sleazeAirportAlways", false))
        assertFalse(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun aminoAcids_tooFullSetsPref() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You're too full to use that.",
            itemId = UseItemSideEffectSync.AMINO_ACIDS,
            count = 1,
            preferences = prefs,
        )
        assertFalse(ok)
        assertEquals(3, prefs.getInt("aminoAcidsUsed", 0))
        assertEquals("Consumption limit reached.", UseItemConsumptionSync.lastUpdate)
    }

    @Test
    fun sonar_advancesBatQuest() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "The rubble leading west from Guano Junction collapses in a heap.",
            itemId = UseItemSideEffectSync.SONAR,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals("step1", QuestDatabase(prefs).getProgress(Quest.BAT))
    }

    @Test
    fun moveableFeast_recordsFamiliar() {
        val prefs = Preferences(MapSettings())
        val character = KoLCharacter()
        character.updateFamiliar(id = 1, name = "Blip", weight = 1, exp = 0)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "Blip chows down on the moveable feast, then leans back.",
            itemId = UseItemSideEffectSync.MOVEABLE_FEAST,
            count = 1,
            preferences = prefs,
            character = character,
        )
        assertTrue(ok)
        assertEquals(1, prefs.getInt("_feastUsed", 0))
        assertEquals("Blip", prefs.getString("_feastedFamiliars", ""))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun uraniumSeal_capsSummons() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You've summoned too many Infernal seals today.",
            itemId = UseItemSideEffectSync.DEPLETED_URANIUM_SEAL,
            count = 1,
            preferences = prefs,
        )
        assertFalse(ok)
        assertEquals(5, prefs.getInt("_sealsSummoned", 0))
    }

    @Test
    fun tablet_recordsDemonName() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = """The tablet reads <font color=#cccccc>Xyrr'k</font>.""",
            itemId = UseItemSideEffectSync.SINISTER_ANCIENT_TABLET,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals("Xyrr'k", prefs.getString("demonName9", ""))
    }

    @Test
    fun danceCard_startsCounter() {
        val prefs = Preferences(MapSettings())
        val character = KoLCharacter()
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You use the dance card.",
            itemId = UseItemConsumptionSync.DANCE_CARD,
            count = 1,
            preferences = prefs,
            character = character,
        )
        assertTrue(ok)
        assertEquals(3, prefs.getInt("_danceCardFightsLeft", 0))
        val labels = TurnCounter.load(prefs).map { it.parsedLabel() }
        assertTrue(labels.any { it.equals("Dance Card", ignoreCase = true) })
    }

    @Test
    fun gatesScroll_finishesToppingWhenFlattered() {
        val prefs = Preferences(MapSettings())
        val inventory = inventory()
        inventory.gainItemLocally(UseItemSideEffectSync.DICTIONARY, 1)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "Even though your name isn't Lee, you're flattered and hand over your dictionary.",
            itemId = UseItemSideEffectSync.GATES_SCROLL,
            count = 1,
            preferences = prefs,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(QuestDatabase.FINISHED, QuestDatabase(prefs).getProgress(Quest.TOPPING))
        assertEquals(QuestDatabase.FINISHED, QuestDatabase(prefs).getProgress(Quest.LOL))
        assertEquals(0, inventory.getCount(UseItemSideEffectSync.DICTIONARY))
    }

    @Test
    fun legionFold_consumesReusableWhenLatches() {
        val inventory = inventory()
        inventory.gainItemLocally(4908, 1)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You spend a while messing with all of the latches and clasps.",
            itemId = 4908,
            count = 1,
            preferences = Preferences(MapSettings()),
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(0, inventory.getCount(4908))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun giftPackage_abortsWhenCannotReceive() {
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You can't receive things from other players right now.",
            itemId = 1167,
            count = 1,
            preferences = Preferences(MapSettings()),
        )
        assertFalse(ok)
        assertEquals("You can't open that package yet.", UseItemConsumptionSync.lastUpdate)
    }

    @Test
    fun saucePotion_abortsWhenAlreadyPressured() {
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "If you took this one, you'd explode.",
            itemId = 3561,
            count = 1,
            preferences = Preferences(MapSettings()),
        )
        assertFalse(ok)
        assertEquals("You're already under pressure.", UseItemConsumptionSync.lastUpdate)
    }

    @Test
    fun rollingPin_removesAllDoughAndKeepsPin() {
        val inventory = inventory()
        inventory.gainItemLocally(873, 1)
        inventory.gainItemLocally(159, 4)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You roll out the dough.",
            itemId = 873,
            count = 1,
            preferences = Preferences(MapSettings()),
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(1, inventory.getCount(873))
        assertEquals(0, inventory.getCount(159))
    }

    @Test
    fun lovebugs_stayWhenNotPermanentlyUnlocked() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "The pheromones waft around.",
            itemId = 8134,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("lovebugsUnlocked", false))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun telegram_startsNecklaceQuest() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "A telegram from Spookyraven.",
            itemId = 7304,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(QuestDatabase.STARTED, QuestDatabase(prefs).getProgress(Quest.SPOOKYRAVEN_NECKLACE))
        assertFalse(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun robotParts_incrementUpgradesOnWhirr() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "The pile emits a satisfied whirr.",
            itemId = 6100,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(1, prefs.getInt("homemadeRobotUpgrades", 0))
    }

    @Test
    fun merkinWordquiz_setsMasteryAndTakesCheatsheet() {
        val prefs = Preferences(MapSettings())
        val inventory = inventory()
        inventory.gainItemLocally(4204, 1)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "Your Mer-kin vocabulary mastery is now at <b>40%</b>",
            itemId = 4205,
            count = 1,
            preferences = prefs,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(40, prefs.getInt("merkinVocabularyMastery", 0))
        assertEquals(0, inventory.getCount(4204))
    }

    @Test
    fun dreadscroll_marksHighPriest() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "I guess you're the Mer-kin High Priest now. Cool!",
            itemId = 6353,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("isMerkinHighPriest", false))
        assertEquals("scholar", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun desertPamphlet_addsExploration() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("desertExploration", 90)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You read the pamphlet.",
            itemId = 6854,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(100, prefs.getInt("desertExploration", 0))
        assertEquals(QuestDatabase.FINISHED, QuestDatabase(prefs).getProgress(Quest.DESERT))
    }

    @Test
    fun mayo_incrementsLevelUnlessMouthFull() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You squirt the mayo.",
            itemId = 8261,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(1, prefs.getInt("mayoLevel", 0))
        assertEquals("Mayonex", prefs.getString("mayoInMouth", ""))
    }

    @Test
    fun circleDrum_recordsRhythmAndKeeps() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You join the 1,427 other people sitting in a drum circle.",
            itemId = 8784,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(143, prefs.getInt("_feelinTheRhythm", 0))
        assertTrue(prefs.getBoolean("_circleDrumUsed", false))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun sourceTerminalChip_keepsWhenAlreadyInstalled() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You've already installed the maximum number. You have 10 so far.",
            itemId = 9040,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(10, prefs.getInt("sourceTerminalPram", 0))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun spaceBabyBook_recordsFluency() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You learn a few words. Fluency is now 12%.",
            itemId = 9463,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(12, prefs.getInt("spaceBabyLanguageFluency", 0))
    }

    @Test
    fun fantasyGuest_keepsWhenAlreadyInside() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You've already got access to FantasyRealm.",
            itemId = 9836,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertFalse(prefs.getBoolean("_frToday", false))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun ghostDogChow_notesUninterestedFamiliar() {
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "Your familiar doesn't seem interested in the chow.",
            itemId = 8640,
            count = 1,
            preferences = Preferences(MapSettings()),
        )
        assertTrue(ok)
        assertEquals(
            "Your familiar is not interested in that item.",
            UseItemConsumptionSync.lastUpdate,
        )
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun fantasyMountainMap_unlocksAndConsumes() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You unfold the map.",
            itemId = 9873,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("frMountainsUnlocked", false))
        assertFalse(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun cheeseWheel_keepsWhenNoCheeseIsPicked() {
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "The wheel just sits there.",
            itemId = 9937,
            count = 1,
            preferences = Preferences(MapSettings()),
        )
        assertTrue(ok)
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun glitchItem_longerPhraseSetsLevelTwo() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "This needs more implementation.",
            itemId = 10207,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(2, prefs.getInt("glitchItemImplementationLevel", 0))
        assertEquals(2, prefs.getInt("glitchItemImplementationCount", 0))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun chessPieces_removeTheFullSet() {
        val inventory = inventory()
        inventory.gainItemLocally(10628, 8)
        inventory.gainItemLocally(10623, 1)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You assemble a complete chess set.",
            itemId = 10628,
            count = 1,
            preferences = Preferences(MapSettings()),
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(0, inventory.getCount(10628))
        assertEquals(0, inventory.getCount(10623))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun birdCalendar_recordsBirdAndLearnsSeek() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "Today's bird is the puffin!",
            itemId = 10434,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals("puffin", prefs.getString("_birdOfTheDay", ""))
        assertTrue(prefs.getBoolean("_canSeekBirds", false))
        assertEquals(1, prefs.getInt("skillLevel7323", 0))
    }

    @Test
    fun workshed_keepsWhenAlreadyRearranged() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You've already rearranged your workshed today.",
            itemId = 6964,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(-1, CampgroundItemSync.currentWorkshedItemId(prefs))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun aminoAcids_incrementOnASuccessfulBite() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "you ate some delicious, delicious amino acids",
            itemId = 4006,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(1, prefs.getInt("aminoAcidsUsed", 0))
    }

    @Test
    fun moonSpoon_marksTunedAndKeeps() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You twist the spoon around until the moon looks right.",
            itemId = 10254,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("moonTuned", false))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun screwdriver_unscrewsTheRememberedItem() {
        val prefs = Preferences(MapSettings())
        val inventory = inventory()
        inventory.gainItemLocally(100, 3)
        UseItemRequestState.remember(
            "inv_use.php?whichitem=4926&action=screw&dowhichitem=100&untinkerall=on",
            preferences = prefs,
            inventory = inventory,
        )
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You jam your screwdriver into the seam.",
            itemId = 4926,
            count = 1,
            preferences = prefs,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(0, inventory.getCount(100))
        assertTrue(UseItemConsumptionSync.lastUpdate.contains("Successfully unscrewed"))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun jackingMap_consumesTheRememberedFruit() {
        val inventory = inventory()
        inventory.gainItemLocally(223, 1)
        UseItemRequestState.remember("inv_use.php?whichitem=4560&action=addfruit&whichfruit=223")
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "The fruit disappears into the tube.",
            itemId = 4560,
            count = 1,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(0, inventory.getCount(223))
        assertTrue(UseItemConsumptionSync.suppressEffectRemoval)
    }

    @Test
    fun moonSpoon_setsSignAndResetsDailySpecialWhenTheZoneChanges() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_dailySpecial", "knob goblin")
        val character = KoLCharacter()
        character.setZodiacSign("Mongoose")
        UseItemRequestState.remember("inv_use.php?whichitem=10254&whichsign=2&doit=96")
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You twist the spoon around until the moon looks right.",
            itemId = 10254,
            count = 1,
            preferences = prefs,
            character = character,
        )
        assertTrue(ok)
        assertEquals("Wallaby", character.state.value.zodiacSign)
        assertEquals("", prefs.getString("_dailySpecial", "unset"))
        assertTrue(prefs.getBoolean("moonTuned", false))
    }

    @Test
    fun register_marksExpressCardWhenTheUseUrlIsRemembered() {
        val prefs = Preferences(MapSettings())
        UseItemRequestState.remember("inv_use.php?whichitem=1687", preferences = prefs)
        assertTrue(prefs.getBoolean("expressCardUsed", false))
    }

    @Test
    fun claymore_refreshesIslandKillCounts() = runBlocking {
        val prefs = Preferences(MapSettings())
        val engine = MockEngine { request ->
            val body = if (request.url.toString().contains("bigisland")) {
                "bfleft1 bfright2"
            } else {
                "You bury the claymore in the clay"
            }
            respond(body, HttpStatusCode.OK)
        }
        val request = UseItemRequest(
            HttpClient(engine),
            preferences = prefs,
            inventoryManager = inventory(),
        )
        val result = request.use(6669, 1)
        assertTrue(result.isSuccess)
        assertEquals("started", prefs.getString("warProgress", ""))
        assertEquals(3, prefs.getInt("fratboysDefeated", 0))
        assertEquals(9, prefs.getInt("hippiesDefeated", 0))
    }

    @Test
    fun asdonMartin_refreshesWorkshedFuel() = runBlocking {
        val prefs = Preferences(MapSettings())
        val engine = MockEngine { request ->
            val body = if (request.url.toString().contains("campground")) {
                "asdongarage.gif fuel gauge reads 1,250 litres"
            } else {
                "You install the Asdon Martin."
            }
            respond(body, HttpStatusCode.OK)
        }
        val request = UseItemRequest(
            HttpClient(engine),
            preferences = prefs,
            inventoryManager = inventory(),
        )
        val result = request.use(9508, 1)
        assertTrue(result.isSuccess)
        assertEquals(1250, prefs.getInt("asdonMartinFuel", 0))
    }

    private fun inventory(): InventoryManager =
        InventoryManager(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }),
            GameEventBus(),
        )
}
