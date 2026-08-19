package net.sourceforge.kolmafia.quest

import kotlin.math.min
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] leftover combat-win writers.
 */
object QuestCombatWinExtrasSync {

    private val RUFUS_SHADOW_BOSSES = setOf(
        "shadow spire",
        "shadow orrery",
        "shadow tongue",
        "shadow scythe",
        "shadow cauldron",
        "shadow matrix",
    )

    fun apply(
        monster: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        ascensionNumber: Int = 0,
    ): Boolean {
        if (monster.isBlank()) return false
        val lower = monster.trim().lowercase()
        return when (lower) {
            "screambat" -> {
                val db = questDatabase ?: return false
                if (db.isQuestLaterThan(Quest.BAT, "step2")) return false
                db.advanceQuest(Quest.BAT)
                true
            }
            "boss bat" -> {
                val db = questDatabase ?: return false
                db.setQuestIfBetter(Quest.BAT, "step4")
                true
            }
            "knob goblin king" -> {
                val db = questDatabase ?: return false
                db.setQuestIfBetter(Quest.GOBLIN, QuestDatabase.FINISHED)
                true
            }
            "groar" -> {
                val db = questDatabase ?: return false
                db.setQuestIfBetter(Quest.TRAPPER, "step5")
                true
            }
            "panicking knott yeti" -> {
                val db = questDatabase ?: return false
                db.setQuestIfBetter(Quest.TRAPPER, "step4")
                true
            }
            "hulking bridge troll" -> {
                val prefs = preferences ?: return false
                prefs.setInt("chasmBridgeProgress", 0)
                true
            }
            "source agent" -> {
                val prefs = preferences ?: return false
                prefs.setInt("sourceAgentsDefeated", prefs.getInt("sourceAgentsDefeated", 0) + 1)
                true
            }
            "wart dinsey" -> {
                val prefs = preferences ?: return false
                prefs.setInt("lastWartDinseyDefeated", ascensionNumber)
                true
            }
            "baron von ratsworth" -> {
                val prefs = preferences ?: return false
                val square = prefs.getInt("lastTavernSquare", 0)
                TavernCellarSync.addTavernLocation(prefs, square, '6', ascensionNumber)
                true
            }
            "protector spectre" -> {
                val db = questDatabase ?: return false
                db.setProgress(Quest.WORSHIP, QuestDatabase.FINISHED)
                true
            }
            "wu tang the betrayer" -> {
                val prefs = preferences ?: return false
                prefs.setInt("lastWuTangDefeated", ascensionNumber)
                true
            }
            "the superconductor" -> {
                val prefs = preferences ?: return false
                prefs.setBoolean("superconductorDefeated", true)
                true
            }
            "x-32-f combat training snowman" -> {
                val prefs = preferences ?: return false
                val parts = prefs.getInt("_snojoParts", 0)
                prefs.setInt("_snojoFreeFights", min(parts, 10))
                if (parts <= 10) {
                    when (prefs.getString("snojoSetting", "NONE")) {
                        "MUSCLE" -> prefs.setInt("snojoMuscleWins", prefs.getInt("snojoMuscleWins", 0) + 1)
                        "MYSTICALITY" -> prefs.setInt(
                            "snojoMysticalityWins",
                            prefs.getInt("snojoMysticalityWins", 0) + 1,
                        )
                        "MOXIE" -> prefs.setInt("snojoMoxieWins", prefs.getInt("snojoMoxieWins", 0) + 1)
                    }
                }
                true
            }
            else -> if (lower in RUFUS_SHADOW_BOSSES) {
                val db = questDatabase ?: return false
                db.setQuestIfBetter(Quest.RUFUS, "step1")
                true
            } else {
                false
            }
        }
    }
}
