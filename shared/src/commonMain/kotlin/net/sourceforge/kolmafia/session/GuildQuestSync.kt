package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestLogSync
import net.sourceforge.kolmafia.shop.DesertBeachUnlockSync

/** Desktop [net.sourceforge.kolmafia.request.GuildRequest.handleGuildQuests] side effects. */
object GuildQuestSync {
    private val PLACE_PATTERN = Regex("""(?:^|[?&])place=([a-z]+)""", RegexOption.IGNORE_CASE)

    fun applyPlaceVisit(
        place: String?,
        html: String,
        character: KoLCharacter?,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
        inventoryManager: InventoryManager?,
        sessionLogger: SessionLogger?,
    ) {
        when (place?.lowercase()) {
            "paco" -> applyPacoVisit(
                html = html,
                character = character,
                preferences = preferences,
                questDatabase = questDatabase,
                inventoryManager = inventoryManager,
                sessionLogger = sessionLogger,
            )
            "ocg" -> {
                val db = questDatabase ?: return
                val context = buildQuestContext(place, preferences, inventoryManager)
                QuestLogSync.applyEgoKeyTurnIn(db, context, html)
                QuestLogSync.applyEgoBookTurnIn(db, context, html)
            }
            "challenge" -> {
                val db = questDatabase ?: return
                QuestLogSync.applyGuildChallengeTurnIn(
                    db,
                    buildQuestContext(place, preferences, inventoryManager),
                    html,
                )
            }
        }
    }

    private fun applyPacoVisit(
        html: String,
        character: KoLCharacter?,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
        inventoryManager: InventoryManager?,
        sessionLogger: SessionLogger?,
    ) {
        val prefs = preferences ?: return
        val ascension = character?.state?.value?.ascensionNumber ?: return

        if (html.contains("South of the Border")) {
            DesertBeachUnlockSync.setAvailable(ascension, prefs)
            sessionLogger?.appendRawLine("Desert Beach unlocked")
        }

        val db = questDatabase ?: return
        val hasEnvelope = inventoryManager?.state?.value?.items
            ?.containsKey(QuestLogSync.FACTORY_ENVELOPE_ID) == true
        if (!hasEnvelope) return

        QuestLogSync.applyFactoryTurnIn(
            db,
            buildQuestContext("paco", prefs, inventoryManager),
        )
        sessionLogger?.appendRawLine("Guild factory delivery complete")
    }

    private fun buildQuestContext(
        place: String?,
        preferences: Preferences?,
        inventoryManager: InventoryManager?,
    ): QuestLogSync.QuestSyncContext =
        QuestLogSync.QuestSyncContext(
            hasItemId = { id -> inventoryManager?.state?.value?.items?.containsKey(id) == true },
            place = place,
            preferences = preferences,
            consumeItem = { itemId, quantity ->
                inventoryManager?.consumeItemLocally(itemId, quantity)
            },
        )

    fun placeFromUrl(url: String): String? =
        PLACE_PATTERN.find(url)?.groupValues?.getOrNull(1)?.lowercase()
}
