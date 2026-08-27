package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Airport Control Panel choice 986.
 */
object ControlPanelChoiceSync {

    const val CHOICE_ID = 986

    private val OMEGA_PATTERN = Regex("""<br>Current power level: (\d+)%</td>""")

    private val PANEL_FLAGS = listOf(
        "controlPanel1" to "All-Ranchero FM station: VOLUNTARY",
        "controlPanel2" to "&pi; sleep-hypnosis generators: OFF",
        "controlPanel3" to "Simian Ludovico Wednesdays: CANCELLED",
        "controlPanel4" to "Monkey food safety protocols: OBEYED",
        "controlPanel5" to "Shampoo Dispensers: CHILD-SAFE",
        "controlPanel6" to "Assemble-a-Bear kiosks: CLOSED",
        "controlPanel7" to "Training algorithm: ROUND ROBIN",
        "controlPanel8" to "Re-enactment supply closet: LOCKED",
        "controlPanel9" to "Thermostat setting: 76 DEGREES",
    )

    private val OMEGA_QUESTS = listOf(
        Quest.EVE,
        Quest.FAKE_MEDIUM,
        Quest.SERUM,
        Quest.SMOKES,
        Quest.OUT_OF_ORDER,
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase? = null,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        for ((pref, safeText) in PANEL_FLAGS) {
            preferences.setBoolean(pref, !html.contains(safeText))
        }
        OMEGA_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("controlPanelOmega", it)
        }
        if (html.contains("Omega device activated")) {
            preferences.setInt("controlPanelOmega", 0)
            questDatabase?.let { db ->
                for (quest in OMEGA_QUESTS) {
                    db.setProgress(quest, QuestDatabase.UNSTARTED)
                }
            }
        }
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision !in 1..9) return false
        preferences.setBoolean("_controlPanelUsed", true)
        if (!html.contains("minimum of 24 hours")) {
            val next = (preferences.getInt("controlPanelOmega", 0) + 11).coerceAtMost(100)
            preferences.setInt("controlPanelOmega", next)
        }
        return true
    }
}
