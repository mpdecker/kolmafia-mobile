package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop FantasyRealm combat kill table ([QuestManager.addFantasyRealmKill]).
 */
object FantasyRealmCombatSync {

    const val DREAD_VILLAGE = 339

    private val fantasyRealmMonsters = setOf(
        "flock of every birds",
        "plywood cultists",
        "barrow wraith?",
        "regular thief",
        "swamp troll",
        "crypt creeper",
        "\"Phoenix\"",
        "Sewage Treatment Dragon",
        "Duke Vampire",
        "Spider Queen",
        "Archwizard",
        "Ley Incursion",
        "Ghoul King",
        "Ogre Chieftain",
        "Ted Schwartz, Master Thief",
        "Skeleton Lord",
    )

    fun applyCombatWin(
        monsterName: String?,
        adventureId: String?,
        preferences: Preferences?,
        won: Boolean = true,
    ): Boolean {
        if (!won || preferences == null) return false
        val name = monsterName?.trim().orEmpty()
        if (name.isEmpty()) return false
        val shouldCount = when {
            name in fantasyRealmMonsters -> true
            name.equals("spooky ghost", ignoreCase = true) ->
                adventureId?.toIntOrNull() != DREAD_VILLAGE
            else -> false
        }
        if (!shouldCount) return false
        addFantasyRealmKill(name, preferences)
        return true
    }

    fun addFantasyRealmKill(monster: String, preferences: Preferences) {
        var kills = preferences.getString("_frMonstersKilled", "")
        val pattern = when {
            monster.contains("barrow wraith") -> Regex("""barrow wraith\?:(\d+),""")
            monster.contains("Phoenix") -> Regex(""""Phoenix":(\d+),""")
            else -> Regex(Regex.escape(monster) + """:(\d+),""")
        }
        val match = pattern.find(kills)
        if (match != null) {
            val count = (match.groupValues[1].toIntOrNull() ?: 0) + 1
            kills = kills.replace(match.value, "$monster:$count,")
        } else {
            kills += "$monster:1,"
        }
        preferences.setString("_frMonstersKilled", kills)
    }
}
