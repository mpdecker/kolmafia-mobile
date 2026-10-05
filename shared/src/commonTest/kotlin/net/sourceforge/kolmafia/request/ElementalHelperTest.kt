package net.sourceforge.kolmafia.request

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ElementalHelperTest {

    @AfterTest
    fun tearDown() {
        ElementalHelper.resetForTest()
    }

    @Test
    fun prepare_removesHotformAndChecksHp() = runTest {
        var uneffected = false
        var hp = 50
        ElementalHelper.hasEffect = { it.equals("Hotform", ignoreCase = true) && !uneffected }
        ElementalHelper.uneffect = {
            uneffected = true
            true
        }
        ElementalHelper.currentHp = { hp }
        ElementalHelper.elementalResistancePercent = { 0.0 }
        ElementalHelper.recoverHpTo = {
            hp = it
            true
        }

        val result = ElementalHelper.prepare("Hotform", "hot", 1000)
        assertEquals("", result)
        assertTrue(uneffected)
        assertTrue(hp > 1000)
    }

    @Test
    fun prepare_abortsWhenUneffectFails() = runTest {
        ElementalHelper.hasEffect = { true }
        ElementalHelper.uneffect = { false }
        ElementalHelper.currentHp = { 9999 }

        val result = ElementalHelper.prepare("Coldform", "cold", 1000)
        assertTrue(result.contains("Unable to remove Coldform"))
    }

    @Test
    fun prepare_abortsWhenHpInsufficient() = runTest {
        ElementalHelper.hasEffect = { false }
        ElementalHelper.currentHp = { 10 }
        ElementalHelper.elementalResistancePercent = { 0.0 }
        ElementalHelper.recoverHpTo = { false }

        val result = ElementalHelper.prepare("Hotform", "hot", 1000)
        assertTrue(result.contains("Unable to gain enough HP"))
    }

    @Test
    fun prepareForUtensil_routesForkAndMug() = runTest {
        ElementalHelper.hasEffect = { false }
        ElementalHelper.currentHp = { 5000 }
        ElementalHelper.elementalResistancePercent = { 50.0 }

        assertEquals("", ElementalHelper.prepareForUtensil(ElementalHelper.SCRATCHS_FORK))
        assertEquals("", ElementalHelper.prepareForUtensil(ElementalHelper.FROSTYS_MUG))
        assertEquals("", ElementalHelper.prepareForUtensil(1))
    }
}
