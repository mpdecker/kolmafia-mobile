package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaximizerSynergyAdjustmentsTest {

    @BeforeTest
    fun setup() {
        ModifierDatabase.resetForTest()
    }

    @AfterTest
    fun teardown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun twoItemSynergy_clearsAutomaticWhenPairNotBetter() = runBlocking {
        registerSynergyPair(
            idA = 901,
            nameA = "syn-a",
            modsA = "Meat Drop: +10, Synergetic",
            idB = 902,
            nameB = "syn-b",
            modsB = "Meat Drop: +10, Synergetic",
            synergyMods = "Meat Drop: +5",
        )
        val db = stubDb(
            registerAccessory(901, "syn-a", "Meat Drop: +10, Synergetic"),
            registerAccessory(902, "syn-b", "Meat Drop: +10, Synergetic"),
            registerAccessory(903, "filler-a", "Meat Drop: +30"),
            registerAccessory(904, "filler-b", "Meat Drop: +28"),
            modifiers = mapOf(
                "syn-a" to "Meat Drop: +10, Synergetic",
                "syn-b" to "Meat Drop: +10, Synergetic",
                "filler-a" to "Meat Drop: +30",
                "filler-b" to "Meat Drop: +28",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = enumerateWithAuto(db, spec, setOf(901, 902, 903, 904))
        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "syn-a" && it.automatic })
        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "syn-b" && it.automatic })

        MaximizerSynergyAdjustments.apply(buckets, spec, CharacterState(), db)

        assertFalse(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "syn-a" && it.automatic })
        assertFalse(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "syn-b" && it.automatic })
    }

    @Test
    fun twoItemSynergy_keepsAutomaticWhenPairBetter() = runBlocking {
        registerSynergyPair(
            idA = 911,
            nameA = "strong-a",
            modsA = "Meat Drop: +20, Synergetic",
            idB = 912,
            nameB = "strong-b",
            modsB = "Meat Drop: +20, Synergetic",
            synergyMods = "Meat Drop: +50",
        )
        val db = stubDb(
            registerAccessory(911, "strong-a", "Meat Drop: +20, Synergetic"),
            registerAccessory(912, "strong-b", "Meat Drop: +20, Synergetic"),
            registerAccessory(913, "filler-a", "Meat Drop: +30"),
            registerAccessory(914, "filler-b", "Meat Drop: +25"),
            modifiers = mapOf(
                "strong-a" to "Meat Drop: +20, Synergetic",
                "strong-b" to "Meat Drop: +20, Synergetic",
                "filler-a" to "Meat Drop: +30",
                "filler-b" to "Meat Drop: +25",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = enumerateWithAuto(db, spec, setOf(911, 912, 913, 914))
        MaximizerSynergyAdjustments.apply(buckets, spec, CharacterState(), db)

        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "strong-a" && it.automatic })
        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "strong-b" && it.automatic })
    }

    @Test
    fun tripleAccessory_reinstatesAutomaticWhenTrioWins() = runBlocking {
        ModifierDatabase.injectForTest("Item", "monstrous monocle", "Meat Drop: +10, Synergetic")
        ModifierDatabase.injectForTest("Item", "musty moccasins", "Meat Drop: +10, Synergetic")
        ModifierDatabase.injectForTest("Item", "molten medallion", "Meat Drop: +10, Synergetic")
        ModifierDatabase.injectForTest(
            "Synergy",
            "monstrous monocle/musty moccasins/molten medallion",
            "Meat Drop: +50",
        )
        val db = stubDb(
            registerAccessory(4108, "monstrous monocle", "Meat Drop: +10, Synergetic"),
            registerAccessory(4109, "musty moccasins", "Meat Drop: +10, Synergetic"),
            registerAccessory(4110, "molten medallion", "Meat Drop: +10, Synergetic"),
            registerAccessory(4201, "acc-a", "Meat Drop: +20"),
            registerAccessory(4202, "acc-b", "Meat Drop: +19"),
            registerAccessory(4203, "acc-c", "Meat Drop: +18"),
            modifiers = mapOf(
                "monstrous monocle" to "Meat Drop: +10, Synergetic",
                "musty moccasins" to "Meat Drop: +10, Synergetic",
                "molten medallion" to "Meat Drop: +10, Synergetic",
                "acc-a" to "Meat Drop: +20",
                "acc-b" to "Meat Drop: +19",
                "acc-c" to "Meat Drop: +18",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = enumerateWithAuto(
            db,
            spec,
            setOf(4108, 4109, 4110, 4201, 4202, 4203),
        )
        MaximizerEquipmentEnumerator.setAutomaticByName(buckets, "monstrous monocle", false)
        MaximizerEquipmentEnumerator.setAutomaticByName(buckets, "musty moccasins", false)
        MaximizerEquipmentEnumerator.setAutomaticByName(buckets, "molten medallion", false)

        MaximizerSynergyAdjustments.apply(buckets, spec, CharacterState(), db)

        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "monstrous monocle" && it.automatic })
        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "musty moccasins" && it.automatic })
        assertTrue(buckets.allItems(MaximizerSlot.ACC1).any { it.name == "molten medallion" && it.automatic })
    }

    @Test
    fun mergeBuckets_respectsClearedAutomatic() = runBlocking {
        registerSynergyPair(
            idA = 931,
            nameA = "weak-a",
            modsA = "Meat Drop: +10, Synergetic",
            idB = 932,
            nameB = "weak-b",
            modsB = "Meat Drop: +10, Synergetic",
            synergyMods = "Meat Drop: +5",
        )
        val db = stubDb(
            registerAccessory(931, "weak-a", "Meat Drop: +10, Synergetic"),
            registerAccessory(932, "weak-b", "Meat Drop: +10, Synergetic"),
            registerAccessory(933, "filler-a", "Meat Drop: +30"),
            registerAccessory(934, "filler-b", "Meat Drop: +28"),
            modifiers = mapOf(
                "weak-a" to "Meat Drop: +10, Synergetic",
                "weak-b" to "Meat Drop: +10, Synergetic",
                "filler-a" to "Meat Drop: +30",
                "filler-b" to "Meat Drop: +28",
            ),
        )
        val spec = MaximizeSpec(
            primary = DoubleModifier.MEATDROP,
            evaluator = Evaluator("meat"),
        )
        val buckets = enumerateWithAuto(db, spec, setOf(931, 932, 933, 934))
        MaximizerSynergyAdjustments.apply(buckets, spec, CharacterState(), db)
        val merged = MaximizerEquipmentEnumerator.mergeBuckets(
            buckets,
            listOf(MaximizerSlot.ACC1),
            limit = 1,
        )
        assertFalse("weak-a" in merged.map { it.first })
        assertFalse("weak-b" in merged.map { it.first })
        assertEquals("filler-a", merged.first().first)
    }

    private fun registerSynergyPair(
        idA: Int,
        nameA: String,
        modsA: String,
        idB: Int,
        nameB: String,
        modsB: String,
        synergyMods: String,
    ) {
        ModifierDatabase.injectForTest("Item", nameA, modsA)
        ModifierDatabase.injectForTest("Item", nameB, modsB)
        ModifierDatabase.injectForTest("Synergy", "$nameA/$nameB", synergyMods)
    }

    private fun registerAccessory(
        id: Int,
        name: String,
        mods: String,
    ): Pair<Int, ItemData> {
        ModifierDatabase.injectForTest("Item", name, mods)
        return id to accessory(id, name)
    }

    private fun enumerateWithAuto(
        db: GameDatabase,
        spec: MaximizeSpec,
        candidateIds: Set<Int>,
    ): SlotList<MaximizerRankedItem> =
        MaximizerEquipmentEnumerator.enumerate(
            candidateIds = candidateIds,
            spec = spec,
            gameDatabase = db,
            checkedItem = { itemId -> MaximizerCheckedItem(itemId, db.item(itemId)?.name ?: "", initial = 1) },
            scoreItem = { name, ev ->
                val entry = db.itemModifier(name) ?: return@enumerate 0.0
                ev.getItemContribution(net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers))
            },
            itemMeetsConstraints = { _, _ -> true },
            autoContext = MaximizerAutoContext.from(spec.evaluator),
        )

    private fun accessory(id: Int, name: String) =
        ItemData(id, name, "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null)

    private fun stubDb(
        vararg items: Pair<Int, ItemData>,
        modifiers: Map<String, String> = emptyMap(),
    ): GameDatabase = object : GameDatabase() {
        private val byId = items.toMap()
        override fun item(id: Int): ItemData? = byId[id]
        override fun item(name: String): ItemData? =
            byId.values.find { it.name.equals(name, ignoreCase = true) }
        override fun itemModifier(name: String): ModifierEntry? =
            modifiers.entries.find { it.key.equals(name, ignoreCase = true) }
                ?.let { ModifierEntry("Item", it.key, it.value) }
    }
}
