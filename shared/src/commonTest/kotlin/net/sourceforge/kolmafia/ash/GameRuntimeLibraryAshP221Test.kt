package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class GameRuntimeLibraryAshP221Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        FoldGroupDatabase.resetForTest()
    }

    @Test
    fun getRelated_fold_returnsChainPositions() {
        registerItem(FOLD_A, "fold item a", ItemPrimaryUse.WEAPON)
        registerItem(FOLD_B, "fold item b", ItemPrimaryUse.WEAPON)
        registerItem(FOLD_C, "fold item c", ItemPrimaryUse.WEAPON)
        FoldGroupDatabase.registerGroupForTest(
            FoldGroup(
                hpDamagePct = 0,
                items = listOf("fold item a", "fold item b", "fold item c"),
            ),
        )
        val lib = GameRuntimeLibrary()
        val out = outputLib(
            lib,
            """
                item i = to_item("fold item b");
                print(get_related(i, "fold")[to_item("fold item a")]);
                print(get_related(i, "fold")[to_item("fold item b")]);
                print(get_related(i, "fold")[to_item("fold item c")]);
            """.trimIndent(),
        )
        assertEquals(
            """
                1
                2
                3
            """.trimIndent(),
            out.trim(),
        )
    }

    @Test
    fun getRelated_fold_unknownItem_returnsEmptyAggregate() {
        registerItem(TEST_ITEM, "lonely item", ItemPrimaryUse.WEAPON)
        val lib = GameRuntimeLibrary()
        val out = outputLib(
            lib,
            """
                item i = to_item("lonely item");
                print(count(get_related(i, "fold")));
            """.trimIndent(),
        )
        assertEquals("0", out.trim())
    }

    private fun registerItem(id: Int, name: String, primaryUse: ItemPrimaryUse) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = primaryUse,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val FOLD_A = 9101
        private const val FOLD_B = 9102
        private const val FOLD_C = 9103
        private const val TEST_ITEM = 9104
    }
}
