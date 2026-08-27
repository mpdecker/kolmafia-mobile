package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Is There A Doctor In The House? choice 1340.
 * Defers quest-log HTTP refetch when item empty.
 */
object DoctorBagChoiceSync {

    const val CHOICE_ID = 1340

    private val DOCTOR_BAG_PATTERN =
        Regex("""We've received a report of a patient (.*?), in (.*?)\.""")

    private val MALADY_ITEMS = listOf(
        "tropical heatstroke" to "palm-frond fan",
        "archaic cough" to "antique bottle of cough syrup",
        "broken limb" to "cast",
        "low vim and vigor" to "Doc Galaktik's Vitality Serum",
        "bad clams" to "anti-anti-antidote",
        "criss-cross laceration" to "plaid bandage",
        "knocked out by a random encounter" to "phonics down",
        "Thin Blood Syndrome" to "red blood cells",
        "a blood shortage" to "bag of pygmy blood",
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val match = DOCTOR_BAG_PATTERN.find(html) ?: return false
        val malady = match.groupValues[1]
        val item = MALADY_ITEMS.firstOrNull { malady.contains(it.first) }?.second.orEmpty()
        preferences.setString("doctorBagQuestItem", item)
        preferences.setString("doctorBagQuestLocation", match.groupValues[2].trim())
        return true
    }

    fun applyAccept(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        questDatabase: QuestDatabase,
        itemCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1) return false
        val itemName = preferences.getString("doctorBagQuestItem", "")
        val itemId = if (itemName.isNotEmpty()) ItemDatabase.getByName(itemName)?.id ?: 0 else 0
        val step = if (itemId > 0 && itemCount(itemId) > 0) {
            "step1"
        } else {
            QuestDatabase.STARTED
        }
        questDatabase.setProgress(Quest.DOCTOR_BAG, step)
        return true
    }
}
