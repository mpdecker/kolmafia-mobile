package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase

/** Desktop [net.sourceforge.kolmafia.request.FightRequest.specialMonsterCategory] WAR_HIPPY / WAR_FRATBOY. */
enum class BattlefieldMonsterKind {
    WAR_HIPPY,
    WAR_FRATBOY,
    UNKNOWN,
    UNEXPECTED,
}

object IslandWarBattlefieldMonsters {

    private const val FRAT_UNIFORM_ZONE = "The Battlefield (Frat Uniform)"
    private const val HIPPY_UNIFORM_ZONE = "The Battlefield (Hippy Uniform)"

    private val warHippyNames: Set<String> by lazy {
        zoneMonsterNames(FRAT_UNIFORM_ZONE)
    }

    private val warFratboyNames: Set<String> by lazy {
        zoneMonsterNames(HIPPY_UNIFORM_ZONE)
    }

    private fun zoneMonsterNames(zoneName: String): Set<String> {
        val zone = CombatDatabase.getByLocation(zoneName) ?: return emptySet()
        return zone.monsters.map { it.name.lowercase() }.toSet()
    }

    fun classify(monsterName: String): BattlefieldMonsterKind {
        val name = monsterName.trim()
        if (name.isEmpty()) return BattlefieldMonsterKind.UNKNOWN
        val key = name.lowercase()
        if (key in warHippyNames) return BattlefieldMonsterKind.WAR_HIPPY
        if (key in warFratboyNames) return BattlefieldMonsterKind.WAR_FRATBOY
        if (MonsterDatabase.getByName(name) == null) return BattlefieldMonsterKind.UNKNOWN
        return BattlefieldMonsterKind.UNEXPECTED
    }

    fun isBattlefieldMonster(monsterName: String): Boolean =
        when (classify(monsterName)) {
            BattlefieldMonsterKind.WAR_HIPPY,
            BattlefieldMonsterKind.WAR_FRATBOY,
            -> true
            BattlefieldMonsterKind.UNKNOWN,
            BattlefieldMonsterKind.UNEXPECTED,
            -> false
        }

    fun unknownMonsterMessage(name: String): String =
        "Unknown monster found on battlefield: $name"

    fun unexpectedMonsterMessage(name: String): String =
        "Unexpected monster found on battlefield: $name"
}
