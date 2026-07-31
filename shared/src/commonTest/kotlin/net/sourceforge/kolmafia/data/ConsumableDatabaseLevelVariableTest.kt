package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsumableDatabaseLevelVariableTest {

    @BeforeTest
    fun setUp() {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "astral pilsner",
                type = ConsumableType.DRINK,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.UNKNOWN,
                advMin = 11,
                advMax = 22,
                muscMin = 0,
                muscMax = 22,
                mystMin = 0,
                mystMax = 22,
                moxieMin = 0,
                moxieMax = 22,
                notes = "bundled",
            ),
        )
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "astral hot dog",
                type = ConsumableType.FOOD,
                amount = 3,
                levelReq = 1,
                quality = ConsumableQuality.UNKNOWN,
                advMin = 20,
                advMax = 24,
                muscMin = 176,
                muscMax = 220,
                mystMin = 176,
                mystMax = 220,
                moxieMin = 176,
                moxieMax = 220,
                notes = "bundled",
            ),
        )
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "Cold One",
                type = ConsumableType.DRINK,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.UNKNOWN,
                advMin = 6,
                advMax = 6,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "bundled",
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun setLevelVariableConsumables_level5_astralPilsner() {
        ConsumableDatabase.setLevelVariableConsumables(5)
        assertEquals("5", ConsumableDatabase.getAdventureRange("astral pilsner"))
        assertEquals("0-10", ConsumableDatabase.getMuscleRange("astral pilsner"))
    }

    @Test
    fun setLevelVariableConsumables_level11_astralPilsner() {
        ConsumableDatabase.setLevelVariableConsumables(11)
        assertEquals("11", ConsumableDatabase.getAdventureRange("astral pilsner"))
        assertEquals("0-22", ConsumableDatabase.getMuscleRange("astral pilsner"))
    }

    @Test
    fun setLevelVariableConsumables_level5_astralHotDog() {
        ConsumableDatabase.setLevelVariableConsumables(5)
        assertEquals("9-11", ConsumableDatabase.getAdventureRange("astral hot dog"))
        assertEquals("80-100", ConsumableDatabase.getMuscleRange("astral hot dog"))
    }

    @Test
    fun setLevelVariableConsumables_level11_coldOne() {
        ConsumableDatabase.setLevelVariableConsumables(11)
        assertEquals("6", ConsumableDatabase.getAdventureRange("Cold One"))
    }

    @Test
    fun setLevelVariableConsumables_floorsLevelBelow3() {
        ConsumableDatabase.setLevelVariableConsumables(1)
        assertEquals("3", ConsumableDatabase.getAdventureRange("astral pilsner"))
    }
}
