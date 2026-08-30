package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/** ChibiBuddy choice and daily-state synchronization. */
object ChibiBuddyManager {
    const val FIRST_CHOICE = 627
    const val NAME_CHOICE = 633
    val CHOICE_IDS = 627..633

    private val namePattern = Regex("""&quot;I am (.*?), and I am sure we will be the best of friends!&quot;""")
    private val statPattern = Regex("""<td height=25>(.*?): </td><td><img.*?title=["'](\d+) dots""")
    private val agePattern = Regex("""</s><center>.*? is (\d+) days old\.<p>""")

    fun haveChibiBuddy(inventory: InventoryManager): Boolean =
        haveChibiBuddyOn(inventory) || haveChibiBuddyOff(inventory)
    fun haveChibiBuddyOn(inventory: InventoryManager): Boolean =
        (inventory.state.value.items[ItemPool.CHIBIBUDDY_ON]?.quantity ?: 0) > 0
    fun haveChibiBuddyOff(inventory: InventoryManager): Boolean =
        (inventory.state.value.items[ItemPool.CHIBIBUDDY_OFF]?.quantity ?: 0) > 0

    fun visit(choice: Int, text: String?, preferences: Preferences?, character: KoLCharacter?, inventory: InventoryManager?): Boolean {
        if (choice !in CHOICE_IDS || text.isNullOrBlank() || preferences == null) return false
        if (text.contains("<b>Oh no!</b>") || text.contains("but the batteries are dead")) {
            if (preferences.getInt("chibiBirthday", -1) >= 0) {
                val age = getAge(preferences, character)
                val name = preferences.getString("chibiName", "")
                inventory?.consumeItemLocally(ItemPool.CHIBIBUDDY_ON, 1)
                inventory?.gainItemLocally(ItemPool.CHIBIBUDDY_OFF, 1)
                preferences.setString("chibiLastDeath", "$name:$age")
            }
            reset(preferences)
            return true
        }
        val day = character?.state?.value?.dayCount ?: preferences.getInt("daycount", 0)
        preferences.setInt("chibiLastVisit", day)
        agePattern.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("chibiBirthday", day - it)
        }
        if (text.contains("value=\"Put your ChibiBuddy&trade; away\"")) {
            preferences.setBoolean("_chibiChanged", !text.contains("value=\"Have a ChibiChat&trade;\">"))
        }
        var changed = false
        statPattern.findAll(text).forEach {
            preferences.setInt("chibi${it.groupValues[1]}", it.groupValues[2].toInt())
            changed = true
        }
        return changed
    }

    fun postChoice(
        choice: Int,
        decision: Int,
        text: String?,
        preferences: Preferences?,
        inventory: InventoryManager?,
        character: KoLCharacter? = null,
    ): Boolean {
        if (preferences == null || text == null) return false
        return when (choice) {
            FIRST_CHOICE -> if (decision == 5) {
                preferences.setBoolean("_chibiChanged", true)
                true
            } else false
            in 628..631 -> if (text.contains("Results:") && decision in 1..2) {
                preferences.setInt("_chibiAdventures", (preferences.getInt("_chibiAdventures", 0) + 1).coerceAtMost(5))
                true
            } else false
            NAME_CHOICE -> if (decision == 1) {
                inventory?.consumeItemLocally(ItemPool.CHIBIBUDDY_OFF, 1)
                inventory?.gainItemLocally(ItemPool.CHIBIBUDDY_ON, 1)
                namePattern.find(text)?.groupValues?.getOrNull(1)?.let { preferences.setString("chibiName", it) }
                val day = character?.state?.value?.dayCount ?: preferences.getInt("daycount", 0)
                preferences.setInt("chibiBirthday", day)
                preferences.setInt("chibiLastVisit", day)
                true
            } else false
            else -> false
        }
    }

    fun getAge(preferences: Preferences, character: KoLCharacter?): Int {
        val birthday = preferences.getInt("chibiBirthday", -1)
        if (birthday < 0) return -1
        return (character?.state?.value?.dayCount ?: preferences.getInt("daycount", 0)) - birthday + 1
    }

    fun reset(preferences: Preferences) {
        listOf("_chibiAdventures", "chibiAlignment", "chibiBirthday", "chibiFitness",
            "chibiIntelligence", "chibiLastVisit", "chibiName", "chibiSocialization")
            .forEach(preferences::removeKey)
    }
}
