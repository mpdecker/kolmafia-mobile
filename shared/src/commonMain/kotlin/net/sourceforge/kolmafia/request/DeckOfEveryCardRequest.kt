package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop DeckOfEveryCardRequest — buff/stat/random play path (Maximizer Phase 393). */
class DeckOfEveryCardRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
) {
    data class EveryCard(val id: Int, val name: String)

    suspend fun play(
        card: EveryCard?,
        preferences: Preferences?,
        inventoryCounts: (Int) -> Int,
        inLegacyOfLoathing: Boolean,
    ): Result<String> {
        val deckId = selectDeck(inventoryCounts, inLegacyOfLoathing)
            ?: return Result.failure(
                IllegalStateException("You don't have a Deck of Every Card available"),
            )
        val drawsUsed = preferences?.getInt(DRAWS_PREF, 0) ?: 0
        val drawsNeeded = if (card == null) 1 else 5
        if (drawsUsed + drawsNeeded > 15) {
            return Result.failure(
                IllegalStateException(
                    "You don't have enough draws left from the deck to do that today",
                ),
            )
        }
        val useHtml = useDeck(deckId, cheat = card != null).getOrElse { return Result.failure(it) }
        val useError = parseUseErrors(useHtml, preferences)
        if (useError != null) return Result.failure(IllegalStateException(useError))

        return if (card == null) {
            choiceRequest.choose(RANDOM_CHOICE, 1).map { (html, _) ->
                preferences?.setInt(DRAWS_PREF, drawsUsed + 1)
                parseCardEncounter(html, preferences)
                html
            }
        } else {
            parseAvailableCards(useHtml, preferences)
            if (!useHtml.contains(card.name, ignoreCase = false) &&
                !useHtml.contains(card.name, ignoreCase = true)
            ) {
                // Attempt cancel if card missing from cheat dropdown
                choiceRequest.choose(CHEAT_CHOICE, 2)
                return Result.failure(IllegalStateException("That card is not currently available."))
            }
            choiceRequest.choose(
                CHEAT_CHOICE,
                1,
                mapOf("which" to card.id.toString()),
            ).getOrElse { return Result.failure(it) }.also { (html, _) ->
                if (html.contains("<span class='guts'>Huh?</span>")) {
                    return Result.failure(
                        IllegalStateException("You already drew that card today."),
                    )
                }
            }
            choiceRequest.choose(RANDOM_CHOICE, 1).map { (html, _) ->
                preferences?.setInt(DRAWS_PREF, drawsUsed + 5)
                parseCardEncounter(html, preferences)
                html
            }
        }
    }

    private suspend fun useDeck(deckId: Int, cheat: Boolean): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/inv_use.php") {
            parameter("whichitem", deckId)
            if (cheat) {
                parameter("cheat", 1)
            } else {
                parameter("which", 3)
            }
            parameter("ajax", 1)
        }
        if (response.status.isSuccess()) {
            Result.success(response.bodyAsText())
        } else {
            Result.failure(Exception("HTTP ${response.status.value}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val DECK_ID = 8382
        const val REPLICA_DECK_ID = 11230
        const val DRAWS_PREF = "_deckCardsDrawn"
        const val SEEN_PREF = "_deckCardsSeen"
        const val RANDOM_CHOICE = 1085
        const val CHEAT_CHOICE = 1086

        private val CHEAT_SELECT_PATTERN =
            Regex("""<select name="which".*?</select>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val AVAILABLE_CARD_PATTERN =
            Regex("""<option [^>]*>(.*?)</option>""", RegexOption.IGNORE_CASE)
        private val DRAW_CARD_PATTERN =
            Regex(
                """<div id="blurb">.*?You draw a card: <b>(.*?)</b><p>(.*?)</div>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            )

        /** Desktop [DeckOfEveryCardRequest.allCardNames] — full cheat deck roster. */
        val ALL_CARD_NAMES: Set<String> = setOf(
            "X of Clubs", "X of Diamonds", "X of Hearts", "X of Spades",
            "X of Papayas", "X of Kumquats", "X of Salads", "X of Cups",
            "X of Coins", "X of Swords", "X of Wands",
            "XVI - The Tower", "Professor Plum", "Spare Tire", "Extra Tank",
            "Sheep", "Year of Plenty", "Mine", "Laboratory",
            "Plains", "Swamp", "Mountain", "Forest", "Island",
            "Lead Pipe", "Rope", "Wrench", "Candlestick", "Knife", "Revolver",
            "Gift Card", "1952 Mickey Mantle",
            "XXI - The World", "III - The Empress", "VI - The Lovers",
            "Healing Salve", "Dark Ritual", "Lightning Bolt", "Giant Growth", "Ancestral Recall",
            "XI - Strength", "I - The Magician", "0 - The Fool",
            "X - The Wheel of Fortune", "The Race Card",
            "Green Card", "IV - The Emperor", "IX - The Hermit",
            "Werewolf", "The Hive", "XVII - The Star", "VII - The Chariot",
            "XV - The Devil", "V - The Hierophant", "Fire Elemental", "Christmas Card",
            "Go Fish", "Goblin Sapper", "II - The High Priestess", "XIV - Temperance",
            "XVIII - The Moon", "Hunky Fireman Card", "Aquarius Horoscope",
            "XII - The Hanged Man", "Suit Warehouse Discount Card", "Pirate Birthday Card",
            "Plantable Greeting Card", "Slimer Trading Card", "XIII - Death", "Unstable Portal",
        )

        /**
         * Desktop [DeckOfEveryCardRequest.parseAvailableCards] —
         * cards missing from the cheat dropdown are already drawn today.
         */
        fun parseAvailableCards(html: String, preferences: Preferences?): Boolean {
            if (preferences == null) return false
            val select = CHEAT_SELECT_PATTERN.find(html)?.value ?: return false
            val available = AVAILABLE_CARD_PATTERN.findAll(select)
                .map { it.groupValues[1].trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            val drawn = ALL_CARD_NAMES.filter { it !in available }.sorted()
            preferences.setString(SEEN_PREF, drawn.joinToString("|"))
            return true
        }

        /**
         * Desktop [DeckOfEveryCardRequest.parseCardEncounter] —
         * append munged drawn card name to `_deckCardsSeen`.
         */
        fun parseCardEncounter(html: String, preferences: Preferences?): String? {
            if (preferences == null) return null
            val cardName = DRAW_CARD_PATTERN.find(html)?.groupValues?.getOrNull(1) ?: return null
            val of = cardName.indexOf(" of ")
            val munged = if (of == -1) cardName else "X" + cardName.substring(of)
            val prior = preferences.getString(SEEN_PREF, "")
            preferences.setString(
                SEEN_PREF,
                if (prior.isEmpty()) munged else "$prior|$munged",
            )
            return cardName
        }

        val STRENGTH = EveryCard(51, "XI - Strength")
        val MAGICIAN = EveryCard(50, "I - The Magician")
        val FOOL = EveryCard(49, "0 - The Fool")
        val WHEEL = EveryCard(67, "X - The Wheel of Fortune")
        val RACE = EveryCard(48, "The Race Card")

        val WORLD = EveryCard(68, "XXI - The World")
        val EMPRESS = EveryCard(70, "III - The Empress")
        val LOVERS = EveryCard(69, "VI - The Lovers")

        private val BUFF_ALIASES: Map<String, EveryCard> = buildMap {
            fun putAll(vararg keys: String, card: EveryCard) {
                keys.forEach { put(canonicalize(it), card) }
            }
            putAll("muscle", "strongly motivated", card = STRENGTH)
            putAll("mysticality", "myst", "magicianship", card = MAGICIAN)
            putAll("moxie", "mox", "dancin' fool", "dancin fool", card = FOOL)
            putAll(
                "items",
                "item",
                "item drop",
                "fortune of the wheel",
                card = WHEEL,
            )
            putAll("initiative", "racing", "racing!", card = RACE)
        }

        private val NAMED_CARDS: List<EveryCard> = listOf(
            STRENGTH, MAGICIAN, FOOL, WHEEL, RACE, WORLD, EMPRESS, LOVERS,
        )

        fun selectDeck(
            inventoryCounts: (Int) -> Int,
            inLegacyOfLoathing: Boolean,
        ): Int? = when {
            inventoryCounts(DECK_ID) > 0 -> DECK_ID
            inLegacyOfLoathing && inventoryCounts(REPLICA_DECK_ID) > 0 -> REPLICA_DECK_ID
            else -> null
        }

        fun resolvePlay(
            parameters: String,
            mainStat: MainStat = MainStat.MUSCLE,
        ): Result<EveryCard?> {
            val trimmed = parameters.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("Play what?"))
            }
            val lower = trimmed.lowercase()
            return when {
                lower.startsWith("random") -> Result.success(null)
                lower.startsWith("phylum") -> Result.failure(
                    IllegalArgumentException("Phylum combat plays are not supported yet."),
                )
                lower.startsWith("stat") -> resolveStat(trimmed.substring(4).trim(), mainStat)
                lower.startsWith("buff") -> resolveBuff(trimmed.substring(4).trim())
                else -> resolveNamedCard(trimmed)
            }
        }

        fun resolveBuff(parameter: String): Result<EveryCard?> {
            if (parameter.isBlank()) {
                return Result.failure(IllegalArgumentException("Which buff do you want?"))
            }
            val card = matchAlias(parameter, BUFF_ALIASES)
                ?: return Result.failure(IllegalArgumentException("Which buff is $parameter?"))
            return Result.success(card)
        }

        fun resolveStat(parameter: String, mainStat: MainStat): Result<EveryCard?> {
            if (parameter.isBlank()) {
                return Result.failure(IllegalArgumentException("Which stat do you want?"))
            }
            val lower = parameter.lowercase()
            val stat = when {
                lower.startsWith("main") -> mainStat
                lower.startsWith("mus") -> MainStat.MUSCLE
                lower.startsWith("mys") -> MainStat.MYSTICALITY
                lower.startsWith("mox") -> MainStat.MOXIE
                else -> return Result.failure(
                    IllegalArgumentException("Which stat is $parameter?"),
                )
            }
            return Result.success(statToCard(stat))
        }

        fun statToCard(stat: MainStat): EveryCard = when (stat) {
            MainStat.MUSCLE -> WORLD
            MainStat.MYSTICALITY -> EMPRESS
            MainStat.MOXIE -> LOVERS
        }

        fun resolveNamedCard(parameter: String): Result<EveryCard?> {
            val matches = NAMED_CARDS.filter {
                canonicalize(it.name).contains(canonicalize(parameter)) ||
                    canonicalize(parameter).contains(canonicalize(it.name))
            }
            return when {
                matches.isEmpty() -> Result.failure(
                    IllegalArgumentException("I don't know how to play $parameter"),
                )
                matches.size > 1 -> Result.failure(
                    IllegalArgumentException("'$parameter' is an ambiguous card name"),
                )
                else -> Result.success(matches.first())
            }
        }

        private fun matchAlias(parameter: String, aliases: Map<String, EveryCard>): EveryCard? {
            val key = canonicalize(parameter)
            aliases[key]?.let { return it }
            val starts = aliases.entries.filter { (alias, _) ->
                alias.startsWith(key) || key.startsWith(alias)
            }
            return starts.singleOrNull()?.value
        }

        private fun canonicalize(s: String): String =
            s.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

        fun parseUseErrors(html: String, preferences: Preferences?): String? = when {
            html.contains("You're too beaten up", ignoreCase = true) ->
                "You are too beaten up to draw a card"
            html.contains("You've already drawn your day's allotment of cards", ignoreCase = true) -> {
                preferences?.setInt(DRAWS_PREF, 15)
                "You've already used all your draws for the day"
            }
            html.contains("You don't have enough energy left to cheat today", ignoreCase = true) ->
                "You don't have enough draws left to cheat today"
            html.contains("You don't have time to draw a card right now", ignoreCase = true) ->
                "You don't have any adventures left"
            else -> null
        }
    }
}
