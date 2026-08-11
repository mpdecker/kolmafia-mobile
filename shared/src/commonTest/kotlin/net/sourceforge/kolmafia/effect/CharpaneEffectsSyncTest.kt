package net.sourceforge.kolmafia.effect

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectData as DbEffectData
import net.sourceforge.kolmafia.data.EffectQuality

class CharpaneEffectsSyncTest {

    @Test
    fun parse_compactEffectRows() {
        EffectDatabase.registerForTest(
            DbEffectData(
                id = 1001,
                name = "Muscle Memory",
                image = "mm.gif",
                descId = "abc123desc",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
            ),
        )
        val html = """
            <br>Lvl. 5
            <img alt="Muscle Memory" onClick='eff("abc123desc",1);'><td>(5)
            <img alt="Unknown Thing" onClick='eff("missing",1);'><td>(2)
        """.trimIndent()
        val effects = CharpaneEffectsSync.parse(html)
        assertEquals(1, effects.size)
        assertEquals(1001, effects[0].id)
        assertEquals("Muscle Memory", effects[0].name)
        assertEquals(5, effects[0].duration)
    }

    @Test
    fun parse_infiniteDuration() {
        EffectDatabase.registerForTest(
            DbEffectData(
                id = 1002,
                name = "Today Only",
                image = "today.gif",
                descId = "todaydesc",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
            ),
        )
        val html = """
            <br>Lvl. 5
            <img alt="Today Only" onClick='eff("todaydesc",1);'><td>(Today)
        """.trimIndent()
        val effects = CharpaneEffectsSync.parse(html)
        assertEquals(1, effects.size)
        assertEquals(Int.MAX_VALUE, effects[0].duration)
    }
}
