package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.modifiers.BitmapModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModifierDatabaseMutexTest {
    @BeforeTest
    fun setUp() {
        ModifierDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun rebuildMutexBits_assignsDistinctBitsToMutexGroupPieces() {
        ModifierDatabase.injectForTest("Item", "mutex hat a", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Item", "mutex hat b", "Meat Drop: +2")
        ModifierDatabase.injectForTest("MutexI", "mutex hat a/mutex hat b", "none")
        ModifierDatabase.rebuildMutexBitsForTest()

        val a = ModifierDatabase.getItem("mutex hat a")!!
        val b = ModifierDatabase.getItem("mutex hat b")!!
        val parsedA = ModifierParser.parse(a.modifiers)
        val parsedB = ModifierParser.parse(b.modifiers)
        assertTrue(parsedA.get(BitmapModifier.MUTEX) != 0)
        assertEquals(parsedA.get(BitmapModifier.MUTEX), parsedB.get(BitmapModifier.MUTEX))
    }

    @Test
    fun rebuildMutexBits_effectGroupsAssignMutexBits() {
        ModifierDatabase.injectForTest("Effect", "mutex effect a", "Meat Drop: +1")
        ModifierDatabase.injectForTest("Effect", "mutex effect b", "Meat Drop: +2")
        ModifierDatabase.injectForTest("MutexE", "mutex effect a/mutex effect b", "none")
        ModifierDatabase.rebuildMutexBitsForTest()

        val a = ModifierDatabase.getEffect("mutex effect a")!!
        val b = ModifierDatabase.getEffect("mutex effect b")!!
        assertTrue(ModifierParser.parse(a.modifiers).get(BitmapModifier.MUTEX) != 0)
        assertEquals(
            ModifierParser.parse(a.modifiers).get(BitmapModifier.MUTEX),
            ModifierParser.parse(b.modifiers).get(BitmapModifier.MUTEX),
        )
    }

    @Test
    fun load_assignsMutexBitsFromBundledData() {
        runBlocking { ModifierDatabase.load() }
        val halo = ModifierDatabase.getItem("shining halo")
        val frosty = ModifierDatabase.getItem("frosty halo")
        if (halo != null && frosty != null) {
            val haloMutex = ModifierParser.parse(halo.modifiers).get(BitmapModifier.MUTEX)
            val frostyMutex = ModifierParser.parse(frosty.modifiers).get(BitmapModifier.MUTEX)
            assertTrue(haloMutex != 0)
            assertEquals(haloMutex, frostyMutex)
        }
    }
}
