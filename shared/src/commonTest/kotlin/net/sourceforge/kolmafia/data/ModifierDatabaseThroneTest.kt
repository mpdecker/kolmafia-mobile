package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ModifierDatabaseThroneTest {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    @Test
    fun getThrone_caseInsensitive() {
        ModifierDatabase.injectForTest("Throne", "Angry Goat", "Muscle Percent: +15")
        assertNotNull(ModifierDatabase.getThrone("angry goat"))
    }

    @Test
    fun getBjorn_aliasesThrone() {
        ModifierDatabase.injectForTest("Throne", "Seal Larva", "Muscle: +5")
        assertEquals(
            ModifierDatabase.getThrone("Seal Larva")?.modifiers,
            ModifierDatabase.getBjorn("Seal Larva")?.modifiers,
        )
    }

    @Test
    fun getThrone_unknownRace_returnsNull() {
        assertNull(ModifierDatabase.getThrone("Unknown Familiar"))
    }
}
