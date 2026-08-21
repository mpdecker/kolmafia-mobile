package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.SaberRequest] choices 1386 and 1387. */
object SaberChoiceSync {
    const val UPGRADE_CHOICE = 1386
    const val FORCE_CHOICE = 1387

    fun applyUpgrade(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != UPGRADE_CHOICE || preferences == null) return false
        val modifier = when (decision) {
            1 -> 1.takeIf { html.contains("Kaiburr crystal", ignoreCase = true) }
            2 -> 2.takeIf { html.contains("blue crystal", ignoreCase = true) }
            3 -> 3.takeIf { html.contains("resistance multiplier", ignoreCase = true) }
            4 -> 4.takeIf { html.contains("empathy chip", ignoreCase = true) }
            else -> null
        } ?: return false
        preferences.setInt("_saberMod", modifier)
        return true
    }

    fun applyForce(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        currentMonsterName: String = "",
        currentTurn: Int = 0,
        banishManager: BanishManager? = null,
        autoCreateBonerdagonNecklace: (() -> Unit)? = null,
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        if (choiceId != FORCE_CHOICE || preferences == null || decision !in 1..3) return false
        when (decision) {
            1 -> if (currentMonsterName.isNotBlank()) {
                banishManager?.banishMonster(currentMonsterName, Banisher.SABER_FORCE, currentTurn)
            }
            2 -> {
                preferences.setString("_saberForceMonster", currentMonsterName)
                preferences.setInt("_saberForceMonsterCount", 3)
            }
            3 -> {
                if (autoCreateBonerdagonNecklace != null) {
                    autoCreateBonerdagonNecklace()
                } else {
                    sessionLog("Saber Force item drops processed; Bonerdagon necklace autocraft deferred.")
                }
            }
        }
        preferences.setInt(
            "_saberForceUses",
            preferences.getInt("_saberForceUses", 0) + 1,
        )
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        currentMonsterName: String = "",
        currentTurn: Int = 0,
        banishManager: BanishManager? = null,
        autoCreateBonerdagonNecklace: (() -> Unit)? = null,
        sessionLog: (String) -> Unit = {},
    ): Boolean =
        applyUpgrade(choiceId, decision, html, preferences) ||
            applyForce(
                choiceId,
                decision,
                preferences,
                currentMonsterName,
                currentTurn,
                banishManager,
                autoCreateBonerdagonNecklace,
                sessionLog,
            )
}
