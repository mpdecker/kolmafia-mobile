package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.quest.CatBurglarChoiceSync

/**
 * Desktop [HeistManager] — Cat Burglar heist targets via main.php?heist=1 + choice 1320.
 */
class HeistManager(private val client: HttpClient) {

    data class HeistMonster(val id: Int, val pronoun: String, val name: String)
    data class HeistItem(val id: Int, val name: String)
    data class HeistData(
        val heists: Int,
        val heistables: Map<HeistMonster, List<HeistItem>>,
    )

    private var cachedHtml: String? = null

    suspend fun getHeistTargets(): Result<HeistData> = try {
        val html = fetchHeistPage()
        Result.success(parse(html))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun heist(count: Int, itemId: Int): Result<Unit> = try {
        val html = fetchHeistPage()
        val match = ITEM_PATTERN.findAll(html).firstOrNull { it.groupValues[2].toIntOrNull() == itemId }
            ?: return Result.failure(IllegalStateException("Could not find item $itemId to heist"))
        val monsterId = match.groupValues[1]
        val itemName = match.groupValues[3]
        repeat(count.coerceAtLeast(1)) {
            client.submitForm(
                url = "$KOL_BASE_URL/choice.php",
                formParameters = parameters {
                    append("whichchoice", CatBurglarChoiceSync.CHOICE_ID.toString())
                    append("option", "1")
                    append("st:$monsterId:$itemId", itemName)
                },
            )
        }
        cachedHtml = null
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun fetchHeistPage(): String {
        cachedHtml?.let { return it }
        val html = client.get("$KOL_BASE_URL/main.php?heist=1").bodyAsText()
        cachedHtml = html
        return html
    }

    companion object {
        private val HEIST_COUNT = Regex("""(\d+) more heists available""")
        private val MONSTER = Regex(
            """From (?<pronoun>[^ ]*) (?<monster>.*?):<br />(?<items>(?:<input [^/]+ />)+)""",
        )
        private val ITEM_PATTERN = Regex(
            """<input type="submit" name="st:(?<monsterId>\d+):(?<itemId>\d+)" value="(?<itemName>[^"]+)" class="button" />""",
        )

        fun parse(html: String): HeistData {
            val heists = HEIST_COUNT.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val heistables = linkedMapOf<HeistMonster, List<HeistItem>>()
            for (monsterMatch in MONSTER.findAll(html)) {
                val pronoun = monsterMatch.groupValues[1]
                val monsterName = monsterMatch.groupValues[2]
                val itemsHtml = monsterMatch.groupValues[3]
                var monsterId = -1
                val items = mutableListOf<HeistItem>()
                for (itemMatch in ITEM_PATTERN.findAll(itemsHtml)) {
                    monsterId = itemMatch.groupValues[1].toIntOrNull() ?: -1
                    items += HeistItem(
                        id = itemMatch.groupValues[2].toIntOrNull() ?: 0,
                        name = itemMatch.groupValues[3],
                    )
                }
                heistables[HeistMonster(monsterId, pronoun, monsterName)] = items
            }
            return HeistData(heists, heistables)
        }
    }
}
