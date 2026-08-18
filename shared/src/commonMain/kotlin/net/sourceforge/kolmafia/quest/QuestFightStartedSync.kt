package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.TurnCounter

/**
 * Desktop [QuestManager.updateQuestFightStarted] extras beyond volcanic NEMESIS / Cake Lord.
 */
object QuestFightStartedSync {

    const val GINGERSERVO = 9223

    private val SNOWMAN_PATTERN = Regex("otherimages/combatsnowman/")

    private val VOTE_MONSTERS = setOf(
        "angry ghost",
        "annoyed snake",
        "government bureaucrat",
        "terrible mutant",
        "slime blob",
    )

    fun isCombatActionUrl(url: String?): Boolean {
        val value = url.orEmpty()
        return value.contains("action=", ignoreCase = true) ||
            value.contains("whichitem=", ignoreCase = true)
    }

    fun apply(
        monster: String,
        html: String,
        preferences: Preferences?,
        turnsPlayed: Int,
        equipment: Map<EquipmentSlot, String> = emptyMap(),
        itemName: (Int) -> String = { ItemDatabase.getItemName(it) },
        clearSlot: (EquipmentSlot) -> Unit = {},
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        allowUnequippedConsume: Boolean = true,
    ): Boolean {
        if (monster.isBlank()) return false
        val trimmed = monster.trim()
        val lower = trimmed.lowercase()
        return when {
            lower == "gng-3-r" -> {
                val discarded = EquipmentDiscard.discardIfEquipped(
                    itemId = GINGERSERVO,
                    equipment = equipment,
                    itemName = itemName,
                    clearSlot = clearSlot,
                    consumeItem = consumeItem,
                )
                if (!discarded && allowUnequippedConsume) consumeItem(GINGERSERVO, 1)
                true
            }
            lower == "x-32-f combat training snowman" -> {
                val prefs = preferences ?: return false
                prefs.setInt("_snojoParts", SNOWMAN_PATTERN.findAll(html).count() - 2)
                true
            }
            lower in VOTE_MONSTERS -> {
                val prefs = preferences ?: return false
                val alreadyCounted =
                    prefs.getInt("lastVoteMonsterTurn", -1) == turnsPlayed &&
                        prefs.getString("_voteMonster", "").equals(trimmed, ignoreCase = true)
                if (!alreadyCounted) {
                    prefs.setInt("_voteFreeFights", (prefs.getInt("_voteFreeFights", 0) + 1).coerceAtMost(3))
                }
                prefs.setInt("lastVoteMonsterTurn", turnsPlayed)
                prefs.setString("_voteMonster", trimmed)
                TurnCounter.stopCounting(prefs, "Vote Monster")
                true
            }
            else -> false
        }
    }
}
