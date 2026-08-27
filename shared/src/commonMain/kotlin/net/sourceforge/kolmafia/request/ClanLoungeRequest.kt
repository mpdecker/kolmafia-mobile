package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ParametersBuilder
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.clan.ClanLoungeVipSync
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ConsumptionEligibility
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.HotDogDatabase
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanLoungeVipOptions.CANNONBALL

open class ClanLoungeRequest(private val client: HttpClient) {

    /** Search a VIP lounge floor and update the per-clan furniture cache. */
    open suspend fun searchLounge(
        floor: Int = 1,
        preferences: Preferences? = null,
    ): Result<String> = try {
        require(floor >= 1) { "Invalid lounge floor: $floor" }
        val response = client.get("$KOL_BASE_URL/clan_viplounge.php") {
            if (floor != 1) parameter("whichfloor", floor)
        }
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            response.bodyAsText().let { html ->
                parseResponse(
                    if (floor == 1) "clan_viplounge.php" else "clan_viplounge.php?whichfloor=$floor",
                    html,
                    preferences,
                )
                Result.success(html)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Use the Deluxe Klaw machine once. Returns response HTML (caller checks _deluxeKlawSummons). */
    open suspend fun useKlaw(): Result<String> = postAction("klaw")
    open suspend fun useKlaw(preferences: Preferences): Result<String> =
        postAction("klaw", preferences)

    /** Visit the looking glass for a free buff. */
    open suspend fun useLookingGlass(): Result<Unit> = postAction("lookingglass").map {}
    open suspend fun useLookingGlass(preferences: Preferences): Result<Unit> =
        postAction("lookingglass", preferences).map {}

    /** Visit the fireworks shop. */
    open suspend fun visitFireworks(): Result<Unit> = postAction("fireworks").map {}
    open suspend fun visitFireworks(preferences: Preferences): Result<Unit> =
        postAction("fireworks", preferences).map {}

    /** Desktop ClanLoungeRequest(Action.CRIMBO_TREE) — visit the VIP Crimbo tree. */
    open suspend fun visitCrimboTree(): Result<String> = postAction("crimbotree")
    open suspend fun visitCrimboTree(preferences: Preferences): Result<String> =
        postAction("crimbotree", preferences)

    /**
     * Desktop ClanLoungeRequest(Action.POOL_TABLE, stance).
     * [stance] 1–3 plays a game; 0 visits the table (breakfast / watch).
     */
    open suspend fun playPoolGame(
        stance: Int = 0,
        preferences: Preferences? = null,
    ): Result<String> {
        if (stance !in 0..3) {
            return Result.failure(IllegalArgumentException("Invalid pool stance: $stance"))
        }
        if (stance != 0 && preferences != null &&
            preferences.getInt(ClanLoungeVipSync.POOL_GAMES_PREF, 0) >= 3
        ) {
            return Result.failure(IllegalStateException("You're kind of pooled out for today."))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/clan_viplounge.php",
                formParameters = parameters {
                    if (stance != 0) {
                        append("preaction", "poolgame")
                        append("stance", stance.toString())
                    } else {
                        append("action", "pooltable")
                    }
                    append("whichfloor", "2")
                },
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                val html = response.bodyAsText()
                ClanLoungeVipSync.syncPoolGameFromResponse(html, preferences)
                Result.success(html)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Visit the clan VIP lounge fax machine. */
    open suspend fun visitFaxMachine(): Result<String> = postFaxForm(preaction = null)

    /** Soak in the clan VIP lounge hot tub. Desktop ClanLoungeRequest(Action.HOTTUB). */
    open suspend fun useHotTub(preferences: Preferences? = null): Result<String> = postLoungeForm(
        formParams = { append("action", "hottub") },
        syncUrlSuffix = "?action=hottub",
        preferences = preferences,
    )

    /** Send a photocopied monster via the clan fax machine. */
    open suspend fun sendFax(): Result<String> = postFaxForm(preaction = "sendfax")

    /** Receive a fax from the clan fax machine. */
    open suspend fun receiveFax(): Result<String> = postFaxForm(preaction = "receivefax")

    /** Desktop ClanLoungeRequest.visitLounge(HOT_DOG_STAND) — refresh stand availability. */
    open suspend fun visitHotDogStand(
        preferences: Preferences? = null,
        state: CharacterState? = null,
    ): Result<String> {
        if (state != null && !ClanLoungeSync.isHotDogStandAllowed(state)) {
            return Result.failure(IllegalStateException("Clan hot dog stand not allowed"))
        }
        return postLoungeForm(
            formParams = { append("action", "hotdogstand") },
            syncUrlSuffix = "?action=hotdogstand",
            preferences = preferences,
        )
    }

    /** Desktop ClanLoungeRequest.visitLounge(FLOUNDRY) — refresh fish stock availability. */
    open suspend fun visitFloundry(
        preferences: Preferences? = null,
    ): Result<String> = postLoungeForm(
        formParams = { append("action", "floundry") },
        syncUrlSuffix = "?action=floundry",
        preferences = preferences,
    )

    /** Desktop ClanLoungeRequest.visitLounge(SPEAKEASY) — refresh drink availability. */
    open suspend fun visitSpeakeasy(
        preferences: Preferences? = null,
        state: CharacterState? = null,
    ): Result<String> {
        if (state != null && !ClanLoungeSync.isSpeakeasyAllowed(state)) {
            return Result.failure(IllegalStateException("Clan speakeasy not allowed"))
        }
        return postLoungeForm(
            formParams = {
                append("action", "speakeasy")
                append("whichfloor", "2")
            },
            syncUrlSuffix = "?action=speakeasy",
            preferences = preferences,
        )
    }

    /** Desktop ClanLoungeRequest buyHotDogRequest — eat hot dog by cafe id. */
    open suspend fun eatHotDog(
        cafeId: Int,
        preferences: Preferences? = null,
        state: CharacterState? = null,
    ): Result<String> {
        val name = HotDogDatabase.cafeIdToName(cafeId)
            ?: return Result.failure(IllegalArgumentException("Unknown hot dog cafe id: $cafeId"))
        preflightHotDog(name, state, preferences).onFailure { return Result.failure(it) }
        return postLoungeForm(
            formParams = {
                append("preaction", "eathotdog")
                append("whichdog", cafeId.toString())
            },
            syncUrlSuffix = "?preaction=eathotdog&whichdog=$cafeId",
            preferences = preferences,
        )
    }

    /** Eat hot dog by name (delegates to [HotDogDatabase] cafe id). */
    open suspend fun eatHotDog(
        name: String,
        preferences: Preferences? = null,
        state: CharacterState? = null,
    ): Result<String> {
        val index = HotDogDatabase.nameToIndex(name)
        if (index < 0) {
            return Result.failure(IllegalArgumentException("Unknown hot dog: $name"))
        }
        return eatHotDog(HotDogDatabase.indexToCafeId(index), preferences, state)
    }

    /** Desktop ClanLoungeRequest buySpeakeasyDrinkRequest — drink by lounge row id. */
    open suspend fun drinkSpeakeasy(
        loungeId: Int,
        preferences: Preferences? = null,
        state: CharacterState? = null,
    ): Result<String> {
        val index = SpeakeasyDatabase.loungeIdToIndex(loungeId)
        if (index < 0) {
            return Result.failure(IllegalArgumentException("Unknown speakeasy drink id: $loungeId"))
        }
        val name = SpeakeasyDatabase.indexToName(index)
            ?: return Result.failure(IllegalArgumentException("Unknown speakeasy drink id: $loungeId"))
        preflightSpeakeasy(name, state, preferences).onFailure { return Result.failure(it) }
        return postLoungeForm(
            formParams = {
                append("preaction", "speakeasydrink")
                append("drink", loungeId.toString())
                append("whichfloor", "2")
            },
            syncUrlSuffix = "?preaction=speakeasydrink&drink=$loungeId",
            preferences = preferences,
        )
    }

    /** Drink speakeasy cocktail by name. */
    open suspend fun drinkSpeakeasy(
        name: String,
        preferences: Preferences? = null,
        state: CharacterState? = null,
    ): Result<String> {
        val index = SpeakeasyDatabase.nameToIndex(name)
        if (index < 0) {
            return Result.failure(IllegalArgumentException("Unknown speakeasy drink: $name"))
        }
        return drinkSpeakeasy(SpeakeasyDatabase.entries[index].loungeId, preferences, state)
    }

    /** Desktop ClanLoungeRequest(Action.APRIL_SHOWER, option). */
    open suspend fun takeShower(
        option: Int,
        preferences: Preferences? = null,
    ): Result<String> {
        if (option !in 1..5) {
            return Result.failure(IllegalArgumentException("Invalid shower option: $option"))
        }
        return postLoungeForm(
            formParams = {
                append("preaction", "takeshower")
                append("temperature", option.toString())
                append("whichfloor", "2")
            },
            syncUrlSuffix = "?preaction=takeshower&temperature=$option",
            preferences = preferences,
        )
    }

    /**
     * Desktop ClanLoungeRequest(Action.SWIMMING_POOL, option).
     * Cannonball also runs choice 585 flip → treasure → leave.
     */
    open suspend fun swimPool(
        option: Int,
        preferences: Preferences? = null,
        choiceRequest: ChoiceRequest? = null,
    ): Result<String> {
        val subaction = ClanLoungeVipOptions.swimmingSubaction(option)
            ?: return Result.failure(IllegalArgumentException("Invalid swimming option: $option"))
        val result = postLoungeForm(
            formParams = {
                append("preaction", "goswimming")
                append("subaction", subaction)
                append("whichfloor", "2")
            },
            syncUrlSuffix = "?preaction=goswimming&subaction=$subaction",
            preferences = preferences,
        )
        if (result.isFailure) return result
        if (option != CANNONBALL) return result

        val choice = choiceRequest
            ?: return Result.failure(IllegalStateException("Choice request is not available."))
        // Desktop ClanLoungeSwimmingPoolRequest: flip → treasure → leave
        choice.choose(585, 1, mapOf("action" to "flip")).onFailure { return Result.failure(it) }
        val treasure = choice.choose(585, 1, mapOf("action" to "treasure"))
        treasure.onSuccess { (html, url) ->
            ClanLoungeVipSync.syncSwimTreasureFromResponse(html, url, preferences)
        }.onFailure { return Result.failure(it) }
        choice.choose(585, 1, mapOf("action" to "leave")).onFailure { return Result.failure(it) }
        preferences?.setBoolean(ClanLoungeVipSync.OLYMPIC_SWIMMING_POOL_PREF, true)
        return result
    }

    /** Desktop ClanLoungeRequest(Action.FORTUNE) — open love tester for fortune buff. */
    open suspend fun visitFortuneTeller(
        preferences: Preferences? = null,
    ): Result<String> = postLoungeForm(
        formParams = {
            append("preaction", "lovetester")
            append("whichfloor", "2")
        },
        syncUrlSuffix = "?preaction=lovetester",
        preferences = preferences,
    )

    fun findFaxOption(tag: String): Int {
        val normalized = tag.trim().lowercase()
        return when (normalized) {
            "send", "put" -> SEND_FAX
            "receive", "get" -> RECEIVE_FAX
            else -> 0
        }
    }

    private suspend fun postLoungeForm(
        formParams: ParametersBuilder.() -> Unit,
        syncUrlSuffix: String,
        preferences: Preferences? = null,
    ): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_viplounge.php",
            formParameters = parameters(formParams),
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            if (preferences != null) {
                ClanLoungeSync.apply(
                    preferences,
                    html,
                    "$KOL_BASE_URL/clan_viplounge.php$syncUrlSuffix",
                )
            }
            parseResponse(
                "clan_viplounge.php$syncUrlSuffix",
                html,
                preferences,
            )
            Result.success(html)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun postFaxForm(preaction: String?): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_viplounge.php",
            formParameters = parameters {
                if (preaction != null) {
                    append("preaction", preaction)
                } else {
                    append("action", "faxmachine")
                }
                append("whichfloor", "2")
            }
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            Result.success(response.bodyAsText())
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun postAction(
        action: String,
        preferences: Preferences? = null,
    ): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_viplounge.php",
            formParameters = parameters { append("action", action) }
        )
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else {
            val html = response.bodyAsText()
            parseResponse("clan_viplounge.php?action=$action", html, preferences)
            Result.success(html)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val SEND_FAX = 1
        const val RECEIVE_FAX = 2

        private val LOUNGE_PATTERN = Regex(
            """<table.*?<b style="color: white">Clan VIP Lounge \(Ground Floor\)</b>.*?<center><b>(?:<a.*?>)?(.*?)(?:</a>)?</b>.*?</center>(<table.*?</table>)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val LOUNGE2_PATTERN = Regex(
            """<table.*?<b style="color: white">Clan VIP Lounge \(Attic\)</b>.*?<center><b>(?:<a.*?>)?(.*?)(?:</a>)?</b>.*?</center>(<table.*?</table>)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val TREE_LEVEL_PATTERN = Regex("""tree(\d+)(?:nopressie|)\.gif""")
        private val HOTTUB_PATTERN = Regex("""hottub(\d)\.gif""")
        private val FURNITURE_IMAGES = linkedMapOf(
            "vipfloundry.gif" to "Clan floundry",
            "fortuneteller.gif" to "Clan Carnival Game",
            "fireworks.gif" to "Clan Underground Fireworks Shop",
            "photobooth.gif" to "Clan Photo Booth",
            "lookingglass.gif" to "Looking Glass",
            "aprilshower.gif" to "April Shower",
            "pooltable.gif" to "Pool Table",
            "faxmachine.gif" to "Fax Machine",
            "hotdogstand.gif" to "Hot Dog Stand",
            "vippool.gif" to "Olympic-sized Clan pool",
            "speakeasy.gif" to "Speakeasy",
        )

        fun parseResponse(url: String, html: String, preferences: Preferences? = null) {
            if (!url.contains("clan_viplounge.php", ignoreCase = true)) return
            val sections = listOfNotNull(LOUNGE_PATTERN.find(html), LOUNGE2_PATTERN.find(html))
            for (section in sections) {
                val clanName = section.groupValues[1].replace(Regex("<.*?>"), "").trim()
                if (clanName.isNotEmpty() && clanName != ClanManager.getClanName()) {
                    ClanManager.setClanName(clanName)
                    ClanManager.setClanId(0)
                }
                parseFurniture(section.groupValues[2], preferences)
            }
            // Action responses frequently omit the page wrapper.
            if (sections.isEmpty()) parseFurniture(html, preferences)
        }

        private fun parseFurniture(html: String, preferences: Preferences?) {
            for ((image, name) in FURNITURE_IMAGES) {
                if (html.contains(image, ignoreCase = true)) ClanManager.addToLounge(name)
            }
            TREE_LEVEL_PATTERN.find(html)?.groupValues?.get(1)?.toIntOrNull()?.let { level ->
                ClanManager.addToLounge("Crimbough", level)
            }
            when {
                html.contains("tree5.gif", ignoreCase = true) -> {
                    preferences?.setInt("crimboTreeDays", 0)
                    preferences?.setBoolean("_crimboTree", true)
                }
                !html.contains("crimbotree", ignoreCase = true) &&
                    !TREE_LEVEL_PATTERN.containsMatchIn(html) ->
                    preferences?.setBoolean("_crimboTree", false)
            }
            HOTTUB_PATTERN.find(html)?.groupValues?.get(1)?.toIntOrNull()?.let {
                preferences?.setInt(ClanLoungeSync.HOT_TUB_SOAKS_PREF, 5 - it)
            }
        }

        fun findShowerOption(tag: String): Int = ClanLoungeVipOptions.findShowerOption(tag)

        fun findSwimmingOption(tag: String): Int = ClanLoungeVipOptions.findSwimmingOption(tag)

        fun findPoolGame(tag: String): Int = ClanLoungeVipOptions.findPoolGame(tag)

        internal fun preflightHotDog(
            name: String,
            state: CharacterState?,
            prefs: Preferences?,
        ): Result<Unit> {
            val concoction = ConcoctionDatabase.getByResult(name)
                ?: return Result.failure(IllegalStateException("No concoction for hot dog: $name"))
            if (state != null) {
                if (!ClanLoungeSync.isHotDogStandAllowed(state)) {
                    return Result.failure(IllegalStateException("Clan hot dog stand not allowed"))
                }
                if (!ConcoctionPermitted.isPermittedMethod(
                        concoction,
                        state,
                        prefs = prefs,
                        limitMode = state.limitMode,
                    )
                ) {
                    return Result.failure(IllegalStateException("Hot dog not permitted: $name"))
                }
                val fullness = HotDogDatabase.nameToFullness(name)
                if (fullness > 0 &&
                    ConsumptionEligibility.effectiveFullnessRemaining(state) < fullness
                ) {
                    return Result.failure(IllegalStateException("Not enough fullness for: $name"))
                }
            }
            if (ConcoctionDatabase.totalCount(name) <= 0) {
                return Result.failure(IllegalStateException("Hot dog not available: $name"))
            }
            return Result.success(Unit)
        }

        internal fun preflightSpeakeasy(
            name: String,
            state: CharacterState?,
            prefs: Preferences?,
        ): Result<Unit> {
            val concoction = ConcoctionDatabase.getByResult(name)
                ?: return Result.failure(IllegalStateException("No concoction for drink: $name"))
            if (state != null) {
                if (!ClanLoungeSync.isSpeakeasyAllowed(state)) {
                    return Result.failure(IllegalStateException("Clan speakeasy not allowed"))
                }
                if (!ConcoctionPermitted.isPermittedMethod(
                        concoction,
                        state,
                        prefs = prefs,
                        limitMode = state.limitMode,
                    )
                ) {
                    return Result.failure(IllegalStateException("Speakeasy drink not permitted: $name"))
                }
                val inebriety = SpeakeasyDatabase.nameToInebriety(name)
                if (inebriety > 0 &&
                    ConsumptionEligibility.effectiveInebrietyRemaining(state) < inebriety
                ) {
                    return Result.failure(IllegalStateException("Not enough liver for: $name"))
                }
            }
            if (ConcoctionDatabase.totalCount(name) <= 0) {
                return Result.failure(IllegalStateException("Speakeasy drink not available: $name"))
            }
            return Result.success(Unit)
        }
    }
}
