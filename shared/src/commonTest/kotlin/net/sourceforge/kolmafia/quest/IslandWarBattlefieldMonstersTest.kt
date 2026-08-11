package net.sourceforge.kolmafia.quest

import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase

class IslandWarBattlefieldMonstersTest {

    @BeforeTest
    fun loadDatabases() = runBlocking {
        GameDatabase().load()
    }

    @Test
    fun classify_warHippyMonster_returnsWarHippy() {
        assertEquals(
            BattlefieldMonsterKind.WAR_HIPPY,
            IslandWarBattlefieldMonsters.classify("War Hippy Baker"),
        )
    }

    @Test
    fun classify_warFratMonster_returnsWarFratboy() {
        assertEquals(
            BattlefieldMonsterKind.WAR_FRATBOY,
            IslandWarBattlefieldMonsters.classify("War Frat Wartender"),
        )
    }

    @Test
    fun classify_bossMonsters_matchZoneLists() {
        assertEquals(
            BattlefieldMonsterKind.WAR_HIPPY,
            IslandWarBattlefieldMonsters.classify("The Big Wisniewski"),
        )
        assertEquals(
            BattlefieldMonsterKind.WAR_FRATBOY,
            IslandWarBattlefieldMonsters.classify("The Man"),
        )
    }

    @Test
    fun classify_blankName_returnsUnknown() {
        assertEquals(BattlefieldMonsterKind.UNKNOWN, IslandWarBattlefieldMonsters.classify(""))
        assertEquals(BattlefieldMonsterKind.UNKNOWN, IslandWarBattlefieldMonsters.classify("   "))
    }

    @Test
    fun classify_unknownName_returnsUnknown() {
        assertEquals(
            BattlefieldMonsterKind.UNKNOWN,
            IslandWarBattlefieldMonsters.classify("totally fake monster"),
        )
    }

    @Test
    fun classify_knownNonWarMonster_returnsUnexpected() {
        assertEquals(
            BattlefieldMonsterKind.UNEXPECTED,
            IslandWarBattlefieldMonsters.classify("spooky gravy fairy guard"),
        )
    }

    @Test
    fun isBattlefieldMonster_validWarMonsters_returnsTrue() {
        assertEquals(true, IslandWarBattlefieldMonsters.isBattlefieldMonster("War Hippy Infantryman"))
        assertEquals(true, IslandWarBattlefieldMonsters.isBattlefieldMonster("War Frat 110th Infantryman"))
    }

    @Test
    fun messageFormatters_matchDesktop() {
        assertEquals(
            "Unknown monster found on battlefield: foo",
            IslandWarBattlefieldMonsters.unknownMonsterMessage("foo"),
        )
        assertEquals(
            "Unexpected monster found on battlefield: bar",
            IslandWarBattlefieldMonsters.unexpectedMonsterMessage("bar"),
        )
    }
}
