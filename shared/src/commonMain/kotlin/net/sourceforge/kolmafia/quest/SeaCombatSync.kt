package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [FightRequest] Sea / invader combat-win writers.
 */
object SeaCombatSync {

    const val SHARK_JUMPER = 3522
    const val SCALE_MAIL_UNDERWEAR = 6392
    const val MOM_PROGRESS_CAP = 40

    private val MOM_MONSTERS = setOf(
        "eye in the darkness",
        "peanut",
        "school of many",
        "slithering thing",
    )

    fun apply(
        monster: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        hasItemEquipped: (Int) -> Boolean = { false },
    ): Boolean {
        val lower = monster.trim().lowercase()
        return when {
            lower == "the invader" -> {
                val prefs = preferences ?: return false
                prefs.setBoolean("spaceInvaderDefeated", true)
                true
            }
            lower in MOM_MONSTERS -> {
                val prefs = preferences ?: return false
                var momCount = 1
                if (hasItemEquipped(SHARK_JUMPER)) momCount++
                if (hasItemEquipped(SCALE_MAIL_UNDERWEAR)) momCount++
                val next = (prefs.getInt("momSeaMonkeeProgress", 0) + momCount)
                    .coerceAtMost(MOM_PROGRESS_CAP)
                prefs.setInt("momSeaMonkeeProgress", next)
                true
            }
            lower == "nautical seaceress" -> {
                val db = questDatabase ?: return false
                db.setProgress(Quest.FINAL, QuestDatabase.FINISHED)
                true
            }
            else -> false
        }
    }
}
