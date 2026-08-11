package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.math.min

/** Desktop [IslandManager.handleBattlefield] / [IslandManager.handleBattlefieldMonster] Island War combat hooks. */
object IslandWarCombatSync {

    private const val BATTLEFIELD_FRAT_UNIFORM = "131"
    private const val BATTLEFIELD_HIPPY_OUTFIT = "132"

    private const val BOSS_BIG_WISNIEWSKI = "the big wisniewski"
    private const val BOSS_THE_MAN = "the man"
    private const val DIRTY_THIEVING_BRIGAND = "dirty thieving brigand"

    fun applyNunsSidequestWin(
        preferences: Preferences?,
        monster: String,
        responseText: String,
        won: Boolean,
    ): Boolean {
        val prefs = preferences ?: return false
        if (!won || monster.isBlank()) return false
        if (monster.trim().lowercase() != DIRTY_THIEVING_BRIGAND) return false
        return when {
            responseText.contains("could serve as a hospital") -> {
                prefs.setString("sidequestNunsCompleted", "hippy")
                true
            }
            responseText.contains("could serve as a massage parlor") -> {
                prefs.setString("sidequestNunsCompleted", "fratboy")
                true
            }
            else -> false
        }
    }

    fun applyEndOfWar(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        adventureId: String,
        monster: String,
        responseText: String,
        won: Boolean,
        isKingdomOfExploathing: Boolean = false,
    ): Boolean {
        val prefs = preferences ?: return false
        val db = questDatabase ?: return false
        if (!won || !responseText.contains("WINWINWIN")) return false
        if (prefs.getString("warProgress", "unstarted") == "finished") return false

        if (responseText.contains("Giant explosions in slow motion")) {
            return finishWar(prefs, db, loser = "both", isKingdomOfExploathing = isKingdomOfExploathing)
        }

        if (adventureId != BATTLEFIELD_FRAT_UNIFORM && adventureId != BATTLEFIELD_HIPPY_OUTFIT) {
            return false
        }

        return when (monster.trim().lowercase()) {
            BOSS_BIG_WISNIEWSKI ->
                finishWar(prefs, db, loser = "hippies", isKingdomOfExploathing = isKingdomOfExploathing)
            BOSS_THE_MAN ->
                finishWar(prefs, db, loser = "fratboys", isKingdomOfExploathing = isKingdomOfExploathing)
            else -> false
        }
    }

    fun applyCombatWin(
        preferences: Preferences?,
        adventureId: String,
        responseText: String,
        won: Boolean,
        isKingdomOfExploathing: Boolean = false,
    ): Boolean {
        val prefs = preferences ?: return false
        if (!won || !responseText.contains("WINWINWIN")) return false
        if (prefs.getString("warProgress", "unstarted") == "finished") return false

        val routing = when (adventureId) {
            BATTLEFIELD_FRAT_UNIFORM -> "hippiesDefeated" to IslandWarBattlefieldMessages.HIPPY_MESSAGES
            BATTLEFIELD_HIPPY_OUTFIT -> "fratboysDefeated" to IslandWarBattlefieldMessages.FRAT_MESSAGES
            else -> return false
        }
        val (prefKey, messages) = routing

        var delta = IslandWarBattlefieldMessages.battlefieldDelta(responseText, messages)
        if (responseText.contains("rocket launcher blasts 3 extra")) {
            delta += 3
        }
        if (responseText.contains("fall to Felicity's rifle")) {
            delta += 3
        }

        val max = if (isKingdomOfExploathing) 333 else 1000
        return incrementBattlefieldCounter(prefs, prefKey, delta, max)
    }

    internal fun finishWar(
        preferences: Preferences,
        questDatabase: QuestDatabase,
        loser: String,
        isKingdomOfExploathing: Boolean,
    ): Boolean {
        val total = if (isKingdomOfExploathing) 333 else 1000
        when (loser) {
            "fratboys" -> preferences.setInt("fratboysDefeated", total)
            "hippies" -> preferences.setInt("hippiesDefeated", total)
            "both" -> {
                preferences.setInt("fratboysDefeated", 1000)
                preferences.setInt("hippiesDefeated", 1000)
            }
            else -> return false
        }
        preferences.setString("sideDefeated", loser)
        preferences.setString("warProgress", "finished")
        val warQuest = if (isKingdomOfExploathing) Quest.HIPPY_FRAT else Quest.ISLAND_WAR
        questDatabase.setProgress(warQuest, QuestDatabase.FINISHED)
        return true
    }

    internal fun incrementBattlefieldCounter(
        preferences: Preferences,
        prefKey: String,
        delta: Int,
        max: Int,
    ): Boolean {
        val current = preferences.getInt(prefKey, 0)
        val next = min(max, current + delta)
        if (next == current) return false
        preferences.setInt(prefKey, next)
        return true
    }
}
