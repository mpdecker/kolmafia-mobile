package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleArrrboretumChange] + [QuestManager.handlePoopDeckChange].
 */
object PirateNcSync {

    const val ARRRBORETUM = 174
    const val POOP_DECK = 159

    fun applyFromAdventure(
        adventureId: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        url: String? = null,
    ): Boolean {
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        return when (area) {
            ARRRBORETUM -> {
                if (preferences == null) return false
                if (!html.contains("Plant a Tree, Plant a Tree!") &&
                    !html.contains("Stumped") &&
                    !html.contains("Timbarrrr!")
                ) {
                    preferences.setInt(
                        "_saplingsPlanted",
                        preferences.getInt("_saplingsPlanted", 0) + 1,
                    )
                    true
                } else false
            }
            POOP_DECK -> {
                if (questDatabase == null) return false
                if (html.contains("unlocks a padlock on a trap door")) {
                    questDatabase.setProgress(Quest.PIRATE, QuestDatabase.FINISHED)
                    true
                } else false
            }
            else -> false
        }
    }
}
