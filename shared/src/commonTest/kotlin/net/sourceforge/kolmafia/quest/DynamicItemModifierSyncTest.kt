package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class DynamicItemModifierSyncTest {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        ItemDatabase.resetForTest()
        EffectDatabase.resetForTest()
    }

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun stubDb(vararg items: ItemData): GameDatabase =
        object : GameDatabase() {
            override fun item(name: String): ItemData? =
                items.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

    private fun testItem(
        id: Int,
        name: String,
        descId: String = "desc$id",
    ): ItemData {
        val item = ItemData(
            id = id,
            name = name,
            descId = descId,
            image = "test.gif",
            primaryUse = ItemPrimaryUse.HAT,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        ItemDatabase.registerForTest(item)
        return item
    }

    private fun testEffect(
        id: Int,
        name: String,
        descId: String = "eff$id",
    ): EffectData {
        val effect = EffectData(
            id = id,
            name = name,
            image = "eff.gif",
            descId = descId,
            quality = EffectQuality.GOOD,
            attributes = emptySet(),
        )
        EffectDatabase.registerForTest(effect)
        return effect
    }

    private fun emptyContext() = DynamicItemModifierSync.CheckContext(
        inventoryItemIds = emptySet(),
        equippedItemNames = emptySet(),
        activeEffectNames = emptySet(),
    )

    @Test
    fun applyCachedOverrides_restoresItemModifierFromPref() {
        ModifierDatabase.injectForTest(
            "Item",
            "pantogram pants",
            "Lasts Until Rollover",
        )
        val p = prefs()
        p.setString(
            "_pantogramModifier",
            "Muscle: +10, Meat Drop: +60, Lasts Until Rollover",
        )
        DynamicItemModifierSync.applyCachedOverrides(p)
        val entry = ModifierDatabase.getItem("pantogram pants")
        assertEquals(
            "Muscle: +10, Meat Drop: +60, Lasts Until Rollover",
            entry?.modifiers,
        )
    }

    @Test
    fun applyCachedOverrides_ignoresEmptyPref() {
        ModifierDatabase.injectForTest(
            "Item",
            "no hat",
            "Lasts Until Rollover",
        )
        val p = prefs()
        DynamicItemModifierSync.applyCachedOverrides(p)
        assertEquals("Lasts Until Rollover", ModifierDatabase.getItem("no hat")?.modifiers)
    }

    @Test
    fun applyCachedOverrides_restoresEffectModifierFromPref() {
        ModifierDatabase.injectForTest(
            "Effect",
            "Grafted",
            "Lasts Until Rollover",
        )
        val p = prefs()
        p.setString(
            "zootGraftedMods",
            "Muscle: +10, Meat Drop: +60, Lasts Until Rollover",
        )
        DynamicItemModifierSync.applyCachedOverrides(p)
        val entry = ModifierDatabase.getEffect("Grafted")
        assertEquals(
            "Muscle: +10, Meat Drop: +60, Lasts Until Rollover",
            entry?.modifiers,
        )
    }

    @Test
    fun applyCachedOverrides_restoresG9FromNumericPref() {
        ModifierDatabase.injectForTest(
            "Effect",
            "Experimental Effect G-9",
            "Lasts Until Rollover",
        )
        val p = prefs()
        p.setString("_g9Effect", "5")
        DynamicItemModifierSync.applyCachedOverrides(p)
        assertEquals(
            "Muscle Percent: +5, Mysticality Percent: +5, Moxie Percent: +5",
            ModifierDatabase.getEffect("Experimental Effect G-9")?.modifiers,
        )
    }

    @Test
    fun checkMods_visitsItemDescWhenPrefEmptyAndInInventory() {
        val noHat = testItem(9270, "no hat", "887807812")
        val db = stubDb(noHat)
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(9270),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkMods(p, context, db)
        assertEquals(1, visits.size)
        assertEquals(
            DynamicItemModifierSync.DescVisit.Item("887807812"),
            visits.single(),
        )
    }

    @Test
    fun checkMods_skipsItemWhenPrefPopulated() {
        val noHat = testItem(9270, "no hat", "887807812")
        val db = stubDb(noHat)
        val p = prefs()
        p.setString("_noHatModifier", "Muscle: +5")
        ModifierDatabase.injectForTest("Item", "no hat", "Lasts Until Rollover")
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(9270),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkMods(p, context, db)
        assertTrue(visits.isEmpty())
        assertEquals("Muscle: +5", ModifierDatabase.getItem("no hat")?.modifiers)
    }

    @Test
    fun checkMods_skipsItemWhenNotOwned() {
        testItem(9270, "no hat", "887807812")
        val db = stubDb()
        val p = prefs()
        assertTrue(DynamicItemModifierSync.checkMods(p, emptyContext(), db).isEmpty())
    }

    @Test
    fun checkMods_visitsItemDescWhenEquippedByName() {
        val jickSword = testItem(6146, "Sword of Procedural Generation", "268014939")
        val db = stubDb(jickSword)
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = setOf("Sword of Procedural Generation"),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkMods(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("268014939")),
            visits,
        )
    }

    @Test
    fun checkMods_visitsEffectDescWhenActiveAndPrefEmpty() {
        testEffect(1, "Buzzed on Distillate", "buzzed-desc")
        val db = stubDb()
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = setOf("Buzzed on Distillate"),
        )
        val visits = DynamicItemModifierSync.checkMods(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Effect("buzzed-desc")),
            visits,
        )
    }

    @Test
    fun checkMods_skipsEffectWhenInactive() {
        testEffect(1, "Buzzed on Distillate", "buzzed-desc")
        val db = stubDb()
        val p = prefs()
        assertTrue(DynamicItemModifierSync.checkMods(p, emptyContext(), db).isEmpty())
    }

    @Test
    fun checkMods_skipsZootEffectsHandledByExtended() {
        testEffect(1, "Grafted", "grafted-desc")
        val db = stubDb()
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = setOf("Grafted"),
        )
        assertTrue(DynamicItemModifierSync.checkMods(p, context, db).isEmpty())
    }

    @Test
    fun checkOwnedItemDescriptions_visitsKgbWhenInInventory() {
        val kgb = testItem(9493, "Kremlin's Greatest Briefcase", "311743898")
        val db = stubDb(kgb)
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(9493),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkOwnedItemDescriptions(
            context,
            db,
            listOf("Kremlin's Greatest Briefcase"),
        )
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("311743898")),
            visits,
        )
    }

    @Test
    fun checkOwnedItemDescriptions_visitsBaseballDiamondWhenEquipped() {
        val diamond = testItem(12216, "Baseball Diamond", "229573660")
        val db = stubDb(diamond)
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = setOf("Baseball Diamond"),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkOwnedItemDescriptions(
            context,
            db,
            listOf("Baseball Diamond"),
        )
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("229573660")),
            visits,
        )
    }

    @Test
    fun checkOwnedItemDescriptions_visitsDartHolsterWhenEquipped() {
        val holster = testItem(11561, "Everfull Dart Holster", "dart-desc")
        val db = stubDb(holster)
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = setOf("Everfull Dart Holster"),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkOwnedItemDescriptions(
            context,
            db,
            DynamicItemModifierSync.OWNED_DESC_ITEMS,
        )
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("dart-desc")),
            visits,
        )
    }

    @Test
    fun checkOwnedItemDescriptions_visitsMimicEggWhenInInventory() {
        val egg = testItem(11542, "mimic egg", "egg-desc")
        val db = stubDb(egg)
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(11542),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkOwnedItemDescriptions(
            context,
            db,
            DynamicItemModifierSync.OWNED_DESC_ITEMS,
        )
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("egg-desc")),
            visits,
        )
    }

    @Test
    fun isEquippedOrInInventory_matchesCaseInsensitive() {
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = setOf("NO HAT"),
            activeEffectNames = emptySet(),
        )
        assertTrue(DynamicItemModifierSync.isEquippedOrInInventory(9270, "no hat", context))
    }

    @Test
    fun isAccessible_matchesClosetOnly() {
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(10899),
        )
        assertTrue(DynamicItemModifierSync.isAccessible(10899, "unbreakable umbrella", context))
        assertTrue(!DynamicItemModifierSync.isEquippedOrInInventory(10899, "unbreakable umbrella", context))
    }

    @Test
    fun checkExtendedMods_visitsSaberFromClosetWhenPrefZero() {
        val saber = testItem(10251, "Fourth of May Cosplay Saber", "saber-desc")
        val db = stubDb(saber)
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(10251),
        )
        val visits = DynamicItemModifierSync.checkExtendedMods(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("saber-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Item>(),
        )
    }

    @Test
    fun checkExtendedMods_skipsSaberWhenPrefSet() {
        val saber = testItem(10251, "Fourth of May Cosplay Saber", "saber-desc")
        val db = stubDb(saber)
        val p = prefs()
        p.setString("_saberMod", "1")
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(10251),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        assertTrue(DynamicItemModifierSync.checkExtendedMods(p, context, db).isEmpty())
    }

    @Test
    fun checkExtendedMods_visitsReplicaSaberOnLegacyOfLoathing() {
        val saber = testItem(10251, "Fourth of May Cosplay Saber", "saber-desc")
        val replica = testItem(11240, "replica Fourth of May Cosplay Saber", "replica-desc")
        val db = stubDb(saber, replica)
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(11240),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            ascensionPath = AscensionPath.LEGACY_OF_LOATHING,
        )
        val visits = DynamicItemModifierSync.checkExtendedMods(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("saber-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Item>()
                .filter { it.descId == "saber-desc" },
        )
    }

    @Test
    fun checkExtendedMods_visitsUmbrellaFromCloset() {
        val umbrella = testItem(10899, "unbreakable umbrella", "umbrella-desc")
        val db = stubDb(umbrella)
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(10899),
        )
        val visits = DynamicItemModifierSync.checkExtendedMods(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("umbrella-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Item>()
                .filter { it.descId == "umbrella-desc" },
        )
    }

    @Test
    fun checkExtendedMods_visitsVampireWineInInventoryOnly() {
        val wine = testItem(10800, "1950 Vampire Vintner wine", "wine-desc")
        val db = stubDb(wine)
        val p = prefs()
        val inInventory = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(10800),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("wine-desc")),
            DynamicItemModifierSync.checkExtendedMods(p, inInventory, db)
                .filterIsInstance<DynamicItemModifierSync.DescVisit.Item>()
                .filter { it.descId == "wine-desc" },
        )
        val inCloset = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(10800),
        )
        assertTrue(
            DynamicItemModifierSync.checkExtendedMods(p, inCloset, db)
                .none { it.path.contains("wine-desc") },
        )
    }

    @Test
    fun checkExtendedMods_crimboManualRespectsSkillPref() {
        val manual = testItem(11046, "Crimbo training manual", "manual-desc")
        val db = stubDb(manual)
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(11046),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("manual-desc")),
            DynamicItemModifierSync.checkExtendedMods(p, context, db)
                .filterIsInstance<DynamicItemModifierSync.DescVisit.Item>()
                .filter { it.descId == "manual-desc" },
        )
        p.setInt("crimboTrainingSkill", 5)
        assertTrue(
            DynamicItemModifierSync.checkExtendedMods(p, context, db)
                .none { it.path.contains("manual-desc") },
        )
    }

    @Test
    fun checkExtendedMods_visitsRingFromCloset() {
        val ring = testItem(10252, "ring", "ring-desc")
        val db = stubDb(ring)
        val p = prefs().also { it.setBoolean("autoSatisfyWithCloset", true) }
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(10252),
        )
        val visits = DynamicItemModifierSync.checkExtendedMods(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("ring-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Item>()
                .filter { it.descId == "ring-desc" },
        )
    }

    @Test
    fun checkExtendedMods_visitsRingFromStorage() {
        val ring = testItem(10252, "ring", "ring-desc")
        val db = stubDb(ring)
        val p = prefs().also { it.setBoolean("autoSatisfyWithStorage", true) }
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            storageItemIds = setOf(10252),
            canInteract = true,
        )
        val visits = DynamicItemModifierSync.checkExtendedMods(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("ring-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Item>()
                .filter { it.descId == "ring-desc" },
        )
    }

    @Test
    fun checkExtendedMods_visitsRingWhenMallAvailable() {
        val ring = ItemData(
            id = 10252,
            name = "ring",
            descId = "ring-desc",
            image = "dcring.gif",
            primaryUse = ItemPrimaryUse.ACCESSORY,
            secondaryUses = emptySet(),
            access = setOf('t'),
            autosellPrice = 0,
            plural = null,
        )
        ItemDatabase.registerForTest(ring)
        val db = stubDb(ring)
        val p = prefs().also { it.setBoolean("autoSatisfyWithMall", true) }
        val context = emptyContext()
        val visits = DynamicItemModifierSync.checkExtendedMods(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("ring-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Item>()
                .filter { it.descId == "ring-desc" },
        )
    }

    @Test
    fun checkExtendedMods_alwaysVisitsG9EffectDesc() {
        testEffect(1744, "Experimental Effect G-9", "g9-desc")
        val db = stubDb()
        val p = prefs()
        val visits = DynamicItemModifierSync.checkExtendedMods(p, emptyContext(), db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Effect("g9-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Effect>()
                .filter { it.descId == "g9-desc" },
        )
    }

    @Test
    fun checkExtendedMods_zootomistVisitsAllThreeEffectsWhenInactive() {
        testEffect(1, "Grafted", "grafted-desc")
        testEffect(2, "Milk of Familiar Kindness", "kindness-desc")
        testEffect(3, "Milk of Familiar Cruelty", "cruelty-desc")
        val db = stubDb()
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            ascensionPath = AscensionPath.Z_IS_FOR_ZOOTOMIST,
        )
        val effectVisits = DynamicItemModifierSync.checkExtendedMods(p, context, db)
            .filterIsInstance<DynamicItemModifierSync.DescVisit.Effect>()
        assertEquals(3, effectVisits.size)
        assertEquals(
            setOf("grafted-desc", "kindness-desc", "cruelty-desc"),
            effectVisits.map { it.descId }.toSet(),
        )
    }

    @Test
    fun checkExtendedMods_heartstoneVisitsWhenActiveEvenWithPref() {
        testEffect(3071, "Heartstone Attunement", "heart-desc")
        val db = stubDb()
        val p = prefs()
        p.setString("heartstoneAttunementMods", "Muscle: +10")
        ModifierDatabase.injectForTest("Effect", "Heartstone Attunement", "Lasts Until Rollover")
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = setOf("Heartstone Attunement"),
        )
        val visits = DynamicItemModifierSync.checkExtendedMods(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Effect("heart-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Effect>()
                .filter { it.descId == "heart-desc" },
        )
        assertEquals("Muscle: +10", ModifierDatabase.getEffect("Heartstone Attunement")?.modifiers)
    }

    @Test
    fun combinedVisits_deduplicateByPath() {
        val noHat = testItem(9270, "no hat", "887807812")
        testEffect(1744, "Experimental Effect G-9", "g9-desc")
        val db = stubDb(noHat)
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(9270),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val visits = (
            DynamicItemModifierSync.checkMods(p, context, db) +
                DynamicItemModifierSync.checkExtendedMods(p, context, db)
            ).distinctBy { it.path }
        assertEquals(2, visits.size)
        assertEquals(2, visits.map { it.path }.toSet().size)
    }

    @Test
    fun checkLoginDescChecks_visitsCrownFromClosetWhenNotEquipped() {
        val crown = testItem(4614, "Crown of Thrones", "crown-desc")
        val db = stubDb(crown)
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(4614),
        )
        val visits = DynamicItemModifierSync.checkLoginDescChecks(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("crown-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Item>()
                .filter { it.descId == "crown-desc" },
        )
    }

    @Test
    fun checkLoginDescChecks_skipsCrownWhenEquipped() {
        testItem(4614, "Crown of Thrones", "crown-desc")
        val db = stubDb()
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = setOf("Crown of Thrones"),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(4614),
        )
        assertTrue(
            DynamicItemModifierSync.checkLoginDescChecks(p, context, db)
                .none { it.path.contains("crown-desc") },
        )
    }

    @Test
    fun checkLoginDescChecks_visitsBuddyBjornWhenInInventory() {
        val bjorn = testItem(7200, "Buddy Bjorn", "bjorn-desc")
        val db = stubDb(bjorn)
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(7200),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkLoginDescChecks(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("bjorn-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Item>()
                .filter { it.descId == "bjorn-desc" },
        )
    }

    @Test
    fun checkLoginDescChecks_alwaysVisitsEntauntauned() {
        testEffect(2578, "Entauntauned", "enta-desc")
        val db = stubDb()
        val p = prefs()
        val visits = DynamicItemModifierSync.checkLoginDescChecks(p, emptyContext(), db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Effect("enta-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Effect>()
                .filter { it.descId == "enta-desc" },
        )
    }

    @Test
    fun checkLoginDescChecks_savageBeastVisitsWhenActiveEvenWithPref() {
        testEffect(2898, "Savage Beast", "beast-desc")
        val db = stubDb()
        val p = prefs()
        p.setString("_savageBeastMods", "Combat Rate: +25")
        ModifierDatabase.injectForTest("Effect", "Savage Beast", "Lasts Until Rollover")
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = setOf("Savage Beast"),
        )
        val visits = DynamicItemModifierSync.checkLoginDescChecks(p, context, db)
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Effect("beast-desc")),
            visits.filterIsInstance<DynamicItemModifierSync.DescVisit.Effect>()
                .filter { it.descId == "beast-desc" },
        )
        assertEquals("Combat Rate: +25", ModifierDatabase.getEffect("Savage Beast")?.modifiers)
    }

    @Test
    fun checkLoginDescChecks_skipsSavageBeastWhenInactive() {
        testEffect(2898, "Savage Beast", "beast-desc")
        val db = stubDb()
        val p = prefs()
        assertTrue(
            DynamicItemModifierSync.checkLoginDescChecks(p, emptyContext(), db)
                .none { it.path.contains("beast-desc") },
        )
    }

    @Test
    fun checkMods_skipsSavageBeastHandledByLoginChecks() {
        testEffect(2898, "Savage Beast", "beast-desc")
        val db = stubDb()
        val p = prefs()
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = setOf("Savage Beast"),
        )
        assertTrue(DynamicItemModifierSync.checkMods(p, context, db).isEmpty())
    }

    @Test
    fun checkOwnedItemDescriptions_visitsKgbFromCloset() {
        val kgb = testItem(9493, "Kremlin's Greatest Briefcase", "311743898")
        val db = stubDb(kgb)
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(9493),
        )
        val visits = DynamicItemModifierSync.checkOwnedItemDescriptions(
            context,
            db,
            listOf("Kremlin's Greatest Briefcase"),
        )
        assertEquals(
            listOf(DynamicItemModifierSync.DescVisit.Item("311743898")),
            visits,
        )
    }

    @Test
    fun checkCoatOfPaint_visitsWhenAccessibleInClosetAndPrefEmpty() {
        val coat = testItem(10732, DynamicItemModifierSync.COAT_OF_PAINT_ITEM, "640494952")
        val db = stubDb(coat)
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = emptySet(),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
            closetItemIds = setOf(coat.id),
        )
        val visits = DynamicItemModifierSync.checkCoatOfPaint(prefs(), context, db, playerClassChanged = false)
        assertEquals(listOf(DynamicItemModifierSync.DescVisit.Item("640494952")), visits)
    }

    @Test
    fun checkCoatOfPaint_cachedPrefSkipsVisitWithoutClassChange() {
        val coat = testItem(10732, DynamicItemModifierSync.COAT_OF_PAINT_ITEM, "640494952")
        val db = stubDb(coat)
        val p = prefs()
        p.setString("_coatOfPaintModifier", "Muscle: +10")
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(coat.id),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkCoatOfPaint(p, context, db, playerClassChanged = false)
        assertTrue(visits.isEmpty())
        assertEquals(
            "Muscle: +10",
            ModifierDatabase.getItem(DynamicItemModifierSync.COAT_OF_PAINT_ITEM)?.modifiers,
        )
    }

    @Test
    fun checkCoatOfPaint_classChangeForcesVisitDespiteCachedPref() {
        val coat = testItem(10732, DynamicItemModifierSync.COAT_OF_PAINT_ITEM, "640494952")
        val db = stubDb(coat)
        val p = prefs()
        p.setString("_coatOfPaintModifier", "Muscle: +10")
        val context = DynamicItemModifierSync.CheckContext(
            inventoryItemIds = setOf(coat.id),
            equippedItemNames = emptySet(),
            activeEffectNames = emptySet(),
        )
        val visits = DynamicItemModifierSync.checkCoatOfPaint(p, context, db, playerClassChanged = true)
        assertEquals(listOf(DynamicItemModifierSync.DescVisit.Item("640494952")), visits)
    }
}
