package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MonsterDatabaseSprinklesTest {

    @Test
    fun parse_sprinkleMinMax_numericAndExpression() = runBlocking {
        MonsterDatabase.load()
        val pigeon = MonsterDatabase.getByName("gingerbread pigeon")!!
        assertEquals(1, pigeon.minSprinkles)
        assertNull(pigeon.minSprinklesExpression)
        assertEquals(3, pigeon.maxSprinkles)
        assertNull(pigeon.maxSprinklesExpression)

        val alligator = MonsterDatabase.getByName("gingerbread alligator")!!
        assertEquals(0, alligator.minSprinkles)
        assertEquals("28+70*pref(_gingerBiggerAlligators)", alligator.minSprinklesExpression)
        assertEquals(0, alligator.maxSprinkles)
        assertEquals("30+70*pref(_gingerBiggerAlligators)", alligator.maxSprinklesExpression)

        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        assertEquals(0, mosquito.minSprinkles)
        assertNull(mosquito.minSprinklesExpression)
        assertEquals(0, mosquito.maxSprinkles)
        assertNull(mosquito.maxSprinklesExpression)
    }
}
