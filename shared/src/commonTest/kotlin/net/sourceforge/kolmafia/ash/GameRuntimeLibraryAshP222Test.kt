package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ZapGroupDatabase

class GameRuntimeLibraryAshP222Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ZapGroupDatabase.resetForTest()
    }

    @Test
    fun getRelated_zap_returnsOtherGroupMembersWithZero() {
        registerItem(ZAP_A, "zap item a", ItemPrimaryUse.NONE)
        registerItem(ZAP_B, "zap item b", ItemPrimaryUse.NONE)
        registerItem(ZAP_C, "zap item c", ItemPrimaryUse.NONE)
        ZapGroupDatabase.registerGroupForTest(listOf("zap item a", "zap item b", "zap item c"))
        val lib = GameRuntimeLibrary()
        val out = outputLib(
            lib,
            """
                item i = to_item("zap item b");
                print(count(get_related(i, "zap")));
                print(contains_key(get_related(i, "zap"), to_item("zap item a")));
                print(contains_key(get_related(i, "zap"), to_item("zap item b")));
                print(contains_key(get_related(i, "zap"), to_item("zap item c")));
                print(get_related(i, "zap")[to_item("zap item a")]);
            """.trimIndent(),
        )
        assertEquals(
            """
                2
                true
                false
                true
                0
            """.trimIndent(),
            out.trim(),
        )
    }

    @Test
    fun revision_isphase222() {
        assertEquals("phase485", GameRuntimeLibrary.REVISION)
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
        private const val ZAP_A = 9201
        private const val ZAP_B = 9202
        private const val ZAP_C = 9203
    }
}
