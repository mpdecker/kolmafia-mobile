package net.sourceforge.kolmafia.faxbot

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition

/** Desktop [FaxBotDatabase.Monster] — one faxable monster row from bot XML. */
data class FaxBotMonster(
    val name: String,
    val actualName: String,
    val command: String,
    val category: String,
    val monsterId: Int,
) {
    val stringForm: String = "$name [$command]"

    companion object {
        private val monsterIdInBrackets = Regex("\\[(\\d+)\\]")
        private val monsterIdComment = Regex("<!-- monsterid: (\\d+) -->")

        fun fromXmlFields(
            displayName: String,
            actualName: String,
            command: String,
            category: String,
            gameDatabase: GameDatabase?,
        ): FaxBotMonster? {
            if (displayName.isBlank() || displayName.equals("none", ignoreCase = true)) return null
            if (actualName.isBlank()) return null
            if (command.isBlank()) return null

            val monster = deriveMonster(command, actualName, gameDatabase) ?: return null
            return FaxBotMonster(
                name = monster.name,
                actualName = monster.name,
                command = command,
                category = category,
                monsterId = monster.id,
            )
        }

        private fun deriveMonster(
            command: String,
            actualName: String,
            gameDatabase: GameDatabase?,
        ): MonsterDefinition? {
            monsterIdInBrackets.find(command)?.groupValues?.get(1)?.toIntOrNull()?.let { id ->
                return gameDatabase?.monster(id) ?: MonsterDatabase.getById(id)
            }
            monsterIdComment.find(command)?.groupValues?.get(1)?.toIntOrNull()?.let { id ->
                return gameDatabase?.monster(id) ?: MonsterDatabase.getById(id)
            }
            val decodedActual = decodeEntities(actualName)
            return gameDatabase?.monster(decodedActual)
                ?: MonsterDatabase.getByName(decodedActual)
        }

        private fun decodeEntities(text: String): String = text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }
}
