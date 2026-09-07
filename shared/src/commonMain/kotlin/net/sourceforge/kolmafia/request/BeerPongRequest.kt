package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.PirateInsults
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.BeerPongRequest]. */
object BeerPongRequest {
    private val ROUND1 = Regex(
        """The pirate lobs his ball \w+ your cups. &quot;(.*?)&quot; he taunts""",
    )
    private val ROUND2 = Regex("""&quot;However -- (.*?)&quot;""")
    private val ROUND3 = Regex("""and growls &quot;(.*?)&quot;""")
    private val OPTION = Regex("""<option value=(\d+)>""")

    val INSULTS = listOf(
        "Arrr, the power of me serve'll flay the skin from yer bones!",
        "Do ye hear that, ye craven blackguard?  It be the sound of yer doom!",
        "Suck on <i>this</i>, ye miserable, pestilent wretch!",
        "The streets will run red with yer blood when I'm through with ye!",
        "Yer face is as foul as that of a drowned goat!",
        "When I'm through with ye, ye'll be crying like a little girl!",
        "In all my years I've not seen a more loathsome worm than yerself!",
        "Not a single man has faced me and lived to tell the tale!",
    )

    fun findRicketsInsult(html: String): String? =
        ROUND1.find(html)?.groupValues?.getOrNull(1)
            ?: ROUND2.find(html)?.groupValues?.getOrNull(1)
            ?: ROUND3.find(html)?.groupValues?.getOrNull(1)

    fun findPirateInsult(insult: String): Int {
        val idx = INSULTS.indexOfFirst { it == insult }
        return if (idx >= 0) idx + 1 else 0
    }

    fun pirateRetort(insult: Int): String? = PirateInsults.RETORTS.getOrNull(insult - 1)

    fun parseResponse(
        url: String,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        if (!url.contains("beerpong.php", ignoreCase = true)) return
        if (html.contains("After a few victory laps atop the ocean of revelers")) {
            questDatabase?.setQuestIfBetter(Quest.PIRATE, "step5")
        }
        findRicketsInsult(html)?.let { sessionLogger?.appendRawLine("Insult: $it") }
        val form = Regex("""<form action=beerpong.php.*?</form>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.value ?: return
        for (match in OPTION.findAll(form)) {
            val option = match.groupValues[1].toIntOrNull() ?: continue
            if (option in 1..PirateInsults.VALID_COUNT) {
                preferences?.setBoolean(PirateInsults.prefKey(option), true)
            }
        }
    }

    fun registerRequest(url: String): Boolean = url.contains("beerpong.php", ignoreCase = true)
}
