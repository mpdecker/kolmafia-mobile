package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] A Pound of Cure choice 1341 —
 * consume quest cure item + lights/upgrades (via [QuestSpecialSync.completeDoctorBagDelivery]).
 */
object DoctorBagCureChoiceSync {

    const val CHOICE_ID = 1341

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || decision != 1 || preferences == null) return false
        val itemName = preferences.getString("doctorBagQuestItem", "")
        if (itemName.isNotBlank()) {
            ItemDatabase.getByName(itemName)?.id?.takeIf { it > 0 }?.let { consumeItem(it, 1) }
        }
        return QuestSpecialSync.completeDoctorBagDelivery(html, questDatabase, preferences)
    }
}
