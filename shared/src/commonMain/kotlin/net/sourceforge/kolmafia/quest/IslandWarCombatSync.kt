package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.math.min

/** Desktop [IslandManager.handleBattlefield] / [IslandManager.handleBattlefieldMonster] Island War combat hooks. */
object IslandWarCombatSync {

    private const val BATTLEFIELD_FRAT_UNIFORM = "132"
    private const val BATTLEFIELD_HIPPY_UNIFORM = "140"

    private val BATTLEFIELD_IDS = setOf(BATTLEFIELD_FRAT_UNIFORM, BATTLEFIELD_HIPPY_UNIFORM)

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
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        val prefs = preferences ?: return false
        val db = questDatabase ?: return false
        if (!won || !responseText.contains("WINWINWIN")) return false
        if (prefs.getString("warProgress", "unstarted") == "finished") return false

        if (responseText.contains("Giant explosions in slow motion")) {
            return finishWar(
                prefs,
                db,
                loser = "both",
                isKingdomOfExploathing = isKingdomOfExploathing,
                sessionLogger = sessionLogger,
            )
        }

        if (adventureId !in BATTLEFIELD_IDS) {
            return false
        }

        return when (monster.trim().lowercase()) {
            BOSS_BIG_WISNIEWSKI ->
                finishWar(
                    prefs,
                    db,
                    loser = "hippies",
                    isKingdomOfExploathing = isKingdomOfExploathing,
                    sessionLogger = sessionLogger,
                )
            BOSS_THE_MAN ->
                finishWar(
                    prefs,
                    db,
                    loser = "fratboys",
                    isKingdomOfExploathing = isKingdomOfExploathing,
                    sessionLogger = sessionLogger,
                )
            else -> false
        }
    }

    fun applyCombatWin(
        preferences: Preferences?,
        adventureId: String,
        responseText: String,
        won: Boolean,
        monster: String = "",
        isKingdomOfExploathing: Boolean = false,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        val prefs = preferences ?: return false
        if (!won || !responseText.contains("WINWINWIN")) return false
        if (prefs.getString("warProgress", "unstarted") == "finished") return false

        val onBattlefield = adventureId in BATTLEFIELD_IDS
        val kind = IslandWarBattlefieldMonsters.classify(monster)
        val routing = when (kind) {
            BattlefieldMonsterKind.WAR_HIPPY ->
                "hippiesDefeated" to IslandWarBattlefieldMessages.HIPPY_MESSAGES
            BattlefieldMonsterKind.WAR_FRATBOY ->
                "fratboysDefeated" to IslandWarBattlefieldMessages.FRAT_MESSAGES
            BattlefieldMonsterKind.UNKNOWN -> {
                if (onBattlefield) {
                    sessionLogger?.appendRawLine(
                        IslandWarBattlefieldMonsters.unknownMonsterMessage(monster.ifBlank { "?" }),
                    )
                }
                return false
            }
            BattlefieldMonsterKind.UNEXPECTED -> {
                if (onBattlefield) {
                    sessionLogger?.appendRawLine(
                        IslandWarBattlefieldMonsters.unexpectedMonsterMessage(monster),
                    )
                }
                return false
            }
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
        val last = prefs.getInt(prefKey, 0)
        if (!incrementBattlefieldCounter(prefs, prefKey, delta, max)) {
            return false
        }
        val current = prefs.getInt(prefKey, 0)
        logBattlefieldVictory(
            sessionLogger = sessionLogger,
            defeatingFratSide = prefKey == "hippiesDefeated",
            last = last,
            current = current,
            isKingdomOfExploathing = isKingdomOfExploathing,
        )
        return true
    }

    internal fun finishWar(
        preferences: Preferences,
        questDatabase: QuestDatabase,
        loser: String,
        isKingdomOfExploathing: Boolean,
        sessionLogger: SessionLogger? = null,
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
        sessionLogger?.appendRawLine(IslandWarBattlefieldMessages.finishWarMessage(loser))
        return true
    }

    internal fun logBattlefieldVictory(
        sessionLogger: SessionLogger?,
        defeatingFratSide: Boolean,
        last: Int,
        current: Int,
        isKingdomOfExploathing: Boolean,
    ) {
        if (sessionLogger == null) return
        sessionLogger.appendRawLine(
            IslandWarBattlefieldMessages.victoryMessage(
                defeatingFratSide = defeatingFratSide,
                last = last,
                current = current,
                isKingdomOfExploathing = isKingdomOfExploathing,
            ),
        )
        IslandWarBattlefieldMessages.areaMessage(
            defeatingFratSide = defeatingFratSide,
            last = last,
            current = current,
            isKingdomOfExploathing = isKingdomOfExploathing,
        )?.let { sessionLogger.appendRawLine(it) }
        IslandWarBattlefieldMessages.heroMessage(
            defeatingFratSide = defeatingFratSide,
            last = last,
            current = current,
            isKingdomOfExploathing = isKingdomOfExploathing,
        )?.let { sessionLogger.appendRawLine(it) }
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
