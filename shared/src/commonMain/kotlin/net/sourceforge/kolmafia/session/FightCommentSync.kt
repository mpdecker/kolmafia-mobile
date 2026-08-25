package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop fight HTML comment markers (Phases 1416–1430).
 */
object FightCommentSync {

    private val MONSTER_ID = Regex("""<!--\s*MONSTERID:\s*(\d+)\s*-->""")

    var lastWon: Boolean? = null
    var lastMonsterId: Int? = null

    fun reset() {
        lastWon = null
        lastMonsterId = null
    }

    fun apply(html: String, preferences: Preferences? = null): Boolean {
        var changed = false
        val previousMonsterId = lastMonsterId
        when {
            html.contains("<!--WINWINWIN-->") -> {
                lastWon = true
                changed = true
            }
            html.contains("<!--LOSELOSELOSE-->") -> {
                lastWon = false
                changed = true
            }
        }
        MONSTER_ID.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { id ->
            lastMonsterId = id
            val transformed = FightNodeSync.transformMonsterId(previousMonsterId, id, preferences)
            if (!transformed) {
                val name = MonsterDatabase.getById(id)?.name
                if (!name.isNullOrBlank()) {
                    preferences?.setString(Preferences.LAST_MONSTER, name)
                    if (MonsterStatusTracker.getLastMonster() == null) {
                        MonsterDatabase.getById(id)?.let { def ->
                            MonsterStatusTracker.setNextMonster(def, emptyList())
                        }
                    }
                }
            }
            changed = true
        }
        return changed
    }
}
