package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ClanFortuneRequest — VIP love-tester buff path (choice 1278). */
class ClanFortuneRequest(
    private val loungeRequest: ClanLoungeRequest,
    private val choiceRequest: ChoiceRequest,
) {
    enum class Buff(val which: String) {
        FAMILIAR("-1"),
        ITEM("-2"),
        MEAT("-3"),
        MUSCLE("-4"),
        MYSTICALITY("-5"),
        MOXIE("-6"),
    }

    suspend fun takeBuff(
        buff: Buff,
        preferences: Preferences,
        word1: String? = null,
        word2: String? = null,
        word3: String? = null,
    ): Result<String> {
        if (preferences.getBoolean(BUFF_USED_PREF, false)) {
            return Result.failure(
                IllegalStateException("You already received a buff from the clan fortune teller."),
            )
        }
        val q1 = word1 ?: preferences.getString(WORD1_PREF, "")
        val q2 = word2 ?: preferences.getString(WORD2_PREF, "")
        val q3 = word3 ?: preferences.getString(WORD3_PREF, "")

        loungeRequest.visitFortuneTeller(preferences).onFailure { return Result.failure(it) }
        val result = choiceRequest.choose(
            CHOICE_ID,
            1,
            mapOf(
                "which" to buff.which,
                "q1" to q1,
                "q2" to q2,
                "q3" to q3,
            ),
        )
        return result.map { (html, url) ->
            parseResponse(url, html, preferences)
            html
        }
    }

    companion object {
        const val CHOICE_ID = 1278
        const val BUFF_USED_PREF = "_clanFortuneBuffUsed"
        const val WORD1_PREF = "clanFortuneWord1"
        const val WORD2_PREF = "clanFortuneWord2"
        const val WORD3_PREF = "clanFortuneWord3"

        fun findBuff(name: String): Buff? {
            val n = name.trim().lowercase()
            if (n.isEmpty()) return null
            return when {
                n.startsWith("susie") || n.startsWith("fam") -> Buff.FAMILIAR
                n.startsWith("hagnk") || n.startsWith("item") -> Buff.ITEM
                n.startsWith("meat") -> Buff.MEAT
                n.startsWith("gunther") || n.startsWith("mus") -> Buff.MUSCLE
                n.startsWith("gorgonzola") || n.startsWith("mys") -> Buff.MYSTICALITY
                n.startsWith("shifty") || n.startsWith("mox") -> Buff.MOXIE
                else -> null
            }
        }

        fun parseResponse(url: String, html: String, preferences: Preferences?) {
            if (preferences == null) return
            if (!url.contains("choice.php", ignoreCase = true) &&
                !url.contains("preaction=lovetester", ignoreCase = true)
            ) {
                return
            }
            if (!html.contains("Relationship Fortune Teller")) return
            preferences.setBoolean(BUFF_USED_PREF, !html.contains("resident of Seaside Town"))
        }
    }
}
