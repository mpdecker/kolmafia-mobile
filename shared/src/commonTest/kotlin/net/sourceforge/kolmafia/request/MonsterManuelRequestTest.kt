package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.MonsterManuelManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonsterManuelRequestTest {
    @AfterTest
    fun clearCache() = MonsterManuelManager.flushCache()

    @Test
    fun parsesAndCachesManuelEntryAndFactoids() {
        val html = """
            <a name='mon42'><table>
            <td rowspan=4 valign=top class=small><b><font size=+2>test beast</font></b><!-- article:a -->
            <ul><li>First fact.<li>Second fact.</ul>
            </table>
        """.trimIndent()

        assertEquals(1, MonsterManuelRequest.parseResponse("questlog.php?which=6&vl=t", html))
        assertTrue(MonsterManuelManager.getCachedManuelText(42).contains("test beast"))
        assertEquals(listOf("First fact.", "Second fact."), MonsterManuelManager.getFactoids(42))
        assertEquals(2, MonsterManuelManager.getFactoidsAvailable(42))
    }

    @Test
    fun fightResponseCanSeedFactoidCache() {
        val html = "<a name=\"mon7\"><table><ul><li>A fact.</ul></table>"
        assertEquals(1, MonsterManuelRequest.parseResponse("fight.php", html))
        assertEquals(1, MonsterManuelManager.getFactoidsAvailable(7))
    }
}
