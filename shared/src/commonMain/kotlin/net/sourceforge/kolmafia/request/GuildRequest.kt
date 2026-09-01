package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.GuildQuestSync
import net.sourceforge.kolmafia.session.GuildVisitSync
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.skill.SkillManager

/** Headless equivalent of desktop [GuildRequest] for guild visits and quest side effects. */
open class GuildRequest(
    private val client: HttpClient,
    private val character: KoLCharacter,
    private val preferences: Preferences,
    private val questDatabase: QuestDatabase,
    private val inventoryManager: InventoryManager? = null,
    private val skillManager: SkillManager? = null,
    private val eventBus: GameEventBus? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    suspend fun visit(place: String): Result<String> {
        val normalized = place.trim().lowercase()
        if (normalized !in VALID_PLACES) {
            return Result.failure(IllegalArgumentException("Unknown guild location: $place"))
        }
        val path = "guild.php?place=$normalized"
        RequestLogger.registerRequest(path, sessionLogger, preferences)
        return try {
            val response = client.get("$KOL_BASE_URL/$path")
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Guild visit failed."))
            }
            val html = response.bodyAsText()
            GuildVisitSync.syncStoreOpen(html, character, preferences)
            GuildVisitSync.parseFromVisit(
                url = path,
                html = html,
                eventBus = eventBus,
                sessionLogger = sessionLogger,
                character = character,
                preferences = preferences,
                skillManager = skillManager,
                inventoryManager = inventoryManager,
                questDatabase = questDatabase,
            )
            Result.success(html)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        val VALID_PLACES = setOf("challenge", "paco", "ocg", "scg", "trainer", "still")

        fun whichGuild(mainStat: net.sourceforge.kolmafia.character.MainStat): String =
            when (mainStat) {
                net.sourceforge.kolmafia.character.MainStat.MUSCLE -> "The Brotherhood of the Smackdown"
                net.sourceforge.kolmafia.character.MainStat.MYSTICALITY -> "The League of Chef-Magi"
                net.sourceforge.kolmafia.character.MainStat.MOXIE ->
                    "The Department of Shadowy Arts and Crafts"
            }

        fun getStoreName(mainStat: net.sourceforge.kolmafia.character.MainStat): String =
            when (mainStat) {
                net.sourceforge.kolmafia.character.MainStat.MUSCLE -> "The Smacketeria"
                net.sourceforge.kolmafia.character.MainStat.MYSTICALITY ->
                    "Gouda's Grimoire and Grocery"
                net.sourceforge.kolmafia.character.MainStat.MOXIE -> "The Shadowy Store"
            }

        fun getNPCName(
            place: String?,
            characterClass: net.sourceforge.kolmafia.character.CharacterClass,
        ): String? {
            if (place == null) return null
            if (!characterClass.isStandardClass) return null
            return when (place.lowercase()) {
                "challenge" -> when (characterClass.mainStat) {
                    net.sourceforge.kolmafia.character.MainStat.MUSCLE ->
                        "Gunther, Lord of the Smackdown"
                    net.sourceforge.kolmafia.character.MainStat.MYSTICALITY ->
                        "Gorgonzola, the Chief Chef"
                    net.sourceforge.kolmafia.character.MainStat.MOXIE ->
                        "Shifty, the Thief Chief"
                }
                "paco" -> when (characterClass.mainStat) {
                    net.sourceforge.kolmafia.character.MainStat.MUSCLE -> "Olaf the Janitor"
                    net.sourceforge.kolmafia.character.MainStat.MYSTICALITY -> "Blaine"
                    net.sourceforge.kolmafia.character.MainStat.MOXIE -> "Izzy the Lizard"
                }
                "trainer" -> when (characterClass.mainStat) {
                    net.sourceforge.kolmafia.character.MainStat.MUSCLE -> "Torg, the Trainer"
                    net.sourceforge.kolmafia.character.MainStat.MYSTICALITY -> "Brie, the Trainer"
                    net.sourceforge.kolmafia.character.MainStat.MOXIE -> "Lefty, the Trainer"
                }
                else -> null
            }
        }

        fun registerRequest(
            url: String,
            characterClass: net.sourceforge.kolmafia.character.CharacterClass? = null,
            logger: SessionLogger? = null,
        ): Boolean {
            if (!url.startsWith("guild.php", ignoreCase = true)) return false
            val place = Regex("""(?:^|[?&])place=([^&]+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1)
            val npc = getNPCName(place, characterClass ?: net.sourceforge.kolmafia.character.CharacterClass.UNKNOWN)
            val message = when {
                npc != null -> "Visiting $npc"
                url.contains("action=train", ignoreCase = true) -> "Training at the guild"
                else -> "Visiting the guild"
            }
            logger?.appendRawLine(message)
            return true
        }
    }
}
