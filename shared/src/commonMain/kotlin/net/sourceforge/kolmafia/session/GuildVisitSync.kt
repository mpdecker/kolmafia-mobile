package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.GuildCreationSync
import net.sourceforge.kolmafia.skill.SkillManager

/** Desktop [net.sourceforge.kolmafia.request.GuildRequest.parseResponse] visit hooks. */
object GuildVisitSync {
    private val ACTION_PATTERN = Regex("""action=([^&]+)""")

    fun syncStoreOpen(
        html: String,
        character: KoLCharacter?,
        prefs: Preferences?,
    ) {
        if (prefs == null || character == null) return
        if (!html.contains("shop.php", ignoreCase = true)) return

        val state = character.state.value
        val characterClass = CharacterClass.fromId(state.characterClass)
        if (!characterClass.isStandardClass) return

        val ascension = state.ascensionNumber
        if (ascension < 0) return

        prefs.setInt("lastGuildStoreOpen", ascension)
    }

    fun parseFromVisit(
        url: String,
        html: String,
        eventBus: GameEventBus? = null,
        sessionLogger: SessionLogger? = null,
        character: KoLCharacter? = null,
        preferences: Preferences? = null,
        skillManager: SkillManager? = null,
        inventoryManager: InventoryManager? = null,
        questDatabase: QuestDatabase? = null,
    ) {
        if (!url.contains("guild.php", ignoreCase = true)) return
        GuildQuestSync.applyPlaceVisit(
            place = GuildQuestSync.placeFromUrl(url),
            html = html,
            character = character,
            preferences = preferences,
            questDatabase = questDatabase,
            inventoryManager = inventoryManager,
            sessionLogger = sessionLogger,
        )
        when (actionFromUrl(url)) {
            "malussmash" -> GuildCreationSync.parseMalus(url, html, eventBus)
            "makestaff" -> GuildCreationSync.parseStaff(url, html, eventBus, sessionLogger)
            "buyskill" -> GuildSkillSync.parseBuyskill(
                url,
                html,
                character,
                preferences,
                skillManager,
                inventoryManager,
            )
        }
    }

    private fun actionFromUrl(url: String): String? =
        ACTION_PATTERN.find(url)?.groupValues?.getOrNull(1)?.lowercase()
}
