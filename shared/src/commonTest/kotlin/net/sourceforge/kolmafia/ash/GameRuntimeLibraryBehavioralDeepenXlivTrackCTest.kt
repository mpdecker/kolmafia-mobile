package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionRuntimeState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.maximizer.MaximizerBoost

/** XLIV Track C — maximize records + creatable runtime (phases 6531–6550). */
class GameRuntimeLibraryBehavioralDeepenXlivTrackCTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun maximizerBoostToRecord_splitsDisplayAndAfterdisplay() {
        val boost = MaximizerBoost(
            cmd = "cast 1 saucegeyser",
            text = "cast 1 Saucegeyser (+12.5)",
            delta = 12.5,
            isEquipment = false,
            effectName = "Saucegeyser",
            itemId = 0,
        )
        val rec = maximizerBoostToRecord(boost, MAXIMIZER_RESULT_FULL_REC, full = true)
        assertEquals("cast 1 Saucegeyser", rec.getField("display").toString())
        assertEquals("cast 1 saucegeyser", rec.getField("command").toString())
        assertEquals(12.5, rec.getField("score").toDouble())
        assertEquals("Saucegeyser", rec.getField("effect").toString())
        assertEquals("(+12.5)", rec.getField("afterdisplay").toString())
    }

    @Test
    fun filterMaximizerBoosts_skipsLeadingEquipmentWhenHidden() {
        val equip = MaximizerBoost(
            cmd = "equip hat fedora",
            text = "equip hat fedora",
            isEquipment = true,
        )
        val cast = MaximizerBoost(
            cmd = "cast 1 noodle",
            text = "cast 1 Noodle (+1)",
            delta = 1.0,
            isEquipment = false,
            effectName = "Noodle",
        )
        val filtered = filterMaximizerBoostsForAsh(listOf(equip, cast), showEquipment = false)
        assertEquals(1, filtered.size)
        assertEquals("cast 1 noodle", filtered[0].cmd)
    }

    @Test
    fun maximizerBoostsToAshRecords_fullIncludesAfterdisplayAndItem() {
        val boosts = listOf(
            MaximizerBoost(
                cmd = "equip hat crown",
                text = "equip hat crown (+3)",
                delta = 3.0,
                isEquipment = true,
                itemName = "crown",
            ),
            MaximizerBoost(
                cmd = "cast 1 empathy",
                text = "cast 1 Empathy (+5)",
                delta = 5.0,
                isEquipment = false,
                effectName = "Empathy",
            ),
        )
        val records = maximizerBoostsToAshRecords(
            filterMaximizerBoostsForAsh(boosts, showEquipment = false),
            full = false,
        )
        assertEquals(1L, records.size().toLong())
        val first = records[AshValue.of(0)] as RecordValue
        assertEquals("cast 1 Empathy", first.getField("display").toString())
        assertEquals("cast 1 empathy", first.getField("command").toString())
        assertEquals(5.0, first.getField("score").toDouble())

        val full = maximizerBoostsToAshRecords(boosts, full = true)
        assertEquals(2L, full.size().toLong())
        val equipRec = full[AshValue.of(0)] as RecordValue
        assertEquals("(+3)", equipRec.getField("afterdisplay").toString())
        assertEquals("crown", equipRec.getField("item").toString())
    }

    @Test
    fun creatableAmount_prefersRuntimeQuantityPossible() {
        registerItem(6601, "runtime brew")
        registerItem(6602, "runtime malt")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "runtime brew",
                resultQuantity = 1,
                methods = setOf("MIX"),
                ingredients = listOf(ConcoctionIngredient("runtime malt", 1)),
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            "runtime brew",
            ConcoctionRuntimeState(creatable = 7, visibleTotal = 9, total = 9),
        )
        val lib = GameRuntimeLibrary()
        assertEquals(
            "7",
            outputLib(lib, """print(creatable_amount(to_item("runtime brew")));""").trim(),
        )
    }

    @Test
    fun currentMaximizerScore_blankGoalIsZero_andLastSucceededDefaultsFalse() {
        val lib = GameRuntimeLibrary()
        assertEquals("false", outputLib(lib, "print(last_maximizer_succeeded());").trim().lowercase())
        assertEquals("0.0", outputLib(lib, """print(current_maximizer_score(""));""").trim())
        assertEquals("0.0", outputLib(lib, "print(current_maximizer_score());").trim())
    }

    @Test
    fun haveOutfit_unknownNameIsFalse() {
        assertEquals(
            "false",
            outputLib(GameRuntimeLibrary(), """print(have_outfit("no-such-outfit-xyz"));""")
                .trim()
                .lowercase(),
        )
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
