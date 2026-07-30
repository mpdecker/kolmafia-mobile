package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class FamiliarDefinitionProxyTest {

    @Test
    fun mosquitoCombatTypeFlags() = runBlocking {
        FamiliarDefinitionDatabase.load()
        val mosquito = FamiliarDefinitionDatabase.getById(1)
        assertTrue(mosquito != null)
        assertTrue(mosquito.isCombatType())
        assertTrue(mosquito.isCombat0Type())
        assertTrue(mosquito.isHp0Type())
        assertEquals("sentient; organic; insect; animal; haseyes; bite; haswings; flies; fast", mosquito.combinedAttributes())
    }
}
