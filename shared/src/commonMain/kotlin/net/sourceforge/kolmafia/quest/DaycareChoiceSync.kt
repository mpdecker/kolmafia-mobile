package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Boxing Daycare choice 1336 (recruit / scavenge / hire / spar).
 */
object DaycareChoiceSync {

    const val CHOICE_ID = 1336

    private val DAYCARE_PATTERN = Regex(
        """(?:Looks like|Probably around) (.*?) pieces in all\. (.*?) toddlers are training with (.*?) instructor""",
    )
    private val EARLY_DAYCARE_PATTERN = Regex(
        """mostly empty\. (.*?) toddlers are training with (.*?) instructor""",
    )
    private val DAYCARE_RECRUITS_PATTERN = Regex(
        """<font color=blue><b>\[(.*?) Meat]</b></font>""",
    )
    private val DAYCARE_INSTRUCTOR_ITEM_PATTERN = Regex(
        """<input  class=button type=submit value="Hire an instructor "></td><td valign=center><font color=blue><b>\[(\d*) (.*?)]</b>""",
    )
    private val DAYCARE_RECRUIT_PATTERN = Regex("""attract (.*?) new children""")
    private val DAYCARE_EQUIPMENT_PATTERN = Regex("""manage to find (.*?) used""")
    private val DAYCARE_ITEM_PATTERN = Regex(
        """<td valign=center>You lose an item: </td>.*?<b>(.*?)</b> \((.*?)\)</td>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val main = DAYCARE_PATTERN.find(html)
        if (main != null) {
            preferences.setString("daycareEquipment", main.groupValues[1].replace(",", ""))
            preferences.setString("daycareToddlers", main.groupValues[2].replace(",", ""))
            preferences.setString("daycareInstructors", normalizeInstructorCount(main.groupValues[3]))
        } else {
            val early = EARLY_DAYCARE_PATTERN.find(html)
            if (early != null) {
                preferences.setString("daycareToddlers", early.groupValues[1].replace(",", ""))
                preferences.setString(
                    "daycareInstructors",
                    normalizeInstructorCount(early.groupValues[2]),
                )
            }
        }
        DAYCARE_RECRUITS_PATTERN.find(html)?.groupValues?.getOrNull(1)?.let { meat ->
            preferences.setInt("_daycareRecruits", meat.replace(",", "").length - 3)
        }
        DAYCARE_INSTRUCTOR_ITEM_PATTERN.find(html)?.let { m ->
            val qty = m.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            preferences.setInt("daycareInstructorItemQuantity", qty)
            preferences.setInt(
                "daycareInstructorItem",
                ItemDatabase.getByName(m.groupValues[2])?.id ?: 0,
            )
        }
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        hasBoxingDayBreakfast: Boolean = false,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        return when (decision) {
            1 -> {
                if (!DAYCARE_RECRUIT_PATTERN.containsMatchIn(html)) return false
                preferences.setInt(
                    "_daycareRecruits",
                    preferences.getInt("_daycareRecruits", 0) + 1,
                )
                true
            }
            2 -> {
                val countString = DAYCARE_EQUIPMENT_PATTERN.find(html)
                    ?.groupValues?.getOrNull(1) ?: return false
                val equipment = countString.replace(",", "").toIntOrNull() ?: return false
                preferences.setInt(
                    "daycareLastScavenge",
                    equipment / (if (hasBoxingDayBreakfast) 2 else 1),
                )
                preferences.setInt(
                    "_daycareGymScavenges",
                    preferences.getInt("_daycareGymScavenges", 0) + 1,
                )
                true
            }
            3 -> {
                var changed = html.contains("new teacher joins the staff")
                DAYCARE_ITEM_PATTERN.find(html)?.let { m ->
                    val itemName = m.groupValues[1]
                    val itemCount = m.groupValues[2].replace(",", "").toIntOrNull() ?: 0
                    val itemId = ItemDatabase.getByName(itemName)?.id
                    if (itemId != null && itemCount > 0) {
                        consumeItem(itemId, itemCount)
                        changed = true
                    }
                }
                changed
            }
            4 -> {
                if (!html.contains("step into the ring")) return false
                preferences.setBoolean("_daycareFights", true)
                true
            }
            else -> false
        }
    }

    private fun normalizeInstructorCount(raw: String): String =
        if (raw.trim().equals("an", ignoreCase = true)) "1" else raw.trim()
}
