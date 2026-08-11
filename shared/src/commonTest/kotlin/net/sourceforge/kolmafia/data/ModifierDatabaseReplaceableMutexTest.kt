package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModifierDatabaseReplaceableMutexTest {
    @BeforeTest
    fun setUp() {
        ModifierDatabase.resetForTest()
        EffectDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        EffectDatabase.resetForTest()
    }

    @Test
    fun rebuildReplaceableMutexEffects_mapsPeerSetForEachMember() {
        registerEffect(9001, "replaceable song a")
        registerEffect(9002, "replaceable song b")
        ModifierDatabase.injectForTest(
            "MutexER",
            "replaceable song a/replaceable song b",
            "none",
        )
        ModifierDatabase.rebuildReplaceableMutexEffectsForTest()

        val peersA = ModifierDatabase.getReplaceableMutexFor(9001)
        val peersB = ModifierDatabase.getReplaceableMutexFor(9002)
        assertEquals(setOf(9001, 9002), peersA)
        assertEquals(peersA, peersB)
    }

    @Test
    fun rebuildReplaceableMutexEffects_resolvesBracketIdPieces() {
        registerEffect(1553, "Slicked-Back Do")
        registerEffect(9003, "Pompadour")
        ModifierDatabase.injectForTest(
            "MutexER",
            "Pompadour/[1553]Slicked-Back Do",
            "none",
        )
        ModifierDatabase.rebuildReplaceableMutexEffectsForTest()

        assertEquals(
            setOf(1553, 9003),
            ModifierDatabase.getReplaceableMutexFor(1553),
        )
        assertEquals(
            setOf(1553, 9003),
            ModifierDatabase.getReplaceableMutexFor(9003),
        )
    }

    @Test
    fun resolveEffectId_parsesBracketPrefix() {
        registerEffect(1553, "Slicked-Back Do")
        assertEquals(1553, EffectDefinitionProxy.resolveEffectId("[1553]Slicked-Back Do"))
        assertEquals(
            "Slicked-Back Do",
            EffectDefinitionProxy.getByIdOrName("[1553]Slicked-Back Do")?.name,
        )
    }

    @Test
    fun getReplaceableMutexFor_unknownEffect_returnsEmpty() {
        assertTrue(ModifierDatabase.getReplaceableMutexFor(99999).isEmpty())
    }

    private fun registerEffect(id: Int, name: String) {
        EffectDatabase.registerForTest(
            EffectData(
                id = id,
                name = name,
                image = "test.gif",
                descId = "test$id",
                quality = EffectQuality.NEUTRAL,
                attributes = emptySet(),
                actions = "cast 1 $name",
            ),
        )
    }
}
