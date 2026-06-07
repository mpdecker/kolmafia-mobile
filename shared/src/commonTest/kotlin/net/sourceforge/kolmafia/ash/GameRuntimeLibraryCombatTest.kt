package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryCombatTest {

    @Test
    fun inMultiFight_alwaysFalse() {
        assertEquals("false",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(in_multi_fight()));"))
    }

    @Test
    fun fightFollowsChoice_alwaysFalse() {
        assertEquals("false",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(fight_follows_choice()));"))
    }

    @Test
    fun lastMonster_readsFromPref() {
        val p = prefs()
        p.setString(Preferences.LAST_MONSTER, "bunny")
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("bunny", outputLib(lib, "print(last_monster());"))
    }

    @Test
    fun lastMonster_emptyWhenNoPrefSet() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("", outputLib(lib, "print(last_monster());"))
    }

    @Test
    fun copiersUsed_returnsZero() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(copiers_used(to_skill(\"Accordion Thief\"))));"))
    }
}
